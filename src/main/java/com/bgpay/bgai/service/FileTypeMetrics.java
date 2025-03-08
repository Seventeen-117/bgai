//package com.bgpay.bgai.service;
//
//import io.micrometer.core.instrument.Gauge;
//import io.micrometer.core.instrument.MeterRegistry;
//import org.springframework.stereotype.Service;
//
//@Service
//public class FileTypeMetrics {
//    private final MeterRegistry meterRegistry;
//    private final FileTypeService fileTypeService;
//
//    public FileTypeMetrics(MeterRegistry meterRegistry, FileTypeService fileTypeService) {
//        this.meterRegistry = meterRegistry;
//        this.fileTypeService = fileTypeService;
//
//        Gauge.builder("file.type.config.count",
//                        () -> fileTypeService.getMimeTypeCount())
//                .register(meterRegistry);
//    }
//}