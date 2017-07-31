package com.github.qfusion.fakeclient;

import android.os.SystemClock;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClientTest {

    @Before
    public void setUp() {
        System.init(new DummyConsole());
    }

    @After
    public void tearDown() {
        System.shutdown();
    }

    @Test
    public void testNewDelete() {
        System system = System.getInstance();
        Client client = system.newClient(new DummyConsole());
        system.deleteClient(client);
    }

    @Test
    public void testSettingValidListener() {
        System system = System.getInstance();
        Client client = system.newClient(new DummyConsole());
        try {
            client.setListener(new DummyClientListener());
        } finally {
            system.deleteClient(client);
        }
    }

    @Test
    public void testSettingNullListener() {
        System system = System.getInstance();
        Client client = system.newClient(new DummyConsole());
        try {
            client.setListener(null);
        } finally {
            system.deleteClient(client);
        }
    }

    @Test
    public void testSettingValidListenerInPlaceOfOld() {
        System system = System.getInstance();
        Client client = system.newClient(new DummyConsole());
        try {
            client.setListener(new DummyClientListener());
            client.setListener(new DummyClientListener());
        } finally {
            system.deleteClient(client);
        }
    }

    @Test
    public void testSettingNullListenerInPlaceOfOld() {
        System system = System.getInstance();
        Client client = system.newClient(new DummyConsole());
        try {
            client.setListener(new DummyClientListener());
            client.setListener(null);
        } finally {
            system.deleteClient(client);
        }
    }

    @Test
    public void testListenerCalls() {
        System system = System.getInstance();
        Client client = system.newClient(new DummyConsole());
        DummyClientListener listener = new DummyClientListener();
        try {
            client.setListener(listener);
            client.executeCommand("test_listener");
        } finally {
            system.deleteClient(client);
        }

        assertEquals(6, listener.getEvents().size());

        List<GenericSingleStringEvent> singleStringEvents = listener.filterEventsByClass(GenericSingleStringEvent.class);
        assertEquals(3, singleStringEvents.size());
        assertEquals("Player", singleStringEvents.get(0).message);
        assertEquals("Message of the day", singleStringEvents.get(1).message);
        assertEquals("King of Bongo!", singleStringEvents.get(2).message);

        List<GenericChatEvent> chatEvents = listener.filterEventsByClass(GenericChatEvent.class);
        assertEquals(3, chatEvents.size());
        for (int i = 0; i < 3; ++i) {
            GenericChatEvent event = chatEvents.get(i);
            assertEquals("Player(1)", event.from);
            assertEquals("Hello, world!", event.message);
        }
    }

    @Test
    public void testIllegalCommand() {
        System system = System.getInstance();
        DummyConsole console = new DummyConsole();
        assertEquals(0, console.getLines().size());
        Client client = system.newClient(console);
        try {
            client.executeCommand("foobar");
            for (CharSequence line: console.getLines()) {
                Log.w(getClass().getCanonicalName(), line.toString());
            }
            assertEquals(1, console.getLines().size());
            String consoleLine = console.getLines().get(0).toString();
            assertEquals(true, consoleLine.contains("unknown command"));
            assertEquals(true, consoleLine.contains("foobar"));
        } finally {
            system.deleteClient(client);
        }
    }

    @Test
    public void testConnectingToNonExistingServer() {
        final System system = System.getInstance();
        final DummyConsole console = new DummyConsole();
        assertEquals(0, console.getLines().size());
        final Client client = system.newClient(console);
        try {
            // We have to spawn a separate thread since we access a network on Android in this case
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // We are sure nobody runs a Qfusion server on the device
                    client.executeCommand("connect 127.0.0.1");
                    for (int i = 0; i < 10; ++i) {
                        system.frame(16);
                        SystemClock.sleep(128);
                    }
                }
            });
            thread.run();

            try {
                thread.join();
            } catch (InterruptedException e) {
                Log.e(getClass().getCanonicalName(), e.getMessage(), e);
            }

            List<CharSequence> consoleLines = console.getLines();
            assertEquals(true, consoleLines.size() > 0);
            for (CharSequence line : consoleLines) {
                assertEquals("Requesting challenge...", line.toString());
            }
        } finally {
            system.deleteClient(client);
        }
    }
}

class GenericSingleStringEvent {
    String message;
}

class PlayerNameEvent extends GenericSingleStringEvent {}

class MotdEvent extends GenericSingleStringEvent {}

class CenteredMessageEvent extends GenericSingleStringEvent {}

class GenericChatEvent {
    String from;
    String message;
}

class ChatEvent extends GenericChatEvent {}

class TeamChatEvent extends GenericChatEvent {}

class TVChatEvent extends GenericChatEvent {}

class DummyClientListener extends NativeBridgeClientListener {
    private List<Object> events = new ArrayList<Object>();

    public List<Object> getEvents() { return events; }

    public <T> List<T> filterEventsByClass(Class<T> eventClass) {
        List<T> result = new ArrayList<T>();
        for (Object event: events) {
            if (eventClass.isAssignableFrom(event.getClass())) {
                result.add(eventClass.cast(event));
            }
        }
        return result;
    }

    // CBA to write constructors
    private void addGenericSingleStringEvent(Class<? extends GenericSingleStringEvent> eventClass, String message) {
        try {
            GenericSingleStringEvent event = eventClass.newInstance();
            event.message = message;
            events.add(event);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void addGenericChatEvent(Class<? extends GenericChatEvent> eventClass, String from, String message) {
        try {
            GenericChatEvent event = eventClass.newInstance();
            event.from = from;
            event.message = message;
            events.add(event);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void onShownPlayerNameSet(String name) {
        addGenericSingleStringEvent(PlayerNameEvent.class, name);
    }

    @Override
    public void onMessageOfTheDaySet(String motd) {
        addGenericSingleStringEvent(MotdEvent.class, motd);
    }

    @Override
    public void onCenteredMessage(String message) {
        addGenericSingleStringEvent(CenteredMessageEvent.class, message);
    }

    @Override
    public void onChatMessage(String from, String message) {
        addGenericChatEvent(ChatEvent.class, from, message);
    }

    @Override
    public void onTeamChatMessage(String from, String message) {
        addGenericChatEvent(TeamChatEvent.class, from, message);
    }

    @Override
    public void onTVChatMessage(String from, String message) {
        addGenericChatEvent(TVChatEvent.class, from, message);
    }
}

