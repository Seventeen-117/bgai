package com.bgpay.bgai.config;

import lombok.Data;

import java.util.Set;

@Data
public class FileTypeConfig {
    private String mimeType;
    private Set<String> extensions;
}
