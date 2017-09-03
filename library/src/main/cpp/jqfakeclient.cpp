#include "jqfakeclient.h"
#include "system.h"
#include "client.h"
#include <server_list.h>

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
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

static ClassHolder serverListListener_Class( "com/github/qfusion/fakeclient/NativeBridgeServerListListener" );

static MethodHolder serverListListener_onServerAdded( &serverListListener_Class, "onServerAdded", "(I)V" );
static MethodHolder serverListListener_onServerUpdated( &serverListListener_Class, "onServerUpdated", "(II)V" );
static MethodHolder serverListListener_onServerRemoved( &serverListListener_Class, "onServerRemoved", "(I)V" );

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

// Make unqualified aliases for exported Java constants
#define DECLARE_UNQUALIFIED_ALIAS( constantName ) \
	const auto constantName = com_github_qfusion_fakeclient_ScoreboardData_ ## constantName

DECLARE_UNQUALIFIED_ALIAS( PLAYER_NAME_SIZE );
DECLARE_UNQUALIFIED_ALIAS( TEAM_NAME_SIZE );

DECLARE_UNQUALIFIED_ALIAS( UPDATE_CHARS_WRITTEN_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_HINT_READ_FULL_DATA_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( PLAYERS_UPDATE_MASK_OFFSET );

DECLARE_UNQUALIFIED_ALIAS( HAS_PLAYER_INFO_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( MAX_PLAYERS );
DECLARE_UNQUALIFIED_ALIAS( SCOREBOARD_DATA_OFFSET );

DECLARE_UNQUALIFIED_ALIAS( ADDRESS_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( ADDRESS_SIZE );
DECLARE_UNQUALIFIED_ALIAS( SERVER_NAME_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( SERVER_NAME_SIZE );
DECLARE_UNQUALIFIED_ALIAS( MODNAME_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( MODNAME_SIZE );
DECLARE_UNQUALIFIED_ALIAS( GAMETYPE_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( GAMETYPE_SIZE );
DECLARE_UNQUALIFIED_ALIAS( MAPNAME_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( MAPNAME_SIZE );

DECLARE_UNQUALIFIED_ALIAS( TIME_MINUTES_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( TIME_MINUTES_SIZE );
DECLARE_UNQUALIFIED_ALIAS( LIMIT_MINUTES_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( LIMIT_MINUTES_SIZE );
DECLARE_UNQUALIFIED_ALIAS( TIME_SECONDS_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( TIME_SECONDS_SIZE );
DECLARE_UNQUALIFIED_ALIAS( LIMIT_SECONDS_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( LIMIT_SECONDS_SIZE );
DECLARE_UNQUALIFIED_ALIAS( TIME_FLAGS_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( TIME_FLAG_WARMUP );
DECLARE_UNQUALIFIED_ALIAS( TIME_FLAG_COUNTDOWN );
DECLARE_UNQUALIFIED_ALIAS( TIME_FLAG_OVERTIME );
DECLARE_UNQUALIFIED_ALIAS( TIME_FLAG_SUDDENDEATH );
DECLARE_UNQUALIFIED_ALIAS( TIME_FLAG_FINISHED );
DECLARE_UNQUALIFIED_ALIAS( TIME_FLAG_TIMEOUT );

DECLARE_UNQUALIFIED_ALIAS( ALPHA_NAME_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( ALPHA_NAME_SIZE );
DECLARE_UNQUALIFIED_ALIAS( ALPHA_SCORE_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( ALPHA_SCORE_SIZE );
DECLARE_UNQUALIFIED_ALIAS( BETA_NAME_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( BETA_NAME_SIZE );
DECLARE_UNQUALIFIED_ALIAS( BETA_SCORE_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( BETA_SCORE_SIZE );

DECLARE_UNQUALIFIED_ALIAS( MAX_CLIENTS_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( MAX_CLIENTS_SIZE );
DECLARE_UNQUALIFIED_ALIAS( NUM_CLIENTS_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( NUM_CLIENTS_SIZE );
DECLARE_UNQUALIFIED_ALIAS( NUM_BOTS_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( NUM_BOTS_SIZE );

DECLARE_UNQUALIFIED_ALIAS( NEED_PASSWORD_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( NEED_PASSWORD_SIZE );

DECLARE_UNQUALIFIED_ALIAS( PLAYERS_DATA_OFFSET );

DECLARE_UNQUALIFIED_ALIAS( PLAYER_PING_RELATIVE_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( PLAYER_PING_SIZE );
DECLARE_UNQUALIFIED_ALIAS( PLAYER_SCORE_RELATIVE_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( PLAYER_SCORE_SIZE );
DECLARE_UNQUALIFIED_ALIAS( PLAYER_NAME_RELATIVE_OFFSET );
DECLARE_UNQUALIFIED_ALIAS( PLAYER_TEAM_RELATIVE_OFFSET );

DECLARE_UNQUALIFIED_ALIAS( PLAYER_DATA_STRIDE );

DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_ADDRESS );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_SERVER_NAME );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_MODNAME );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_GAMETYPE );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_MAPNAME );

DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_TIME_MINUTES );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_LIMIT_MINUTES );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_TIME_SECONDS );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_LIMIT_SECONDS );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_TIME_FLAGS );

DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_ALPHA_NAME );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_ALPHA_SCORE );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_BETA_NAME );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_BETA_SCORE );

DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_MAX_CLIENTS );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_NUM_CLIENTS );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_NUM_BOTS );

DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_NEED_PASSWORD );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_HAS_PLAYER_INFO );
DECLARE_UNQUALIFIED_ALIAS( UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES );

DECLARE_UNQUALIFIED_ALIAS( PLAYERINFO_UPDATE_FLAG_PING );
DECLARE_UNQUALIFIED_ALIAS( PLAYERINFO_UPDATE_FLAG_SCORE );
DECLARE_UNQUALIFIED_ALIAS( PLAYERINFO_UPDATE_FLAG_NAME );
DECLARE_UNQUALIFIED_ALIAS( PLAYERINFO_UPDATE_FLAG_TEAM );

static void WriteChars( const char *chars, jchar *buffer, unsigned length ) {
	for( unsigned i = 0; i < length; ++i ) {
		buffer[i] = (unsigned char)chars[i];
	}
}

static unsigned WriteInt32AsString( jchar *buffer, int32_t value ) {
	// TODO: Optimize by avoiding the sprintf call and array copying.
	// This implementation is a way to quickly make the feature working.
	char chars[16];
	int numCharsWritten = snprintf( chars, 16, "%d", (int)value );

	for( int i = 0; i < numCharsWritten; ++i ) {
		buffer[i] = chars[i];
	}
	return numCharsWritten;
}

// An obvious solution is to use mutliple overloads for WriteIntegerAsBits() for each needed type.
// However there is a pitfall. If there is no exact overload for a type, it is promoted to an integer,
// and an attempt to write 32 bits is made. This is extremely hard to find and debug.
// Thus we check the exact size of the value.

template<typename T>
inline unsigned WriteIntegerAsBits( jchar *buffer, T value ) {
	switch( sizeof( T ) ) {
		case 1:
			buffer[0] = (jchar)value;
			return 1;
		case 2:
			buffer[0] = (jchar)value;
			return 1;
		case 4:
			uint32_t uvalue = (uint32_t)value;
			buffer[0] = (jchar)( ( uvalue >> 16 ) & 0xFFFF );
			buffer[1] = (jchar)( ( uvalue >> 00 ) & 0xFFFF );
			return 2;
	}
}

class ServerInfoWriter
{
	jbyte *const byteBuffer;
	jchar *const charBuffer;
	const PolledGameServer &server;

	unsigned numUpdatedChars;
	// We can get number of ServerInfo fields written counting set bits a server info update mask,
	// but we still have to use a separate counter for player info fields.
	// Use this counter for field writes of all kind to stay uniform.
	unsigned numFieldsWritten;

	jint WriteFullServerInfo();
	jint WriteServerInfoDelta();

	void WriteFullPlayersInfo();
	bool WritePlayersInfoDelta();

	void WriteAddress();

	template<uint8_t N>
	void WriteBufferAndLength( const BufferAndLength<N> &value, int offset, int maxSize ) {
		WriteStringAndLength( value.chars, value.actualLength, offset, maxSize );
	}

	void WriteStringAndLength( const char *chars, unsigned length, int offset, int maxSize ) {
		assert( length + 1 <= (unsigned)maxSize );
		assert( length < std::numeric_limits<jchar>::max() );
		charBuffer[offset] = (jchar)length;
		WriteChars( chars, charBuffer + offset + 1, length );
		// numUpdatedChars is really used for allocation of delta updates char[] array on the Java side
		// a delta entry, a subrange of the delta updates char[] array
		// consists of charaters written by this function and/or its callers
		// (a string, its length, maybe some binary parts attached) and the total delta entry size.
		// Thus we have to add not only an extra character for the length of the string,
		// but a character for the delta entry size too.
		numUpdatedChars += length + 2;
		numFieldsWritten++;
	}

	inline void WriteServerName() {
		WriteBufferAndLength( server.CurrInfo()->serverName, SERVER_NAME_OFFSET, SERVER_NAME_SIZE );
	}
	inline void WriteModName() {
		WriteBufferAndLength( server.CurrInfo()->modname, MODNAME_OFFSET, MODNAME_SIZE );
	}
	inline void WriteGametype() {
		WriteBufferAndLength( server.CurrInfo()->gametype, GAMETYPE_OFFSET, GAMETYPE_SIZE );
	}
	inline void WriteMapName() {
		WriteBufferAndLength( server.CurrInfo()->mapname, MAPNAME_OFFSET, MAPNAME_SIZE );
	}
	inline void WriteAlphaName() {
		WriteBufferAndLength( server.CurrInfo()->score.AlphaScore().name, ALPHA_NAME_OFFSET, ALPHA_NAME_SIZE );
	}
	inline void WriteBetaName() {
		WriteBufferAndLength( server.CurrInfo()->score.BetaScore().name, BETA_NAME_OFFSET, BETA_NAME_SIZE );
	}

	template<typename T>
	void WriteIntegerAsCharsAndBits( T value, int offset, int maxSize ) {
		unsigned intBitsLength = WriteIntegerAsBits( charBuffer + offset, value );
		offset += intBitsLength;
		jchar *lengthPtr = &charBuffer[offset];
		offset++;

		static_assert( sizeof( T ) <= sizeof( int32_t ), "" );
		unsigned stringLength = WriteInt32AsString( &charBuffer[offset], (int32_t)value );
		assert( stringLength + intBitsLength + 1 <= maxSize );
		*lengthPtr = (jchar)stringLength;

		numUpdatedChars += stringLength + intBitsLength + 1;
		numFieldsWritten++;
	}

	inline void WriteTimeMinutes() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->time.timeMinutes, TIME_MINUTES_OFFSET, TIME_MINUTES_SIZE );
	}
	inline void WriteLimitMinutes() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->time.limitMinutes, LIMIT_MINUTES_OFFSET, LIMIT_MINUTES_SIZE );
	}
	inline void WriteTimeSeconds() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->time.timeSeconds, TIME_SECONDS_OFFSET, TIME_SECONDS_SIZE );
	}
	inline void WriteLimitSeconds() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->time.limitSeconds, LIMIT_SECONDS_OFFSET, LIMIT_SECONDS_SIZE );
	}

	inline void WriteAlphaScore() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->score.AlphaScore().score, ALPHA_SCORE_OFFSET, ALPHA_SCORE_SIZE );
	}
	inline void WriteBetaScore() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->score.BetaScore().score, BETA_SCORE_OFFSET, BETA_SCORE_SIZE );
	}

	inline void WriteMaxClients() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->maxClients, MAX_CLIENTS_OFFSET, MAX_CLIENTS_SIZE );
	}
	inline void WriteNumClients() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->numClients, NUM_CLIENTS_OFFSET, NUM_CLIENTS_SIZE );
	}
	inline void WriteNumBots() {
		WriteIntegerAsCharsAndBits( server.CurrInfo()->numBots, NUM_BOTS_OFFSET, NUM_BOTS_SIZE );
	}

	void WriteTimeFlags() {
		jchar flags = 0;
		const MatchTime &time = server.CurrInfo()->time;

		if( time.isWarmup ) {
			flags |= TIME_FLAG_WARMUP;
		}
		if( time.isCountdown ) {
			flags |= TIME_FLAG_COUNTDOWN;
		}
		if( time.isSuddenDeath ) {
			flags |= TIME_FLAG_SUDDENDEATH;
		}
		if( time.isOvertime ) {
			flags |= TIME_FLAG_OVERTIME;
		}
		if( time.isFinished ) {
			flags |= TIME_FLAG_FINISHED;
		}
		if( time.isTimeout ) {
			flags |= TIME_FLAG_TIMEOUT;
		}

		charBuffer[TIME_FLAGS_OFFSET] = flags;
		numUpdatedChars++;
		numFieldsWritten++;
	}

	void WriteNeedPassword() {
		if( server.CurrInfo()->needPassword ) {
			WriteStringAndLength( "yes", 3, NEED_PASSWORD_OFFSET, NEED_PASSWORD_SIZE );
		} else {
			WriteStringAndLength( "no", 2, NEED_PASSWORD_OFFSET, NEED_PASSWORD_SIZE );
		}
	}

	void WriteHasPlayerInfo() {
		charBuffer[HAS_PLAYER_INFO_OFFSET] = (jchar)server.CurrInfo()->hasPlayerInfo;
		numUpdatedChars += 1;
	}

	void WritePlayerPing( const PlayerInfo *info, int baseOffset ) {
		WriteIntegerAsCharsAndBits( info->ping, baseOffset + PLAYER_PING_RELATIVE_OFFSET, PLAYER_PING_SIZE );
	}
	void WritePlayerScore( const PlayerInfo *info, int baseOffset ) {
		WriteIntegerAsCharsAndBits( info->score, baseOffset + PLAYER_SCORE_RELATIVE_OFFSET, PLAYER_SCORE_SIZE );
	}
	void WritePlayerName( const PlayerInfo *info, int baseOffset ) {
		WriteBufferAndLength( info->name, baseOffset + PLAYER_NAME_RELATIVE_OFFSET, PLAYER_NAME_SIZE );
	}
	void WritePlayerTeam( const PlayerInfo *info, int baseOffset ) {
		charBuffer[baseOffset + PLAYER_SCORE_RELATIVE_OFFSET] = info->team;
		numUpdatedChars++;
	}

	jbyte *PlayersUpdateMask() {
		return byteBuffer + PLAYERS_UPDATE_MASK_OFFSET * ( sizeof( jchar ) / sizeof( jbyte ) );
	}

	void WriteNumCharsWrittenAndHints();

