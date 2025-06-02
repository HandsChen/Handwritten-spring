package com.chen.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// can be added on any Class, interface (including annotation interface), enum, or record declaration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME) // work in RUNTIME
public @interface Component {
    String name() default ""; //这里给Component新增一个name属性，同时给他一个默认值，为空字符串。如果不写名字，那么就默认使用当前类名
}
