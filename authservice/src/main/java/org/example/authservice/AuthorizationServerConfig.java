package org.example.authservice;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Set;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/auth/jwks", "/.well-known/**", "/oauth2/jwks").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder,
            @Value("${auth.oauth-client-id}") String clientId,
            @Value("${auth.oauth-client-secret}") String clientSecret,
            @Value("${auth.oauth-redirect-uri}") String redirectUri
    ) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .scopes(scopes -> scopes.addAll(
                        Set.of("user.read", "user.write",
                                OidcScopes.OPENID,
                                OidcScopes.PROFILE)))
                .tokenSettings(TokenSettings.builder()
                        .reuseRefreshTokens(false) // Rotation för säkerhet
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(@Value("${auth.issuer}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            JwsHeader.Builder headers = context.getJwsHeader();
            if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
                headers.algorithm(SignatureAlgorithm.ES256);
            } else if (context.getTokenType().getValue().equals(OidcParameterNames.ID_TOKEN)) {
                headers.algorithm(SignatureAlgorithm.ES256);
            }
        };
    }

    @Bean
    public ECKey ecKey(
            @Value("${auth.signing-key-jwk:}") String signingKeyJwk,
            @Value("${auth.signing-key-path:.local/authservice-ec-key.json}") String signingKeyPath
    ) {
        try {
            if (signingKeyJwk != null && !signingKeyJwk.isBlank()) {
                return ECKey.parse(signingKeyJwk);
            }

            Path keyPath = Path.of(signingKeyPath);
            if (Files.exists(keyPath)) {
                return ECKey.parse(Files.readString(keyPath, StandardCharsets.UTF_8));
            }

            ECKey generatedKey = generateEcKey();
            writeKey(keyPath, generatedKey);
            return generatedKey;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ECKey generateEcKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1")); // P-256

        KeyPair kp = kpg.generateKeyPair();
        ECPublicKey pub = (ECPublicKey) kp.getPublic();
        ECPrivateKey priv = (ECPrivateKey) kp.getPrivate();

        return new ECKey.Builder(Curve.P_256, pub)
                .privateKey(priv)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private void writeKey(Path keyPath, ECKey generatedKey) throws IOException {
        Path parent = keyPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(keyPath, generatedKey.toJSONString(), StandardCharsets.UTF_8);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(ECKey ecKey) {
        JWKSet jwkSet = new JWKSet(ecKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }
}
