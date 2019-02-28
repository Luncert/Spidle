package org.luncert.spidle.repository.mongodb;

import org.bson.types.ObjectId;
import org.luncert.spidle.model.mongodb.TaskResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskResultRepos extends MongoRepository<TaskResult, ObjectId>
{
    
}