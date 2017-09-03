package com.github.qfusion.fakeclient;

public interface ServerListListener {
    void onServerAdded(int instanceId);

    void onServerUpdated(int instanceId, int serverInfoUpdateMask);

    void onServerRemoved(int instanceId);
}
