package com.github.qfusion.fakeclient;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;

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

    private static native boolean nativeAddMasterServerIpV4(long nativeSystem, int bytes, short port);
    private static native boolean nativeAddMasterServerIpV6(long nativeSystem, long bytes1, long bytes2, short port);
    private static native boolean nativeRemoveMasterServerIpV4(long nativeSystem, int bytes, short port);
    private static native boolean nativeRemoveMasterServerIpV6(long nativeSystem, long hiPart, long loPart, short port);
    private static native boolean nativeIsMasterServerIpV4(long nativeSystem, int bytes, short port);
    private static native boolean nativeIsMasterServerIpV6(long nativeSystem, long hiPart, long loPart, short port);

    private static native boolean nativeStartUpdatingServerList(long nativeSystem, NativeBridgeServerListListener listener,
                                                                ByteBuffer byteIoBuffer, CharBuffer charIoBuffer);

    private static native void nativeSetServerListUpdateOptions(long nativeSystem,
                                                                boolean showEmptyServers,
                                                                boolean showPlayerInfo);

    private static native void nativeStopUpdatingServerList(long nativeSystem);

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

    public boolean addMasterServer(InetAddress address, short port) {
        if (address instanceof Inet4Address) {
            return addMasterServer((Inet4Address)address, port);
        }
        if (address instanceof Inet6Address) {
            return addMasterServer((Inet6Address)address, port);
        }
        throw new IllegalArgumentException("The address " + address + " has an illegal type");
    }

    public boolean addMasterServer(Inet4Address address, short port) {
        return nativeAddMasterServerIpV4(nativeSystem, bytesToInt(address.getAddress(), 0), port);
    }

    public boolean addMasterServer(Inet6Address address, short port) {
        byte[] addressBytes = address.getAddress();
        long hiPart = bytesToLong(addressBytes, 0);
        long loPart = bytesToLong(addressBytes, 8);
        return nativeAddMasterServerIpV6(nativeSystem, hiPart, loPart, port);
    }

    public boolean removeMasterServer(InetAddress address, short port) {
        if (address instanceof Inet4Address) {
            return removeMasterServer((Inet4Address)address, port);
        }
        if (address instanceof Inet6Address) {
            return removeMasterServer((Inet6Address)address, port);
        }
        throw new IllegalArgumentException("The address " + address + " has an illegal type");
    }

    public boolean removeMasterServer(Inet4Address address, short port) {
        return nativeRemoveMasterServerIpV4(nativeSystem, bytesToInt(address.getAddress(), 0), port);
    }

    public boolean removeMasterServer(Inet6Address address, short port) {
        byte[] addressBytes = address.getAddress();
        long hiPart = bytesToLong(addressBytes, 0);
        long loPart = bytesToLong(addressBytes, 8);
        return nativeRemoveMasterServerIpV6(nativeSystem, hiPart, loPart, port);
    }

    public boolean isMasterServer(InetAddress address, short port) {
        if (address instanceof Inet4Address) {
            return isMasterServer((Inet4Address)address, port);
        }
        if (address instanceof Inet6Address) {
            return isMasterServer((Inet6Address)address, port);
        }
        throw new IllegalArgumentException("The address " + address + " has an illegal type");
    }

    public boolean isMasterServer(Inet4Address address, short port) {
        return nativeIsMasterServerIpV4(nativeSystem, bytesToInt(address.getAddress(), 0), port);
    }

    public boolean isMasterServer(Inet6Address address, short port) {
        byte[] addressBytes = address.getAddress();
        long hiPart = bytesToLong(addressBytes, 0);
        long loPart = bytesToLong(addressBytes, 8);
        return nativeIsMasterServerIpV6(nativeSystem, hiPart, loPart, port);
    }

    /**
     * Constructs an integer value from the given bytes.
     * These bytes are assumed to be in network byte order.
     * The result is in native byte order and can be used as a regular integer.
     */
    private static int bytesToInt(byte[] bytes, int offset) {
        int result = 0;
        result |= ((bytes[offset + 0] & 0xFF) << 24);
        result |= ((bytes[offset + 1] & 0xFF) << 16);
        result |= ((bytes[offset + 2] & 0xFF) << 8);
        result |= ((bytes[offset + 3] & 0xFF) << 0);
        return result;
    }

    /**
     * Constructs an long value from the given bytes.
     * These bytes are assumed to be in network byte order.
     * The result is in native byte order and can be used as a regular long.
     */
    private static long bytesToLong(byte[] bytes, int offset) {
        long result = 0;
        result |= ((long)(bytes[offset + 0] & 0xFF) << 56);
        result |= ((long)(bytes[offset + 1] & 0xFF) << 48);
        result |= ((long)(bytes[offset + 2] & 0xFF) << 40);
        result |= ((long)(bytes[offset + 3] & 0xFF) << 32);
        result |= ((long)(bytes[offset + 4] & 0xFF) << 24);
        result |= ((long)(bytes[offset + 5] & 0xFF) << 16);
        result |= ((long)(bytes[offset + 6] & 0xFF) << 8);
        result |= ((long)(bytes[offset + 7] & 0xFF) << 0);
        return result;
    }

    public boolean startUpdatingServerList(NativeBridgeServerListListener listener) {
        return nativeStartUpdatingServerList(nativeSystem, listener, listener.byteIoBuffer, listener.charIoBuffer);
    }

    public void setServerListUpdateOptions(boolean showEmptyServers, boolean showPlayerInfo) {
        nativeSetServerListUpdateOptions(nativeSystem, showEmptyServers, showPlayerInfo);
    }

    public void stopUpdatingServerList() {
        nativeStopUpdatingServerList(nativeSystem);
    }
}
