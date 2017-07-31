package com.github.qfusion.fakeclient;

/**
 * Notifies an associated {@link Client} of several in-game events.
 */
public interface ClientListener {
    void onShownPlayerNameSet(String name);
    void onMessageOfTheDaySet(String motd);
    void onCenteredMessage(String message);
    void onChatMessage(String from, String message);
    void onTeamChatMessage(String from, String message);
    void onTVChatMessage(String from, String message);
}
