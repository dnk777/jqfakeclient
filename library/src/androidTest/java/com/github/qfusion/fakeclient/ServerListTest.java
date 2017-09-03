package com.github.qfusion.fakeclient;

import static com.github.qfusion.fakeclient.ScoreboardData.*;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ServerListTest {
    private InetAddress ipV4Address;
    private InetAddress ipV6Address;

    private static final short PORT1 = 1337;
    private static final short PORT2 = 1338;

    @Before
    public void setUp() throws Exception {
        System.init(new DummyConsole());
        ipV4Address = Inet4Address.getByName("127.0.0.1");
        assertTrue(ipV4Address instanceof Inet4Address);
        ipV6Address = Inet6Address.getByName("::1");
        assertTrue(ipV6Address instanceof Inet6Address);
    }

    @After
    public void tearDown() {
        System.shutdown();
    }

    @Test
    public void testAddMasterServerAddress() throws Exception {
        System system = System.getInstance();
        // The underlying native code should be capable to store at least 4 master server addresses
        assertTrue(system.addMasterServer(ipV4Address, PORT1));
        assertFalse(system.addMasterServer(ipV4Address, PORT1));

        assertTrue(system.addMasterServer(ipV6Address, PORT1));
        assertFalse(system.addMasterServer(ipV6Address, PORT1));

        assertTrue(system.addMasterServer(ipV4Address, PORT2));
        assertTrue(system.addMasterServer(ipV6Address, PORT2));
    }

    @Test
    public void testRemoveMasterServerAddress() {
        System system = System.getInstance();
        assertTrue(system.addMasterServer(ipV4Address, PORT1));
        assertFalse(system.removeMasterServer(ipV4Address, PORT2));
        assertTrue(system.removeMasterServer(ipV4Address, PORT1));
        assertFalse(system.removeMasterServer(ipV4Address, PORT1));

        assertTrue(system.addMasterServer(ipV6Address, PORT1));
        assertFalse(system.removeMasterServer(ipV6Address, PORT2));
        assertTrue(system.removeMasterServer(ipV6Address, PORT1));
        assertFalse(system.removeMasterServer(ipV6Address, PORT1));
    }

    @Test
    public void testIsMasterServerAddress() {
        System system = System.getInstance();

        assertTrue(system.addMasterServer(ipV4Address, PORT1));
        assertTrue(system.isMasterServer(ipV4Address, PORT1));
        assertFalse(system.isMasterServer(ipV4Address, PORT2));
        system.removeMasterServer(ipV4Address, PORT1);
        assertFalse(system.isMasterServer(ipV4Address, PORT1));

        assertTrue(system.addMasterServer(ipV6Address, PORT1));
        assertTrue(system.isMasterServer(ipV6Address, PORT1));
        assertFalse(system.isMasterServer(ipV6Address, PORT2));
        system.removeMasterServer(ipV6Address, PORT1);
        assertFalse(system.isMasterServer(ipV6Address, PORT1));
    }

    @Test
    public void testSetServerListUpdateOptions() {
        // Just do not fail
        System.getInstance().setServerListUpdateOptions( false, false );
        System.getInstance().setServerListUpdateOptions( false, true );
    }

    private final AtomicBoolean hasPollingFailed = new AtomicBoolean(false);

    @Test
    public void testServerListComplexPolling() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        ConnectivityManager connectivityManager
            = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        boolean hasConnection = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        assertTrue("This test requires a working Internet connection", hasConnection);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System system = System.getInstance();
                try {
                    assertTrue(system.addMasterServer(Inet4Address.getByName("188.226.221.185"), (short)27950));
                    assertTrue(system.addMasterServer(Inet4Address.getByName("92.62.40.72"), (short)27950));
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }

                MessagePipe pipe = new DummyMessagePipe();
                ServerListListenerPipeEndpoint endpoint = new DummyServerListListenerPipeEndpoint();
                NativeBridgeServerListListener listener = new FeedingUiThreadServerListListener(pipe, endpoint);

                system.setServerListUpdateOptions(true, true);

                if (!system.startUpdatingServerList(listener)) {
                    Log.w(getClass().getCanonicalName(), "startUpdatingServerList() call has failed");
                    hasPollingFailed.set(true);
                } else {
                    Log.i(getClass().getCanonicalName(), "This test might take several seconds");
                    for (int i = 0, end = 64; i < end; ++i) {
                        Log.i(getClass().getCanonicalName(), "Testing, frame " + i + "/" + end);
                        // This large sleep period even makes some servers be dropped by timeout.
                        // Its a good thing
                        SystemClock.sleep(512);
                        system.frame(16);
                    }
                }

                system.stopUpdatingServerList();
            }
        });

        thread.run();

        assertFalse(hasPollingFailed.get());
    }
}

class DummyMessagePipe implements MessagePipe {
    @Override
    public void post(Runnable runnable) {
        runnable.run();
    }
}

class DummyServerListListenerPipeEndpoint extends ServerListListenerPipeEndpoint {
    private static final String TAG = DummyServerListListenerPipeEndpoint.class.getName();

    private Map<Integer, ScoreboardData> scoreboardDataMap = new HashMap<Integer, ScoreboardData>();

    @Override
    protected ScoreboardData newScoreboardData(int instanceId) {
        assertFalse(scoreboardDataMap.containsKey(instanceId));
        ScoreboardData scoreboardData = new ScoreboardData();
        scoreboardDataMap.put(instanceId, scoreboardData);
        return scoreboardData;
    }

