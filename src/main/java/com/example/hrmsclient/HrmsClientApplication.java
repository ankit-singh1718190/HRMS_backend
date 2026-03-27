package com.example.hrmsclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication  
@EnableMethodSecurity
@EnableScheduling 
@EnableAsync
public class HrmsClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(HrmsClientApplication.class, args);
	}

}
