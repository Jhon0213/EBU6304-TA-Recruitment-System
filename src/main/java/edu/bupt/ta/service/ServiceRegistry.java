package edu.bupt.ta.service;

import edu.bupt.ta.repository.ApplicantProfileRepository;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.AuditLogRepository;
import edu.bupt.ta.repository.FavouriteJobRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.NotificationRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.repository.UserRepository;
import edu.bupt.ta.repository.UserSettingsRepository;
import edu.bupt.ta.repository.WorkloadRepository;

public class ServiceRegistry {

    private final UserRepository userRepository = new UserRepository();
    private final ApplicantProfileRepository applicantProfileRepository = new ApplicantProfileRepository();
    private final ResumeInfoRepository resumeInfoRepository = new ResumeInfoRepository();
    private final JobRepository jobRepository = new JobRepository();
    private final ApplicationRepository applicationRepository = new ApplicationRepository();
    private final WorkloadRepository workloadRepository = new WorkloadRepository();
    private final AuditLogRepository auditLogRepository = new AuditLogRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository(
            edu.bupt.ta.config.AppPaths.notificationsJson());
    private final FavouriteJobRepository favouriteJobRepository = new FavouriteJobRepository(
            edu.bupt.ta.config.AppPaths.favouriteJobsJson());
    private final UserSettingsRepository userSettingsRepository = new UserSettingsRepository();

    private final AuthenticationService authenticationService = new AuthenticationService(userRepository);
    private final ApplicantProfileService applicantProfileService = new ApplicantProfileService(
            applicantProfileRepository, userRepository
    );
    private final ResumeService resumeService = new ResumeService(resumeInfoRepository);
    private final JobService jobService = new JobService(jobRepository, auditLogRepository);
    private final WorkloadService workloadService = new WorkloadService(
            workloadRepository, applicationRepository, jobRepository, resumeInfoRepository
    );
    private final MatchingService matchingService = new MatchingService(
            resumeInfoRepository, jobRepository, workloadService
    );
    private final DeepSeekJobMatchService deepSeekJobMatchService = new DeepSeekJobMatchService();
    private final ApplicationService applicationService = new ApplicationService(
            applicationRepository, jobRepository, applicantProfileRepository, resumeInfoRepository,
            auditLogRepository, matchingService
    );
    private final ReviewService reviewService = new ReviewService(
            applicationRepository, jobRepository, applicantProfileRepository, resumeInfoRepository,
            workloadService, auditLogRepository
    );
    private final ExportService exportService = new ExportService(
            workloadRepository, applicationRepository, jobRepository
    );
    private final AdminMonitoringService adminMonitoringService = new AdminMonitoringService(
            userRepository, applicantProfileRepository, resumeInfoRepository, jobRepository,
            applicationRepository, workloadRepository, auditLogRepository, jobService, workloadService
    );
    private NotificationService notificationService;
    private RiskChangeTracker riskChangeTracker;

    public ServiceRegistry() {
        this.notificationService = new NotificationService(
                notificationRepository, jobRepository, applicationRepository, applicantProfileRepository
        );
        this.riskChangeTracker = new RiskChangeTracker(notificationRepository, userRepository);
        applicationService.setNotificationService(notificationService);
        reviewService.setNotificationService(notificationService);
    }

    public RiskChangeTracker riskChangeTracker() {
        return riskChangeTracker;
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public ApplicantProfileRepository applicantProfileRepository() {
        return applicantProfileRepository;
    }

    public ResumeInfoRepository resumeInfoRepository() {
        return resumeInfoRepository;
    }

    public JobRepository jobRepository() {
        return jobRepository;
    }

    public ApplicationRepository applicationRepository() {
        return applicationRepository;
    }

    public WorkloadRepository workloadRepository() {
        return workloadRepository;
    }

    public AuditLogRepository auditLogRepository() {
        return auditLogRepository;
    }

    public AuthenticationService authenticationService() {
        return authenticationService;
    }

    public ApplicantProfileService applicantProfileService() {
        return applicantProfileService;
    }

    public ResumeService resumeService() {
        return resumeService;
    }

    public JobService jobService() {
        return jobService;
    }

    public ApplicationService applicationService() {
        return applicationService;
    }

    public WorkloadService workloadService() {
        return workloadService;
    }

    public ReviewService reviewService() {
        return reviewService;
    }

    public ExportService exportService() {
        return exportService;
    }

    public MatchingService matchingService() {
        return matchingService;
    }

    public DeepSeekJobMatchService deepSeekJobMatchService() {
        return deepSeekJobMatchService;
    }

    public AdminMonitoringService adminMonitoringService() {
        return adminMonitoringService;
    }

    public NotificationService notificationService() {
        return notificationService;
    }

    public FavouriteJobRepository favouriteJobRepository() {
        return favouriteJobRepository;
    }

    public UserSettingsRepository userSettingsRepository() {
        return userSettingsRepository;
    }
}
