package com.bgpay.bgai.config;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author longyang.zhang
 */
@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        if (applicationContext == null) {
            applicationContext = arg0;
        }
    }


    public static ConfigurableApplicationContext getApplicationContext() {
        return (ConfigurableApplicationContext) applicationContext;
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    //byName判读对象是否存在
    public static  boolean contains(String name){
        return applicationContext.containsBean(name);
    }
    //判断对象是否是单例
    public static boolean isSignleton(String name){
        return applicationContext.isSingleton(name);
    }
    //判断注册对象的类型
    public static Class<?> getType(String name){
        return applicationContext.getType(name);
    }
    //返回bean的别名
    public static String[] getAliases(String name){
        return applicationContext.getAliases(name);
    }

    //根据类型获取bean的名字
    public String[] getBeanNamesForType(Class<?> clazz){
        return applicationContext.getBeanNamesForType(clazz);
    }
}