#include "jqfakeclient.h"
#include "system.h"
#include "client.h"

#include <stdlib.h>
#include <android/log.h>

inline void LogWarning( const char *tag, const char *format, ... ) __attribute__( ( format( printf, 2, 3 ) ) );

inline void LogWarning( const char *tag, const char *format, ... ) {
	va_list va;

	va_start( va, format );
	__android_log_vprint( ANDROID_LOG_WARN, tag, format, va );
	va_end( va );
}

inline void LogError( const char *tag, const char *format, ... ) __attribute__( ( format( printf, 2, 3 ) ) );

inline void LogError( const char *tag, const char *format, ... ) {
	va_list va;

	va_start( va, format );
	__android_log_vprint( ANDROID_LOG_ERROR, tag, format, va );
	va_end( va );
}

inline void FailWith( const char *tag, const char *message ) __attribute__( ( noreturn ) );

inline void FailWith( const char *tag, const char *message ) {
	__android_log_assert( "A fatal error occurred, aborting...", tag, "%s", message );
}

static inline System *HandleToSystem( jlong handle ) {
	return (System *)( (uintptr_t)handle );
}

static inline jlong SystemToHandle( const System *system ) {
	return (jlong)( (uintptr_t)system );
}

static inline Client *HandleToClient( jlong handle ) {
	return (Client *)( (uintptr_t)handle );
}

static inline jlong ClientToHandle( const Client *client ) {
	return (jlong)( (uintptr_t)client );
}

static JavaVM *globalJvm = nullptr;

static inline JNIEnv *GetJNIEnv() {
	JNIEnv *env;
	if( globalJvm->GetEnv( (void **)&env, JNI_VERSION_1_6 ) >= 0 ) {
		return env;
	}

	return nullptr;
}

class StringBuffer
{
	char internalBuffer[128];
	size_t bufferSize;
	size_t bufferOffset;
	char *buffer;
	bool wentOutOfMemory;

public:
	StringBuffer( const StringBuffer &that ) = delete;
	StringBuffer &operator=( const StringBuffer &that ) = delete;
	StringBuffer( StringBuffer &&that ) = delete;
	StringBuffer &operator=( StringBuffer &&that ) = delete;

	StringBuffer() {
		bufferSize = sizeof( internalBuffer );
		bufferOffset = 0;
		buffer = internalBuffer;
		wentOutOfMemory = false;
		internalBuffer[0] = '\0';
	}

	~StringBuffer() {
		if( buffer != internalBuffer ) {
			free( buffer );
		}
	}

	const char *Buffer() const { return buffer; }
	size_t Length() const { return bufferOffset; }
	bool WentOutOfMemory() const { return wentOutOfMemory; }

	void Clear();

	void Printf( const char *format, ... ) __attribute__( ( format( printf, 2, 3 ) ) );
	void VPrintf( const char *format, va_list va );
};

void StringBuffer::Clear() {
	bufferOffset = 0;

	if( bufferSize > 8192 ) {
		assert( buffer != internalBuffer );
		free( buffer );
		buffer = internalBuffer;
		bufferSize = sizeof( internalBuffer );
	}
	buffer[0] = '\0';
	wentOutOfMemory = false;
}

void StringBuffer::Printf( const char *format, ... ) {
	va_list va;

	va_start( va, format );
	VPrintf( format, va );
	va_end( va );
}

static inline size_t NextBufferSize( size_t oldSize ) {
	if( oldSize < 1024 ) {
		return 1024;
	}

	if( oldSize < 4096 ) {
		return oldSize * 2;
	}
	return ( 3 * oldSize ) / 2;
}

void StringBuffer::VPrintf( const char *format, va_list va ) {
	for(;; ) {
		int result = vsnprintf( buffer, bufferSize - bufferOffset, format, va );

		if( result >= 0 || result < bufferSize - bufferOffset ) {
			bufferOffset += result;
			break;
		}
		size_t oldBufferSize = bufferSize;

		if( result < 0 ) {
			bufferSize = NextBufferSize( bufferSize );
		} else {
			bufferSize = std::max( (size_t)( result + 16u ), NextBufferSize( bufferSize ) );
		}
		char *newBuffer = (char *)malloc( bufferSize );

		// Should not happen unless memory overcommit is off.
		// Just try to restore the buffer.
		if( !newBuffer ) {
			bufferSize = oldBufferSize;

			if( result >= 0 ) {
				bufferOffset += result;
			}
			wentOutOfMemory = true;
			continue;
		}
		memcpy( newBuffer, buffer, bufferOffset );

		if( buffer != internalBuffer ) {
			free( buffer );
		}
		buffer = newBuffer;
	}
	buffer[bufferOffset] = 0;
}

