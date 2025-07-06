package com.bgpay.bgai.base;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import javax.sql.DataSource;

/**
 * 数据库测试基类
 * 提供数据库连接和MyBatis会话工厂，用于直接查询数据库以验证服务响应
 */
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.url=jdbc:mysql://localhost:3306/bgai_test?useSSL=false&serverTimezone=UTC",
    "spring.datasource.username=root",
    "spring.datasource.password=root"
})
@EnableTransactionManagement
@MapperScan("com.bgpay.bgai.mapper")
public abstract class AbstractDatabaseTest extends NoSpringContextTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected SqlSessionFactory sqlSessionFactory;
    
    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;
    
    @Value("${spring.datasource.url:jdbc:mysql://8.133.246.113:3306/deepseek?useSSL=false&serverTimezone=UTC}")
    private String url;
    
    @Value("${spring.datasource.username:bgtech}")
    private String username;
    
    @Value("${spring.datasource.password:Zly689258..}")
    private String password;
    
    /**
     * 初始化数据库连接和MyBatis会话工厂
     */
    @BeforeClass
    public void initDatabase() {
        log.info("初始化数据库连接和MyBatis会话工厂");
        try {
            // 创建数据源
            DataSource dataSource = createDataSource();
            
            // 创建MyBatis环境配置
            Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
            
            // 创建MyBatis配置
            MybatisConfiguration configuration = new MybatisConfiguration(environment);
            configuration.setMapUnderscoreToCamelCase(true);
            
            // 添加Mapper
            addMappers(configuration);
            
            // 创建SqlSessionFactory
            sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            
            log.info("成功创建SqlSessionFactory");
        } catch (Exception e) {
            log.error("初始化数据库连接失败", e);
            throw new RuntimeException("初始化数据库连接失败", e);
        }
    }
    
    /**
     * 关闭数据库连接
     */
    @AfterClass
    public void closeDatabase() {
        log.info("清理数据库资源");
        // 在这里可以添加数据库资源清理的逻辑
    }
    
    /**
     * 创建数据源
     * 子类可以重写此方法以提供自定义数据源
     */
    protected DataSource createDataSource() {
        log.info("创建数据源: URL={}, Username={}", url, username);
        return new PooledDataSource(driverClassName, url, username, password);
    }
    
    /**
     * 获取SqlSession用于执行数据库操作
     */
    protected SqlSession getSqlSession() {
        return sqlSessionFactory.openSession(true);
    }
    
    /**
     * 向MyBatis配置添加Mapper类
     * 子类应该重写此方法以添加特定的Mapper
     */
    protected abstract void addMappers(MybatisConfiguration configuration);
} 