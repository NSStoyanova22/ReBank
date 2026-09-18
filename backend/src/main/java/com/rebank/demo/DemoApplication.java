package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner testDatabaseConnection(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                System.out.println("==========================================");
                System.out.println("SUCCESS: Connected to PostgreSQL Database!");
                System.out.println("Database Product Name: " + connection.getMetaData().getDatabaseProductName());
                System.out.println("Database URL: " + connection.getMetaData().getURL());
                System.out.println("==========================================");
            } catch (Exception e) {
                System.err.println("==========================================");
                System.err.println("ERROR: Failed to connect to the database!");
                System.err.println("Reason: " + e.getMessage());
                System.err.println("==========================================");
            }
        };
    }
}