    @Override
    protected ScoreboardData findScoreboardData(int instanceId) {
        assertNotNull(scoreboardDataMap.get(instanceId));
        return scoreboardDataMap.get(instanceId);
    }

    @Override
    protected void deleteScoreboardData(int instanceId) {
        assertNotNull(scoreboardDataMap.remove(instanceId));
    }

    @Override
    public void onServerAdded(int instanceId) {
        CharSequence serverName = scoreboardDataMap.get(instanceId).getServerName();
        Log.i(TAG, "A server (id=" + instanceId + ", name="  + serverName + ") has been added");
    }

    @Override
    public void onServerUpdated(int instanceId, int serverInfoUpdateMask) {
        ScoreboardData scoreboardData = scoreboardDataMap.get(instanceId);
        CharSequence serverName = scoreboardData.getServerName();
        Log.i(TAG, "A server (id=" + instanceId + ", name=" + serverName + ") has been updated");
        Log.i(TAG, "Server info updates: " + serverInfoUpdateMaskToString(serverInfoUpdateMask));
        if ((serverInfoUpdateMask & UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES) != 0) {
            Log.i(TAG, "Player info updates: " + playerInfoUpdatesMaskToString(scoreboardData));
        }
    }

    @Override
    public void onServerRemoved(int instanceId) {
        Log.i(TAG, "A server (id=" + instanceId + ") has been removed" );
    }

    // We do not use enums not only because they have some overhead,
    // but primary for generation of JNI numeric constants.
    private static class FlagDesc {
        int flag;
        String name;
        FlagDesc(int flag, String name) {
            this.flag = flag;
            this.name = name;
        }
    }

    private static final FlagDesc[] serverInfoFlagsDesc = {
        new FlagDesc(UPDATE_FLAG_ADDRESS, "address"),
        new FlagDesc(UPDATE_FLAG_SERVER_NAME, "server name"),
        new FlagDesc(UPDATE_FLAG_MODNAME, "mod name"),
        new FlagDesc(UPDATE_FLAG_GAMETYPE, "gametype"),
        new FlagDesc(UPDATE_FLAG_MAPNAME, "map name"),
        new FlagDesc(UPDATE_FLAG_TIME_MINUTES, "time minutes"),
        new FlagDesc(UPDATE_FLAG_TIME_SECONDS, "time seconds"),
        new FlagDesc(UPDATE_FLAG_LIMIT_MINUTES, "limit minutes"),
        new FlagDesc(UPDATE_FLAG_LIMIT_SECONDS, "limit seconds"),
        new FlagDesc(UPDATE_FLAG_TIME_FLAGS, "time flags"),
        new FlagDesc(UPDATE_FLAG_ALPHA_NAME, "ALPHA name"),
        new FlagDesc(UPDATE_FLAG_BETA_NAME, "BETA name"),
        new FlagDesc(UPDATE_FLAG_ALPHA_SCORE, "ALPHA score"),
        new FlagDesc(UPDATE_FLAG_BETA_SCORE, "BETA score"),
        new FlagDesc(UPDATE_FLAG_MAX_CLIENTS, "max clients"),
        new FlagDesc(UPDATE_FLAG_NUM_CLIENTS, "num clients"),
        new FlagDesc(UPDATE_FLAG_NUM_BOTS, "num bots"),
        new FlagDesc(UPDATE_FLAG_NEED_PASSWORD, "need password"),
        new FlagDesc(UPDATE_FLAG_WERE_PLAYER_INFO_UPDATES, "player info")
    };

    private static String serverInfoUpdateMaskToString(int serverInfoUpdateFlags) {
        StringBuilder sb = new StringBuilder("{");
        for (FlagDesc flagDesc: serverInfoFlagsDesc) {
            if ((flagDesc.flag & serverInfoUpdateFlags) != 0) {
                sb.append(flagDesc.name).append(',');
            }
        }
        if (serverInfoUpdateFlags != 0) {
            sb.setCharAt(sb.length() - 1, '}');
        } else {
            sb.append('}');
        }
        return sb.toString();
    }

    private static final FlagDesc[] playerInfoFlagsDesc = {
        new FlagDesc(PLAYERINFO_UPDATE_FLAG_PING, "ping"),
        new FlagDesc(PLAYERINFO_UPDATE_FLAG_NAME, "name"),
        new FlagDesc(PLAYERINFO_UPDATE_FLAG_SCORE, "score"),
        new FlagDesc(PLAYERINFO_UPDATE_FLAG_TEAM, "team")
    };

    private static String playerInfoUpdatesMaskToString(ScoreboardData scoreboardData) {
        StringBuilder sb = new StringBuilder("{");
        boolean wereUpdates = false;
        for (int i = 0, end = scoreboardData.getNumClientsValue(); i < end; ++i) {
            int mask = scoreboardData.playersInfoUpdateMask[i];
            if (mask != 0) {
                wereUpdates = true;
                sb.append("name=").append(scoreboardData.getPlayerName(i)).append(",fields={");
                for (FlagDesc flagDesc : playerInfoFlagsDesc) {
                    if ((flagDesc.flag & mask) != 0) {
                        sb.append(flagDesc.name).append(',');
                    }
                }
                sb.setLength(sb.length() - 1);
                sb.append("},");
            }
        }

        if (wereUpdates) {
            sb.setCharAt(sb.length() - 1, '}');
        } else {
            sb.append('}');
        }

        return sb.toString();
    }
}