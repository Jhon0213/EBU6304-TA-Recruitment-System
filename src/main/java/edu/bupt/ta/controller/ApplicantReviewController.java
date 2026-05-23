package edu.bupt.ta.controller;

import edu.bupt.ta.dto.ApplicantReviewDTO;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.awt.Desktop;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ApplicantReviewController {

    private final ServiceRegistry services;
    private final User user;
    private final String applicationId;
    private final boolean readOnly;

    private final VBox view = new VBox(14);
    private final TextArea decisionNote = new TextArea();
    private ApplicantReviewDTO reviewData;

    public ApplicantReviewController(ServiceRegistry services, User user, String applicationId) {
        this(services, user, applicationId, false);
    }

    public ApplicantReviewController(ServiceRegistry services, User user, String applicationId, boolean readOnly) {
        this.services = services;
        this.user = user;
        this.applicationId = applicationId;
        this.readOnly = readOnly;
        I18n.initTranslations();
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        ApplicantReviewDTO dto;
        try {
            dto = services.reviewService()
                    .getApplicantReviewData(applicationId, user.getUserId(), isAdmin() || readOnly);
        } catch (Exception e) {
            view.setPadding(new Insets(32));
            view.getStyleClass().add("app-surface");
            view.setAlignment(javafx.geometry.Pos.CENTER);
            Label msg = new Label(I18n.t("unable_to_load") + "\n" + e.getMessage());
            msg.setWrapText(true);
            msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
            view.getChildren().add(msg);
            return;
        }
        this.reviewData = dto;

        view.setPadding(new Insets(16));
        view.getStyleClass().add("app-surface");

        Label title = new Label(dto.applicantName());
        title.setStyle("-fx-font-size: 40px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label subtitle = new Label(I18n.t("applicant_id_label", "Applicant ID: {id}").replace("{id}", dto.applicantId()));
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        VBox basicInfo = buildBasicInformation(dto);
        VBox statementCard = buildApplicantStatement(dto);

        VBox cvCard = buildCvCard(dto.applicantId());

        if (readOnly) {
            VBox noteCard = new VBox(8);
            noteCard.getStyleClass().add("panel-card");
            noteCard.setPadding(new Insets(14));
            Label noteLabel = new Label(I18n.t("decision_note"));
            noteLabel.getStyleClass().add("field-label");
            Label noteValue = new Label(dto.decisionNote() == null || dto.decisionNote().isBlank()
                    ? I18n.t("no_decision_note") : dto.decisionNote());
            noteValue.setWrapText(true);
            noteValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
            noteCard.getChildren().addAll(noteLabel, noteValue);
            view.getChildren().addAll(title, subtitle, basicInfo, statementCard, cvCard, noteCard);
        } else {
            VBox noteCard = new VBox(8);
            noteCard.getStyleClass().add("panel-card");
            noteCard.setPadding(new Insets(14));
            Label noteLabel = new Label(I18n.t("decision_note"));
            noteLabel.getStyleClass().add("field-label");
            decisionNote.setPromptText(I18n.t("add_observation"));
            decisionNote.setPrefRowCount(4);
            decisionNote.setText(dto.decisionNote() == null ? "" : dto.decisionNote());
            noteCard.getChildren().addAll(noteLabel, decisionNote);

            Button accept = new Button(I18n.t("accept_candidate"));
            accept.getStyleClass().add("primary-button");
            accept.setOnAction(event -> doAccept());
            accept.setMaxWidth(Double.MAX_VALUE);

            Button reject = new Button(I18n.t("reject_candidate"));
            reject.getStyleClass().add("danger-outline");
            reject.setOnAction(event -> doReject());
            reject.setMaxWidth(Double.MAX_VALUE);

            HBox actions = new HBox(12, accept, reject);
            view.getChildren().addAll(title, subtitle, basicInfo, statementCard, cvCard, noteCard, actions);
        }
    }

    private VBox buildBasicInformation(ApplicantReviewDTO dto) {
        ApplicantProfile profile = services.applicantProfileRepository().findById(dto.applicantId()).orElse(null);

        int resumeCompletion;
        try {
            resumeCompletion = services.resumeService().calculateResumeCompletion(dto.applicantId());
        } catch (Exception ignored) {
            resumeCompletion = 0;
        }

        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));

        Label header = new Label(I18n.t("basic_information"));
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        VBox body = new VBox(24);

        String fullName = profile == null ? blankToDash(dto.applicantName()) : blankToDash(profile.getFullName());
        String studentId = profile == null ? "-" : blankToDash(profile.getStudentId());
        String programme = profile == null ? "-" : blankToDash(profile.getProgramme());
        String email = profile == null ? "-" : blankToDash(profile.getEmail());
        String phone = profile == null ? "-" : blankToDash(profile.getPhone());
        String campus = profile == null ? "-" : blankToDash(profile.getCampus());
        String acceptCross = profile == null ? "-" : (profile.isAcceptCrossCampus() ? "Yes" : "No");
        String year = profile == null || profile.getYear() <= 0 ? "-" : I18n.t("year_label", "Year {n}").replace("{n}", String.valueOf(profile.getYear()));

        HBox row1 = new HBox(24,
                infoCell(I18n.t("full_name_label"), fullName),
                infoCell(I18n.t("student_id_label"), studentId),
                infoCell(I18n.t("degree_program_label"), programme)
        );

        HBox row2 = new HBox(24,
                infoCell(I18n.t("email_label"), email),
                infoCell(I18n.t("cv_completion_label"), I18n.t("cv_complete_percent", "{n}% complete").replace("{n}", String.valueOf(resumeCompletion))),
                infoCell(I18n.t("phone_label"), phone)
        );

        HBox row3 = new HBox(24,
                infoCell(I18n.t("campus_label_app"), campus),
                infoCell(I18n.t("accept_cross_campus_label"), acceptCross),
                infoCell(I18n.t("academic_year_label"), year)
        );

        body.getChildren().addAll(row1, row2, row3);
        card.getChildren().addAll(header, body);
        return card;
    }

    private VBox infoCell(String label, String value) {
        VBox box = new VBox(6);
        box.setMinWidth(0);
        box.setPrefWidth(0);
        HBox.setHgrow(box, Priority.ALWAYS);

        Label kicker = new Label(label);
        kicker.getStyleClass().add("section-kicker");

        Label v = new Label(value == null || value.isBlank() ? "-" : value);
        v.setWrapText(true);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        v.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(kicker, v);
        return box;
    }

    private VBox buildApplicantStatement(ApplicantReviewDTO dto) {
        VBox card = new VBox(10);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));

        Label header = new Label(I18n.t("applicant_statement"));
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label statement = new Label(blankToDash(dto.statement()));
        statement.setWrapText(true);
        statement.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        card.getChildren().addAll(header, statement);
        return card;
    }


    private VBox info(String label, String value) {
        VBox box = new VBox(2);
        Label t = new Label(label);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
        Label v = new Label(value == null || value.isBlank() ? "-" : value);
        v.setWrapText(true);
        v.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        box.getChildren().addAll(t, v);
        return box;
    }

    private String safeJoin(java.util.List<String> items) {
        return (items == null || items.isEmpty()) ? "None" : String.join(", ", items);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void doAccept() {
        if ("HIGH".equalsIgnoreCase(reviewData.riskLevel())) {
            DialogControllerFactory.workloadWarning(
                    "High risk candidate. Please review carefully.",
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
        boolean confirmed = DialogControllerFactory.confirmAction(I18n.t("accept_candidate"),
                I18n.t("accept_confirm_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) return;
        ValidationResult result = services.reviewService()
                .acceptApplication(applicationId, user.getUserId(), decisionNote.getText(), isAdmin());
        showResult("Accept Application", result);
    }

    private void doReject() {
        boolean confirmed = DialogControllerFactory.confirmAction(I18n.t("reject_candidate"),
                I18n.t("reject_confirm_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) return;
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
        DialogControllerFactory.success(header, I18n.t("operation_completed"),
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
            DialogControllerFactory.info("CV Not Found", I18n.t("no_cv_msg"),
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

    private VBox buildCvCard(String applicantId) {
        ResumeInfo resume = services.resumeService().getOrCreateResume(applicantId);
        boolean hasCv = resume.getCvStoredPath() != null && !resume.getCvStoredPath().isBlank()
                && resume.getCvFileName() != null && !resume.getCvFileName().isBlank();

        VBox card = new VBox(10);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));

        HBox sectionHeader = new HBox(6);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        sectionHeader.getChildren().addAll(
                IconFactory.glyph(IconFactory.IconType.FILE, 13, Color.web("#354a5f")),
                labelStyle(I18n.t("applicant_cv_label"), "-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;")
        );

        HBox fileRow = new HBox(12);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        fileRow.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-padding: 12;");

        StackPane fileIcon = new StackPane();
        fileIcon.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 8;");
        fileIcon.setPrefSize(36, 36);
        fileIcon.setMinSize(36, 36);
        fileIcon.setMaxSize(36, 36);
        fileIcon.getChildren().add(IconFactory.glyph(IconFactory.IconType.FILE, 16, Color.web("#ef4444")));

        VBox fileMeta = new VBox(2);
        fileMeta.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fileMeta, Priority.ALWAYS);

        Label fileName = labelStyle(hasCv ? resume.getCvFileName() : I18n.t("no_cv_uploaded"),
                "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + (hasCv ? "#0f172a" : "#94a3b8") + ";");
        fileName.setMaxWidth(Double.MAX_VALUE);

        String metaText = hasCv
                ? readableSize(resume.getCvFileSizeBytes()) + "  ·  Uploaded "
                  + (resume.getCvUploadedAt() != null
                        ? resume.getCvUploadedAt().format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
                        : "Unknown date")
                : I18n.t("no_cv_msg");
        Label fileSub = labelStyle(metaText, "-fx-font-size: 11px; -fx-text-fill: #64748b;");
        fileSub.setMaxWidth(Double.MAX_VALUE);
        fileMeta.getChildren().addAll(fileName, fileSub);

        Button openBtn = new Button(I18n.t("open_btn"));
        openBtn.getStyleClass().add("secondary-button");
        openBtn.setGraphic(IconFactory.glyph(IconFactory.IconType.EYE, 13, Color.web("#354a5f")));
        openBtn.setContentDisplay(ContentDisplay.LEFT);
        openBtn.setTooltip(new Tooltip(I18n.t("open_cv_system")));
        openBtn.setDisable(!hasCv);
        openBtn.setOnAction(e -> openCvFile(applicantId));

        fileRow.getChildren().addAll(fileIcon, fileMeta, openBtn);
        card.getChildren().addAll(sectionHeader, fileRow);
        return card;
    }

    private Label labelStyle(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }

    private String readableSize(long bytes) {
        if (bytes <= 0) return "0 KB";
        if (bytes < 1024 * 1024) return Math.max(1, bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
