package com.rafel.netty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;


@SpringBootApplication
@MapperScan(basePackages = "com.rafel.netty.mapper")
public class RafelNettyApplication {

	public static void main(String[] args) {
		SpringApplication.run(RafelNettyApplication.class, args);
	}

}
