package com.bgpay.bgai.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bgpay.bgai.entity.MimeTypeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileTypeMapper extends BaseMapper<MimeTypeConfig> {

    @Select("SELECT * FROM mime_type_config WHERE enabled = true")
    List<MimeTypeConfig> selectActiveMimeTypes();

    @Select("SELECT mime_type FROM allowed_file_type WHERE enabled = true")
    List<String> selectAllowedTypes();
}