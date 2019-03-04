package org.luncert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RedirectStream {

    private boolean closed = false;
    private boolean blocking;
    byte[] buf;
    private int count;
    private int pos;

    public RedirectStream() {
        this(false);
    }

    public RedirectStream(boolean blocking) {
        this.blocking = blocking;
        buf = new byte[32];
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

    // interface

    public void write(int b) throws IOException
    {
        synchronized(this) {
            if (closed)
                throw new IOException("write on closed stream");
    
            ensureCapacity(count + 1);
            buf[count] = (byte) b;
            count += 1;
            
            notifyAll();
        }

        // 为了保证线程安全，直接获取一个数组而不是一个可迭代对象
        for (Object rs : redirects.values().toArray()) {
            ((OutputStream) rs).write(b);
        }
    }

    /**
     * @return 没有新数据可读时，且非阻塞时或者允许阻塞但stream已关闭时，
     * 直接返回-1
     */

    public int read() throws IOException
    {
        int ret = -1;
        synchronized(this) {
            // pos == count代表当前无新数据可读
            if (pos == count) {
                // 允许阻塞时，才去等待condition，在不允许阻塞的情况下直接返回ret（即-1）
                if (blocking) {
                    while (!closed && pos == count) {
                        try {
                            wait();
                            break;
                        } catch (InterruptedException e) {
                            // 等待被中断，重新等待
                        }
                    }
                    // 有可能reader在writer关闭stream之前进入等待，所以，writer在关闭stream后要调用condition.signalAll唤醒所有在阻塞队列中的线程。而这时可能仍然没有新数据可读，所以这里要有pos < count的判断
                    if (pos < count) {
                        ret = buf[pos++] & 0xff;
                    }
                }
            }
            else ret = buf[pos++] & 0xff;
        }
        return ret;
    }

    public void close() throws IOException {
        if (!closed) {
            synchronized(this) {
                if (!closed) {
                    closed = true;
                    notifyAll();
                }
            }
            // 为了保证线程安全，直接获取一个数组而不是一个可迭代对象
            for (Object rs : redirects.values().toArray()) {
                ((OutputStream) rs).close();
            }
        }

    }

    /**
     * 获取一个OutputStream用于写入数据
     */
    public OutputStream writePoint() {
        return new ByteOutputStream();
    }

    /**
     * 获取一个InputStream用于读出数据
     */
    public InputStream readPoint() {
        return new ByteInputStream();
    }

    private class ByteOutputStream extends OutputStream {

        public void write(int b) throws IOException {
            RedirectStream.this.write(b);
        }
        
        public void close() throws IOException {
            RedirectStream.this.close();
        }

    }

    private class ByteInputStream extends InputStream {
        public int read() throws IOException {
            return RedirectStream.this.read();
        }
        
        public void close() throws IOException {
            RedirectStream.this.close();
        }

    }

    // redirect

    private ConcurrentMap<Integer, OutputStream> redirects = new ConcurrentHashMap<>();

    /**
     * 创建一个channel来重定向输出到另一个stream，
     * 在该channel上的数据流动不会影响其他数据流，
     * 该操作本质上就是：向RedirectStream写入数据的同时，将数据写入另一个流中
     */
    public Channel redirect(OutputStream os) {
        int id = os.hashCode();
        if (redirects.containsKey(id))
            throw new RuntimeException("duplicate redirect to the same stream");
        
        redirects.put(id, os);
        return new Channel(id);
    }

    public class Channel
    {
        private int id;
        private boolean closed = false;

        private Channel(int id) {
            this.id = id;
        }

        /**
         * 关闭重定向通道
         */
        public void close() {
            if (!closed) {
                redirects.remove(id);
                closed = true;
            }
        }
    }

}