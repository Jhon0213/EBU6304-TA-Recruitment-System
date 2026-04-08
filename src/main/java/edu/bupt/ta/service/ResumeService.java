package edu.bupt.ta.service;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.repository.ResumeInfoRepository;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.ValidationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public class ResumeService {

    public static final long CV_MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final ResumeInfoRepository resumeInfoRepository;

    public ResumeService(ResumeInfoRepository resumeInfoRepository) {
        this.resumeInfoRepository = resumeInfoRepository;
    }

    public ResumeInfo getOrCreateResume(String applicantId) {
        return resumeInfoRepository.findByApplicantId(applicantId).orElseGet(() -> {
            ResumeInfo resume = new ResumeInfo();
            resume.setApplicantId(applicantId);
            resume.setMaxWeeklyHours(8);
            resume.setLastUpdated(DateTimeUtils.now());
            resumeInfoRepository.save(resume);
            return resume;
        });
    }

    public ValidationResult saveResume(ResumeInfo resumeInfo) {
        List<String> errors = new ArrayList<>();
        if (resumeInfo.getMaxWeeklyHours() <= 0) {
            errors.add("maxWeeklyHours must be greater than 0");
        }
        if (resumeInfo.getAvailability() == null || resumeInfo.getAvailability().isEmpty()) {
            errors.add("at least one availability is required");
        }
        if (resumeInfo.getPersonalStatement() != null && resumeInfo.getPersonalStatement().length() > 500) {
            errors.add("personalStatement must be <= 500 chars");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.fail(errors);
        }

        resumeInfo.setLastUpdated(DateTimeUtils.now());
        resumeInfoRepository.save(resumeInfo);
        return ValidationResult.ok();
    }

    /**
     * Saves a CV file (PDF or DOCX) under {@code data/cv_uploads/{applicantId}/cv.{ext}}.
     * Does not parse file contents; structured fields remain manual.
     */
    public ValidationResult uploadCvFile(String applicantId, byte[] fileBytes, String originalFilename) {
        if (applicantId == null || applicantId.isBlank()) {
            return ValidationResult.fail("Applicant ID is required.");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            return ValidationResult.fail("File is empty.");
        }
        if (fileBytes.length > CV_MAX_FILE_BYTES) {
            return ValidationResult.fail("File exceeds 10 MB limit.");
        }
        String name = originalFilename == null ? "" : originalFilename.trim();
        String lower = name.toLowerCase(Locale.ROOT);
        String ext;
        if (lower.endsWith(".pdf")) {
            ext = "pdf";
        } else if (lower.endsWith(".docx")) {
            ext = "docx";
        } else {
            return ValidationResult.fail("Only PDF and DOCX files are supported.");
        }

        try {
            Path dir = AppPaths.cvUploadDir(applicantId);
            Files.createDirectories(dir);
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> {
                    String fn = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return fn.startsWith("cv.") && (fn.endsWith(".pdf") || fn.endsWith(".docx"));
                }).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
            Path target = dir.resolve("cv." + ext);
            Files.write(target, fileBytes);

            ResumeInfo resume = getOrCreateResume(applicantId);
            resume.setCvOriginalFileName(name.isEmpty() ? "cv." + ext : name);
            resume.setCvStoredExtension(ext);
            resume.setCvFileUploadedAt(DateTimeUtils.now());
            resume.setLastUpdated(DateTimeUtils.now());
            resumeInfoRepository.save(resume);
            return ValidationResult.ok();
        } catch (IOException e) {
            return ValidationResult.fail("Could not save file: " + e.getMessage());
        }
    }

    public boolean hasCvFileOnDisk(String applicantId) {
        return findCvFilePath(applicantId).isPresent();
    }

    /** Path to saved CV: {@code cv.pdf} or {@code cv.docx}, if present. */
    public Optional<Path> findCvFilePath(String applicantId) {
        if (applicantId == null || applicantId.isBlank()) {
            return Optional.empty();
        }
        Path pdf = AppPaths.cvUploadDir(applicantId).resolve("cv.pdf");
        if (Files.isRegularFile(pdf)) {
            return Optional.of(pdf);
        }
        Path docx = AppPaths.cvUploadDir(applicantId).resolve("cv.docx");
        if (Files.isRegularFile(docx)) {
            return Optional.of(docx);
        }
        return Optional.empty();
    }

    public List<String> getMissingResumeSections(String applicantId) {
        ResumeInfo resume = getOrCreateResume(applicantId);
        List<String> missing = new ArrayList<>();
        if (resume.getRelevantModules() == null || resume.getRelevantModules().isEmpty()) {
            missing.add("relevantModules");
        }
        if (resume.getTechnicalSkills() == null || resume.getTechnicalSkills().isEmpty()) {
            missing.add("technicalSkills");
        }
        if (resume.getAvailability() == null || resume.getAvailability().isEmpty()) {
            missing.add("availability");
        }
        if (resume.getPersonalStatement() == null || resume.getPersonalStatement().isBlank()) {
            missing.add("personalStatement");
        }
        return missing;
    }

    public int calculateResumeCompletion(String applicantId) {
        ResumeInfo resume = getOrCreateResume(applicantId);
        int total = 8;
        int complete = 0;
        if (resume.getCvFileUploadedAt() != null && hasCvFileOnDisk(applicantId)) {
            complete++;
        }
        if (resume.getRelevantModules() != null && !resume.getRelevantModules().isEmpty()) complete++;
        if (resume.getTechnicalSkills() != null && !resume.getTechnicalSkills().isEmpty()) complete++;
        if (resume.getLanguageSkills() != null && !resume.getLanguageSkills().isEmpty()) complete++;
        if (resume.getExperienceText() != null && !resume.getExperienceText().isBlank()) complete++;
        if (resume.getPersonalStatement() != null && !resume.getPersonalStatement().isBlank()) complete++;
        if (resume.getAvailability() != null && !resume.getAvailability().isEmpty()) complete++;
        if (resume.getMaxWeeklyHours() > 0) complete++;
        return complete * 100 / total;
    }
}
