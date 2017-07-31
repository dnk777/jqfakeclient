package com.github.qfusion.fakeclient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Use this class instead of {@link android.util.Log} to allow several tests to be run in standard JVM environment
 */
public class LogProxy {

    public static void w(String tag, String msg) {
        getImpl().w(tag, msg);
    }

    private static Impl impl;
    private static Impl getImpl() {
        if (impl == null) {
            impl = newImpl();
        }
        return impl;
    }

    private static Impl newImpl() {
        return Testing.isActive() ? DummyImpl.INSTANCE : RealImpl.INSTANCE;
    }

    private static abstract class Impl {
        protected abstract void w(String tag, String msg);
    }

    private static class DummyImpl extends Impl {
        @Override
        protected void w(String tag, String msg) {
            java.lang.System.out.println("WARNING: " + tag + " : " + msg);
        }

        static Impl INSTANCE = new DummyImpl();
    }

    /**
     * Reflected calls are nasty but if any of methods provided by the class
     * are user there are more significant problems in the code.
     */
    private static class RealImpl extends Impl {
        @Override
        protected void w(String tag, String msg) {
            try {
                method.invoke(null, tag, msg);
            } catch (InvocationTargetException e) {
                throw new AssertionError(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        static Impl INSTANCE;
        private static Class<?> clazz;
        private static Method method;

        static {
            try {
                clazz = Class.forName("android.util.Log");
                method = clazz.getMethod("w", String.class, String.class);
                INSTANCE = new RealImpl();
            } catch (ClassNotFoundException e) {
                INSTANCE = DummyImpl.INSTANCE;
            } catch (NoSuchMethodException e) {
                INSTANCE = DummyImpl.INSTANCE;
            }
        }

    }
}

class Testing {
    private static boolean hasCheckedWhetherActive = false;
    private static boolean isActive = false;

    static boolean isActive() {
        if (!hasCheckedWhetherActive) {
            isActive = checkWhetherActive();
            hasCheckedWhetherActive = true;
        }
        return isActive;
    }

    private static boolean checkWhetherActive() {
        try {
            Class.forName("junit.framework.TestCase");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
