package edu.bupt.ta.repository;

import edu.bupt.ta.model.Notification;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationRepository extends AbstractJsonRepository<Notification, String> {

    public NotificationRepository(Path filePath) {
        super(filePath, Notification.class);
    }

    public List<Notification> findByRecipientUserId(String userId) {
        return findAll().stream()
                .filter(n -> userId.equals(n.getRecipientUserId()))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Notification> findUnreadByRecipientUserId(String userId) {
        return findAll().stream()
                .filter(n -> userId.equals(n.getRecipientUserId()) && !n.isRead())
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public int countUnreadByRecipientUserId(String userId) {
        return (int) findAll().stream()
                .filter(n -> userId.equals(n.getRecipientUserId()) && !n.isRead())
                .count();
    }

    public void markAsRead(String notificationId) {
        findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            save(notification);
        });
    }

    public void markAllAsReadByRecipientUserId(String userId) {
        findByRecipientUserId(userId).stream()
                .filter(n -> !n.isRead())
                .forEach(n -> {
                    n.setRead(true);
                    save(n);
                });
    }

    public void deleteAllByRecipientUserId(String userId) {
        List<Notification> toDelete = findAll().stream()
                .filter(n -> userId.equals(n.getRecipientUserId()))
                .collect(Collectors.toList());
        toDelete.forEach(n -> deleteById(n.getNotificationId()));
    }
}
