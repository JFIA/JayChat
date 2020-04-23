package com.rafel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;


@SpringBootApplication
@MapperScan(basePackages = "com.rafel.mapper")
@ComponentScan(basePackages = "com.rafel")
public class RafelNettyApplication {

    public static void main(String[] args) {
        SpringApplication.run(RafelNettyApplication.class, args);
    }

}
