package org.luncert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.luncert.Topology.Bolt;
import org.python.util.PythonInterpreter;

public class TopologyContext {

    private ExecutorService threadPool;
    private Map<String, BoltContext> boltCtxs = new HashMap<>();

    TopologyContext(ExecutorService threadPool, Topology topology) {
        this.threadPool = threadPool;
        for (Bolt bolt : topology.getBolts()) {
            BoltContext boltCtx = new BoltContext(bolt);
            boltCtxs.put(bolt.getName(), boltCtx);

            // 在所有前置任务的输出流上监听数据输出，然后将数据输出作为本任务的数据输入
            for (Bolt successor : bolt.getSuccessors()) {
                String successorName = successor.getName();
                BoltContext sBoltCtx = boltCtxs.get(successorName);
                if (sBoltCtx == null) {
                    sBoltCtx = new BoltContext(bolt);
                    boltCtxs.put(successorName, sBoltCtx);
                }
                boltCtx.getBoltOutput().attachDataInput(successorName, (i) -> {
                    try {
                        boltCtxs.get(successorName).getBoltInput().writePoint().write(i);
                    } catch (IOException e) {
                        // TODO:
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private class BoltContext
    {
        Bolt bolt;
        PythonInterpreter interpreter = new PythonInterpreter();
        RedirectStream boltIn = new RedirectStream(), boltOut = new RedirectStream();

        BoltContext(Bolt bolt) {
            this.bolt = bolt;
            interpreter.setIn(boltIn.readPoint());
            interpreter.setOut(boltOut.writePoint());
        }

        public void startBolt() {
            threadPool.submit(() -> {
                interpreter.exec(bolt.getScripts());
            });
        }

        public RedirectStream getBoltInput() {
            return boltIn;
        }

        public RedirectStream getBoltOutput() {
            return boltOut;
        }

        @Override
        public void finalize() {
            interpreter.close();
        }

    }

}