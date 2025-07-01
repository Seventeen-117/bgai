package com.bgpay.bgai.seata;

import com.bgpay.bgai.service.TransactionLogService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Seata事务监听器
 * 用于处理Seata相关事件，特别是在应用启动时进行一些初始化工作
 */
@Slf4j
@Aspect
@Component
public class SeataTransactionListener implements ApplicationListener<ApplicationReadyEvent>, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(SeataTransactionListener.class);

    @Autowired
    private TransactionLogService transactionLogService;

    @PostConstruct
    public void init() {
        // 注册事务钩子
        TransactionHookManager.registerHook(new TransactionHook() {
            @Override
            public void beforeBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction beginning: XID={}", xid);
                }
            }

            @Override
            public void afterBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction began: XID={}", xid);
                    try {
                        transactionLogService.updateTransactionStatus(xid, "ACTIVE", null);
                    } catch (Exception e) {
                        log.error("Failed to record transaction begin: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void beforeCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction committing: XID={}", xid);
                    try {
                        transactionLogService.updateTransactionStatus(xid, "COMMITTING", null);
                    } catch (Exception e) {
                        log.error("Failed to record transaction commit: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void afterCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction committed: XID={}", xid);
                    try {
                        transactionLogService.updateTransactionStatus(xid, "COMMITTED", null);
                    } catch (Exception e) {
                        log.error("Failed to record transaction completion: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void beforeRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction rolling back: XID={}", xid);
                    try {
                        transactionLogService.updateTransactionStatus(xid, "ROLLBACKING", null);
                    } catch (Exception e) {
                        log.error("Failed to record transaction rollback: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void afterRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction rolled back: XID={}", xid);
                    try {
                        transactionLogService.updateTransactionStatus(xid, "ROLLBACKED", null);
                    } catch (Exception e) {
                        log.error("Failed to record transaction rollback completion: XID={}", xid, e);
                    }
                }
            }

            @Override
            public void afterCompletion() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.info("Transaction completed: XID={}", xid);
                }
            }
        });
    }

    /**
     * 在事务方法执行前记录
     */
    @Before("@annotation(globalTransactional)")
    public void beforeTransaction(JoinPoint point, GlobalTransactional globalTransactional) {
        String xid = RootContext.getXID();
        if (xid != null) {
            String methodName = point.getSignature().getName();
            log.info("Transaction method starting: XID={}, method={}", xid, methodName);
        }
    }

    /**
     * 在事务方法执行后记录
     */
    @After("@annotation(globalTransactional)")
    public void afterTransaction(JoinPoint point, GlobalTransactional globalTransactional) {
        String xid = RootContext.getXID();
        if (xid != null) {
            String methodName = point.getSignature().getName();
            log.info("Transaction method completed: XID={}, method={}", xid, methodName);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 在bean初始化时执行
        logger.info("Seata事务监听器初始化");
        
        // 设置系统属性，确保Saga状态机不会自动注册
        System.setProperty("seata.saga.state-machine.auto-register", "false");
        
        logger.info("已设置seata.saga.state-machine.auto-register=false，防止状态机重复注册");
    }
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 应用程序就绪后执行
        logger.info("应用程序已就绪，Seata事务监听器激活");
        
        // 输出Seata相关配置信息
        logger.info("Seata配置信息:");
        logger.info(" - seata.saga.state-machine.auto-register: {}", 
                System.getProperty("seata.saga.state-machine.auto-register", "未设置"));
        logger.info(" - seata.enabled: {}", 
                System.getProperty("seata.enabled", "未设置"));
        logger.info(" - saga.enabled: {}", 
                System.getProperty("saga.enabled", "未设置"));
    }
} 