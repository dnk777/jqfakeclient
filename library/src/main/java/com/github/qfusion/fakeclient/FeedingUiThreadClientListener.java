package com.github.qfusion.fakeclient;

public class FeedingUiThreadClientListener extends NativeBridgeClientListener {
    protected MessagePipe uiThreadPipe;
    protected ClientListener uiThreadListener;

    public final ClientListener getUiThreadListener() {
        return uiThreadListener;
    }

    public final void setUiThreadListener(ClientListener listener) {
        this.uiThreadListener = listener;
    }

    public FeedingUiThreadClientListener(MessagePipe uiThreadPipe, ClientListener uiThreadListener) {
        this.uiThreadPipe = uiThreadPipe;
        this.uiThreadListener = uiThreadListener;
    }

    @Override
    public void onShownPlayerNameSet(final String name) {
        uiThreadPipe.post(new Runnable() {
            public void run() {
                uiThreadListener.onShownPlayerNameSet(name);
            }
        });
    }

    @Override
    public void onMessageOfTheDaySet(final String motd) {
        uiThreadPipe.post(new Runnable() {
            public void run() {
                uiThreadListener.onMessageOfTheDaySet(motd);
            }
        });
    }

    @Override
    public void onCenteredMessage(final String message) {
        uiThreadPipe.post(new Runnable() {
            public void run() {
                uiThreadListener.onCenteredMessage(message);
            }
        });
    }

    @Override
    public void onChatMessage(final String from, final String message) {
        uiThreadPipe.post(new Runnable() {
            public void run() {
                uiThreadListener.onChatMessage(from, message);
            }
        });
    }

    @Override
    public void onTeamChatMessage(final String from, final String message) {
        uiThreadPipe.post(new Runnable() {
            public void run() {
                uiThreadListener.onTeamChatMessage(from, message);
            }
        });
    }

    @Override
    public void onTVChatMessage(final String from, final String message) {
        uiThreadPipe.post(new Runnable() {
            public void run() {
                uiThreadListener.onTVChatMessage(from, message);
            }
        });
    }
}



