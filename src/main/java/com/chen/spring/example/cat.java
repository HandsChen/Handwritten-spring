package com.chen.spring.example;

import com.chen.spring.AutoWired;
import com.chen.spring.Component;
import com.chen.spring.PostConstruct;

@Component
public class cat {
    @AutoWired
    private dog mydog;

    @PostConstruct
    public void init() {
        System.out.println("开始调用了后置方法,创建了 " + mydog);
    }
}
