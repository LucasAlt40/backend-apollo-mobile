package br.com.apollomusic.app.infra.config;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.text.Normalizer;
import java.util.UUID;

@Component
public class ImageUploader {

    private final String bucket = "publico";
    private final String endpoint = "http://192.168.0.17:9000";
    private final String accessKey = "minioadmin";
    private final String secretKey = "minioadmin123";

    private final S3Client s3Client;

    public ImageUploader() {
        this.s3Client = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .forcePathStyle(true) // NECESSÁRIO para MinIO
                .build();
    }

    private String normalize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[^\\p{ASCII}]", "");   // remove acentos
        normalized = normalized.replaceAll("\\s+", "_");           // troca espaço por _
        normalized = normalized.replaceAll("[^A-Za-z0-9._-]", ""); // remove caracteres estranhos
        return normalized.toLowerCase();
    }

    public String upload(MultipartFile file) throws Exception {

        String original = file.getOriginalFilename();
        String cleanName = normalize(original);

        String fileName = UUID.randomUUID() + "-" + cleanName;

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .acl("public-read")  // importante p/ visualizar
                .build();

        s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));

        return endpoint + "/" + bucket + "/" + fileName;
    }
}
