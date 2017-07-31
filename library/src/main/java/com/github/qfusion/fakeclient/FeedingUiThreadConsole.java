package com.github.qfusion.fakeclient;

import android.os.Looper;

public class FeedingUiThreadConsole extends RingBufferConsole {

    MessagePipe uiThreadPipe;

    public FeedingUiThreadConsole(MessagePipe uiThreadPipe, int capacity) {
        super(capacity);
        this.uiThreadPipe = uiThreadPipe;
    }

    private void expectUiThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new AssertionError("This call is expected to be done in an UI thread");
        }
    }

    public class FeedingUiThreadIngoingBytesProcessor extends IngoingBytesProcessor {

        public FeedingUiThreadIngoingBytesProcessor(RingLinesBuffer buffer) {
            super(buffer);
        }

        @Override
        protected byte[] newBufferArray(int length) {
            return new byte[length];
        }

        @Override
        protected void onNewBufferData(byte[] bytes, int offset, int length) {
            uiThreadPipe.post(new UiThreadNewBufferDataCaller(bytes, offset, length));
        }

        private void uiThreadOnNewBufferData(byte[] bytes, int offset, int length) {
            if (BuildConfig.DEBUG) {
                expectUiThread();
            }
            super.onNewBufferData(bytes, offset, length);
        }

        @Override
        protected void appendLinePart(String bufferString, int offset, int length) {
            if (BuildConfig.DEBUG) {
                expectUiThread();
            }
            super.appendLinePart(bufferString, offset, length);
        }

        @Override
        protected void completeLineBuilding() {
            if (BuildConfig.DEBUG) {
                expectUiThread();
            }
            super.completeLineBuilding();
        }

        @Override
        protected void notifyOfNewLine(boolean backLineRemoved) {
            if (BuildConfig.DEBUG) {
                expectUiThread();
            }
            super.notifyOfNewLine(backLineRemoved);
        }

        class UiThreadNewBufferDataCaller implements Runnable {
            byte[] bytes;
            int offset;
            int length;

            UiThreadNewBufferDataCaller(byte[] bytes, int offset, int length) {
                this.bytes = bytes;
                this.offset = offset;
                this.length = length;
            }

            @Override public void run() {
                FeedingUiThreadIngoingBytesProcessor.this.uiThreadOnNewBufferData(bytes, offset, length);
            }
        }
    }

    @Override
    protected AbstractIngoingBytesProcessor newIngoingBytesProcessor() {
        return new FeedingUiThreadIngoingBytesProcessor(buffer);
    }
}
