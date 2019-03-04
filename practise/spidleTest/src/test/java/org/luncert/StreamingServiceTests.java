package org.luncert;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.luncert.Topology.Bolt;

@RunWith(JUnit4.class)
public class StreamingServiceTests {

    @Test
    public void test() throws Exception
    {
        List<Bolt> bolts = new ArrayList<>();
        Bolt bolt = new Bolt(false, new String(App.read(new FileInputStream("C:\\Users\\i507145\\Desktop\\Project\\Spidle\\practise\\spidleTest\\producer.py"))));
        bolt.before(new Bolt(false, new String(App.read(new FileInputStream("C:\\Users\\i507145\\Desktop\\Project\\Spidle\\practise\\spidleTest\\consumer.py")))));
        bolts.add(bolt);
        Topology topology = new Topology("test", bolts);

        StreamingService streamingService = new StreamingService();
        streamingService.submit(topology);
        streamingService.start("test");

        streamingService.shutdown();
    }

}