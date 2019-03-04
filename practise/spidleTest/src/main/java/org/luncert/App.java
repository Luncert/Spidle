package org.luncert;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.luncert.Topology.Bolt;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        List<Bolt> bolts = new ArrayList<>();
        Bolt bolt = new Bolt(false, new String(read(new FileInputStream("C:\\Users\\i507145\\Desktop\\Project\\test\\spidleTest\\producer.py"))));
        bolt.before(new Bolt(false, new String(read(new FileInputStream("C:\\Users\\i507145\\Desktop\\Project\\test\\spidleTest\\consumer.py")))));
        bolts.add(bolt);
        Topology topology = new Topology("test", bolts);

        StreamingService streamingService = new StreamingService();
        streamingService.submit(topology);
        streamingService.start("test");
        streamingService.shutdown();
    }
    
    public static byte[] read(InputStream inputStream) throws IOException {
		BufferedInputStream buffer = null;
		DataInputStream dataIn = null;
		ByteArrayOutputStream bos = null;
		DataOutputStream dos = null;
		byte[] bArray = null;
		try {
			buffer = new BufferedInputStream(inputStream);
			dataIn = new DataInputStream(buffer);
			bos = new ByteArrayOutputStream();
			dos = new DataOutputStream(bos);
			byte[] buf = new byte[1024];
			while (true) {
				int len = dataIn.read(buf);
				if (len < 0)
					break;
				dos.write(buf, 0, len);
			}
            bArray = bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (dataIn != null) dataIn.close();
			if (buffer != null) buffer.close();
			if (bos != null) bos.close();
			if (dos != null) dos.close();
		}
		return bArray;
    }

}