struct ClassHolder {
	const char *name;
	ClassHolder *next;
	jclass globalClassRef;

	static ClassHolder *listHead;

	explicit ClassHolder( const char *name ) {
		this->name = name;
		this->next = nullptr;
		this->globalClassRef = nullptr;
		this->next = listHead;
		listHead = this;
	}

	bool Init( JNIEnv *env, StringBuffer &messageSink );
	const char *Name() const { return name; }
	jclass Get() { return globalClassRef; }

	/**
	 * Forces releasing of the globally held class reference
	 */
	void ForceReleaseRef( JNIEnv *env ) {
		if( globalClassRef ) {
			env->DeleteGlobalRef( globalClassRef );
			globalClassRef = nullptr;
		}
	}
};

ClassHolder *ClassHolder::listHead = nullptr;

bool ClassHolder::Init( JNIEnv *env, StringBuffer &messageSink ) {
	jclass clazz = env->FindClass( name );

	if( !clazz ) {
		messageSink.Printf( "Can't find class `%s`;", name );
		globalClassRef = nullptr;
		if( env->ExceptionCheck() ) {
			env->ExceptionClear();
		}
		return false;
	}

	globalClassRef = (jclass)env->NewGlobalRef( clazz );

	// Don't try anything in this case
	if( !globalClassRef ) {
		messageSink.Printf( "Can't make a new global ref to class `%s`;", name );
		return false;
	}
	return true;
}

struct MethodHolder {
	ClassHolder *clazz;
	const char *name;
	const char *signature;
	MethodHolder *next;
	jmethodID methodID;

	static MethodHolder *listHead;

	MethodHolder( ClassHolder *clazz_, const char *name_, const char *signature_ ) {
		this->clazz = clazz_;
		this->name = name_;
		this->signature = signature_;
		this->methodID = nullptr;
		this->next = listHead;
		listHead = this;
	}

	bool Init( JNIEnv *env, StringBuffer &messageSink );

	jmethodID Get() { return methodID; }
};

MethodHolder *MethodHolder::listHead = nullptr;

bool MethodHolder::Init( JNIEnv *env, StringBuffer &messageSink ) {
	if( !( this->methodID = env->GetMethodID( clazz->Get(), name, signature ) ) ) {
		messageSink.Printf( "Can't find method `%s` of signature `%s` in class `%s`;", name, signature, clazz->Name() );
		if( env->ExceptionCheck() ) {
			env->ExceptionClear();
		}
		return false;
	}
	return true;
}

struct FieldHolder {
	ClassHolder *clazz;
	const char *name;
	const char *signature;
	FieldHolder *next;
	jfieldID fieldID;

	static FieldHolder *listHead;

	FieldHolder( ClassHolder *clazz_, const char *name_, const char *signature_ ) {
		this->clazz = clazz_;
		this->name = name_;
		this->signature = signature_;

		this->next = listHead;
		listHead = this;
	}

	bool Init( JNIEnv *env, StringBuffer &messageSink );

	jfieldID Get() { return fieldID; }
};

FieldHolder *FieldHolder::listHead = nullptr;

bool FieldHolder::Init( JNIEnv *env, StringBuffer &messageSink ) {
	if( !( fieldID = env->GetFieldID( clazz->Get(), name, signature ) ) ) {
		messageSink.Printf( "Can't find field `%s` of signature `%s` in class %s;", name, signature, clazz->Name() );
		if( env->ExceptionCheck() ) {
			env->ExceptionClear();
		}
		return false;
	}
	return true;
}

static ClassHolder illegalArgumentException_Class( "java/lang/IllegalArgumentException" );
static ClassHolder classNotFoundException_Class( "java/lang/ClassNotFoundException" );
static ClassHolder noSuchMethodException_Class( "java/lang/NoSuchMethodException" );
static ClassHolder noSuchFieldException_Class( "java/lang/NoSuchFieldException" );
static ClassHolder outOfMemoryException_Class( "java/lang/OutOfMemoryError" );

static ClassHolder console_Class( "com/github/qfusion/fakeclient/NativeBridgeConsole" );

static FieldHolder console_ioBuffer_Field( &console_Class, "ioBuffer", "Ljava/nio/ByteBuffer;" );

