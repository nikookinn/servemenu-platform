package com.servemenu.userservice.infrastructure.grpc;

/**
 * Custom exception for gRPC communication errors
 */
public class GrpcCommunicationException extends RuntimeException {

    public GrpcCommunicationException(String message) {
        super(message);
    }

    public GrpcCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

