package com.mockavior.it;

import com.mockavior.app.MockaviorApplication;
import com.mockavior.app.config.AdminProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = MockaviorApplication.class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractMockaviorIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AdminProperties adminProperties;

    protected WebClient client;

    @BeforeAll
    void initClient() {
        this.client = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    protected void loadContract(String classpathLocation) {
        String currentVersion = fetchCurrentContractVersion();
        String contractBody = readClasspathFile(classpathLocation);

        client.put()
                .uri(adminPath("/contract"))
                .header("If-Match", currentVersion)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(contractBody)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    protected String fetchCurrentContractVersion() {

        ResponseEntity<String> response = client.get()
                .uri(adminPath("/contract"))
                .retrieve()
                .toEntity(String.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Admin /contract response is null");
        }

        String version = response.getHeaders()
                .getFirst("Mockavior-Contract-Version");

        if (version == null) {
            throw new IllegalStateException(
                    "Missing Mockavior-Contract-Version header in admin /contract response"
            );
        }

        return version;
    }

    protected String adminPath(String suffix) {
        return adminProperties.getPrefix() + suffix;
    }

    protected String readClasspathFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read contract file: " + path,
                    e
            );
        }
    }
}
