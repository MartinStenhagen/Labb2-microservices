package org.example.userservice.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.example.grpc.user.UserProfileRequest;
import org.example.grpc.user.UserProfileResponse;
import org.example.grpc.user.UserProfileServiceGrpc;
import org.example.userservice.dto.UserResponse;
import org.example.userservice.service.UserService;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class UserGrpcService extends UserProfileServiceGrpc.UserProfileServiceImplBase {

    private final UserService userService;

    public UserGrpcService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void getUserProfile(
            UserProfileRequest request,
            StreamObserver<UserProfileResponse> responseObserver
    ) {
        try {
            UserResponse user = userService.getUser(request.getUserId());

            UserProfileResponse response = UserProfileResponse.newBuilder()
                    .setUserId(user.id())
                    .setUsername(user.username())
                    .setDisplayName(user.displayName())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (RuntimeException exception) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found: " + request.getUserId())
                            .asRuntimeException()
            );
        }
    }
}
