package com.bgpay.bgai.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于指定YAML数据源文件的注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface YamlSource {
    /**
     * YAML文件名（不含路径和扩展名）
     */
    String value();
} 