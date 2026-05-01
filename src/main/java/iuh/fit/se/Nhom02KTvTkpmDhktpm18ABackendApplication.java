package iuh.fit.se;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

// @SpringBootApplication(exclude = {
//         org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration.class
// })
@SpringBootApplication
@EnableScheduling
public class Nhom02KTvTkpmDhktpm18ABackendApplication {

    public static void main(String[] args) {
        io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .systemProperties()
                .load();

        SpringApplication.run(Nhom02KTvTkpmDhktpm18ABackendApplication.class, args);
    }

}
