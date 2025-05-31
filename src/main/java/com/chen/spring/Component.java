package com.chen.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // can be added on any Class, interface (including annotation interface), enum, or record declaration
@Retention(RetentionPolicy.RUNTIME) // work in RUNTIME
public @interface Component {
}
