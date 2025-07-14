package com.bgpay.bgai.config;

import com.bgpay.bgai.entity.User;
import com.bgpay.bgai.mapper.UserMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;

/**
 * 提供UserMapper的Mock实现，用于测试环境
 */
@Configuration
public class UserMapperMockConfig {

    /**
     * 提供一个Mock的UserMapper bean，替代原始实现
     */
    @Bean
    @Primary
    public UserMapper userMapper() {
        UserMapper mockMapper = Mockito.mock(UserMapper.class);
        
        // 配置基本行为
        User mockUser = User.builder()
                .id(1L)
                .userId("test-user-id")
                .username("testuser")
                .email("test@example.com")
                .avatarUrl("https://example.com/avatar.png")
                .lastLoginTime(LocalDateTime.now())
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .tokenExpireTime(LocalDateTime.now().plusHours(1))
                .status(1)
                .createTime(LocalDateTime.now().minusDays(30))
                .updateTime(LocalDateTime.now())
                .password("encoded-password")
                .enabled(true)
                .deleted(false)
                .build();
        
        Mockito.when(mockMapper.findByUserId(Mockito.anyString())).thenReturn(mockUser);
        Mockito.when(mockMapper.findByAccessToken(Mockito.anyString())).thenReturn(mockUser);
        Mockito.when(mockMapper.findByRefreshToken(Mockito.anyString())).thenReturn(mockUser);
        Mockito.when(mockMapper.updateLoginInfo(Mockito.anyString(), Mockito.anyString())).thenReturn(1);
        
        return mockMapper;
    }
} 