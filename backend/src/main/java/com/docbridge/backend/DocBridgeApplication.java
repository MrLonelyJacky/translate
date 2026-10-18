package com.docbridge.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.docbridge.backend.**.mapper")
@SpringBootApplication
public class DocBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocBridgeApplication.class, args);
        System.out.println("--------------------- DocBridgeApplication started --------------------");
    }
}