public:
	ServerInfoWriter( jbyte *byteBuffer_, jchar *charBuffer_, const PolledGameServer &server_ )
		: byteBuffer( byteBuffer_ ), charBuffer( charBuffer_ ), server( server_ ) {
		assert( (void *)byteBuffer_ == (void *)charBuffer_ );
	}

	jint Write();
};

void ServerInfoWriter::WriteAddress() {
	const NetworkAddress &address = server.Address();
	char buffer[INET6_ADDRSTRLEN + 8];
	size_t hostPartLength;

	if( address.IsIpV4Address() ) {
		assert( inet_ntop( address.Family(), address.AsGenericSockaddr(), buffer, sizeof( buffer ) ) );
		hostPartLength = strlen( buffer );
	} else if( address.IsIpV6Address() ) {
		buffer[0] = '[';
		assert( inet_ntop( address.Family(), address.AsGenericSockaddr(), buffer + 1, sizeof( buffer ) ) );
		hostPartLength = strlen( buffer + 1 ) + 1;
		buffer[hostPartLength - 1] = ']';
	} else {
		// Should not happen but we do not have good error-handling way for this case
		const char *stub = "<Unknown address>";
		size_t stubLength = strlen( stub );
		WriteStringAndLength( stub, stubLength, ADDRESS_OFFSET, stubLength + 1 );
		numUpdatedChars += stubLength + 1;
		return;
	}

	size_t bufferBytesLeft = sizeof( buffer ) - hostPartLength;
	int printfResult = snprintf( buffer + hostPartLength, bufferBytesLeft, ":%d", (int)address.Port() );
	assert( printfResult > 0 && printfResult < bufferBytesLeft );

	WriteChars( buffer, charBuffer + ADDRESS_OFFSET, (unsigned)( hostPartLength + printfResult ) );
	numUpdatedChars += hostPartLength + printfResult;
}

