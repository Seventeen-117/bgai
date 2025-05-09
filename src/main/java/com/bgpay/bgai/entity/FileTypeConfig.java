package com.bgpay.bgai.entity;

import lombok.Data;

import java.util.Set;

@Data
public class FileTypeConfig {
    private String mimeType;
    private Set<String> extensions;
}
