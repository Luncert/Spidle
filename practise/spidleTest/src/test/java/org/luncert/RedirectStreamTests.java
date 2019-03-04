package org.luncert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RedirectStreamTests {

    @Test
    public void testReadAndWrite() throws IOException
    {
        RedirectStream rs = new RedirectStream();
        
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(rs.writePoint()));
        writer.write("Hello, ");
        writer.write("It's me");
        writer.close(); // or: writer.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(rs.readPoint()));
        System.out.println(reader.readLine());
        reader.close();
    }

    /**
     * 当规律输出：
     * <li>{@code < 0}
     * <li>{@code > 0}
     * <li>{@code < 1}
     * <li>{@code > 1}
     * <li>{@code ...}
     * <li>时，说明condition起作用了
     */
    @Test
    public void testMultiThreadsRW() throws InterruptedException
    {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        RedirectStream rs1 = new RedirectStream(true),
                        rs2 = new RedirectStream(true);
        rs1.redirect(rs2.writePoint());

        threadPool.submit(() -> {
            OutputStream writer = rs1.writePoint();

            for (int i = 0; i < 10; i++) {
                try {
                    writer.write(i + 48);
                    System.out.println("< " + i);
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        threadPool.submit(() -> {
            InputStream reader = rs2.readPoint();
            int c;
            try {
                while ((c = reader.read()) != -1) {
                    System.out.println("> " + (char) c);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}