jint ServerInfoWriter::Write() {
	numUpdatedChars = 0;
	numFieldsWritten = 0;

	// Always write numeric values of these fields regardless are there updated or not.
	charBuffer[NUM_CLIENTS_OFFSET] = server.NumClients();
	charBuffer[HAS_PLAYER_INFO_OFFSET] = (jchar)server.HasPlayerInfo();

	if( server.OldInfo() ) {
		return WriteServerInfoDelta();
	}

	return WriteFullServerInfo();
}

jint ServerInfoWriter::WriteFullServerInfo() {
	WriteAddress();

	WriteServerName();
	WriteModName();
	WriteGametype();
	WriteMapName();

	WriteTimeMinutes();
	WriteLimitMinutes();
	WriteTimeSeconds();
	WriteLimitSeconds();
	WriteTimeFlags();

	WriteAlphaName();
	WriteAlphaScore();
	WriteBetaName();
	WriteBetaScore();

	WriteMaxClients();
	WriteNumClients();
	WriteNumBots();

	WriteHasPlayerInfo();
	WriteNeedPassword();

	if( server.CurrInfo()->hasPlayerInfo ) {
		WriteFullPlayersInfo();
	}

	WriteNumCharsWrittenAndHints();

	return ~0;
}

inline bool TimeFlagsDiffer( const MatchTime &currTime, const MatchTime &oldTime ) {
	if( currTime.isWarmup != oldTime.isWarmup || currTime.isCountdown != oldTime.isCountdown ) {
		return true;
	}

	if( currTime.isSuddenDeath != oldTime.isSuddenDeath || currTime.isOvertime != oldTime.isOvertime ) {
		return true;
	}

	if( currTime.isTimeout != oldTime.isTimeout || currTime.isFinished != oldTime.isFinished ) {
		return true;
	}

	return false;
}

