//package com.bgpay.bgai.config;
//
//
//import com.baomidou.mybatisplus.annotation.FieldFill;
//import com.baomidou.mybatisplus.annotation.IdType;
//import com.baomidou.mybatisplus.generator.FastAutoGenerator;
//import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
//import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
//import com.baomidou.mybatisplus.generator.config.querys.MySqlQuery;
//import com.baomidou.mybatisplus.generator.config.rules.DateType;
//import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
//import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
//import com.baomidou.mybatisplus.generator.fill.Column;
//import com.baomidou.mybatisplus.generator.fill.Property;
//import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
//
//
//public class CodeGenerator {
//    private static final String JDBC_URL = "jdbc:mysql://8.133.246.113:3306/deepseek?useUnicode=true&characterEncoding=UTF-8&useSSL=false&autoReconnect=true&failOverReadOnly=false&serverTimezone=GMT%2B8";
//    private static final String JDBC_USER_NAME = "bgtech";
//    private static final String JDBC_PASSWORD = "Zly689258..";
//    private static final String PACKAGE_NAME = "com.bgpay.bgai";
//    private static final String[] TBL_NAMES = {"mime_type_mapping"};
//    private static final String TABLE_PREFIX = "";
//
//    public static void main(String[] args) {
//        String projectPath = "E:\\project\\bgai";
//
//        DataSourceConfig.Builder dataSourceConfigBuilder = new DataSourceConfig.Builder(JDBC_URL, JDBC_USER_NAME, JDBC_PASSWORD)
//                .dbQuery(new MySqlQuery())
//                .typeConvert(new MySqlTypeConvert())
//                .keyWordsHandler(new MySqlKeyWordsHandler());
//
//        FastAutoGenerator fastAutoGenerator = FastAutoGenerator.create(dataSourceConfigBuilder);
//
//        fastAutoGenerator.globalConfig(
//                globalConfigBuilder -> globalConfigBuilder
//                        .fileOverride()
//                        .disableOpenDir()
//                        .outputDir(projectPath + "/src/main/java")
//                        .author("zly")
//                        .commentDate("yyyy-MM-dd HH:mm:ss")
//                        .dateType(DateType.TIME_PACK)
//                        .enableSwagger()
//        );
//
//        fastAutoGenerator.packageConfig(
//                packageConfigBuilder -> packageConfigBuilder
//                        .parent(PACKAGE_NAME)
//                        .entity("pojo")
//                        .mapper("dao")
//                        .service("service")
//                        .serviceImpl("service.impl")
//                        .controller("controller")
//                        .xml("dao.xml")
//        );
//
//        fastAutoGenerator.strategyConfig(
//                strategyConfigBuilder -> strategyConfigBuilder
//                        .enableCapitalMode()
//                        .enableSkipView()
//                        .disableSqlFilter()
//                        .addInclude(TBL_NAMES)
//                        .addTablePrefix(TABLE_PREFIX)
//        );
//
//        fastAutoGenerator.strategyConfig(
//                strategyConfigBuilder -> strategyConfigBuilder.entityBuilder()
//                        .fileOverride()
//                        .enableTableFieldAnnotation()
//                        .naming(NamingStrategy.underline_to_camel)
//                        .columnNaming(NamingStrategy.underline_to_camel)
//                        .idType(IdType.AUTO)
//                        .enableLombok()
//                        .addTableFills(new Column("create_time", FieldFill.INSERT))
//                        .addTableFills(new Property("updateTime", FieldFill.INSERT_UPDATE))
//                        .versionColumnName("version")
//                        .disableSerialVersionUID()
//                        .enableChainModel()
//        );
//
//        fastAutoGenerator.strategyConfig(
//                strategyConfigBuilder -> strategyConfigBuilder.controllerBuilder()
//                        .fileOverride()
//                        .enableRestStyle()
//                        .enableHyphenStyle()
//        );
//
//        fastAutoGenerator.strategyConfig(
//                strategyConfigBuilder -> strategyConfigBuilder.serviceBuilder()
//                        .fileOverride()
//                        .formatServiceFileName("%sService")
//                        .formatServiceImplFileName("%sServiceImpl")
//        );
//
//        fastAutoGenerator.strategyConfig(
//                strategyConfigBuilder -> strategyConfigBuilder.mapperBuilder()
//                        .fileOverride()
//                        .enableMapperAnnotation()
//                        .formatMapperFileName("%sMapper")
//                        .formatXmlFileName("%sMapper")
//        );
//
//        fastAutoGenerator.templateEngine(new FreemarkerTemplateEngine());
//        fastAutoGenerator.execute();
//    }
//}
