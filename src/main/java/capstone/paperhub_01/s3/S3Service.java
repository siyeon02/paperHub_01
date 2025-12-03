package capstone.paperhub_01.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3;
    private final S3StorageProperties props;

    public String putPdfIfAbsent(String key, byte[] bytes, String sha256) {
        String bucket = props.getBucket();

        // 1) 존재 확인: S3Client.headObject 호출 (S3Config가 아니라 S3Client!)
        boolean exists = false;
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            exists = true;
        } catch (S3Exception e) {
            if (e.statusCode() != 404) throw e; // 404(없음)만 통과, 나머지는 그대로 에러
        }

        if (!exists) {
            // 2) 업로드
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .metadata(Map.of("sha256", sha256))
                    .build();

            s3.putObject(req, RequestBody.fromBytes(bytes));
        }

        return buildUri(key);
    }

    private String buildUri(String key) {
        if ("https".equalsIgnoreCase(props.getBaseUrlStyle())) {
            return "https://" + props.getBucket() + ".s3." + props.getRegion() + ".amazonaws.com/" + key;
        }
        return "s3://" + props.getBucket() + "/" + key;
    }

    public byte[] getBytes(String key) {
        try {
            return s3.getObjectAsBytes(software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build()).asByteArray();
        } catch (S3Exception e) {
            throw new IllegalStateException("Failed to download from S3: " + key, e);
        }
    }
}
