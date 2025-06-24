package com.bgpay.bgai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * 应用程序单元测试类
 * 使用MOCK环境避免需要完整的Web服务器上下文
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class BgaiApplicationTests {

	@Test
	void contextLoads() {
		// 测试Spring上下文是否正常加载
	}

}
