package com.chen.spring;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

//BeanDefinition 相当于创造bean的设计图，而bean就是其产品
public class BeanDefinition {
    private final String name;
    private final Constructor<?> constructor;
    private final Method postConstructMethod;
    private final List<Field> autowiredFields;
    private final Class<?> beanType;

    public BeanDefinition(Class<?> type) {
        this.beanType = type;
        Component component = type.getDeclaredAnnotation(Component.class);
        this.name = component.name().isEmpty() ? type.getSimpleName() : component.name(); //Component有name属性，同时给他一个默认值，为空字符串。如果不写名字，那么就默认使用当前类名
        try {
            this.constructor = type.getConstructor(); //将类的无参构造函数赋值给BeanDefinition的constructor
            this.postConstructMethod = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(PostConstruct.class)).findFirst().orElse(null); //判断类的方法上是否有PostConstruct注解，如果有，就将其标记的后置方法赋值
            this.autowiredFields = Arrays.stream(type.getDeclaredFields()).filter(f -> f.isAnnotationPresent(AutoWired.class)).toList(); //从类中获取标记了AutoWired注解的所有属性
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    //设计图里面该有什么东西呢？每一个bean应该都有一个名字
    public String getName() {
        return name;
    }

    //我们还能通过BeanDefinition去拿到我们想要调用的构造函数
    public Constructor<?> getConstructor() {
        return constructor;
    }

    public Method getPostConstructMethod() {
        return postConstructMethod;
    }

    public List<Field> getAutowiredFields() {
        return autowiredFields;
    }

    public Class<?> getBeanType() {
        return beanType;
    }
}
