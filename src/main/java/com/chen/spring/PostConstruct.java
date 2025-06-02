package com.chen.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) //能够加在方上
@Retention(RetentionPolicy.RUNTIME)
public @interface PostConstruct {
}
