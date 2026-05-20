package org.example.userservice.service;

import org.example.userservice.dto.CreateUserRequest;
import org.example.userservice.dto.UpdateUserRequest;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.model.AppUser;
import org.example.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(CreateUserRequest request) {
        AppUser user = new AppUser(request.username(), request.displayName());
        AppUser savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }

    public UserResponse getUser(Long id) {
        return toResponse(findUser(id));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        AppUser user = findUser(id);
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());

        return toResponse(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        AppUser user = findUser(id);
        userRepository.delete(user);
    }

    private AppUser findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName()
        );
    }
}
