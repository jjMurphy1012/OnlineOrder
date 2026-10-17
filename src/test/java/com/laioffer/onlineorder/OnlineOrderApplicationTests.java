package com.laioffer.onlineorder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "onlineorder.demo-data.enabled=false")
@EnabledIfEnvironmentVariable(named = "ONLINEORDER_TEST_JDBC_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class OnlineOrderApplicationTests {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.configure(registry);
    }

    @Test
    void contextLoads() {
    }

}
