package edu.bupt.ta.service;

import edu.bupt.ta.dto.AdminWorkloadRowDTO;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.Notification;
import edu.bupt.ta.model.Notification.NotificationType;
import edu.bupt.ta.model.User;
import edu.bupt.ta.repository.NotificationRepository;
import edu.bupt.ta.repository.UserRepository;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.IdGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiskChangeTracker {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<String, String> lastRiskByApplicant = new HashMap<>();
    private boolean initialized;

    public RiskChangeTracker(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public synchronized void detectAndNotifyChanges(List<AdminWorkloadRowDTO> rows) {
        if (rows == null) {
            return;
        }
        if (!initialized) {
            for (AdminWorkloadRowDTO row : rows) {
                lastRiskByApplicant.put(row.applicantId(), computeRisk(row.acceptedJobs()));
            }
            initialized = true;
            return;
        }

        for (AdminWorkloadRowDTO row : rows) {
            String current = computeRisk(row.acceptedJobs());
            String previous = lastRiskByApplicant.get(row.applicantId());
            if (previous != null && !previous.equals(current)) {
                notifyAdmins(row.applicantName(), previous, current);
            }
            lastRiskByApplicant.put(row.applicantId(), current);
        }
    }

    private String computeRisk(int acceptedJobs) {
        if (acceptedJobs <= 0) {
            return "LOW";
        }
        if (acceptedJobs >= 7) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private void notifyAdmins(String applicantName, String from, String to) {
        String message = I18n.t("risk_change_message")
                .replace("{name}", applicantName == null ? "TA" : applicantName)
                .replace("{from}", from)
                .replace("{to}", to);
        String title = I18n.t("risk_change_title");

        for (User user : userRepository.findAll()) {
            if (user.getRole() != Role.ADMIN || !user.isActive()) {
                continue;
            }
            String notificationId = IdGenerator.next("NOT",
                    notificationRepository.findAll().stream()
                            .map(Notification::getNotificationId).toList(), 6);
            Notification notification = new Notification(
                    notificationId,
                    user.getUserId(),
                    NotificationType.WORKLOAD_WARNING,
                    title,
                    message,
                    null,
                    null
            );
            notificationRepository.save(notification);
        }
    }
}
