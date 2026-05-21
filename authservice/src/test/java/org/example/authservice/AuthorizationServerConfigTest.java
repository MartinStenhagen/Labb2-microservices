package org.example.authservice;

import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationServerConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    void ecKeyIsPersistedAndReusedFromFile() throws Exception {
        AuthorizationServerConfig config = new AuthorizationServerConfig();
        Path keyPath = tempDir.resolve("authservice-ec-key.json");

        ECKey firstKey = config.ecKey("", keyPath.toString());
        ECKey secondKey = config.ecKey("", keyPath.toString());

        assertThat(Files.exists(keyPath)).isTrue();
        assertThat(secondKey.getKeyID()).isEqualTo(firstKey.getKeyID());
        assertThat(secondKey.toPublicJWK().toJSONString()).isEqualTo(firstKey.toPublicJWK().toJSONString());
    }
}
