package com.servemenu.keycloakevent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Lightweight DTO for email verification events.
 * Contains only essential data to minimize payload size.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailVerificationEventDTO {

    private String eventId;
    private String eventType;
    private String timestamp;
    private String userId;
    private boolean emailVerified;

    public EmailVerificationEventDTO() {
    }

    public EmailVerificationEventDTO(String eventId, String eventType, String timestamp, 
                                   String userId, boolean emailVerified) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.userId = userId;
        this.emailVerified = emailVerified;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public String toString() {
        return "EmailVerificationEventDTO{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", userId='" + userId + '\'' +
                ", emailVerified=" + emailVerified +
                '}';
    }
}
