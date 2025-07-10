package com.bgpay.bgai;

import org.codehaus.janino.ScriptEvaluator;
import org.testng.annotations.Test;

/**
 * 测试Janino依赖是否正常工作
 */
public class JaninoTest {

    @Test
    public void testJaninoClassExists() {
        // 如果Janino依赖正确添加，这个测试应该能通过
        ScriptEvaluator evaluator = new ScriptEvaluator();
        System.out.println("Janino ScriptEvaluator class loaded successfully: " + evaluator.getClass().getName());
    }
} 