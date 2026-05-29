package com.budget.buoy;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

import com.budget.buoy.config.RsaKeyProperties;

@EnableJdbcAuditing
@SpringBootApplication
@EnableConfigurationProperties(RsaKeyProperties.class)
public class Application {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(Application.class, args);
    }
}
