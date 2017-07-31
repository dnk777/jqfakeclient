package com.github.qfusion.fakeclient;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SystemTest {

    // Just do these calls and expect that they don't fail

    @Test
    public void testInitShutdownPairs() {
        DummyConsole console = new DummyConsole();
        System.init(console);
        System.shutdown();

        System.init(console);
        System.shutdown();

        System.init(console);
        System.shutdown();
    }

    @Test
    public void testStackedInitShutdownCalls() {
        DummyConsole console = new DummyConsole();
        System.init(console);
        System.init(console);
        System.init(console);

        System.shutdown();
        System.shutdown();
        System.shutdown();
    }

    @Test
    public void testUnmatchedInitShutdownCalls() {
        DummyConsole console = new DummyConsole();
        try {
            System.shutdown();
            System.shutdown();

            System.init(console);
            System.init(console);
        } finally {
            System.shutdown();
        }
    }

    @Test
    public void testGetSystemInstance() {
        System.init(new DummyConsole());
        try {
            System system1 = System.getInstance();
            System system2 = System.getInstance();

            assertSame(system1, system2);
        } finally {
            System.shutdown();
        }
    }

    @Test
    public void testSystemFrame() {
        System.init(new DummyConsole());
        try {
            System system = System.getInstance();
            system.frame(256);
            system.frame(256);
            system.frame(256);
        } finally {
            System.shutdown();
        }
    }
}
