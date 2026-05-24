package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

public class MyCvController {

    private static final DateTimeFormatter UPDATED_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final ServiceRegistry services;
    private final User user;
    private final Runnable browseJobsAction;
    private final BorderPane view = new BorderPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private final String applicantId;
    private VBox pageRoot;
    private ApplicantProfileController profileController;

    public MyCvController(ServiceRegistry services, User user) {
        this(services, user, () -> {
        });
    }

    public MyCvController(ServiceRegistry services, User user, Runnable browseJobsAction) {
        this.services = services;
        this.user = user;
        this.applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        this.browseJobsAction = browseJobsAction == null ? () -> {
        } : browseJobsAction;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");

        profileController = new ApplicantProfileController(services, user);
        ApplicantProfile profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        ResumeInfo resume = services.resumeService().getOrCreateResume(applicantId);
        int resumeCompletion = services.resumeService().calculateResumeCompletion(applicantId);

        pageRoot = new VBox(24);
        pageRoot.getStyleClass().add("cv-page");
        pageRoot.setPadding(new Insets(20, 16, 24, 16));
        pageRoot.setFillWidth(true);
        pageRoot.setMaxWidth(Double.MAX_VALUE);
        pageRoot.setMinWidth(0);

        pageRoot.getChildren().add(buildTitleBlock());
        pageRoot.getChildren().add(profileController.getView());
        pageRoot.getChildren().add(buildActionColumns(profile, resume, resumeCompletion));
        pageRoot.getChildren().add(buildGuidelineCard());

        scrollPane.setContent(pageRoot);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        view.setCenter(scrollPane);
    }

    private VBox buildTitleBlock() {
        Label heading = new Label(I18n.t("cv_management"));
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label(I18n.t("upload_manage_cv"));
        subtitle.getStyleClass().add("body-muted");
        subtitle.setStyle("-fx-font-size: 16px;");

        VBox titleBlock = new VBox(4, heading, subtitle);
        titleBlock.setMaxWidth(Double.MAX_VALUE);
        return titleBlock;
    }

    private HBox buildActionColumns(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox uploadCard = buildUploadCard();
        VBox statusCard = buildStatusCard(profile, resume, resumeCompletion);

        HBox row = new HBox(32, uploadCard, statusCard);
        row.setFillHeight(true);
        row.setMaxWidth(Double.MAX_VALUE);
        uploadCard.setMinWidth(0);
        uploadCard.setPrefWidth(0);
        statusCard.setMinWidth(0);
        statusCard.setPrefWidth(0);
        HBox.setHgrow(uploadCard, Priority.ALWAYS);
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        return row;
    }

    private VBox buildUploadCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("cv-card");
        card.getStyleClass().add("cv-upload-card");
        card.setPadding(new Insets(24));
        card.setPrefHeight(470);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        VBox header = new VBox(4);
        Label title = new Label(I18n.t("upload_new_cv"));
        title.getStyleClass().add("cv-card-heading");

        Label subtitle = new Label(I18n.t("supported_formats_pdf_docx"));
        subtitle.getStyleClass().add("cv-card-subtitle");

        header.getChildren().addAll(title, subtitle);

        StackPane dropZone = new StackPane();
        dropZone.getStyleClass().add("cv-upload-zone");
        dropZone.setMinHeight(340);
        dropZone.setPrefHeight(340);
        dropZone.setMaxWidth(Double.MAX_VALUE);

        VBox dropContent = new VBox(12);
        dropContent.setAlignment(Pos.CENTER);
        dropContent.setMaxWidth(300);

        StackPane uploadIcon = iconBubble(IconFactory.IconType.UPLOAD, "cv-drop-icon", Color.web("#354a5f"), 24);
        uploadIcon.setPrefSize(64, 64);
        uploadIcon.setMinSize(64, 64);
        uploadIcon.setMaxSize(64, 64);

        Label prompt = new Label(I18n.t("click_to_upload_drag_drop"));
        prompt.getStyleClass().add("cv-upload-copy");
        prompt.setWrapText(true);
        prompt.setMaxWidth(320);

        Label helper = new Label(I18n.t("file_auto_parsed_profile"));
        helper.getStyleClass().add("cv-upload-hint");
        helper.setWrapText(true);
        helper.setMaxWidth(320);

