package org.luncert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.luncert.Topology.Bolt;
import org.python.util.PythonInterpreter;

/**
 * 流处理服务，将输出数据按时间片分组，每产生一个dataset就会启动相关的bolt去消费， 这样的话一个任务就不会要等到前置任务结束才启动
 */
public class StreamingService {

    private static final long INTERVAL = 100; // ms

    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(50);
    private ConcurrentMap<String, Topology> topologies = new ConcurrentHashMap<>();

    public StreamingService() {
        String pythonHome = System.getenv().get("PYTHON_HOME");
        if (pythonHome == null) {
            throw new RuntimeException("Env variable PYTHONE_HOME is not defined");
        }

        Properties props = new Properties();
        props.put("python.home", pythonHome);
        props.put("python.console.encoding", "UTF-8");
        props.put("python.security.respectJavaAccessibility", "false");
        props.put("python.import.site", "false");

        PythonInterpreter.initialize(System.getProperties(), props, new String[0]);
    }

    /**
     * 提交任务拓扑，topology name重复时抛出异常
     */
    public void submit(Topology topology) throws Exception {
        if (topologies.containsKey(topology.getName()))
            throw new Exception("topology name existed: " + topology.getName());
        topologies.put(topology.getName(), topology);
    }

    public void start(String topologyName) throws Exception {
        Topology topology = topologies.get(topologyName);
        if (topology == null)
            throw new Exception("invalid topology name: " + topologyName);
        for (Bolt bolt : topology.getBolts())
            startBolt(bolt, null);
    }

    private void startBolt(Bolt bolt, final byte[] input) {
        threadPool.submit(() -> {
            try (PythonInterpreter interpreter = new PythonInterpreter()) {
                // 输入
                if (input != null) {
                    interpreter.setIn(new ByteArrayInputStream(input));
                }

                // 重定向标准输出流
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                interpreter.setOut(bos);

                interpreter.exec(bolt.getScripts());

                // 启动successors，将输出传递作为输入
                List<Bolt> successors = bolt.getSuccessors();
                if (!successors.isEmpty()) {
                    // 创建一个定时任务，消费任务输出
                    scheduledThreadPool.schedule(() -> {
                        byte[] output = bos.toByteArray();
                        System.out.println(System.currentTimeMillis());
                        System.out.println(new String(output));
                        if (output.length > 0) {
                            for (Bolt sucessor : successors) {
                                startBolt(sucessor, output);
                            }
                            // 任务输出还没有消费完，再创建一个定时任务
                            scheduledThreadPool.schedule(() -> {
                                byte[] output1 = bos.toByteArray();
                                System.out.println(new String(output1));
                                if (output.length > 0) {
                                    for (Bolt sucessor : successors) {
                                        startBolt(sucessor, output1);
                                    }
                                }
                            }, INTERVAL, TimeUnit.MILLISECONDS);
                        }
                    }, INTERVAL, TimeUnit.MILLISECONDS);
                } else System.out.println("Task Finished: " + new String(bos.toByteArray()));
            } catch (Exception e) {
                // TODO: log
                e.printStackTrace();
            }
        });
    }

    public void stop(String topologyName) {
        // TODO:
    }

    public void forceStop(String topologyName) {
        // TODO:
    }

    public void shutdown() throws InterruptedException {
        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        scheduledThreadPool.shutdown();
        scheduledThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

}