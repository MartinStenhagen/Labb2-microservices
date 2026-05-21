package org.example.authservice;

import org.example.authservice.client.UserServiceClient;
import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.example.authservice.dto.RegisterRequest;
import org.example.authservice.model.AuthUser;
import org.example.authservice.repository.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private LoginController loginController;

    @BeforeEach
    void setUp() {
        loginController = new LoginController(
                authUserRepository,
                userServiceClient,
                passwordEncoder,
                jwtEncoder,
                "http://authservice:9000"
        );
    }

    @Test
    void loginReturnsBearerTokenWithUserIdAndUsername() {
        AuthUser user = new AuthUser("martin", "encoded-password", 1L);

        when(authUserRepository.findByUsername("martin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt("token-value"));

        LoginResponse response = loginController.login(new LoginRequest("martin", "password"));

        assertThat(response.accessToken()).isEqualTo("token-value");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("martin");
        assertThat(response.expiresAt()).isAfter(Instant.now());
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void loginRejectsInvalidPassword() {
        AuthUser user = new AuthUser("martin", "encoded-password", 1L);

        when(authUserRepository.findByUsername("martin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> loginController.login(new LoginRequest("martin", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void registerCreatesUserProfileCredentialsAndToken() {
        when(authUserRepository.existsByUsername("sara")).thenReturn(false);
        when(userServiceClient.createUser("sara", "Sara Lind"))
                .thenReturn(new UserServiceClient.UserResponse(7L, "sara", "Sara Lind"));
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt("register-token"));

        LoginResponse response = loginController.register(new RegisterRequest("sara", "secret", "Sara Lind"));

        assertThat(response.accessToken()).isEqualTo("register-token");
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.username()).isEqualTo("sara");
        verify(userServiceClient).createUser("sara", "Sara Lind");
        verify(authUserRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getUsername().equals("sara")
                        && user.getPasswordHash().equals("encoded-secret")
                        && user.getUserId().equals(7L)
        ));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(authUserRepository.existsByUsername("martin")).thenReturn(true);

        assertThatThrownBy(() -> loginController.register(new RegisterRequest("martin", "password", "Martin")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verifyNoInteractions(userServiceClient);
        verify(authUserRepository, never()).save(any(AuthUser.class));
    }

    private Jwt jwt(String tokenValue) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);

        return new Jwt(
                tokenValue,
                issuedAt,
                expiresAt,
                Map.of("alg", "ES256"),
                Map.of("sub", "martin")
        );
    }
}
