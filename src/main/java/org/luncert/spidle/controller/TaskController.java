package org.luncert.spidle.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/task")
public class TaskController
{

    @PutMapping("/{name}")
    public void putMethodName(@PathVariable String name, @RequestBody String scripts) {
        //TODO: process PUT request
        
        return;
    }

}