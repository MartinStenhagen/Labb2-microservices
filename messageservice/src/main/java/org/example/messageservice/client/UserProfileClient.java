package org.example.messageservice.client;

import org.example.grpc.user.UserProfileRequest;
import org.example.grpc.user.UserProfileServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class UserProfileClient {

    private final UserProfileServiceGrpc.UserProfileServiceBlockingStub userProfileStub;

    public UserProfileClient(UserProfileServiceGrpc.UserProfileServiceBlockingStub userProfileStub) {
        this.userProfileStub = userProfileStub;
    }

    public UserProfile getUserProfile(Long userId) {
        var request = UserProfileRequest.newBuilder()
                .setUserId(userId)
                .build();

        var response = userProfileStub.getUserProfile(request);

        return new UserProfile(
                response.getUserId(),
                response.getUsername(),
                response.getDisplayName()
        );
    }

    public record UserProfile(
            Long userId,
            String username,
            String displayName
    ) {
    }
}
