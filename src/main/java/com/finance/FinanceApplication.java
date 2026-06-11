package com.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 个人理财记账系统启动入口。
 */
@SpringBootApplication
public class FinanceApplication {
    /**
     * 启动 Spring Boot 应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
