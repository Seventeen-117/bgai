package com.bgpay.bgai.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class TestCaseExcelService {

    /**
     * 生成API测试用例Excel文件
     *
     * @param filePath 文件路径
     * @param parameters 自定义参数
     * @throws IOException 如果文件操作失败
     */
    public void generateApiTestCaseExcel(String filePath, Map<String, Object> parameters) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建API测试用例Sheet
            Sheet sheet = workbook.createSheet("API测试用例");
            
            // 设置列宽
            sheet.setColumnWidth(0, 3000);  // 用例ID
            sheet.setColumnWidth(1, 5000);  // 接口URL
            sheet.setColumnWidth(2, 3000);  // 请求方法
            sheet.setColumnWidth(3, 8000);  // 请求参数
            sheet.setColumnWidth(4, 3000);  // 预期状态码
            sheet.setColumnWidth(5, 10000); // 预期响应
            
            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"用例ID", "接口URL", "请求方法", "请求参数", "预期状态码", "预期响应"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 添加数据行 - 场景1: 正常请求
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("TC-API-001");
            dataRow1.createCell(1).setCellValue("/api/example");
            dataRow1.createCell(2).setCellValue("POST");
            dataRow1.createCell(3).setCellValue("{\"id\":\"123456\",\"name\":\"测试名称\"}");
            dataRow1.createCell(4).setCellValue("200");
            dataRow1.createCell(5).setCellValue("{\"code\":0,\"message\":\"成功\"}");
            
            // 添加数据行 - 场景2: 参数缺失
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("TC-API-002");
            dataRow2.createCell(1).setCellValue("/api/example");
            dataRow2.createCell(2).setCellValue("POST");
            dataRow2.createCell(3).setCellValue("{\"id\":\"123456\"}");
            dataRow2.createCell(4).setCellValue("400");
            dataRow2.createCell(5).setCellValue("{\"code\":1001,\"message\":\"参数错误\"}");
            
            // 保存Excel文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            log.info("API测试用例Excel文件已生成: {}", filePath);
        }
    }
    
    /**
     * 生成功能测试用例Excel文件
     *
     * @param filePath 文件路径
     * @param parameters 自定义参数
     * @throws IOException 如果文件操作失败
     */
    public void generateFunctionalTestCaseExcel(String filePath, Map<String, Object> parameters) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建功能测试用例Sheet
            Sheet sheet = workbook.createSheet("功能测试用例");
            
            // 设置列宽
            sheet.setColumnWidth(0, 3000);  // 用例ID
            sheet.setColumnWidth(1, 5000);  // 模块
            sheet.setColumnWidth(2, 5000);  // 功能点
            sheet.setColumnWidth(3, 8000);  // 前置条件
            sheet.setColumnWidth(4, 10000); // 测试步骤
            sheet.setColumnWidth(5, 10000); // 预期结果
            
            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"用例ID", "模块", "功能点", "前置条件", "测试步骤", "预期结果"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 添加数据行 - 场景1: 用户成功注册
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("TC-FUNC-001");
            dataRow1.createCell(1).setCellValue("用户管理");
            dataRow1.createCell(2).setCellValue("用户注册");
            dataRow1.createCell(3).setCellValue("系统中不存在相同用户名的账号");
            dataRow1.createCell(4).setCellValue("1. 访问注册页面\n2. 输入有效用户名和密码\n3. 点击注册按钮");
            dataRow1.createCell(5).setCellValue("1. 注册成功，显示成功提示\n2. 系统自动跳转到登录页面");
            
            // 保存Excel文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            log.info("功能测试用例Excel文件已生成: {}", filePath);
        }
    }
    

    

    

} 