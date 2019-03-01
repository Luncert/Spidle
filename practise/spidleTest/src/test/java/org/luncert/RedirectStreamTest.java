package org.luncert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RedirectStreamTest {

    @Test
    public void test() throws IOException
    {
        RedirectStream rs = new RedirectStream();
        
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(rs.writePoint()));
        writer.write("Hello, ");
        writer.write("It's me");
        writer.flush();

        BufferedReader reader = new BufferedReader(new InputStreamReader(rs.readPoint()));
        System.out.println(reader.readLine());
    }

}