static MethodHolder console_onNewBufferData_Method( &console_Class, "onNewBufferData", "(II)V" );

static ClassHolder clientListener_Class( "com/github/qfusion/fakeclient/NativeBridgeClientListener" );

static FieldHolder clientListener_ioBuffer_Field( &clientListener_Class, "ioBuffer", "Ljava/nio/ByteBuffer;" );

// Actually an offset and a length are passed for each string argument
static constexpr const char *SIG_STRING = "(II)V";
static constexpr const char *SIG_2STRINGS = "(IIII)V";

static MethodHolder clientListener_onShownPlayerNameSet_Method( &clientListener_Class, "onShownPlayerNameSet", SIG_STRING );
static MethodHolder clientListener_onMessageOfTheDaySet_Method( &clientListener_Class, "onMessageOfTheDaySet", SIG_STRING );
static MethodHolder clientListener_onCenteredMessage_Method( &clientListener_Class, "onCenteredMessage", SIG_STRING );
static MethodHolder clientListener_onChatMessage_Method( &clientListener_Class, "onChatMessage", SIG_2STRINGS );
static MethodHolder clientListener_onTeamChatMessage_Method( &clientListener_Class, "onTeamChatMessage", SIG_2STRINGS );
static MethodHolder clientListener_onTVChatMessage_Method( &clientListener_Class, "onTVChatMessage", SIG_2STRINGS );

inline void ThrowCheckingPending( JNIEnv *env, ClassHolder &clazz, const char *msg ) {
	if( env->ExceptionCheck() ) {
		env->ExceptionDescribe();
		env->ExceptionClear();
	}
	env->ThrowNew( clazz.Get(), msg );
}

class JavaConsole : public Console
{
	jobject globalConsoleRef;
	jbyte *javaBufferBytes;
	size_t javaBufferCapacity;

	StringBuffer stringBuffer;

	bool CallOnNewBufferData( JNIEnv *env, ssize_t numBytes );

public:
	JavaConsole( jobject globalConsoleRef_, jbyte *bufferBytes_, size_t bufferCapacity_ ) {
		this->globalConsoleRef = globalConsoleRef_;
		this->javaBufferBytes = bufferBytes_;
		this->javaBufferCapacity = bufferCapacity_;
	}

	~JavaConsole() override {
		if( auto env = GetJNIEnv() ) {
			env->DeleteGlobalRef( globalConsoleRef );
		}
	}

	void VPrintf( const char *format, va_list va ) override;
};

static bool CheckForException( JNIEnv *env, const char *function ) {
	if( !env->ExceptionOccurred() ) {
		return false;
	}
	LogError( function, "An exception occurred while calling Java code from %s\n", function );
	env->ExceptionDescribe();
	env->ExceptionClear();
	return false;
}

bool JavaConsole::CallOnNewBufferData( JNIEnv *env, ssize_t numBytes ) {
	jmethodID method = console_onNewBufferData_Method.Get();

	assert( method && globalConsoleRef );
	env->CallVoidMethod( globalConsoleRef, method, (jint)0, (jint)numBytes );
	return CheckForException( env, "JavaConsole::CallOnNewBufferData()" );
}

