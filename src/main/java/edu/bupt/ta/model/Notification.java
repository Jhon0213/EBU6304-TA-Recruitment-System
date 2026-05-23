package edu.bupt.ta.model;

import edu.bupt.ta.repository.Identifiable;

import java.time.LocalDateTime;

public class Notification implements Identifiable<String> {

    public enum NotificationType {
        APPLICATION_ACCEPTED,
        APPLICATION_REJECTED,
        NEW_APPLICATION,
        APPLICATION_UNDER_REVIEW,
        WORKLOAD_WARNING,
        JOB_OPENED,
        JOB_CLOSED
    }

    public enum TargetRole {
        TA,
        MO,
        ALL
    }

    private String notificationId;
    private String recipientUserId;
    private NotificationType type;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
    private String relatedJobId;
    private String relatedApplicationId;

    public Notification() {
    }

    public Notification(String notificationId, String recipientUserId, NotificationType type,
                       String title, String message, String relatedJobId, String relatedApplicationId) {
        this.notificationId = notificationId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
        this.relatedJobId = relatedJobId;
        this.relatedApplicationId = relatedApplicationId;
    }

    @Override
    public String getId() {
        return notificationId;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getRelatedJobId() {
        return relatedJobId;
    }

    public void setRelatedJobId(String relatedJobId) {
        this.relatedJobId = relatedJobId;
    }

    public String getRelatedApplicationId() {
        return relatedApplicationId;
    }

    public void setRelatedApplicationId(String relatedApplicationId) {
        this.relatedApplicationId = relatedApplicationId;
    }
}
