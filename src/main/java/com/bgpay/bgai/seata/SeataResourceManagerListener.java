package com.bgpay.bgai.seata;

import com.bgpay.bgai.service.TransactionLogService;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.AbstractResourceManager;
import io.seata.rm.RMClient;
import io.seata.rm.datasource.DataSourceProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SeataResourceManagerListener extends AbstractResourceManager {

    @Autowired
    private TransactionLogService transactionLogService;

    @Autowired
    private DataSource dataSource;

    @Value("${spring.application.name:default}")
    private String applicationId;

    private final Map<String, Resource> managedResources = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // 初始化RM客户端
            RMClient.init(applicationId, "default-tx-group");
            
            // 创建数据源代理
            DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
            
            // 注册数据源作为资源
            String resourceId = String.format("%s:default", applicationId);
            DataSourceResource dataSourceResource = new DataSourceResource(resourceId, dataSourceProxy);
            registerResource(dataSourceResource);
            
            log.info("Seata Resource Manager registered successfully for application: {}", applicationId);
        } catch (Exception e) {
            log.error("Failed to initialize Seata Resource Manager", e);
        }
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) {
        log.info("Branch committing: XID={}, branchId={}, resourceId={}, data={}", 
                xid, branchId, resourceId, applicationData);
        
        try {
            Resource resource = managedResources.get(resourceId);
            if (resource == null) {
                log.warn("Resource not found for commit: {}", resourceId);
                return BranchStatus.PhaseTwo_CommitFailed_Unretryable;
            }

            transactionLogService.updateTransactionStatus(xid, "BRANCH_COMMITTING", String.valueOf(branchId));
            
            if (resource instanceof DataSourceResource) {
                ((DataSourceResource) resource).commit(xid, branchId, applicationData);
            }
            
            return BranchStatus.PhaseTwo_Committed;
        } catch (Exception e) {
            log.error("Failed to commit branch: XID={}, branchId={}", xid, branchId, e);
            return BranchStatus.PhaseTwo_CommitFailed_Retryable;
        }
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) {
        log.info("Branch rolling back: XID={}, branchId={}, resourceId={}", xid, branchId, resourceId);
        try {
            Resource resource = managedResources.get(resourceId);
            if (resource == null) {
                log.warn("Resource not found for rollback: {}", resourceId);
                return BranchStatus.PhaseTwo_RollbackFailed_Unretryable;
            }

            transactionLogService.updateTransactionStatus(xid, "BRANCH_ROLLBACKING", String.valueOf(branchId));
            
            if (resource instanceof DataSourceResource) {
                ((DataSourceResource) resource).rollback(xid, branchId, applicationData);
            }
            
            return BranchStatus.PhaseTwo_Rollbacked;
        } catch (Exception e) {
            log.error("Failed to rollback branch: XID={}, branchId={}", xid, branchId, e);
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        }
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) {
        log.info("Branch registering: XID={}, resourceId={}, lockKeys={}", xid, resourceId, lockKeys);
        try {
            Resource resource = managedResources.get(resourceId);
            if (resource == null) {
                log.warn("Resource not found for register: {}", resourceId);
                return null;
            }

            long branchId = super.branchRegister(branchType, resourceId, clientId, xid, applicationData, lockKeys);
            transactionLogService.updateTransactionStatus(xid, "BRANCH_REGISTERED", String.valueOf(branchId));
            return branchId;
        } catch (Exception e) {
            log.error("Failed to register branch: XID={}, resourceId={}", xid, resourceId, e);
            return null;
        }
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) {
        log.info("Branch reporting: XID={}, branchId={}, status={}", xid, branchId, status);
        try {
            super.branchReport(branchType, xid, branchId, status, applicationData);
            transactionLogService.updateTransactionStatus(xid, status.name(), String.valueOf(branchId));
        } catch (Exception e) {
            log.error("Failed to report branch status: XID={}, branchId={}, status={}", xid, branchId, status, e);
        }
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) {
        log.info("Lock querying: XID={}, resourceId={}, lockKeys={}", xid, resourceId, lockKeys);
        Resource resource = managedResources.get(resourceId);
        if (resource == null) {
            return false;
        }
        return true;
    }

    @Override
    public void registerResource(Resource resource) {
        String resourceId = resource.getResourceId();
        managedResources.put(resourceId, resource);
        super.registerResource(resource);
        log.info("Resource registered: {}", resourceId);
    }

    @Override
    public void unregisterResource(Resource resource) {
        String resourceId = resource.getResourceId();
        managedResources.remove(resourceId);
        super.unregisterResource(resource);
        log.info("Resource unregistered: {}", resourceId);
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return managedResources;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.AT;
    }
} 