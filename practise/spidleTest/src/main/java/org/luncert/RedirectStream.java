package org.luncert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class RedirectStream {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private boolean closed = false;
    private byte[] buf;
    private int count;
    private int pos;
    private Map<String, Consumer<Integer>> dataInputListener = new HashMap<>();

    public RedirectStream() {
        buf = new byte[32];
    }

    public OutputStream writePoint() {
        return new ByteOutputStream();
    }

    public InputStream readPoint() {
        return new ByteInputStream();
    }

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0)
            grow(minCapacity);
    }

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        buf = Arrays.copyOf(buf, newCapacity);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }

    private class ByteOutputStream extends OutputStream {

        public void write(int b) throws IOException {
            lock.lock();
            if (closed) {
                lock.unlock();
                throw new IOException("write on closed stream");
            }

            ensureCapacity(count + 1);
            buf[count] = (byte) b;
            count += 1;
            
            condition.notifyAll();
            lock.unlock();

            for (Consumer<Integer> consumer : dataInputListener.values())
                consumer.accept(b);
        }
        
        public void close() throws IOException {
            lock.lock();
            closed = true;
            lock.unlock();
        }

    }

    private class ByteInputStream extends InputStream {

        public int read() throws IOException {
            int ret = -1;

            lock.lock();
            if (!closed) {
                if (pos == count) {
                    while (true) {
                        try {
                            condition.await();
                        } catch (InterruptedException e) {
                            // if interruoted, continue to await
                        }
                    }
                }
                // assert pos < count;
                ret = buf[pos++] & 0xff;
            }
            lock.unlock();

            return ret;
        }
        
        public void close() throws IOException {
            lock.lock();
            closed = true;
            lock.unlock();
        }

    }

    /**
     * 监听数据流入
     */
    public void attachDataInput(String name, Consumer<Integer> consumer) {
        dataInputListener.put(name, consumer);
    }

    public void detachDataInput(String name) {
        dataInputListener.remove(name);
    }

}