jint ServerInfoWriter::WriteServerInfoDelta() {
	assert( server.CurrInfo() && server.OldInfo() );
	const ServerInfo &currInfo = *server.CurrInfo();
	const ServerInfo &oldInfo = *server.OldInfo();

	// Should be written always regardless of an actual udpate status
	WriteHasPlayerInfo();

	jint result = 0;

	if( currInfo.serverName != oldInfo.serverName ) {
		WriteServerName();
		result |= UPDATE_FLAG_SERVER_NAME;
	}

	if( currInfo.modname != oldInfo.modname ) {
		WriteModName();
		result |= UPDATE_FLAG_MODNAME;
	}

	if( currInfo.gametype != oldInfo.gametype ) {
		WriteGametype();
		result |= UPDATE_FLAG_GAMETYPE;
	}

	if( currInfo.mapname != oldInfo.mapname ) {
		WriteMapName();
		result |= UPDATE_FLAG_MAPNAME;
	}

	if( currInfo.time.timeMinutes != oldInfo.time.timeMinutes ) {
		WriteTimeMinutes();
		result |= UPDATE_FLAG_TIME_MINUTES;
	}

	if( currInfo.time.limitMinutes != oldInfo.time.limitMinutes ) {
		WriteLimitMinutes();
		result |= UPDATE_FLAG_LIMIT_MINUTES;
	}

	if( currInfo.time.timeSeconds != oldInfo.time.timeSeconds ) {
		WriteTimeSeconds();
		result |= UPDATE_FLAG_TIME_SECONDS;
	}

	if( currInfo.time.limitSeconds != oldInfo.time.limitSeconds ) {
		WriteLimitSeconds();
		result |= UPDATE_FLAG_LIMIT_SECONDS;
	}

	if( TimeFlagsDiffer( currInfo.time, oldInfo.time ) ) {
		WriteTimeFlags();
		result |= UPDATE_FLAG_TIME_FLAGS;
	}

	if( currInfo.maxClients != oldInfo.maxClients ) {
		WriteMaxClients();
		result |= UPDATE_FLAG_MAX_CLIENTS;
	}

	if( currInfo.numClients != oldInfo.numClients ) {
		WriteNumClients();
		result |= UPDATE_FLAG_NUM_CLIENTS;
	}

	if( currInfo.numBots != oldInfo.numBots ) {
		WriteNumBots();
		result |= UPDATE_FLAG_NUM_BOTS;
	}

	if( currInfo.needPassword != oldInfo.needPassword ) {
		WriteNeedPassword();
		result |= UPDATE_FLAG_NEED_PASSWORD;
	}

	if( currInfo.hasPlayerInfo != oldInfo.hasPlayerInfo ) {
		result |= UPDATE_FLAG_HAS_PLAYER_INFO;
		// The datum has already been written
	}

	if( currInfo.hasPlayerInfo ) {
		if( !oldInfo.hasPlayerInfo ) {
			WriteFullPlayersInfo();
			result |= UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES;
		} else if( oldInfo.numClients != currInfo.numClients ) {
			WriteFullPlayersInfo();
			result |= UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES;
		} else {
			if( WritePlayersInfoDelta() ) {
				result |= UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES;
			}
		}
	}

	WriteNumCharsWrittenAndHints();

	return result;
}

