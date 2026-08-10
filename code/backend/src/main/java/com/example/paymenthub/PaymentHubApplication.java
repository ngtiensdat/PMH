package com.example.paymenthub;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
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
                System.out
                        .println("====== DB FIX: Dropped redundant ID column from PMH_COMPONENTS successfully ======");
            } catch (Exception e) {
                System.out.println("====== DB FIX: ID column check: " + e.getMessage() + " ======");
            }

            try {
                jdbcTemplate.execute("ALTER TABLE PMH_GROUP_CATEGORY DROP CONSTRAINT FK_CATEGORY_COMPONENT");
                System.out.println("====== DB FIX: Dropped FK_CATEGORY_COMPONENT constraint successfully ======");
            } catch (Exception e) {
                System.out.println("====== DB FIX: FK_CATEGORY_COMPONENT check: " + e.getMessage() + " ======");
            }

            // --- DEBUG AUDIT LOG TABLE ---
            try {
                Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM PMH_AUDIT_LOG", Integer.class);
                System.out.println("====== DB CHECK: PMH_AUDIT_LOG exists, row count = " + count + " ======");
            } catch (Exception e) {
                System.out.println("====== DB CHECK ERROR: PMH_AUDIT_LOG check failed: " + e.getMessage() + " ======");
            }

            try {
                jdbcTemplate.execute("INSERT INTO PMH_AUDIT_LOG (MODULE, RECORD_ID, ACTION, PERFORMED_BY, ACTION_DATE, DESCRIPTION, IP_ADDRESS) " +
                        "VALUES ('TEST', '1', 'TEST_ACTION', 'SYSTEM', SYSDATE, 'Test description', '127.0.0.1')");
                System.out.println("====== DB CHECK: Inserted test audit log successfully ======");
                // Clean up test log
                jdbcTemplate.execute("DELETE FROM PMH_AUDIT_LOG WHERE MODULE = 'TEST'");
            } catch (Exception e) {
                System.out.println("====== DB CHECK ERROR: Insert test audit log failed: " + e.getMessage() + " ======");
            }
        };
    }
}
