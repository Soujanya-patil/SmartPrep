package com.smartprep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableScheduling
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class SmartprepApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartprepApplication.class, args);
	}

}
