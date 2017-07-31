package com.github.qfusion.fakeclient;

import java.nio.charset.Charset;

/**
 * Represents a fake game client (a player that connects to a server).
 * A client has an internal connection state.
 * A client can be connected to a single server at some moment of time,
 * or can be disconnected from any server at all.
 * Clients lifecycle should be managed by
 * {@link System#newClient(NativeBridgeConsole)}, {@link System#deleteClient} calls.
 */
public class Client {
    static {
        java.lang.System.loadLibrary("jqfakeclient");
    }

    long nativeClient;
    private NativeBridgeConsole console;

    private Charset charset = Charset.forName("UTF-8");

    Client(long nativeClient, NativeBridgeConsole console) {
        this.nativeClient = nativeClient;
        this.console = console;
    }

    private static native void nativeSetListener(long nativeClient, NativeBridgeClientListener listener);
    private static native void nativeExecuteCommand(long nativeClient, byte[] commandUtf8Bytes);

    /**
     * Sets a {@link ClientListener} for the client.
     * Repeated calls are allowed, old listeners are deleted in this case.
     * @param listener A {@link NativeBridgeClientListener} for the native client object.
     *                 A null listener is allowed.
     */
    public void setListener(NativeBridgeClientListener listener) {
        nativeSetListener(nativeClient, listener);
    }

    /**
     * Executes a client command typed by a user (a player) in a console.
     */
    public void executeCommand(String command) {
        nativeExecuteCommand(nativeClient, command.getBytes(charset));
    }
}