        Button selectFile = new Button(I18n.t("select_file"));
        selectFile.getStyleClass().add("cv-primary-button");
        selectFile.setOnAction(event -> handleUploadCv());

        installCvDropTarget(dropZone, card, dropZone);
        installCvDropTarget(card, card, dropZone);
        dropZone.setOnMouseClicked(event -> {
            if (!selectFile.isHover()) {
                handleUploadCv();
            }
        });

        dropContent.getChildren().addAll(uploadIcon, prompt, helper, selectFile);
        dropZone.getChildren().add(dropContent);

        card.getChildren().addAll(header, dropZone);
        return card;
    }

    private void installCvDropTarget(Node target, VBox uploadCard, StackPane visualDropZone) {
        target.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (event.getGestureSource() != target && db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                addDragStyle(uploadCard, visualDropZone);
            }
            event.consume();
        });

        target.setOnDragExited(event -> {
            removeDragStyle(uploadCard, visualDropZone);
            event.consume();
        });

        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                uploadCvFile(db.getFiles().get(0).toPath());
                success = true;
            }
            event.setDropCompleted(success);
            removeDragStyle(uploadCard, visualDropZone);
            event.consume();
        });
    }

    private void addDragStyle(VBox uploadCard, StackPane visualDropZone) {
        if (!uploadCard.getStyleClass().contains("cv-upload-card-dragover")) {
            uploadCard.getStyleClass().add("cv-upload-card-dragover");
        }
        if (!visualDropZone.getStyleClass().contains("cv-upload-zone-dragover")) {
            visualDropZone.getStyleClass().add("cv-upload-zone-dragover");
        }
    }

    private void removeDragStyle(VBox uploadCard, StackPane visualDropZone) {
        uploadCard.getStyleClass().remove("cv-upload-card-dragover");
        visualDropZone.getStyleClass().remove("cv-upload-zone-dragover");
    }

    private VBox buildStatusCard(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox card = new VBox(20);
        card.getStyleClass().add("cv-card");
        card.getStyleClass().add("cv-status-card");
        card.setPadding(new Insets(32));
        card.setPrefHeight(414);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox statusHeader = new HBox(16);
        statusHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = iconBubble(IconFactory.IconType.CHECK_CIRCLE, "cv-status-badge", Color.web("#00c29f"), 22);
        badge.setPrefSize(48, 48);
        badge.setMinSize(48, 48);
        badge.setMaxSize(48, 48);

        VBox headerCopy = new VBox(2);
        boolean hasUploadedCv = hasUploadedCv(resume);
        Label statusTitle = new Label(hasUploadedCv ? I18n.t("cv_uploaded_successfully") : I18n.t("cv_in_progress"));
        statusTitle.getStyleClass().add("cv-status-title");

        Label statusSubtitle = new Label(hasUploadedCv ? I18n.t("verification_complete") : I18n.t("awaiting_completion"));
        statusSubtitle.getStyleClass().add("cv-status-subtitle");

        headerCopy.getChildren().addAll(statusTitle, statusSubtitle);
        statusHeader.getChildren().addAll(badge, headerCopy);

        HBox fileRow = new HBox(12);
        fileRow.getStyleClass().add("cv-file-row");
        fileRow.setAlignment(Pos.CENTER_LEFT);
        fileRow.setPadding(new Insets(21));

        StackPane fileIcon = iconBubble(IconFactory.IconType.FILE, "cv-file-icon", Color.web("#ef4444"), 18);
        fileIcon.setPrefSize(40, 40);
        fileIcon.setMinSize(40, 40);
        fileIcon.setMaxSize(40, 40);

        VBox fileCopy = new VBox(2);
        fileCopy.setFillWidth(true);
        fileCopy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fileCopy, Priority.ALWAYS);
        Label fileTitle = new Label(buildResumeLabel(profile, resume));
        fileTitle.getStyleClass().add("cv-file-title");
        fileTitle.setMaxWidth(Double.MAX_VALUE);
        fileTitle.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label fileMeta = new Label(buildResumeMeta(resume, resumeCompletion));
        fileMeta.getStyleClass().add("cv-file-meta");
        fileMeta.setMaxWidth(Double.MAX_VALUE);
        fileMeta.setTextOverrun(OverrunStyle.ELLIPSIS);

        fileCopy.getChildren().addAll(fileTitle, fileMeta);
        Button openFile = new Button();
        openFile.getStyleClass().add("secondary-button");
        openFile.setGraphic(IconFactory.glyph(IconFactory.IconType.EYE, 16, Color.web("#64748b")));
        openFile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        openFile.setMinWidth(40);
        openFile.setPrefWidth(40);
        openFile.setTooltip(new Tooltip(I18n.t("open_cv_file")));
        openFile.setOnAction(event -> openCvFile());
        openFile.setDisable(!hasUploadedCv);

        Button deleteFile = new Button();
        deleteFile.getStyleClass().add("danger-outline");
        deleteFile.setGraphic(IconFactory.glyph(IconFactory.IconType.TRASH, 16, Color.web("#ef4444")));
        deleteFile.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        deleteFile.setMinWidth(40);
        deleteFile.setPrefWidth(40);
        deleteFile.setTooltip(new Tooltip(I18n.t("delete_cv_file")));
        deleteFile.setOnAction(event -> deleteCvFile());
        deleteFile.setDisable(!hasUploadedCv);

        fileRow.getChildren().addAll(fileIcon, fileCopy, openFile, deleteFile);

        VBox nextSteps = new VBox(16);

        Label nextStepsTitle = new Label(I18n.t("next_steps"));
        nextStepsTitle.getStyleClass().add("cv-step-section-title");

        VBox stepList = new VBox(12);
        stepList.getChildren().addAll(
                stepButton(I18n.t("browse_available_positions"), IconFactory.IconType.SEARCH, () -> browseJobsAction.run()),
                stepButton(I18n.t("complete_profile_details"), IconFactory.IconType.PENCIL, this::showProfileEditor)
        );

        nextSteps.getChildren().addAll(nextStepsTitle, stepList);
        card.getChildren().addAll(statusHeader, fileRow, nextSteps);
        return card;
    }

    private VBox buildGuidelineCard() {
        VBox card = new VBox(0);
        card.getStyleClass().add("cv-guideline-card");
        card.setPadding(new Insets(25));
        card.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);

        StackPane icon = iconBubble(IconFactory.IconType.INFO_CIRCLE, "cv-guideline-icon", Color.web("#354a5f"), 14);
        icon.setPrefSize(24, 24);
        icon.setMinSize(24, 24);
        icon.setMaxSize(24, 24);

        VBox copy = new VBox(8);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label title = new Label(I18n.t("cv_guidelines_applicants"));
        title.getStyleClass().add("cv-guideline-title");

        VBox lines = new VBox(8);
        lines.setMaxWidth(Double.MAX_VALUE);
        List<String> guidance = List.of(
                I18n.t("ensure_gpa"),
                I18n.t("list_previous_ta"),
                I18n.t("include_proficiency"),
                I18n.t("keep_file_size_small")
        );

        for (String line : guidance) {
            Label item = new Label(line);
            item.getStyleClass().add("cv-guideline-line");
            item.setWrapText(true);
            lines.getChildren().add(item);
        }

        copy.getChildren().addAll(title, lines);
        row.getChildren().addAll(icon, copy);
        card.getChildren().add(row);
        return card;
    }

    private Button stepButton(String text, IconFactory.IconType iconType, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("cv-step-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(50);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> {
            if (action != null) {
                action.run();
            }
        });

        StackPane icon = iconBubble(iconType, "cv-step-icon", Color.web("#354a5f"), 12);
        icon.setPrefSize(20, 20);
        icon.setMinSize(20, 20);
        icon.setMaxSize(20, 20);

        Label label = new Label(text);
        label.getStyleClass().add("cv-step-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane arrow = IconFactory.glyph(IconFactory.IconType.CHEVRON_RIGHT, 12, Color.web("#cbd5e1"));

        HBox row = new HBox(12, icon, label, spacer, arrow);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        button.setGraphic(row);
        return button;
    }

    private StackPane iconBubble(IconFactory.IconType iconType, String bubbleStyleClass, Color iconColor, double iconSize) {
        StackPane bubble = new StackPane();
        bubble.getStyleClass().add(bubbleStyleClass);

        bubble.getChildren().add(IconFactory.glyph(iconType, iconSize, iconColor));
        return bubble;
    }

    private void showProfileEditor() {
        if (profileController != null) {
            profileController.enterEditMode();
        }
    }

    private void reloadPage() {
        initialize();
    }

    private String buildResumeMeta(ResumeInfo resume, int resumeCompletion) {
        LocalDateTime updated = resume.getCvUploadedAt() == null ? resume.getLastUpdated() : resume.getCvUploadedAt();
        String updatedText = updated == null ? I18n.t("not_updated_yet") : formatUpdated(updated);
        if (hasUploadedCv(resume)) {
            return readableSize(resume.getCvFileSizeBytes()) + " • " + updatedText;
        }
        return I18n.t("completion_percent", resumeCompletion) + " • " + updatedText;
    }

    private String formatUpdated(LocalDateTime updated) {
        long minutes = ChronoUnit.MINUTES.between(updated, DateTimeUtils.now());
        if (minutes < 1) {
            return I18n.t("updated_just_now");
        }
        if (minutes < 60) {
            return I18n.t("updated_mins_ago", (int) minutes);
        }
        if (minutes < 24 * 60) {
            long hours = ChronoUnit.HOURS.between(updated, DateTimeUtils.now());
            if (hours == 1) {
                return I18n.t("updated_1_hour_ago");
            }
            return I18n.t("updated_hours_ago", (int) hours);
        }
        return UPDATED_FORMAT.format(updated);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void handleUploadCv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("select_cv_file"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(I18n.t("cv_files"), "*.pdf", "*.docx"),
                new FileChooser.ExtensionFilter(I18n.t("pdf_files"), "*.pdf"),
                new FileChooser.ExtensionFilter(I18n.t("docx_files"), "*.docx")
        );
        Path selected = Optional.ofNullable(chooser.showOpenDialog(
                        view.getScene() == null ? null : view.getScene().getWindow()))
                .map(java.io.File::toPath)
                .orElse(null);
        if (selected == null) {
            return;
        }
        uploadCvFile(selected);
    }

    private void uploadCvFile(Path filePath) {
        var result = services.resumeService().uploadCvFile(applicantId, filePath);
        if (!result.isValid()) {
            DialogControllerFactory.validationError(String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success(I18n.t("cv_uploaded_label"), I18n.t("cv_uploaded_success_desc"),
                view.getScene() == null ? null : view.getScene().getWindow());
        reloadPage();
    }

    private void openCvFile() {
        Optional<Path> filePath = services.resumeService().getCvFilePath(applicantId);
        if (filePath.isEmpty()) {
            DialogControllerFactory.info(I18n.t("cv_not_found"), I18n.t("no_uploaded_cv_exists"),
                    view.getScene() == null ? null : view.getScene().getWindow());
            reloadPage();
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                DialogControllerFactory.operationFailed(I18n.t("open_cv_failed"),
                        I18n.t("desktop_open_not_supported"),
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Desktop.getDesktop().open(filePath.get().toFile());
        } catch (IOException e) {
            DialogControllerFactory.operationFailed(I18n.t("open_cv_failed"),
                    I18n.t("unable_to_open_file") + ": " + e.getMessage(),
                    view.getScene() == null ? null : view.getScene().getWindow());
        }
    }

    private void deleteCvFile() {
        boolean confirmed = DialogControllerFactory.confirmAction(I18n.t("delete_cv_file"),
                I18n.t("delete_cv_question"),
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }
        var result = services.resumeService().deleteCvFile(applicantId);
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(I18n.t("delete_cv_failed_label"), String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success(I18n.t("cv_deleted_label"), I18n.t("uploaded_cv_removed"),
                view.getScene() == null ? null : view.getScene().getWindow());
        reloadPage();
    }

    private boolean hasUploadedCv(ResumeInfo resume) {
        return resume.getCvStoredPath() != null && !resume.getCvStoredPath().isBlank()
                && resume.getCvFileName() != null && !resume.getCvFileName().isBlank();
    }

    private String buildResumeLabel(ApplicantProfile profile, ResumeInfo resume) {
        if (hasUploadedCv(resume)) {
            return resume.getCvFileName();
        }
        String name = safe(profile.getFullName()).replaceAll("\\s+", "_");
        if (name.equals("-")) {
            return I18n.t("structured_cv");
        }
        return name + "_CV.pdf";
    }

    private String readableSize(long bytes) {
        if (bytes <= 0) {
            return "0 KB";
        }
        if (bytes < 1024 * 1024) {
            return Math.max(1, bytes / 1024) + " KB";
        }
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }
}
