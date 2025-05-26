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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Aspect
@Component
public class SeataTransactionListener {

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
} 