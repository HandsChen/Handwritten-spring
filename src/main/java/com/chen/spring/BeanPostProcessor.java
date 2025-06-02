package com.chen.spring;

//我们可以通过实现BeanPostProcessor，对初始化的一个bean进行干预，并且我们可以对我们拿到的初始化后的bean进行重制
public interface BeanPostProcessor {

    default Object beforeInitializeBean(Object bean, String beanName) { //不是所有的都需要实现，默认返回bean
        return bean;
    }


    default Object afterInitializeBean(Object bean, String beanName) {
        return bean;
    }
}
