package org.example.authservice;

import org.example.authservice.dto.LoginRequest;
import org.example.authservice.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private LoginController loginController;

    @Test
    void loginReturnsBearerTokenWithUserIdAndUsername() {
        var user = User.withUsername("martin")
                .password("encoded-password")
                .roles("USER")
                .build();

        when(userDetailsService.loadUserByUsername("martin")).thenReturn(user);
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
        var user = User.withUsername("martin")
                .password("encoded-password")
                .roles("USER")
                .build();

        when(userDetailsService.loadUserByUsername("martin")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> loginController.login(new LoginRequest("martin", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
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
