package com.chen.spring;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {
    //既然是创建对象，那么就需要有个地方去装对象。这里使用一个hashMap,其中key是对象的名字，value是创建出来的对象本体
    private final Map<String, Object> ioc = new HashMap<>();
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private final Map<String, Object> loadingIoc = new HashMap<>(); //装载着还没有完全初始化完成的对象
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();

    public ApplicationContext(String packageName) throws IOException {
        this.initContext(packageName);
    }

    // get an Object by its name
    public Object getBean(String name) {
        if (null == name) {
            return null;
        }
        Object bean = this.ioc.get(name);//直接通过bean的名字去ioc中拿就可以了
        if (null != bean) {
            return bean;
        }
        //在给对象中Autowired属性标记的属性赋值时，有可能属性表示的对象还没有创建，这时候我们需要判断这个对象到底是真的没有？还是说只是暂时没创建？
        //那么如何判断，可以去BeanDefinition中判断是否存在该对象的名字，因为BeanDefinition的创建发生在bean创建之前
        if (beanDefinitionMap.containsKey(name)) {
            return createBean(beanDefinitionMap.get(name));
        }
        return null;
    }

    // get an Object by its type  这里通过类型去拿bean，由于没有办法直接去ioc拿，思路是遍历ioc中的所有bean，去看看类型能否与bean的类型匹配.
    // 这里之所以不新创建一个Map,去以bean的类型为key,bean为value的原因是无法处理接口和父类等情况，因为其存在着一对多
    public <T> T getBean(Class<T> beanType) {
//        return this.ioc.values().stream().filter(bean -> beanType.isAssignableFrom(bean.getClass())).map(bean -> (T) bean).findAny().orElse(null);
        //改写成从beanDefinitionMap中遍历，这样即使获取时暂时没有创建目标bean,但是也可以创建bean并返回
        String beanName = this.beanDefinitionMap.values().stream().filter(beanDefinition -> beanType.isAssignableFrom(beanDefinition.getBeanType())).map(BeanDefinition::getName).findFirst().orElse(null);
        return (T) getBean(beanName);
    }

    //get multiple bean objects corresponding to their types
    public <T> List<T> getBeans(Class<T> beanType) {
//        return this.ioc.values().stream().filter(bean -> beanType.isAssignableFrom(bean.getClass())).map(bean -> (T) bean).toList();
        return this.beanDefinitionMap.values().stream().filter(beanDefinition -> beanType.isAssignableFrom(beanDefinition.getBeanType())).map(BeanDefinition::getName).map(this::getBean).map(bean -> (T) bean).toList();
    }

    /*
     * 1. first,we should create a function named initContext aimed to create all beans during the context init period.
     *    then, we should figure out 2 questions: 1) what object to create? 2) how to create an Object?
     * */
    public void initContext(String packageName) throws IOException {
        //changed 0: ApplicationContext.class.getClassLoader().getResource();
        //changed 1: List<BeanDefinition> list = this.scanPackage(packageName).stream().filter(this::scanCreate).map(this::wrapper).toList(); //将扫描到的类map成BeanDefinition
        //changed 2: this.scanPackage(packageName).stream().filter(this::scanCreate).map(this::wrapper).forEach(this::createBean);
        //将上面的拆分成两部，因为我们想要让所有beanDefinition创建完之后，
        // 再统一创建bean,为的是可以实现给某个对象中autowired标记的属性赋值时，在该对象属性还没有创建时，判断该属性是否最终存在。
        // 这一行代码结束以后，我们就将所有的beanDefinition放到了beanDefinitionMap中
        this.scanPackage(packageName).stream().filter(this::scanCreate).forEach(this::wrapper);
        initBeanPostProcessor(); //可以理解为初始化spring的特殊的类
        beanDefinitionMap.values().forEach(this::createBean); //批量将beanDefinition创建成bean
    }

    //给List<BeanPostProcessor> postProcessors 进行赋值
    private void initBeanPostProcessor() {
        beanDefinitionMap.values().stream().filter(beanDefinition -> BeanPostProcessor.class.isAssignableFrom(beanDefinition.getBeanType())).map(this::createBean).map(bean -> (BeanPostProcessor) bean).forEach(postProcessors::add);
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

    //最后就是一个type是如何变成BeanDefinition的
    protected BeanDefinition wrapper(Class<?> type) {
        BeanDefinition beanDefinition = new BeanDefinition(type);
        if (beanDefinitionMap.containsKey(beanDefinition.getName())) { //解决当bean的名字重复时，应该报错
            throw new RuntimeException("bean的名字重复");
        }
        beanDefinitionMap.put(beanDefinition.getName(), beanDefinition);
        return beanDefinition;
    }

    //根据BeanDefinition创建Bean
    protected Object createBean(BeanDefinition beanDefinition) {
        if (ioc.containsKey(beanDefinition.getName())) { //如果ioc中已经存在BeanName，就说明这个对象已经创建好了
            return ioc.get(beanDefinition.getName());
        }
        if (loadingIoc.containsKey(beanDefinition.getName())) { //相当于创建了一个双层缓存，只有当用来存放半成品的容器loadingIoc中没有对应对象时，才创建对象，用于解决循环依赖
            return loadingIoc.get(beanDefinition.getName());
        }
        return doCreateBean(beanDefinition);
    }

    //真正执行创建Bean的逻辑
    private Object doCreateBean(BeanDefinition beanDefinition) {
        Constructor<?> constructor = beanDefinition.getConstructor(); //拿到类的构造函数
        Object bean = null;
        try {
            bean = constructor.newInstance();
            loadingIoc.put(beanDefinition.getName(), bean); //将创建的半成品的bean放入专门的容器loadingIoc中，用于解决循环依赖问题
            //在bean创建之后，需要将bean中autowire的属性进行注入
            autowiredBean(bean, beanDefinition);
            bean = initializeBean(beanDefinition, bean);
            loadingIoc.remove(beanDefinition.getName());
            ioc.put(beanDefinition.getName(), bean); //将完全创建完成的bean，放入ioc中，同时要将半成品容器loadingIoc中的bean移除，用于解决循环依赖
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bean; //将创建出来的bean进行返回
    }

    //处理Bean中使用postConstruct注解标记的方法逻辑
    private Object initializeBean(BeanDefinition beanDefinition, Object bean) throws IllegalAccessException, InvocationTargetException {
        //处理前置bean
        for (BeanPostProcessor postProcessor : postProcessors) {  //生命周期函数
            bean = postProcessor.beforeInitializeBean(bean, beanDefinition.getName());
        }
        Method postConstructMethod = beanDefinition.getPostConstructMethod(); //在bean创建后通过beanDefinition查看其方法是否存在postConstructMethod，如果是,那么通过invoke bean，去调用该后置构造方法
        if (null != postConstructMethod) {
            /*
            * public Object invoke(Object obj, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
            *     第一个参数 obj：要在哪个对象实例上调用这个方法（如果是静态方法，可以传 null）
                  可变参数 args：传给方法的实际参数
                  返回值：被调用方法的返回值（如果是 void 方法，则返回 null）
            * */
            postConstructMethod.invoke(bean); //在bean上调用构造方法
        }
        for (BeanPostProcessor postProcessor : postProcessors) { //生命周期函数
            bean = postProcessor.afterInitializeBean(bean, beanDefinition.getName());
        }
        return bean;
    }

    //向目标bean，自动注入其内部使用autowired注解的属性
    private void autowiredBean(Object bean, BeanDefinition beanDefinition) throws IllegalAccessException {
        for (Field autowiredField : beanDefinition.getAutowiredFields()) {
            autowiredField.setAccessible(true); //将要注入的属性设置为可访问的
            //this.getBean通过属性的类型获取bean
            autowiredField.set(bean, this.getBean(autowiredField.getType())); //obj – the object whose field should be modified    value – the new value for the field of obj being modified
        }
    }

}
