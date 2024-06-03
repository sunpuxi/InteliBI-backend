package com.ai.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 *
 */
@RestController
@RequestMapping("/queue")
@Slf4j
public class QueueController {
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @GetMapping("/add")
    public void add(String name){

        CompletableFuture.runAsync(()->{
            System.out.println("任务执行中"+name+"执行线程为"+Thread.currentThread().getName());
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },threadPoolExecutor);
    }

    @GetMapping("/get")
    public String get(){
        Map<String,Object> map=new HashMap<>();
        int size = threadPoolExecutor.getQueue().size();
        map.put("队列的长度：",size);
        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("任务总数：",taskCount);
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("已经完成的任务数量",completedTaskCount);
        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("正在工作的线程数",activeCount);
        return JSONUtil.toJsonStr(map);
    }

}
