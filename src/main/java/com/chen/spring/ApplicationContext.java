package com.chen.spring;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class ApplicationContext {

    public ApplicationContext(String packageName) throws IOException {
        this.initContext(packageName);
    }

    // get an Object by its name
    public Object getBean(String name) {
        return null;
    }

    // get an Object by its type
    public <T> T getBean(Class<T> beanType) {
        return null;
    }

    //get multiple bean objects corresponding to their types
    public <T> List<T> getBeans(Class<T> beanType) {
        return null;
    }

    /*
     * 1. first,we should create a function named initContext aimed to create all beans during the context init period.
     *    then, we should figure out 2 questions: 1) what object to create? 2) how to create an Object?
     * */
    public void initContext(String packageName) throws IOException {
        //changed 0: ApplicationContext.class.getClassLoader().getResource();
        //changed 1: List<BeanDefinition> list = this.scanPackage(packageName).stream().filter(this::scanCreate).map(this::wrapper).toList(); //将扫描到的类map成BeanDefinition
        this.scanPackage(packageName).stream().filter(this::scanCreate).map(this::wrapper).forEach(this::createBean);
    }

    public List<Class<?>> scanPackage(String packageName) throws IOException {
        List<Class<?>> classList = new ArrayList<>();
        //传进来的包名应该是a.b.c的形式,应该是需要转换为a/b/c的形式，为了保证win和linux下的兼容性，使用File.separator获得最终路径
        URL resource = this.getClass().getClassLoader().getResource(packageName.replace(".", File.separator));
        Path path = Path.of(resource.getFile()); //获取包名对应文件夹的路径
        Files.walkFileTree(path, new SimpleFileVisitor<>() { //Files.walkFileTree可以帮助我们递归的调用一个path,SimpleFileVisitor其实这里表示着一个经典的访问者模式的应用　
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException { //重写visitFile，可以用来处理递归调用文件夹时，每次遍历到文件的处理该文件的逻辑
                Path absolutePath = file.toAbsolutePath(); //获取文件的绝对路径
                if (absolutePath.toString().endsWith(".class")) { //如果文件的绝对路径是以.class结尾时，那么就打印一下该路径
                    String replaceStr = absolutePath.toString().replace(File.separator, "."); //因为最后创建类需要的是a.b.c的包名格式 ，因为需要将其从a/b/c转回来
                    int packageIndex = replaceStr.indexOf(packageName);
                    String className = replaceStr.substring(packageIndex, replaceStr.length() - ".class".length());
                    try {
                        classList.add(Class.forName(className)); //根据包下类名创建对应类，同时组装成一个集合进行返回
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                return FileVisitResult.CONTINUE; //枚举值表示，无论我们碰到什么问题，我们都将所有文件递归遍历完
            }
        });
        return classList;
    }

    //如果有一个类想继承ApplicationContext，那么只需要重写scanCreate函数，就可以自定义决定它想加载什么类
    protected boolean scanCreate(Class<?> type) {
        return type.isAnnotationPresent(Component.class);
    }

    protected BeanDefinition wrapper(Class<?> type) {
        return new BeanDefinition(type);
    }

    //根据BeanDefinition创建Bean
    protected void createBean(BeanDefinition beanDefinition) {
    }

}
