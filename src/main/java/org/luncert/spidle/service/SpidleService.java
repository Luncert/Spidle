package org.luncert.spidle.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.luncert.spidle.model.mongodb.Task;
import org.luncert.spidle.model.mongodb.Task.ScriptsType;
import org.luncert.spidle.model.mongodb.TaskResult;
import org.luncert.spidle.repository.mongodb.TaskRepos;
import org.luncert.spidle.repository.mongodb.TaskResultRepos;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class SpidleService
{

    private static class TaskContext
    {

        Task task;
        volatile boolean running;

        TaskContext(Task task)
        {
            this.task = task;
            running = false;
        }

    }
    
    private ConcurrentMap<String, TaskContext> tasks = new ConcurrentHashMap<>();
    private ExecutorService threadPool = Executors.newCachedThreadPool();

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

    @PreDestroy
    public void destory() {

    }

    @Autowired
    private TaskRepos taskRepos;

    @Autowired
    private TaskResultRepos taskResultRepos;

    /**
     * @return {@code false} if the task name existed
     */
    public boolean submit(String name, ScriptsType scriptsType, String scripts, List<String> predecessors, boolean persistence)
    {
        Task task = Task.builder()
            .name(name)
            .scriptsType(scriptsType)
            .scripts(scripts)
            .predecessors(predecessors)
            .persistence(persistence)
            .build();
        try {
            taskRepos.save(task);
            tasks.put(name, new TaskContext(task));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public boolean startTask(String name)
    {
        TaskContext ctx = tasks.get(name);
        if (ctx != null)
        {
            if (!ctx.running)
            {
                // execute task's scripts
                threadPool.submit(() -> {
                    Task task = ctx.task;
                    if (task.getScriptsType() == ScriptsType.Python)
                    {
                        try (PythonInterpreter interpreter = new PythonInterpreter()) {
                            List<String> predecessors = task.getPredecessors();
                            if (predecessors.size() > 0) {
                                // TODO: await predecessors, 每个任务都返回一个map，这里需要将多个任务的map根据key进行合并，然后转成字符串source，作为本次任务的输入
                                // https://github.com/Luncert/javautils/blob/master/simpleutils/src/main/java/org/luncert/simpleutils/IOHelper.java

                                // redirect input
                                byte[] source = "source".getBytes();
                                interpreter.setIn(new ByteArrayInputStream(source));
                            }
                            // 重定向标准输出流
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            DataOutputStream dos = new DataOutputStream(bos);
                            interpreter.setOut(dos);

                            // 执行任务
                            interpreter.exec(task.getScripts());
                            task.setSuccessedTime(task.getSuccessedTime() + 1);

                            // out: 从重定向的输出流bos中读出数据，解析成map
                            InputStream outStream = new ByteArrayInputStream(bos.toByteArray());
                            Properties props = new Properties();
                            props.load(outStream);
                            TaskResult taskResult = new TaskResult(task.getName(), props);
                            // 持久化任务输出
                            if (task.isPersistence()) {
                                taskResultRepos.save(taskResult);
                            }

                            // TODO: 将TaskResult分发给依赖该任务的任务
                        } catch (Exception e) {
                            // TODO: log
                        }
                        task.setExecuteedTime(task.getExecuteedTime() + 1);
                    }
                });
                return true;
            } else return false;
        } else return false;
    }

    /**
     * 指定任务名称，删除任务
     */
    public boolean delete(String name)
    {
        Task task = taskRepos.findByName(name);
        if (task != null)
        {
            taskRepos.delete(task);
            return true;
        }
        else return false;
    }


}