package com.howners.gestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HownersApplication {

	public static void main(String[] args) {
		SpringApplication.run(HownersApplication.class, args);
	}

}
