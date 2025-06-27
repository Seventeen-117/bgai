package com.bgpay.bgai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bgpay.bgai.entity.MimeTypeConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 文件类型相关数据库操作接口
 */
@Mapper
public interface FileTypeMapper extends BaseMapper<MimeTypeConfig> {

    /**
     * 查询所有活跃的MIME类型配置
     * @return MIME类型配置列表
     */
    default List<MimeTypeConfig> selectActiveMimeTypes() {
        LambdaQueryWrapper<MimeTypeConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MimeTypeConfig::getIsActive, true);
        return this.selectList(queryWrapper);
    }

    /**
     * 更新MIME类型配置
     * @param config MIME类型配置
     * @return 影响的行数
     */
    default int updateMimeTypeConfig(MimeTypeConfig config) {
        // 处理ON DUPLICATE KEY UPDATE逻辑
        // 先查询是否存在
        LambdaQueryWrapper<MimeTypeConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MimeTypeConfig::getMimeType, config.getMimeType());
        
        MimeTypeConfig existingConfig = this.selectOne(queryWrapper);
        if (existingConfig != null) {
            // 存在则更新
            config.setId(existingConfig.getId());
            return this.updateById(config);
        } else {
            // 不存在则插入
            return this.insert(config);
        }
    }
}