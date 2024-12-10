package sit.int221.integratedproject.kanbanborad.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AzureConfig {
    @Value("${azure.tenant-id}")
    private String tenantId;
    @Value("${azure.client-id}")
    private String clientId;
    @Value("${azure.client-secret}")
    private String clientSecret;
    @Value("${azure.token-url}")
    private String tokenUrl;
    @Getter
    private static AzureConfig instance;
    @PostConstruct
    private void initInstance() {
        instance = this;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }
}

