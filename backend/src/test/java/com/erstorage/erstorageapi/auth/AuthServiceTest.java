package com.erstorage.erstorageapi.auth;

import com.erstorage.erstorageapi.common.LimitsProperties;
import com.erstorage.erstorageapi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthServiceTest {

    @Test
    void registerIsRejectedWhenRegistrationIsClosed() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        LimitsProperties closed = new LimitsProperties(false, DataSize.ofGigabytes(8));

        AuthService authService = new AuthService(userRepository, passwordEncoder, tokenService, closed);
        RegisterRequest request = new RegisterRequest("someone@example.com", "password123", "Someone");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(userRepository, passwordEncoder);
    }
}