package iuh.fit.se;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication(exclude = {
//         org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration.class
// })
@SpringBootApplication
public class Nhom02KTvTkpmDhktpm18ABackendApplication {

    public static void main(String[] args) {
        io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .systemProperties()
                .load();

        SpringApplication.run(Nhom02KTvTkpmDhktpm18ABackendApplication.class, args);
    }

}
