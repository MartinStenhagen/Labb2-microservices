package org.example.userservice.service;

import org.example.userservice.dto.CreateUserRequest;
import org.example.userservice.dto.UpdateUserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.model.AppUser;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserSavesUsernameAndDisplayName() {
        AppUser savedUser = new AppUser("martin", "Martin Stenhagen");
        when(userRepository.existsByUsername("martin")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(AppUser.class))).thenReturn(savedUser);

        UserResponse response = userService.createUser(new CreateUserRequest("martin", "Martin Stenhagen"));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getUsername()).isEqualTo("martin");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("Martin Stenhagen");
        assertThat(response.username()).isEqualTo("martin");
        assertThat(response.displayName()).isEqualTo("Martin Stenhagen");
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("martin")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest("martin", "Martin")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(AppUser.class));
    }

    @Test
    void getUsersReturnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(
                new AppUser("martin", "Martin Stenhagen"),
                new AppUser("sara", "Sara Lind")
        ));

        List<UserResponse> users = userService.getUsers();

        assertThat(users)
                .extracting(UserResponse::username)
                .containsExactly("martin", "sara");
    }

    @Test
    void updateUserChangesExistingUser() {
        AppUser existingUser = new AppUser("martin", "Martin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot("martin2", 1L)).thenReturn(false);
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        UserResponse response = userService.updateUser(
                1L,
                new UpdateUserRequest("martin2", "Martin S")
        );

        assertThat(response.username()).isEqualTo("martin2");
        assertThat(response.displayName()).isEqualTo("Martin S");
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUserRejectsUsernameUsedByAnotherUser() {
        AppUser existingUser = new AppUser("martin", "Martin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot("sara", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(
                1L,
                new UpdateUserRequest("sara", "Martin S")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(existingUser);
    }

    @Test
    void deleteUserDeletesExistingUser() {
        AppUser existingUser = new AppUser("martin", "Martin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.deleteUser(1L);

        verify(userRepository).delete(existingUser);
    }

    @Test
    void getUserThrowsNotFoundWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }
}
