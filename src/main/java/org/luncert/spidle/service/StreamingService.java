package org.luncert.spidle.service;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.luncert.spidle.service.Topology.Bolt;
import org.python.util.PythonInterpreter;
import org.springframework.stereotype.Service;

@Service
public class StreamingService
{

    private static final long INTERVAL = 100; // ms

    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private ConcurrentMap<String, Topology> topologies = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {    
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
    public void submit(Topology topology) throws Exception
    {
        if (topologies.containsKey(topology.getName()))
            throw new Exception("topology name existed: " + topology.getName());
        topologies.put(topology.getName(), topology);
    }

    public void start(String topologyName) throws Exception
    {
        Topology topology = topologies.get(topologyName);
        if (topology == null)
            throw new Exception("invalid topology name: " + topologyName);
        for (Bolt bolt : topology.getBolts()) {
            
        }
    }

    private static class BoltContext
    {

    }

    public void stop(String topologyName)
    {

    }

    public void forceStop(String topologyName)
    {

    }


}