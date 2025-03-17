package com.bgpay.bgai.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchRequest {
    /**
     * 存储批量处理的使用计算数据传输对象列表
     * 使用 @NotEmpty 注解确保列表不为空
     * 使用 @Valid 注解确保列表中的每个元素都经过验证
     */
    @NotEmpty(message = "批量请求中的记录列表不能为空")
    @Valid
    private List<UsageCalculationDTO> records;

    /**
     * 无参构造方法
     */
    public BatchRequest() {
    }

    /**
     * 有参构造方法，用于初始化记录列表
     * @param records 批量处理的使用计算数据传输对象列表
     */
    public BatchRequest(List<UsageCalculationDTO> records) {
        this.records = records;
    }
}