void ServerInfoWriter::WriteFullPlayersInfo() {
	assert( server.CurrInfo() && server.CurrInfo()->hasPlayerInfo );
	const ServerInfo &serverInfo = *server.CurrInfo();

	jbyte *const mask = this->PlayersUpdateMask();
	unsigned i = 0;
	int baseOffset = PLAYERS_DATA_OFFSET;
	LinksIterator<PlayerInfo> iterator( serverInfo.playerInfoHead );

	while( iterator.HasNext() ) {
		const PlayerInfo *playerInfo = iterator.Next();
		WritePlayerPing( playerInfo, baseOffset );
		WritePlayerScore( playerInfo, baseOffset );
		WritePlayerName( playerInfo, baseOffset );
		WritePlayerTeam( playerInfo, baseOffset );
		baseOffset += PLAYER_DATA_STRIDE;
		mask[i++] = ~0;
	}

	assert( i == serverInfo.numClients );
}

bool ServerInfoWriter::WritePlayersInfoDelta() {
	assert( server.CurrInfo() && server.CurrInfo()->hasPlayerInfo );
	assert( server.OldInfo() && server.OldInfo()->hasPlayerInfo );

	const ServerInfo &currServerInfo = *server.CurrInfo();
	const ServerInfo &oldServerInfo = *server.OldInfo();
	// Currently delta updates are limited to this case
	assert( currServerInfo.numClients == oldServerInfo.numClients );

	jbyte *const playersUpdateMask = this->PlayersUpdateMask();
	unsigned i = 0;
	int baseOffset = PLAYERS_DATA_OFFSET;
	LinksIterator<PlayerInfo> oldInfoIterator( oldServerInfo.playerInfoHead );
	LinksIterator<PlayerInfo> currInfoIterator( currServerInfo.playerInfoHead );

	int anyUpdatesMask = false;
	for(;; ) {
		if( !currInfoIterator.HasNext() ) {
			assert( !oldInfoIterator.HasNext() );
			break;
		}
		const PlayerInfo *currPlayerInfo = currInfoIterator.Next();
		const PlayerInfo *oldPlayerInfo = oldInfoIterator.Next();

		jbyte mask = 0;

		if( currPlayerInfo->ping != oldPlayerInfo->ping ) {
			WritePlayerPing( currPlayerInfo, baseOffset );
			mask |= PLAYERINFO_UPDATE_FLAG_PING;
		}

		if( currPlayerInfo->score != oldPlayerInfo->score ) {
			WritePlayerScore( currPlayerInfo, baseOffset );
			mask |= PLAYERINFO_UPDATE_FLAG_SCORE;
		}

		if( currPlayerInfo->name != oldPlayerInfo->name ) {
			WritePlayerName( currPlayerInfo, baseOffset );
			mask |= PLAYERINFO_UPDATE_FLAG_NAME;
		}

		if( currPlayerInfo->team != oldPlayerInfo->team ) {
			WritePlayerTeam( currPlayerInfo, baseOffset );
			mask |= PLAYERINFO_UPDATE_FLAG_TEAM;
		}

		anyUpdatesMask |= mask;

		baseOffset += PLAYER_DATA_STRIDE;
		playersUpdateMask[i++] = mask;
	}

	assert( i == currServerInfo.numClients );

	return anyUpdatesMask != 0;
}