void JavaConsole::VPrintf( const char *format, va_list va ) {
	stringBuffer.VPrintf( format, va );

	if( stringBuffer.WentOutOfMemory() ) {
		stringBuffer.Clear();
		LogError( "JavaConsole::VPrintf()", "Cannot allocate a new string buffer\n" );
		return;
	}

	JNIEnv *env;
	globalJvm->GetEnv( (void **)&env, JNI_VERSION_1_6 );
	if( !env ) {
		FailWith( "JavaConsole::VPrintf()", "Can't get the current JNI environment\n" );
	}

	size_t bytesLeft = stringBuffer.Length();
	static_assert( sizeof( char ) == sizeof( uint8_t ), "" );
	uint8_t *stringData = (uint8_t*)stringBuffer.Buffer();
	// Skip the last zero byte
	if( bytesLeft && !stringData[bytesLeft - 1] ) {
		bytesLeft--;
	}

	// Dump data in chunks (the java code cares about newlines)
	while( bytesLeft > 0 ) {
		// Dump data in chunks
		if( bytesLeft <= javaBufferCapacity ) {
			memcpy( javaBufferBytes, stringData, bytesLeft );
			CallOnNewBufferData( env, bytesLeft );
			break;
		}

		// Save the copied chunk start
		const uint8_t *const chunkStart = stringData;
		// Set a coarse chunk end
		stringData += javaBufferCapacity;

		// Do not break UTF8 sequences. Inspect the current byte.
		// If the current byte is not an ASCII character
		if( stringData[-1] > 128 ) {
			constexpr auto HEAD_PATTERN = 0b11000000;
			constexpr auto SEQ_PATTERN = 0b10000000;

			if( ( stringData[-1] & HEAD_PATTERN ) == HEAD_PATTERN ) {
				// Put the chunk end pointer before the sequence start
				stringData--;
			} else {
				// Find the sequence start and put the chunk end pointer before it
				assert( ( stringData[-1] & SEQ_PATTERN ) == SEQ_PATTERN );

				while( ( stringData[-1] & SEQ_PATTERN ) == SEQ_PATTERN ) {
					stringData--;
				}
				assert( ( stringData[-1] & HEAD_PATTERN ) == HEAD_PATTERN );
				stringData--;
			}
		}

		ssize_t chunkSize = stringData - chunkStart;
		assert( chunkSize > 0 );
		memcpy( javaBufferBytes, chunkStart, (size_t)chunkSize );

		// Stop on JNI call failure (should not happen tbh)
		if( !CallOnNewBufferData( env, chunkSize ) ) {
			break;
		}
		bytesLeft -= ( stringData - chunkStart );
	}

	stringBuffer.Clear();
}

static JavaConsole *CreateConsole( JNIEnv *env, jobject javaConsole ) {
	jobject consoleBuffer = env->GetObjectField( javaConsole, console_ioBuffer_Field.Get() );
	if( !consoleBuffer ) {
		ThrowCheckingPending( env, illegalArgumentException_Class, "Can't get ByteBuffer ioBuffer field of a console" );
		return nullptr;
	}

	void *address = env->GetDirectBufferAddress( consoleBuffer );
	if( !address ) {
		const char *message = "Can't get an address of a Buffer native data for the given consoleBuffer argument";
		ThrowCheckingPending( env, illegalArgumentException_Class, message );
		return nullptr;
	}

	jlong capacity = env->GetDirectBufferCapacity( consoleBuffer );
	if( capacity < 64 ) {
		ThrowCheckingPending( env, illegalArgumentException_Class, "The buffer capacity is way too small" );
		return nullptr;
	}

	jobject globalConsoleRef = env->NewGlobalRef( javaConsole );
	if( !globalConsoleRef ) {
		ThrowCheckingPending( env, outOfMemoryException_Class, "Can't make a new global reference to a console" );
		return nullptr;
	}

	void *mem = malloc( sizeof( JavaConsole ) );
	// If somebody has decided to turn overcommit off
	if( !mem ) {
		env->DeleteGlobalRef( globalConsoleRef );
		ThrowCheckingPending( env, outOfMemoryException_Class, "Can't allocate a native Console counterpart" );
		return nullptr;
	}

	return new(mem)JavaConsole( globalConsoleRef, (jbyte *)address, (size_t)capacity );
}

class JavaClientListener : public ClientListener
{
	jobject globalListenerRef;
	jbyte *bufferBytes;
	jlong bufferSize;

	void AddStringToBuffer( const char *string, const char *argNum, jint offset, jint *length, const char *caller );

	void CallSigStringMethod( jmethodID method, const char *string, const char *caller );
	void CallSig2StringsMethod( jmethodID method, const char *string1, const char *string2, const char *caller );

#define CALL_SIG_STRING_METHOD( method, string ) \
	CallSigStringMethod( method.Get(), string, __FUNCTION__ )

#define CALL_SIG_2STRINGS_METHOD( method, string1, string2 ) \
	CallSig2StringsMethod( method.Get(), string1, string2, __FUNCTION__ )

public:
	JavaClientListener( jobject globalListenerRef_, jbyte *bufferBytes_, jlong bufferSize_ ) {
		this->globalListenerRef = globalListenerRef_;
		this->bufferBytes = bufferBytes_;
		this->bufferSize = bufferSize_;
	}

	~JavaClientListener() override {
		if( auto env = GetJNIEnv() ) {
			env->DeleteGlobalRef( globalListenerRef );
		}
	}

	void SetShownPlayerName( const char *name ) override {
		CALL_SIG_STRING_METHOD( clientListener_onShownPlayerNameSet_Method, name );
	}

	void SetMessageOfTheDay( const char *motd ) override {
		CALL_SIG_STRING_METHOD( clientListener_onMessageOfTheDaySet_Method, motd );
	}

