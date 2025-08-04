package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bgpay.bgai.entity.AppSecret;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 应用密钥Mapper接口
 */
@Mapper
public interface AppSecretMapper extends BaseMapper<AppSecret> {

    /**
     * 根据应用ID查询应用密钥
     * 
     * @param appId 应用ID
     * @return 应用密钥信息
     */
    @Select("SELECT * FROM app_secret WHERE app_id = #{appId} AND status = 1 AND deleted = 0")
    AppSecret selectByAppId(@Param("appId") String appId);
} 