void ServerInfoWriter::WriteNumCharsWrittenAndHints() {
	numUpdatedChars += 3;
	numFieldsWritten += 2;
	charBuffer[UPDATE_CHARS_WRITTEN_OFFSET + 0] = (jchar)( ( numUpdatedChars >> 16 ) & 0xFFFF );
	charBuffer[UPDATE_CHARS_WRITTEN_OFFSET + 1] = (jchar)( ( numUpdatedChars >> 00 ) & 0xFFFF );

	// TODO: Use something more sophisticated?
	bool needsFullUpdate = numUpdatedChars > 512 || numFieldsWritten > 24;
	charBuffer[UPDATE_HINT_READ_FULL_DATA_OFFSET] = (jchar)needsFullUpdate;
}

class JavaServerListListener : public ServerListListener
{
	jobject listenerGlobalRef;
	jbyte *byteBuffer;
	jchar *charBuffer;

public:
	JavaServerListListener( jobject listenerGlobalRef_, jbyte *byteBuffer_, jchar *charBuffer_ )
		: listenerGlobalRef( listenerGlobalRef_ ), byteBuffer( byteBuffer_ ), charBuffer( charBuffer_ ) {}

	~JavaServerListListener() override {
		GetJNIEnv()->DeleteGlobalRef( listenerGlobalRef );
	}

