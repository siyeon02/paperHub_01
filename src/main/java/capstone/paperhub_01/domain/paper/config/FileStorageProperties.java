package capstone.paperhub_01.domain.paper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class FileStorageProperties {
    private String baseDir = "/tmp/pdfs";
}
