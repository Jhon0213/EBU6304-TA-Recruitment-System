package edu.bupt.ta.controller;

import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class ApplicantReviewController {

    private final ServiceRegistry services;
    private final User user;
    private final String applicationId;

    private final VBox view = new VBox(14);
    private final TextArea decisionNote = new TextArea();
    private ApplicantReviewDTO reviewData;

    public ApplicantReviewController(ServiceRegistry services, User user, String applicationId) {
        this.services = services;
        this.user = user;
        this.applicationId = applicationId;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        ApplicantReviewDTO dto = services.reviewService().getApplicantReviewData(applicationId, user.getUserId(), isAdmin());
        this.reviewData = dto;

        view.setPadding(new Insets(16));
        view.getStyleClass().add("app-surface");

        view.getChildren().clear();

        ApplicantProfile profile = services.applicantProfileRepository().findById(dto.applicantId()).orElse(null);
        ApplicationStatus status = services.applicationRepository().findById(applicationId)
                .map(Application::getStatus)
                .orElse(null);

        VBox hero = buildHero(dto, profile, status);
        VBox basicInfo = buildBasicInformation(dto, profile);
        VBox skills = buildSkillsCompetencies(applicationId);
        VBox attachments = buildAttachments(dto);
        VBox statement = buildStatement(dto);
        VBox noteCard = buildDecisionNote(dto);
        HBox actions = buildActions();

        HBox middleRow = new HBox(14, skills, attachments);
        middleRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(skills, Priority.ALWAYS);
        skills.setMaxWidth(Double.MAX_VALUE);
        attachments.setMaxWidth(Double.MAX_VALUE);
        attachments.setPrefWidth(320);

        statement.setMaxWidth(Double.MAX_VALUE);

        view.getChildren().addAll(hero, basicInfo, middleRow, statement, noteCard, actions);
    }

    private VBox buildHero(ApplicantReviewDTO dto, ApplicantProfile profile, ApplicationStatus status) {
        VBox left = new VBox(6);
        Label name = new Label(dto.applicantName());
        name.getStyleClass().add("review-hero-title");

        String programme = profile == null ? "" : safe(profile.getProgramme());
        String year = profile == null || profile.getYear() <= 0 ? "" : (" (Year " + profile.getYear() + ")");
        String subtitle = (programme == null || programme.isBlank())
                ? ("Applicant ID: " + dto.applicantId())
                : (programme + year + "   •   Applicant ID: " + dto.applicantId());
        Label meta = new Label(subtitle);
        meta.getStyleClass().add("review-hero-subtitle");
        meta.setWrapText(true);
        left.getChildren().addAll(name, meta);

        Label statusBadge = new Label(formatStatus(status));
        statusBadge.getStyleClass().addAll("status-pill", statusPillClass(status));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, left, spacer, statusBadge);
        top.setAlignment(Pos.TOP_LEFT);

        VBox hero = new VBox(top);
        hero.getStyleClass().addAll("panel-card", "review-hero");
        hero.setPadding(new Insets(16));
        return hero;
    }

    private VBox buildBasicInformation(ApplicantReviewDTO dto, ApplicantProfile profile) {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));

        HBox header = sectionHeader("Basic Information");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("review-grid");
        grid.setHgap(24);
        grid.setVgap(14);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(33);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(33);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(34);
        grid.getColumnConstraints().addAll(c0, c1, c2);

        String name = profile == null ? safe(dto.applicantName()) : safe(profile.getFullName());
        String studentId = profile == null ? "" : safe(profile.getStudentId());
        String email = profile == null ? "" : safe(profile.getEmail());
        String phone = profile == null ? "" : safe(profile.getPhone());
        String major = profile == null ? "" : safe(profile.getProgramme());

        grid.add(field("Name", blankToDash(name)), 0, 0);
        grid.add(field("Student ID", blankToDash(studentId)), 1, 0);
        grid.add(field("Email address", blankToDash(email)), 2, 0);

        grid.add(field("Phone number", blankToDash(phone)), 0, 1);
        grid.add(field("Major", blankToDash(major)), 1, 1);

        card.getChildren().addAll(header, grid);
        return card;
    }

    private VBox buildStatement(ApplicantReviewDTO dto) {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));

        HBox header = sectionHeader("Application Statement");

        Label statement = new Label(blankToDash(dto.statement()));
        statement.getStyleClass().add("review-body");
        statement.setWrapText(true);

        card.getChildren().addAll(header, statement);
        return card;
    }

    private VBox buildSkillsCompetencies(String applicationId) {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));

        HBox header = sectionHeader("Skills & Competencies");

        java.util.List<String> requiredSkills = services.applicationRepository().findById(applicationId)
                .map(Application::getJobId)
                .flatMap(jobId -> services.jobRepository().findById(jobId))
                .map(Job::getRequiredSkills)
                .orElse(java.util.List.of());

        FlowPane chips = new FlowPane();
        chips.setHgap(8);
        chips.setVgap(8);

        boolean hasAny = false;
        for (String skill : requiredSkills) {
            if (skill == null || skill.isBlank()) {
                continue;
            }
            Label chip = new Label(skill.trim());
            chip.getStyleClass().add("tag-chip");
            chips.getChildren().add(chip);
            hasAny = true;
        }

        if (!hasAny) {
            Label empty = new Label("-");
            empty.getStyleClass().add("review-body");
            card.getChildren().addAll(header, empty);
            return card;
        }

        card.getChildren().addAll(header, chips);
        return card;
    }

    private VBox buildAttachments(ApplicantReviewDTO dto) {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16));

        HBox header = sectionHeader("Attachments");

        Optional<Path> cvPathOpt = services.resumeService().getCvFilePath(dto.applicantId());
        boolean hasCv = cvPathOpt.isPresent();
        Path cvPath = cvPathOpt.orElse(null);

        Label fileTitle = new Label(hasCv ? cvPath.getFileName().toString() : "No CV uploaded");
        fileTitle.getStyleClass().add("attachment-title");

        String fileMetaText = "-";
        if (hasCv) {
            try {
                fileMetaText = formatFileSize(Files.size(cvPath));
            } catch (Exception ignored) {
                fileMetaText = "-";
            }
        }
        Label fileMeta = new Label(fileMetaText);
        fileMeta.getStyleClass().add("attachment-meta");

        VBox fileInfo = new VBox(2, fileTitle, fileMeta);

        Button preview = new Button("Preview");
        preview.getStyleClass().add("secondary-button");
        preview.setDisable(!hasCv);
        preview.setOnAction(e -> openCvFile(dto.applicantId()));

        Button download = new Button("Download");
        download.getStyleClass().add("secondary-button");
        download.setDisable(!hasCv);
        download.setOnAction(e -> downloadCvFile(dto.applicantId()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, fileInfo, spacer, preview, download);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("attachment-row");
        row.setPadding(new Insets(12));

        card.getChildren().addAll(header, row);
        return card;
    }

    private VBox buildDecisionNote(ApplicantReviewDTO dto) {
        VBox noteCard = new VBox(12);
        noteCard.getStyleClass().add("panel-card");
        noteCard.setPadding(new Insets(16));

        HBox header = sectionHeader("Decision Note");

        decisionNote.setPromptText("Add observation or justification for the recruitment decision...");
        decisionNote.setPrefRowCount(4);
        decisionNote.setText(dto.decisionNote() == null ? "" : dto.decisionNote());

        noteCard.getChildren().addAll(header, decisionNote);
        return noteCard;
    }

    private HBox buildActions() {
        Button accept = new Button("Accept Candidate");
        accept.getStyleClass().add("primary-button");
        accept.setOnAction(event -> doAccept());
        accept.setMaxWidth(Double.MAX_VALUE);

        Button reject = new Button("Reject Candidate");
        reject.getStyleClass().add("danger-outline");
        reject.setOnAction(event -> doReject());
        reject.setMaxWidth(Double.MAX_VALUE);

        HBox actions = new HBox(12, accept, reject);
        actions.setAlignment(Pos.CENTER);
        HBox.setHgrow(accept, Priority.ALWAYS);
        HBox.setHgrow(reject, Priority.ALWAYS);
        return actions;
    }

    private HBox sectionHeader(String titleText) {
        Label label = new Label(titleText);
        label.getStyleClass().add("review-section-title");
        HBox header = new HBox(label);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private String safeJoin(java.util.List<String> items) {
        if (items == null || items.isEmpty()) {
            return "None";
        }
        return String.join(", ", items);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private VBox field(String label, String value) {
        VBox box = new VBox(4);
        Label l = new Label(label);
        l.getStyleClass().add("section-kicker");
        Label v = new Label(blankToDash(value));
        v.getStyleClass().add("review-field-value");
        v.setWrapText(true);
        box.getChildren().addAll(l, v);
        return box;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatStatus(ApplicationStatus status) {
        return status == null ? "ACTIVE CANDIDATE" : status.name().replace('_', ' ');
    }

    private String statusPillClass(ApplicationStatus status) {
        if (status == null) {
            return "status-pill-draft";
        }
        return switch (status) {
            case ACCEPTED -> "status-pill-active";
            case REJECTED -> "status-pill-danger";
            case UNDER_REVIEW -> "status-pill-draft";
            case SUBMITTED -> "status-pill-warn";
            case CANCELLED -> "tag-chip-muted";
        };
    }

    private String formatFileSize(long sizeBytes) {
        if (sizeBytes < 0) {
            return "-";
        }
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        double kb = sizeBytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    private void doAccept() {
        if ("HIGH".equalsIgnoreCase(reviewData.riskLevel())) {
            DialogControllerFactory.workloadWarning(
                    "Projected hours: " + reviewData.projectedHours() + "h / Max " + reviewData.maxWeeklyHours() + "h.",
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
        boolean confirmed = DialogControllerFactory.confirmAction(
                "Accept Candidate",
                "Accept this applicant and update workload records?",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        ValidationResult result = services.reviewService()
                .acceptApplication(applicationId, user.getUserId(), decisionNote.getText(), isAdmin());
        showResult("Accept Application", result);
    }

    private void doReject() {
        boolean confirmed = DialogControllerFactory.confirmAction(
                "Reject Candidate",
                "Reject this applicant for the selected job?",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        ValidationResult result = services.reviewService()
                .rejectApplication(applicationId, user.getUserId(), decisionNote.getText(), isAdmin());
        showResult("Reject Application", result);
    }

    private void showResult(String header, ValidationResult result) {
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(header, String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success(header, "Operation completed.",
                view.getScene() == null ? null : view.getScene().getWindow());
        if (view.getScene() != null && view.getScene().getWindow() != null) {
            view.getScene().getWindow().hide();
        }
    }

    private boolean isAdmin() {
        return user.getRole() == Role.ADMIN;
    }

    private void openCvFile(String applicantId) {
        Optional<Path> filePath = services.resumeService().getCvFilePath(applicantId);
        if (filePath.isEmpty()) {
            DialogControllerFactory.info("CV Not Found",
                    "No uploaded CV file exists for this account.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                DialogControllerFactory.operationFailed("Open CV Failed",
                        "Desktop open action is not supported in this environment.",
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(filePath.get().toUri());
            } else {
                desktop.open(filePath.get().toFile());
            }
        } catch (Exception ex) {
            DialogControllerFactory.operationFailed("Open CV Failed",
                    "Unable to open file: " + ex.getMessage(),
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
    }

    private void downloadCvFile(String applicantId) {
        Optional<Path> filePath = services.resumeService().getCvFilePath(applicantId);
        if (filePath.isEmpty()) {
            DialogControllerFactory.info("CV Not Found",
                    "No uploaded CV file exists for this account.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save CV");
        chooser.setInitialFileName(filePath.get().getFileName().toString());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        File target = chooser.showSaveDialog(view.getScene() == null ? null : view.getScene().getWindow());
        if (target == null) {
            return;
        }

        try {
            Files.copy(filePath.get(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            DialogControllerFactory.success("Download CV", "File saved.",
                    view.getScene() == null ? null : view.getScene().getWindow());
        } catch (Exception ex) {
            DialogControllerFactory.operationFailed("Download CV Failed",
                    "Unable to save file: " + ex.getMessage(),
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
    }
}