	void OnServerAdded( const PolledGameServer &server ) override;
	void OnServerUpdated( const PolledGameServer &server ) override;
	void OnServerRemoved( const PolledGameServer &server ) override;
};

void JavaServerListListener::OnServerAdded( const PolledGameServer &server ) {
	ServerInfoWriter( byteBuffer, charBuffer, server ).Write();
	const auto instanceId = server.InstanceId();
	static_assert( sizeof( decltype( instanceId ) ) == sizeof( jint ), "" );
	JNIEnv *env = GetJNIEnv();
	env->CallVoidMethod( listenerGlobalRef, serverListListener_onServerAdded.Get(), instanceId );
	CheckForException( env, "JavaServerListListener::OnServerAdded()" );
}

void JavaServerListListener::OnServerUpdated( const PolledGameServer &server ) {
	jint updateMask = ServerInfoWriter( byteBuffer, charBuffer, server ).Write();
	if( !updateMask ) {
		return;
	}

	const auto instanceId = server.InstanceId();
	static_assert( sizeof( decltype( instanceId ) ) == sizeof( jint ), "" );
	JNIEnv *env = GetJNIEnv();
	env->CallVoidMethod( listenerGlobalRef, serverListListener_onServerUpdated.Get(), instanceId, updateMask );
	CheckForException( env, "JavaServerListListener::OnServerUpdated()" );
}

void JavaServerListListener::OnServerRemoved( const PolledGameServer &server ) {
	const auto instanceId = server.InstanceId();

	static_assert( sizeof( decltype( instanceId ) ) == sizeof( jint ), "" );
	JNIEnv *env = GetJNIEnv();
	env->CallVoidMethod( listenerGlobalRef, serverListListener_onServerRemoved.Get(), instanceId );
	CheckForException( env, "JavaServerListListener::OnServerRemoved()" );
}

inline void UnpackBytesFromShort( jshort value, uint8_t *dest ) {
	dest[0] = (uint8_t)( ( (unsigned)value >> 8 ) & 0xFF );
	dest[1] = (uint8_t)( ( (unsigned)value >> 0 ) & 0xFF );
}

// Note: Shifting by a variable should not be generally met in performance-aware code.

inline void UnpackBytesFromInt( jint value, uint8_t *dest ) {
	for( int i = 0, shift = 24; i < 4; ++i, shift -= 8 ) {
		dest[i] = (uint8_t)( ( (unsigned)value >> shift ) & 0xFF );
	}
}

inline void UnpackBytesFromLong( jlong value, uint8_t *dest ) {
	for( int i = 0, shift = 56; i < 8; ++i, shift -= 8 ) {
		dest[i] = (uint8_t)( ( (uint64_t)value >> shift ) & 0xFF );
	}
}

// Args are specified in native byte order
static inline NetworkAddress AddressFromJniArgs( jint host, jshort port ) {
	NetworkAddress result;

	uint8_t hostBytes[4];
	uint8_t portBytes[2];

	UnpackBytesFromShort( port, portBytes );
	UnpackBytesFromInt( host, hostBytes );

	result.SetFromIpV4Data( hostBytes, portBytes );
	return result;
}

