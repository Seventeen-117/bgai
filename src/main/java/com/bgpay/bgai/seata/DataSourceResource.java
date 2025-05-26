package com.bgpay.bgai.seata;

import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class DataSourceResource implements Resource {
    
    private final String resourceId;
    private final DataSource dataSource;
    
    public DataSourceResource(String resourceId, DataSource dataSource) {
        this.resourceId = resourceId;
        this.dataSource = dataSource;
    }
    
    @Override
    public String getResourceId() {
        return resourceId;
    }
    
    @Override
    public String getResourceGroupId() {
        return "DEFAULT";
    }
    
    @Override
    public BranchType getBranchType() {
        return BranchType.AT;
    }

    public void commit(String xid, long branchId, String applicationData) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.commit();
            log.info("Committed transaction branch: XID={}, branchId={}", xid, branchId);
        } catch (SQLException e) {
            log.error("Failed to commit transaction branch: XID={}, branchId={}", xid, branchId, e);
            throw e;
        }
    }
    
    public void rollback(String xid, long branchId, String applicationData) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.rollback();
            log.info("Rolled back transaction branch: XID={}, branchId={}", xid, branchId);
        } catch (SQLException e) {
            log.error("Failed to rollback transaction branch: XID={}, branchId={}", xid, branchId, e);
            throw e;
        }
    }
} 