	void PrintCenteredMessage( const char *message ) override {
		CALL_SIG_STRING_METHOD( clientListener_onCenteredMessage_Method, message );
	}

	void PrintChatMessage( const char *from, const char *message ) override {
		CALL_SIG_2STRINGS_METHOD( clientListener_onChatMessage_Method, from, message );
	}

	void PrintTeamChatMessage( const char *from, const char *message ) override {
		CALL_SIG_2STRINGS_METHOD( clientListener_onTeamChatMessage_Method, from, message );
	}

	void PrintTVChatMessage( const char *from, const char *message ) override {
		CALL_SIG_2STRINGS_METHOD( clientListener_onTVChatMessage_Method, from, message );
	}
};

void JavaClientListener::CallSigStringMethod( jmethodID method, const char *string, const char *caller ) {
	JNIEnv *env = GetJNIEnv();
	if( !env ) {
		LogError( caller, "Can't get a JNI environment\n" );
		return;
	}

	jint offset = 0, length;
	AddStringToBuffer( string, "1/1", 0, &length, caller );
	env->CallVoidMethod( globalListenerRef, method, offset, length );
	CheckForException( env, caller );
}

void JavaClientListener::CallSig2StringsMethod( jmethodID method, const char *string1,
												const char *string2, const char *caller ) {
	JNIEnv *env = GetJNIEnv();
	if( !env ) {
		LogError( caller, "Can't get JNI environment\n" );
		return;
	}

	jint offset1 = 0, length1;
	AddStringToBuffer( string1, "1/2", offset1, &length1, caller );
	jint offset2 = length1, length2;
	AddStringToBuffer( string2, "2/2", offset2, &length2, caller );

	env->CallVoidMethod( globalListenerRef, method, offset1, length1, offset2, length2 );
	CheckForException( env, caller );
}

