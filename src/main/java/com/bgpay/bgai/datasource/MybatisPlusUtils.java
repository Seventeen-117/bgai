package com.bgpay.bgai.datasource;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * mp工具类
 *
 * @author <achao1441470436@gmail.com>
 * @date 2021/5/4 0004 16:35
 */
public class MybatisPlusUtils {

    private MybatisPlusUtils() {
        // Do not new me!
    }

    /**
     * 传入mapper,paramList,源属性，新属性 得到 List<新属性> 用于关联表等
     *
     * @param mapper 持久层操作类
     * @param param  查询参数
     * @param origin 源属性
     * @param target 新属性
     * @param <T>    PO
     * @param <I>    T中的源属性
     * @param <R>    T中的新属性
     * @return java.util.Map<I, T>
     * @author <achao1441470436@gmail.com>
     * @date 2021/5/4 0004 20:37
     */
    public static <T, I, R> List<R> getList(BaseMapper<T> mapper, Supplier<I> param, SFunction<T, I> origin, SFunction<T, R> target) {
        return Optional.ofNullable(param).map(Supplier::get).map(p -> mapper.selectList(new LambdaQueryWrapper<T>().eq(origin, p).select(target))).map(List::parallelStream).map(s -> s.map(target).collect(Collectors.toList())).orElseGet(Collections::emptyList);
    }

    /**
     * 传入mapper,paramList,对应的属性 得到 Map<实体属性, 实体> 用于一对一
     *
     * @param mapper    持久层操作类
     * @param paramList 查询参数
     * @param sFunction 条件
     * @param <T>       PO
     * @param <I>       T中的属性
     * @return java.util.Map<I, T>
     * @author <achao1441470436@gmail.com>
     * @date 2021/5/4 0004 20:37
     */
    public static <T, I> Map<I, T> getMapBy(BaseMapper<T> mapper, List<I> paramList, SFunction<T, I> sFunction) {
        return Optional.ofNullable(paramList).filter(CollectionUtils::isNotEmpty).map(p -> mapper.selectList(new LambdaQueryWrapper<T>().in(sFunction, p))).map(List::parallelStream).map(s -> s.collect(Collectors.toMap(sFunction, Function.identity(), (a, b) -> b))).orElseGet(Collections::emptyMap);
    }

    /**
     * 传入mapper,paramList,对应的属性 得到 Map<实体属性, List<实体>> 用于一对多
     *
     * @param mapper    持久层操作类
     * @param paramList 查询参数
     * @param sFunction 条件
     * @param <T>       PO
     * @param <I>       T中的属性
     * @return java.util.Map<I, List < T> >
     * @author <achao1441470436@gmail.com>
     * @date 2021/5/4 0004 20:37
     */
    public static <T, I> Map<I, List<T>> groupBy(BaseMapper<T> mapper, List<I> paramList, SFunction<T, I> sFunction) {
        return Optional.ofNullable(paramList).filter(CollectionUtils::isNotEmpty).map(p -> mapper.selectList(new LambdaQueryWrapper<T>().in(sFunction, p))).map(List::parallelStream).map(s -> s.collect(Collectors.groupingBy(sFunction))).orElseGet(Collections::emptyMap);
    }

}