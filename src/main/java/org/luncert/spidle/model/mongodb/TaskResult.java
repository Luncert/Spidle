package org.luncert.spidle.model.mongodb;

import java.io.Serializable;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document("TaskOutputs")
public class TaskResult implements Serializable
{

    private static final long serialVersionUID = 1L;

    @Id
    ObjectId id;

    private String taskName;

    /**
     * 正文
     */
    private Map<Object, Object> content;

    public TaskResult(String taskName, Map<Object, Object> content) {
        this.taskName = taskName;
        this.content = content;
    }

}