// Args are specified in native byte order
static inline NetworkAddress AddressFromJniArgs( jlong hostHiPart, jlong hostLoPart, jshort port ) {
	NetworkAddress result;

	uint8_t hostBytes[16];
	uint8_t portBytes[8];

	UnpackBytesFromLong( hostHiPart, hostBytes + 0 );
	UnpackBytesFromLong( hostLoPart, hostBytes + 8 );
	UnpackBytesFromShort( port, portBytes );

	result.SetFromIpV6Data( hostBytes, portBytes );
	return result;
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeAddMasterServerIpV4
 * Signature: (JIS)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_github_qfusion_fakeclient_System_nativeAddMasterServerIpV4
	( JNIEnv *, jclass, jlong nativeSystem, jint addressBytes, jshort port ) {
	return (jboolean)HandleToSystem( nativeSystem )->AddMasterServer( AddressFromJniArgs( addressBytes, port ) );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeAddMasterServerIpV6
 * Signature: (JJJS)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_github_qfusion_fakeclient_System_nativeAddMasterServerIpV6
	( JNIEnv *, jclass, jlong nativeSystem, jlong hiPart, jlong loPart, jshort port ) {
	return (jboolean)HandleToSystem( nativeSystem )->AddMasterServer( AddressFromJniArgs( hiPart, loPart, port ) );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeRemoveMasterServerIpV4
 * Signature: (JIS)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_github_qfusion_fakeclient_System_nativeRemoveMasterServerIpV4
	( JNIEnv *, jclass, jlong nativeSystem, jint addressBytes, jshort port ) {
	return (jboolean)HandleToSystem( nativeSystem )->RemoveMasterServer( AddressFromJniArgs( addressBytes, port ) );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeRemoveMasterServerIpV6
 * Signature: (JJJS)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_github_qfusion_fakeclient_System_nativeRemoveMasterServerIpV6
	( JNIEnv *, jclass, jlong nativeSystem, jlong hiPart, jlong loPart, jshort port ) {
	return (jboolean)HandleToSystem( nativeSystem )->RemoveMasterServer( AddressFromJniArgs( hiPart, loPart, port ) );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeIsMasterServerIpV4
 * Signature: (JIS)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_github_qfusion_fakeclient_System_nativeIsMasterServerIpV4
	( JNIEnv *, jclass, jlong nativeSystem, jint addressBytes, jshort port ) {
	return (jboolean)HandleToSystem( nativeSystem )->IsMasterServer( AddressFromJniArgs( addressBytes, port ) );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeIsMasterServerIpV6
 * Signature: (JJJS)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_github_qfusion_fakeclient_System_nativeIsMasterServerIpV6
	( JNIEnv *, jclass, jlong nativeSystem, jlong hiPart, jlong loPart, jshort port ) {
	return (jboolean)HandleToSystem( nativeSystem )->IsMasterServer( AddressFromJniArgs( hiPart, loPart, port ) );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeStartUpdatingServerList
 * Signature: (JLcom/github/qfusion/fakeclient/NativeBridgeServerListListener;Ljava/nio/ByteBuffer;Ljava/nio/CharBuffer;)Z
 */
extern "C" JNIEXPORT jboolean JNICALL Java_com_github_qfusion_fakeclient_System_nativeStartUpdatingServerList
	( JNIEnv *env, jclass, jlong nativeSystem, jobject listener, jobject byteBuffer, jobject charBuffer ) {
	const char *tag = "nativeStartUpdatingServerList()";
	jobject listenerGlobalRef = env->NewGlobalRef( listener );

	if( !listenerGlobalRef ) {
		FailWith( tag, "Can't make a new global reference to the listener\n" );
	}

	jbyte *byteBufferAddress = (jbyte *)env->GetDirectBufferAddress( byteBuffer );
	if( !byteBufferAddress ) {
		FailWith( tag, "Can't get a direct address of the byte buffer\n" );
	}

	jchar *charBufferAddress = (jchar *)env->GetDirectBufferAddress( charBuffer );
	if( !charBufferAddress ) {
		FailWith( tag, "Can't get a direct address of the byte buffer\n" );
	}

	if( (void *)byteBufferAddress != (void *)charBufferAddress ) {
		FailWith( tag, "Addresses of byte and char buffer do not match\n" );
	}

	jlong capacity = env->GetDirectBufferCapacity( byteBuffer );
	if( capacity < 2 * ( PLAYERS_DATA_OFFSET + MAX_PLAYERS * PLAYER_DATA_STRIDE ) ) {
		FailWith( tag, "The byte buffer has an insufficient capacity\n" );
	}

	void *mem = malloc( sizeof( JavaServerListListener ) );
	if( !mem ) {
		FailWith( tag, "Can't allocate a memory for a native listener\n" );
	}

	auto *nativeListener = new JavaServerListListener( listenerGlobalRef, byteBufferAddress, charBufferAddress );
	return (jboolean)HandleToSystem( nativeSystem )->StartUpdatingServerList( nativeListener );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeSetServerListUpdateOptions
 * Signature: (JZZ)V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_System_nativeSetServerListUpdateOptions
	( JNIEnv *, jclass, jlong nativeSystem, jboolean showEmptyServers, jboolean showPlayerInfo ) {
	HandleToSystem( nativeSystem )->SetServerListUpdateOptions( showEmptyServers, showPlayerInfo );
}

/*
 * Class:     com_github_qfusion_fakeclient_System
 * Method:    nativeStopUpdatingServerList
 * Signature: (J)V
 */
extern "C" JNIEXPORT void JNICALL Java_com_github_qfusion_fakeclient_System_nativeStopUpdatingServerList
	( JNIEnv *, jclass, jlong nativeSystem ) {
	HandleToSystem( nativeSystem )->StopUpdatingServerList();
}