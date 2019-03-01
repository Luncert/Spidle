package org.luncert.spidle.model.mongodb;

import java.io.Serializable;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

/**
 * 任务实体
 * 脚本语言支持：Jython
 * Jython库参考：https://www.jython.org/docs/library/indexprogress.html
 */
@Data
@Builder
@Document("Tasks")
public class Task implements Serializable
{

    public enum ScriptsType
    {
        Python
    }

    private static final long serialVersionUID = 1L;

    @Id
    ObjectId id;

    /**
     * 任务名，唯一
     */
    @Indexed(unique = true)
    private String name;

    /**
     * 上传时间
     */
    private Long uploadTime;

    /**
     * 任务脚本语言
     */
    private ScriptsType scriptsType;

    /**
     * 任务内容
     * spidle通过标准输入输出来与任务进程交互数据
     * 任务输入：任务可以声明自己的前置任务，spidle将把前置任务的输出作为输入传递给该任务
     * 任务输出：任务可以在输出结果中声明是否持久化输出数据
     */
    private String scripts;

    private List<String> predecessors;
    private List<String> successor;

    /**
     * 是否持久化输出
     */
    private boolean persistence;

    // 任务统计信息

    /**
     * 总执行次数
     */
    private int executeedTime;

    /**
     * 成功次数
     */
    private int successedTime;

    /**
     * 任务性能，n bytes/sec
     */
    private float performance;
    
}