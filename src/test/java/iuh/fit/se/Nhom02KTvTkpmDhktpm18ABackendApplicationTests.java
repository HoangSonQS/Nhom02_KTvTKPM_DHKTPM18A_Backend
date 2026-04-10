package iuh.fit.se;

import iuh.fit.se.shared.application.port.out.EmailPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;


@SpringBootTest
class Nhom02KTvTkpmDhktpm18ABackendApplicationTests extends BaseIntegrationTest {

    @MockitoBean
    private EmailPort emailPort;

    @MockitoBean
    private StringRedisTemplate redisTemplate;



    @Test
    void contextLoads() {
    }
}
