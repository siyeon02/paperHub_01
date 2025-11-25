package capstone.paperhub_01.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws.s3")
public class S3StorageProperties {
    private String bucket;
    private String region;
    private String baseUrlStyle; // "s3" or "https"
}
