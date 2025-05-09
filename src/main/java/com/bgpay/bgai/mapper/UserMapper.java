package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bgpay.bgai.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户SSO ID查询用户
     * 
     * @param userId SSO用户ID
     * @return 用户实体
     */
    @Select("SELECT * FROM t_user WHERE user_id = #{userId}")
    User findByUserId(@Param("userId") String userId);
    
    /**
     * 更新用户最后登录时间
     * 
     * @param userId 用户ID
     * @param accessToken 访问令牌
     * @return 影响的行数
     */
    @Update("UPDATE t_user SET last_login_time = NOW(), access_token = #{accessToken}, " +
            "update_time = NOW() WHERE user_id = #{userId}")
    int updateLoginInfo(@Param("userId") String userId, @Param("accessToken") String accessToken);
    
    /**
     * 根据访问令牌查询用户
     * 
     * @param accessToken 访问令牌
     * @return 用户实体
     */
    @Select("SELECT * FROM t_user WHERE access_token = #{accessToken} AND " +
            "token_expire_time > NOW()")
    User findByAccessToken(@Param("accessToken") String accessToken);
} 