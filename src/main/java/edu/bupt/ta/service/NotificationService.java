package edu.bupt.ta.service;

import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.Notification;
import edu.bupt.ta.model.Notification.NotificationType;
import edu.bupt.ta.repository.ApplicantProfileRepository;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.NotificationRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.IdGenerator;

import java.util.List;

public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicantProfileRepository profileRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               JobRepository jobRepository,
                               ApplicationRepository applicationRepository,
                               ApplicantProfileRepository profileRepository) {
        this.notificationRepository = notificationRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.profileRepository = profileRepository;
    }

    public void notifyApplicationAccepted(String applicantId, String applicationId, String jobId, String organiserName) {
        String jobTitle = getJobTitle(jobId);
        String notificationId = IdGenerator.next("NOT",
                notificationRepository.findAll().stream()
                        .map(Notification::getNotificationId).toList(), 6);

        Notification notification = new Notification(
                notificationId,
                applicantId,
                NotificationType.APPLICATION_ACCEPTED,
                "Application Accepted",
                "Your application for \"" + jobTitle + "\" has been accepted by " + organiserName + ".",
                jobId,
                applicationId
        );
        notificationRepository.save(notification);
    }

    public void notifyApplicationRejected(String applicantId, String applicationId, String jobId, String organiserName) {
        String jobTitle = getJobTitle(jobId);
        String notificationId = IdGenerator.next("NOT",
                notificationRepository.findAll().stream()
                        .map(Notification::getNotificationId).toList(), 6);

        Notification notification = new Notification(
                notificationId,
                applicantId,
                NotificationType.APPLICATION_REJECTED,
                "Application Rejected",
                "Your application for \"" + jobTitle + "\" has been rejected by " + organiserName + ".",
                jobId,
                applicationId
        );
        notificationRepository.save(notification);
    }

    public void notifyNewApplication(String organiserId, String applicationId, String jobId, String applicantName) {
        String jobTitle = getJobTitle(jobId);
        String notificationId = IdGenerator.next("NOT",
                notificationRepository.findAll().stream()
                        .map(Notification::getNotificationId).toList(), 6);

        Notification notification = new Notification(
                notificationId,
                organiserId,
                NotificationType.NEW_APPLICATION,
                "New Application Received",
                applicantName + " has applied for \"" + jobTitle + "\".",
                jobId,
                applicationId
        );
        notificationRepository.save(notification);
    }

    public void notifyApplicationUnderReview(String applicantId, String applicationId, String jobId) {
        String jobTitle = getJobTitle(jobId);
        String notificationId = IdGenerator.next("NOT",
                notificationRepository.findAll().stream()
                        .map(Notification::getNotificationId).toList(), 6);

        Notification notification = new Notification(
                notificationId,
                applicantId,
                NotificationType.APPLICATION_UNDER_REVIEW,
                "Application Under Review",
                "Your application for \"" + jobTitle + "\" is now under review.",
                jobId,
                applicationId
        );
        notificationRepository.save(notification);
    }

    public void notifyJobOpened(String targetUserId, String jobId) {
        String jobTitle = getJobTitle(jobId);
        String notificationId = IdGenerator.next("NOT",
                notificationRepository.findAll().stream()
                        .map(Notification::getNotificationId).toList(), 6);

        Notification notification = new Notification(
                notificationId,
                targetUserId,
                NotificationType.JOB_OPENED,
                "New Job Available",
                "A new position \"" + jobTitle + "\" is now open for applications.",
                jobId,
                null
        );
        notificationRepository.save(notification);
    }

    public void notifyJobClosed(String targetUserId, String jobId) {
        String jobTitle = getJobTitle(jobId);
        String notificationId = IdGenerator.next("NOT",
                notificationRepository.findAll().stream()
                        .map(Notification::getNotificationId).toList(), 6);

        Notification notification = new Notification(
                notificationId,
                targetUserId,
                NotificationType.JOB_CLOSED,
                "Job Closed",
                "The position \"" + jobTitle + "\" is now closed.",
                jobId,
                null
        );
        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByRecipientUserId(userId);
    }

    public List<Notification> getUnreadNotificationsForUser(String userId) {
        return notificationRepository.findUnreadByRecipientUserId(userId);
    }

    public int getUnreadCount(String userId) {
        return notificationRepository.countUnreadByRecipientUserId(userId);
    }

    public void markAsRead(String notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByRecipientUserId(userId);
    }

    public void clearAllNotifications(String userId) {
        notificationRepository.deleteAllByRecipientUserId(userId);
    }

    private String getJobTitle(String jobId) {
        return jobRepository.findById(jobId)
                .map(Job::getTitle)
                .orElse("Unknown Position");
    }

    private String getApplicantName(String applicantId) {
        return profileRepository.findById(applicantId)
                .map(profile -> profile.getFullName() != null ? profile.getFullName() : applicantId)
                .orElse(applicantId);
    }

    public void notifyNewApplicationToOrganiser(String applicationId, String jobId) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        Job job = jobRepository.findById(jobId).orElse(null);

        if (application == null || job == null) {
            return;
        }

        String applicantName = getApplicantName(application.getApplicantId());
        notifyNewApplication(job.getOrganiserId(), applicationId, jobId, applicantName);
    }

    public void notifyApplicationStatusChange(String applicationId, boolean accepted, String actorName) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            return;
        }

        if (accepted) {
            notifyApplicationAccepted(application.getApplicantId(), applicationId,
                    application.getJobId(), actorName);
        } else {
            notifyApplicationRejected(application.getApplicantId(), applicationId,
                    application.getJobId(), actorName);
        }
    }
}
