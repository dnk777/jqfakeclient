package com.github.qfusion.fakeclient;

import java.nio.ByteBuffer;

/**
 * A singleton that contains common underlying library logic.
 * The lifecycle of the native library logic must be explicitly managed by
 * {@link System#init(NativeBridgeConsole)} and {@link System#shutdown()} calls.
 * Call {@link System#getInstance()} in-between these calls to obtain a {@link System} instance.
 * The instance object can be used to create new or delete already created instances of a {@link Client}.
 * The underlying library logic relies on repeated {@link System#frame} calls performed by a library user
 * all the time when the system should be active (respond to user/network actions).
 * It is advised to avoid hot loop polling and put {@link Thread#sleep} calls in the loop too.
 * All methods except {@link System::frame} are safe to call from any thread.
 */
public class System {
    static {
        java.lang.System.loadLibrary("jqfakeclient");
        setupNativeLibrary();
    }

    private static native void setupNativeLibrary();
    private static native void nativeInit(NativeBridgeConsole console);
    private static native void nativeShutdown();
    private static native long nativeGetInstance();
    private static native long nativeNewClient(long nativeSystem, NativeBridgeConsole console);
    private static native void nativeDeleteClient(long nativeSystem, long nativeClient);
    private static native void nativeFrame(long nativeSystem, int maxMillis);

    private System(long nativeSystem) {
        this.nativeSystem = nativeSystem;
    }

    private long nativeSystem;
    private static volatile System instance;
    private static volatile boolean initCalled;
    private static final Object lock = new Object();

    public static System getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (!initCalled) {
                    throw new IllegalStateException("Attempt to call getInstance() before init()/after shutdown()");
                }
                if (instance == null) {
                    instance = new System(nativeGetInstance());
                }
            }
        }
        return instance;
    }

    public static void init(NativeBridgeConsole console) {
        if (!initCalled) {
            synchronized (lock) {
                nativeInit(console);
                initCalled = true;
            }
        }
    }

    public static void shutdown() {
        if (initCalled) {
            synchronized (lock) {
                nativeShutdown();
                initCalled = false;
                instance = null;
            }
        }
    }

    /**
     * Creates a new {@link Client}.
     * Delete the client by {@link System#deleteClient} call after use.
     * @return A new client, or null if there are too many clients.
     */
    public Client newClient(NativeBridgeConsole console) {
        // It is not so easy to intercept in native code
        if (console == null) {
            throw new IllegalArgumentException("The argument console is null");
        }
        // No need to lock it since nativeNewClient is thread-safe
        return new Client(nativeNewClient(nativeSystem, console), console);
    }

    /**
     * Deletes a {@link Client} instance.
     * @param client A client instance, can be null.
     */
    public void deleteClient(Client client) {
        if (client != null) {
            // No need to lock it since nativeDeleteClient is thread-safe
            nativeDeleteClient(nativeSystem, client.nativeClient);
        }
    }

    /**
     * Runs a frame of underlying native library.
     * This call becomes pinned to a thread where it has been called first.
     * An attempt to do this call from some other thread leads to an abortion.
     * Since this call probably uses networking it should not be done in the main Android UI thread.
     * @param maxMillis A hint of how many time can the underlying native call use.
     */
    public void frame(int maxMillis) {
        if (maxMillis < 0 || maxMillis > 10000) {
            String message = "The argument maxMillis = " + maxMillis + " is outside of valid [0, 10000] bounds";
            throw new IllegalArgumentException(message);
        }
        nativeFrame(nativeSystem, maxMillis);
    }
}
