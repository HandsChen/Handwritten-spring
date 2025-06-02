package com.chen.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD) //可以放在类的属性上面。Field declaration (includes enum constants)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoWired {
}
