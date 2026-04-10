package iuh.fit.se.modules.payment;

import iuh.fit.se.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentModuleSmokeTest extends BaseIntegrationTest {

    @Test
    void contextLoads() {
        // Kiểm tra xem ApplicationContext có load thành công hay không
        // Nếu có lỗi Schema Validation, test này sẽ FAIL ngay lập tức
    }
}
