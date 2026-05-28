package org.example.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "internal.api-key=test-internal-api-key")
class BffApplicationTests {

    @Test
    void contextLoads() {
    }

}
