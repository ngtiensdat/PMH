package com.example.paymenthub;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PaymentHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentHubApplication.class, args);
    }

    @Bean
    public CommandLineRunner dropComponentIdColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE PMH_COMPONENTS DROP COLUMN ID");
                System.out.println("====== DB FIX: Dropped redundant ID column from PMH_COMPONENTS successfully ======");
            } catch (Exception e) {
                System.out.println("====== DB FIX: ID column check: " + e.getMessage() + " ======");
            }

            try {
                jdbcTemplate.execute("ALTER TABLE PMH_GROUP_CATEGORY DROP CONSTRAINT FK_CATEGORY_COMPONENT");
                System.out.println("====== DB FIX: Dropped FK_CATEGORY_COMPONENT constraint successfully ======");
            } catch (Exception e) {
                System.out.println("====== DB FIX: FK_CATEGORY_COMPONENT check: " + e.getMessage() + " ======");
            }
        };
    }
}
