package org.luncert.spidle.repository.mongodb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.luncert.spidle.model.mongodb.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TaskReposTests
{

    @Autowired
    private TaskRepos taskRepos;

    @Test
    public void testSave()
    {
        Task task = Task.builder()
            .name("TestTask")
            .build();
        taskRepos.save(task);
    }

    @Test
    public void testDelete()
    {
        Task task = taskRepos.findByName("TestTask");
        taskRepos.delete(task);
    }

}