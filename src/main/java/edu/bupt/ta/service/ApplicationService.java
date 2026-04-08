package edu.bupt.ta.service;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.AuditLogEntry;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.repository.ApplicantProfileRepository;
import edu.bupt.ta.repository.ApplicationRepository;
import edu.bupt.ta.repository.AuditLogRepository;
import edu.bupt.ta.repository.JobRepository;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.IdGenerator;
import edu.bupt.ta.util.ValidationResult;

import java.util.List;
import java.util.Optional;

public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ApplicantProfileRepository profileRepository;
    private final ResumeInfoRepository resumeInfoRepository;
    private final AuditLogRepository auditLogRepository;
    private final MatchingService matchingService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              ApplicantProfileRepository profileRepository,
                              ResumeInfoRepository resumeInfoRepository,
                              AuditLogRepository auditLogRepository,
                              MatchingService matchingService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.resumeInfoRepository = resumeInfoRepository;
        this.auditLogRepository = auditLogRepository;
        this.matchingService = matchingService;
    }

    public ValidationResult apply(String applicantId, String jobId, String statement) {
        if (applicantId == null || applicantId.isBlank()) {
            return ValidationResult.fail("Applicant ID is required.");
        }
        if (jobId == null || jobId.isBlank()) {
            return ValidationResult.fail("Job ID is required.");
        }

        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ValidationResult.fail("Job not found.");
        }

        Job job = jobOpt.get();
        if (job.getStatus() != JobStatus.OPEN) {
            return ValidationResult.fail("Only OPEN jobs can be applied.");
        }
        if (job.getDeadline() != null && job.getDeadline().isBefore(DateTimeUtils.today())) {
            return ValidationResult.fail("Job deadline has passed.");
        }

        if (applicationRepository.findByJobIdAndApplicantId(jobId, applicantId).isPresent()) {
            return ValidationResult.fail("Duplicate application is not allowed.");
        }

        if (profileRepository.findById(applicantId).isEmpty()) {
            return ValidationResult.fail("Please complete profile before applying.");
        }
        if (resumeInfoRepository.findByApplicantId(applicantId).isEmpty()) {
            return ValidationResult.fail("Please complete resume before applying.");
        }

        String newId = IdGenerator.next("APP", applicationRepository.findAll().stream()
                .map(edu.bupt.ta.model.Application::getApplicationId).toList(), 3);

        edu.bupt.ta.model.Application application = new edu.bupt.ta.model.Application();
        application.setApplicationId(newId);
        application.setApplicantId(applicantId);
        application.setJobId(jobId);
        application.setApplyDate(DateTimeUtils.now());
        application.setStatement(statement == null ? "" : statement.trim());
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setDecisionNote("");
        MatchExplanationDTO matchExplanation = matchingService.evaluateMatch(applicantId, jobId);
        application.setMatchScore(matchExplanation.score());
        application.setMissingSkills(matchExplanation.missingSkills());

        applicationRepository.save(application);
        auditLogRepository.append(new AuditLogEntry(DateTimeUtils.now(), applicantId, "APPLY_JOB",
                newId + " created for " + jobId));
        return ValidationResult.ok();
    }

    public List<edu.bupt.ta.model.Application> getApplicationsByApplicant(String applicantId) {
        return applicationRepository.findByApplicantId(applicantId);
    }

    public List<edu.bupt.ta.model.Application> getApplicationsByJob(String jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    public void markUnderReview(String applicationId) {
        applicationRepository.findById(applicationId).ifPresent(application -> {
            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            applicationRepository.save(application);
        });
    }

    public ValidationResult cancelApplication(String applicantId, String jobId) {
        if (applicantId == null || applicantId.isBlank()) {
            return ValidationResult.fail("Applicant ID is required.");
        }
        if (jobId == null || jobId.isBlank()) {
            return ValidationResult.fail("Job ID is required.");
        }

        Optional<edu.bupt.ta.model.Application> applicationOpt =
                applicationRepository.findByJobIdAndApplicantId(jobId, applicantId);

        if (applicationOpt.isEmpty()) {
            return ValidationResult.fail("Application not found.");
        }

        edu.bupt.ta.model.Application application = applicationOpt.get();

        // SUBMITTED, UNDER_REVIEW, ACCEPTED 状态都可以取消
        if (application.getStatus() != ApplicationStatus.SUBMITTED &&
            application.getStatus() != ApplicationStatus.UNDER_REVIEW &&
            application.getStatus() != ApplicationStatus.ACCEPTED) {
            return ValidationResult.fail("Cannot cancel application with status: " + application.getStatus());
        }

        // 直接删除申请记录，而不是标记为 CANCELLED
        // 这样用户可以再次申请
        applicationRepository.deleteById(application.getApplicationId());

        auditLogRepository.append(new AuditLogEntry(
                DateTimeUtils.now(),
                applicantId,
                "CANCEL_APPLICATION",
                "Application " + application.getApplicationId() + " deleted for job " + jobId
        ));

        return ValidationResult.ok();
    }

    public boolean hasApplied(String applicantId, String jobId) {
        if (applicantId == null || jobId == null) {
            return false;
        }
        return applicationRepository.findByJobIdAndApplicantId(jobId, applicantId).isPresent();
    }

    public Optional<edu.bupt.ta.model.Application> getApplication(String applicantId, String jobId) {
        if (applicantId == null || jobId == null) {
            return Optional.empty();
        }
        return applicationRepository.findByJobIdAndApplicantId(jobId, applicantId);
    }
}