void JavaClientListener::AddStringToBuffer( const char *string, const char *argNum,
											jint offset, jint *length, const char *caller ) {
	int i = offset;
	char *dest = (char *)bufferBytes;
	while( *string && i < bufferSize ) {
		dest[i++] = *string++;
	}

	if( *string ) {
		LogWarning( caller, "String argument %s overflow", argNum );
	}

	*length = i - offset;
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    setupNativeLibrary
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_System_setupNativeLibrary
	( JNIEnv *env, jclass ) {

	constexpr const char *tag = "System::setupNativeLibary()(JNI native)";

	if( env->GetJavaVM( &globalJvm ) < 0 ) {
		FailWith( tag, "Can't get the global JVM reference\n" );
	}

	bool wereErrors = false;
	StringBuffer messageSink;
	for( ClassHolder *classHolder = ClassHolder::listHead; classHolder; classHolder = classHolder->next ) {
		wereErrors |= !classHolder->Init( env, messageSink );
	}

	if( wereErrors ) {
		if( !classNotFoundException_Class.Get() ) {
			LogError( tag, "com.github.qfusion.System.setupNativeLibrary: there were errors: %s", messageSink.Buffer() );
			FailWith( tag, "Can't use java.lang.ClassNotFoundException class to throw an error\n" );
		}
		ThrowCheckingPending( env, classNotFoundException_Class, messageSink.Buffer() );
		return;
	}

	for( MethodHolder *methodHolder = MethodHolder::listHead; methodHolder; methodHolder = methodHolder->next ) {
		wereErrors |= !methodHolder->Init( env, messageSink );
	}

	if( wereErrors ) {
		ThrowCheckingPending( env, noSuchMethodException_Class, messageSink.Buffer() );
		return;
	}

	for( FieldHolder *fieldHolder = FieldHolder::listHead; fieldHolder; fieldHolder = fieldHolder->next ) {
		wereErrors |= !fieldHolder->Init( env, messageSink );
	}

	if( wereErrors ) {
		ThrowCheckingPending( env, noSuchFieldException_Class, messageSink.Buffer() );
		return;
	}

	// Release these global references that are no longer needed
	classNotFoundException_Class.ForceReleaseRef( env );
	noSuchMethodException_Class.ForceReleaseRef( env );
	noSuchFieldException_Class.ForceReleaseRef( env );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeInit
 * Signature: (Lcom/github/qfusion/fakeclient/NativeBridgeConsole;)V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_System_nativeInit
	( JNIEnv *env, jclass, jobject javaConsole ) {
	if( auto *console = CreateConsole( env, javaConsole ) ) {
		System::Init( console );
	}
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeShutdown
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_System_nativeShutdown
	( JNIEnv *, jclass ) {
	System::Shutdown();
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeGetInstance
 * Signature: ()J
 */
extern "C" JNIEXPORT jlong JNICALL Java_com_github_qfusion_fakeclient_System_nativeGetInstance
	( JNIEnv *, jclass ) {
	return SystemToHandle( System::Instance() );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeNewClient
 * Signature: (JLcom/github/qfusion/fakeclient/NativeBridgeConsole;)J
 */
extern "C" JNIEXPORT jlong JNICALL Java_com_github_qfusion_fakeclient_System_nativeNewClient
	( JNIEnv *env, jclass, jlong nativeSystem, jobject javaConsole ) {
	if( auto *console = CreateConsole( env, javaConsole ) ) {
		if( auto *client = HandleToSystem( nativeSystem )->NewClient( console ) ) {
			return ClientToHandle( client );
		}
		console->~JavaConsole();
		free( console );
	}
	return 0;
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeDeleteClient
 * Signature: (JJ)V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_System_nativeDeleteClient
	( JNIEnv *, jclass, jlong nativeSystem, jlong nativeClient ) {
	HandleToSystem( nativeSystem )->DeleteClient( HandleToClient( nativeClient ) );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeFrame
 * Signature: (JI)V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_System_nativeFrame
	( JNIEnv *, jclass, jlong nativeSystem, jint maxMillis ) {
	HandleToSystem( nativeSystem )->Frame( (unsigned)maxMillis );
}

/*
 * Class:     com_github_qfusion_fakeclient_Client
 * Method:    nativeSetListener
 * Signature: (JLcom/github/qfusion/fakeclient/ClientListener;)V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_Client_nativeSetListener
	( JNIEnv *env, jclass, jlong nativeClient, jobject javaListener ) {

    if( !javaListener ) {
        HandleToClient( nativeClient )->SetListener( nullptr );
		return;
    }

	jobject ioBuffer = env->GetObjectField( javaListener, clientListener_ioBuffer_Field.Get() );
	if( !ioBuffer ) {
		ThrowCheckingPending( env, noSuchFieldException_Class, "Can't get ByteBuffer ioBuffer field of the listener\n" );
		return;
	}

	void *bufferAddress = env->GetDirectBufferAddress( ioBuffer );
	if( !bufferAddress ) {
		const char *message = "Can't get an underlying native address of the ioBuffer listener field";
		ThrowCheckingPending( env, illegalArgumentException_Class, message );
		return;
	}

	jlong bufferCapacity = env->GetDirectBufferCapacity( ioBuffer );
	jobject globalListenerRef = env->NewGlobalRef( javaListener );
	if( !globalListenerRef ) {
		ThrowCheckingPending( env, illegalArgumentException_Class, "Can't make a global reference to the listener" );
		return;
	}

	void *mem = malloc( sizeof( JavaClientListener ) );
	if( !mem ) {
		env->DeleteGlobalRef( globalListenerRef );
		ThrowCheckingPending( env, outOfMemoryException_Class, "Can't allocate a native ClientListener counterpart" );
		return;
	}

	auto *listener = new(mem)JavaClientListener( globalListenerRef, (jbyte *)bufferAddress, (size_t)bufferCapacity );
	HandleToClient( nativeClient )->SetListener( listener );
}

/*
 * Class:     com_github_qfusion_fakeclient_Client
 * Method:    nativeExecuteCommand
 * Signature: (J[B)V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_Client_nativeExecuteCommand
	( JNIEnv *env, jclass, jlong nativeClient, jbyteArray byteArray ) {

	Client *client = HandleToClient( nativeClient );
	jint length = env->GetArrayLength( byteArray );
	if( length <= 127 ) {
		jbyte buffer[128];
		env->GetByteArrayRegion( byteArray, 0, length, buffer );
		buffer[length] = '\0';
		client->ExecuteCommand( (const char *) buffer );
		return;
	}

	jbyte *buffer = (jbyte *)malloc( length + 1 );
	if( !buffer ) {
		ThrowCheckingPending( env, outOfMemoryException_Class, "Can't allocate a buffer for command bytes" );
		return;
	}
	env->GetByteArrayRegion( byteArray, 0, length, buffer );
	buffer[length] = '\0';
	client->ExecuteCommand( (const char *)buffer );
	free( buffer );
}
