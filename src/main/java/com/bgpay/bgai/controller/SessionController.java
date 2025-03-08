package com.bgpay.bgai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.Instant;

@RestController
public class SessionController {

    @GetMapping("/session")
    public String sessionDemo(HttpSession session) {
        // 访问计数器
        Integer count = (Integer) session.getAttribute("count");
        count = (count == null) ? 1 : count + 1;
        session.setAttribute("count", count);

        return String.format("""
            Session 验证成功！
            ID: %s
            访问次数: %d
            创建时间: %s
            最后访问: %s
            """,
                session.getId(),
                count,
                Instant.ofEpochMilli(session.getCreationTime()),
                Instant.ofEpochMilli(session.getLastAccessedTime())
        );
    }
}