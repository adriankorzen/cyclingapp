package com.adkor.cyclingapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CyclingappApplication {

	public static void main(String[] args) {
		SpringApplication.run(CyclingappApplication.class, args);
	}

}
