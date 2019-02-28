package org.luncert.spidle.repository.mongodb;

import org.bson.types.ObjectId;
import org.luncert.spidle.model.mongodb.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepos extends MongoRepository<Task, ObjectId>
{

    Task findByName(String name);

}