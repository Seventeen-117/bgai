package com.bgpay.bgai.service;

import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.entity.UserToken;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 通过SSO登录
     * 
     * @param code SSO授权码
     * @return 用户令牌
     */
    UserToken loginWithSSO(String code);
    
    /**
     * 验证令牌有效性
     * 
     * @param accessToken 访问令牌
     * @return 用户令牌，如果无效返回null
     */
    UserToken validateToken(String accessToken);
    
    /**
     * 获取用户信息
     * 
     * @param userId 用户ID
     * @return 用户实体
     */
    User getUserInfo(String userId);
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的用户令牌
     */
    UserToken refreshToken(String refreshToken);
    
    /**
     * 登出
     * 
     * @param accessToken 访问令牌
     */
    void logout(String accessToken);
} 