package com.cong.fishisland.model.vo.user;

import lombok.Value;

import java.util.List;

@Value
public class DailyPointTasksVO {
    long serverNow;
    long refreshAt;
    List<Task> tasks;

    @Value
    public static class Task {
        String id;
        String state;
        Integer progress;
        Integer goal;
        long earned;
        String detail;
    }
}
