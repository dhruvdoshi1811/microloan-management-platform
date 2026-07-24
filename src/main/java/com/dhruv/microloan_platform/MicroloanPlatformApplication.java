package com.dhruv.microloan_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MicroloanPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicroloanPlatformApplication.class, args);
	}

}
