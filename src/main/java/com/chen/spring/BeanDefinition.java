package com.chen.spring;

import java.lang.reflect.Constructor;

//BeanDefinition 相当于创造bean的设计图，而bean就是其产品
public class BeanDefinition {
    public BeanDefinition(Class<?> type) {

    }

    //设计图里面该有什么东西呢？每一个bean应该都有一个名字
    public String getName() {
        return "";
    }

    //我们还能通过BeanDefinition去拿到我们想要调用的构造函数
    public Constructor<?> getConstructor() {
        return null;
    }
}
