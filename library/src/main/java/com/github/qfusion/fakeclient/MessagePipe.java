package com.github.qfusion.fakeclient;

/**
 * A common interface for sending messages across thread or component boundaries.
 */
public interface MessagePipe {
    /**
     * Posts a message to other thread or component.
     * @param runnable A {@link Runnable} that tells the message (or executes an action)
     *                 having been delivered to the target thread or component.
     */
    void post(Runnable runnable);
}
