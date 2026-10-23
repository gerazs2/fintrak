package com.jgmagalop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class FintrakApplication {

	public static void main(String[] args) {
		SpringApplication.run(FintrakApplication.class, args);
	}

}
