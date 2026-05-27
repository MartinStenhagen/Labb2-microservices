package org.example.authservice;

import jakarta.validation.Valid;
import org.example.authservice.client.UserServiceClient;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.model.AuthUser;
import org.example.authservice.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final AuthUserRepository authUserRepository;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String issuer;

    public LoginController(
            AuthUserRepository authUserRepository,
            UserServiceClient userServiceClient,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${auth.issuer}") String issuer
    ) {
        this.authUserRepository = authUserRepository;
        this.userServiceClient = userServiceClient;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthUser user = authUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return createToken(user);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        if (authUserRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserServiceClient.UserResponse userProfile = userServiceClient.createUser(
                request.username(),
                request.displayName()
        );

        AuthUser authUser = new AuthUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                userProfile.getId()
        );

        AuthUser savedUser = authUserRepository.save(authUser);

        return createToken(savedUser);
    }

    private LoginResponse createToken(AuthUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(1, ChronoUnit.HOURS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("username", user.getUsername())
                .claim("scope", "chat.read chat.write")
                .build();

        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.ES256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", user.getUserId(), user.getUsername(), expiresAt);
    }
}
