package capstone.paperhub_01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "capstone.paperhub_01.domain")
public class PaperHub01Application {

    public static void main(String[] args) {
        SpringApplication.run(PaperHub01Application.class, args);
    }

}
