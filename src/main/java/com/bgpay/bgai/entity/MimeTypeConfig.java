package com.bgpay.bgai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("mime_type_config")
public class MimeTypeConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String mimeType;
    private String extensions;
    private String magicNumbers;
    private Boolean enabled;

    public byte[] getMagicBytes() {
        if (magicNumbers == null || magicNumbers.isEmpty()) {
            return new byte[0];
        }
        String[] hexValues = magicNumbers.split(" ");
        byte[] data = new byte[hexValues.length];
        for (int i = 0; i < hexValues.length; i++) {
            String hex = hexValues[i];
            data[i] = (byte) ((Character.digit(hex.charAt(0), 16) << 4)
                    + Character.digit(hex.charAt(1), 16));
        }
        return data;
    }
}