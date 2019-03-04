package org.luncert;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.luncert.Topology.Bolt;
import org.python.util.PythonInterpreter;

public class StreamingService {

    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private ConcurrentMap<String, Topology> topologies = new ConcurrentHashMap<>();
    private ConcurrentMap<String, BoltContext> boltCtxs = new ConcurrentHashMap<>();

    public StreamingService() {
        // 初始化Jython解释器
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

    /**
     * 关于数据流动：
     * 一个bolt往rsOut中print数据，由于redirect操作这些数据会被及时写入到
     * 另一个bolt的rsIn中，然后由那个bolt主动从rsIn中读出数据
     */
    public void start(String topologyName) throws Exception {
        Topology topology = topologies.get(topologyName);
        if (topology == null) {
            throw new Exception("invalid topology name: " + topologyName);
        }
        // 在启动之前，先要初始化每个bolt的输入输出流，并完成关联的bolt的数据流绑定
        initTopology(topology);
        // 启动
        startTopology(topology);
    }

    private void initTopology(Topology topology)
    {
        for (Bolt bolt : topology.getBolts()) {
            BoltContext boltCtx = new BoltContext(bolt);
            boltCtxs.put(bolt.getName(), boltCtx);
            
            for (Bolt successor : bolt.getSuccessors())
                initSuccessor(successor, boltCtx);
        }
    }

    private void initSuccessor(Bolt bolt, BoltContext preBoltCtx)
    {
        BoltContext boltCtx = new BoltContext(bolt);
        boltCtxs.put(bolt.getName(), boltCtx);

        preBoltCtx.rsOut.redirect(boltCtx.rsIn.writePoint());

        for (Bolt successor : bolt.getSuccessors())
            initSuccessor(successor, boltCtx);
    }

    private void startTopology(Topology topology)
    {
        for (Bolt headBolt : topology.getBolts()) {
            startBolt(headBolt);
        }
    }

    /**
     * 启动bolt：启动Jython解释器执行脚本
     */
    private void startBolt(Bolt bolt)
    {
        BoltContext boltCtx = boltCtxs.get(bolt.getName());
        threadPool.submit(() -> {
            try (PythonInterpreter interpreter = new PythonInterpreter()) {
                interpreter.setIn(boltCtx.rsIn.readPoint());
                interpreter.setOut(boltCtx.rsOut.writePoint());

                interpreter.exec(bolt.getScripts());

                boltCtx.rsIn.close();
                boltCtx.rsOut.close();
                
                boltCtxs.remove(bolt.getName());
            } catch (Exception e) {
                // TODO: log
                e.printStackTrace();
            }
        });
        // 在shutdown()执行后，老的任务会继续处理而不允许在提交新的任务。
        for (Bolt successor : bolt.getSuccessors()) {
            startBolt(successor);
        }
    }

    private static class BoltContext
    {
        // Bolt bolt;
        RedirectStream rsIn, rsOut;
        BoltContext(Bolt bolt) {
            // this.bolt = bolt;
            rsIn = new RedirectStream(true);
            rsOut = new RedirectStream(true);
        }
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
    }

}