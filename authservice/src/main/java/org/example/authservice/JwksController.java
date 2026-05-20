package org.example.authservice;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final ECKey ecKey;

    public JwksController(ECKey ecKey) {
        this.ecKey = ecKey;
    }

    @GetMapping("/auth/jwks")
    public Map<String, Object> jwks() {
        return new JWKSet(ecKey.toPublicJWK()).toJSONObject();
    }
}
