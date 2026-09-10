package com.socialworld.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SocialWorldApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialWorldApplication.class, args);
    }
}
