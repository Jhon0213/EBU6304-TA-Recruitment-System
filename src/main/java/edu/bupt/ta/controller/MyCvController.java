package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.ValidationResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MyCvController {

    private static final DateTimeFormatter UPDATED_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final ServiceRegistry services;
    private final User user;
    private final Runnable browseJobsAction;
    private final BorderPane view = new BorderPane();
    private final ScrollPane scrollPane = new ScrollPane();
    private VBox pageRoot;
    private final VBox editorSection = new VBox(16);

    public MyCvController(ServiceRegistry services, User user) {
        this(services, user, () -> {
        });
    }

    public MyCvController(ServiceRegistry services, User user, Runnable browseJobsAction) {
        this.services = services;
        this.user = user;
        this.browseJobsAction = browseJobsAction == null ? () -> {
        } : browseJobsAction;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        editorSection.setManaged(false);
        editorSection.setVisible(false);
        editorSection.setMaxWidth(Double.MAX_VALUE);

        ApplicantProfile profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        ResumeInfo resume = services.resumeService().getOrCreateResume(profile.getApplicantId());
        int resumeCompletion = services.resumeService().calculateResumeCompletion(profile.getApplicantId());

        pageRoot = new VBox(24);
        pageRoot.getStyleClass().add("cv-page");
        pageRoot.setPadding(new Insets(32, 64, 32, 64));
        pageRoot.setFillWidth(true);
        pageRoot.setMaxWidth(896);

        pageRoot.getChildren().add(buildTitleBlock());
        pageRoot.getChildren().add(buildBasicInfoCard(profile, resume, resumeCompletion));
        pageRoot.getChildren().add(buildActionColumns(profile, resume, resumeCompletion));
        pageRoot.getChildren().add(editorSection);
        pageRoot.getChildren().add(buildGuidelineCard());

        StackPane pageShell = new StackPane(pageRoot);
        StackPane.setAlignment(pageRoot, Pos.TOP_CENTER);

        scrollPane.setContent(pageShell);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        view.setCenter(scrollPane);
    }

    private VBox buildTitleBlock() {
        Label heading = new Label("CV Management");
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label("Upload and manage your curriculum vitae to apply for Teaching Assistant positions.");
        subtitle.getStyleClass().add("body-muted");
        subtitle.setStyle("-fx-font-size: 16px;");

        VBox titleBlock = new VBox(4, heading, subtitle);
        titleBlock.setMaxWidth(Double.MAX_VALUE);
        return titleBlock;
    }

    private VBox buildBasicInfoCard(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox card = new VBox(0);
        card.getStyleClass().add("cv-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox();
        header.getStyleClass().add("cv-basic-card-header");
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Basic Information");
        title.getStyleClass().add("cv-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label more = new Label("...");
        more.getStyleClass().add("cv-card-menu");

        header.getChildren().addAll(title, spacer, more);

        VBox body = new VBox(24);
        body.setPadding(new Insets(24));

        HBox row1 = new HBox(24,
                infoCell("FULL NAME", safe(profile.getFullName())),
                infoCell("STUDENT ID", safe(profile.getStudentId())),
                infoCell("DEGREE PROGRAM", safe(profile.getProgramme()))
        );

        HBox row2 = new HBox(24,
                infoCell("EMAIL", safe(profile.getEmail())),
                infoCell("CV COMPLETION", resumeCompletion + "% complete"),
                infoCell("PHONE", safe(profile.getPhone()))
        );

        body.getChildren().addAll(row1, row2);
        card.getChildren().addAll(header, body);
        return card;
    }

    private HBox buildActionColumns(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox uploadCard = buildUploadCard(profile);
        VBox statusCard = buildStatusCard(profile, resume, resumeCompletion);

        HBox row = new HBox(32, uploadCard, statusCard);
        row.setFillHeight(true);
        HBox.setHgrow(uploadCard, Priority.ALWAYS);
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        return row;
    }

    private VBox buildUploadCard(ApplicantProfile profile) {
        VBox card = new VBox(20);
        card.getStyleClass().add("cv-card");
        card.getStyleClass().add("cv-upload-card");
        card.setPadding(new Insets(32));
        card.setPrefHeight(414);
        card.setMaxWidth(Double.MAX_VALUE);

        VBox header = new VBox(4);
        Label title = new Label("Upload New CV");
        title.getStyleClass().add("cv-card-heading");

        Label subtitle = new Label("Supported formats: PDF, DOCX (Max 10MB)");
        subtitle.getStyleClass().add("cv-card-subtitle");

        header.getChildren().addAll(title, subtitle);

        StackPane dropZone = new StackPane();
        dropZone.getStyleClass().add("cv-upload-zone");
        dropZone.setMinHeight(286);
        dropZone.setPrefHeight(286);
        dropZone.setMaxWidth(Double.MAX_VALUE);

        VBox dropContent = new VBox(12);
        dropContent.setAlignment(Pos.CENTER);
        dropContent.setMaxWidth(255);

        StackPane uploadIcon = iconBubble("↑", "cv-drop-icon", "cv-drop-icon-label");
        uploadIcon.setPrefSize(64, 64);
        uploadIcon.setMinSize(64, 64);
        uploadIcon.setMaxSize(64, 64);

        Label prompt = new Label("Click to upload or drag and drop");
        prompt.getStyleClass().add("cv-upload-copy");
        prompt.setWrapText(true);
        prompt.setMaxWidth(255);

        Label helper = new Label("File is stored securely; complete structured fields below for matching.");
        helper.getStyleClass().add("cv-upload-hint");
        helper.setWrapText(true);
        helper.setMaxWidth(255);

        Button selectFile = new Button("Select File");
        selectFile.getStyleClass().add("cv-primary-button");
        selectFile.setOnAction(event -> pickCvFile(profile.getApplicantId()));

        dropContent.getChildren().addAll(uploadIcon, prompt, helper, selectFile);
        dropZone.getChildren().add(dropContent);

        dropZone.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean ok = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                ok = handleCvFile(profile.getApplicantId(), db.getFiles().get(0));
            }
            event.setDropCompleted(ok);
            event.consume();
        });
        dropZone.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getTarget() instanceof Button) {
                return;
            }
            pickCvFile(profile.getApplicantId());
            event.consume();
        });

        card.getChildren().addAll(header, dropZone);
        return card;
    }

    private void pickCvFile(String applicantId) {
        Window owner = view.getScene() != null ? view.getScene().getWindow() : null;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select CV (PDF or DOCX)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Word document", "*.docx"),
                new FileChooser.ExtensionFilter("PDF or Word", "*.pdf", "*.docx")
        );
        File file = chooser.showOpenDialog(owner);
        if (file != null) {
            handleCvFile(applicantId, file);
        }
    }

    private boolean handleCvFile(String applicantId, File file) {
        Window owner = view.getScene() != null ? view.getScene().getWindow() : null;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            ValidationResult result = services.resumeService().uploadCvFile(applicantId, bytes, file.getName());
            if (!result.isValid()) {
                DialogControllerFactory.operationFailed("Upload failed", String.join("\n", result.getErrors()), owner);
                return false;
            }
            DialogControllerFactory.success("CV uploaded", "Your CV has been saved to your account.", owner);
            refreshCvPageBody();
            return true;
        } catch (IOException e) {
            DialogControllerFactory.operationFailed("Upload failed", e.getMessage(), owner);
            return false;
        }
    }

    private void openCvPreview(String applicantId) {
        Window owner = view.getScene() != null ? view.getScene().getWindow() : null;
        var pathOpt = services.resumeService().findCvFilePath(applicantId);
        if (pathOpt.isEmpty()) {
            DialogControllerFactory.info("No CV", "Please upload a CV file first.", owner);
            return;
        }
        Path path = pathOpt.get();
        String lower = path.getFileName().toString().toLowerCase();
        if (lower.endsWith(".docx")) {
            try {
                if (!Desktop.isDesktopSupported()) {
                    DialogControllerFactory.operationFailed("Preview", "Cannot open DOCX on this system.", owner);
                    return;
                }
                Desktop.getDesktop().open(path.toFile());
            } catch (IOException e) {
                DialogControllerFactory.operationFailed("Open failed", e.getMessage(), owner);
            }
            return;
        }
        if (!lower.endsWith(".pdf")) {
            DialogControllerFactory.info("Preview", "In-app preview is available for PDF only.", owner);
            return;
        }
        try {
            showPdfPreviewWindow(path, owner);
        } catch (IOException e) {
            DialogControllerFactory.operationFailed("Preview failed", e.getMessage(), owner);
        }
    }

    private void showPdfPreviewWindow(Path pdfPath, Window owner) throws IOException {
        PDDocument document = Loader.loadPDF(pdfPath.toFile());
        int numPages = document.getNumberOfPages();
        if (numPages < 1) {
            document.close();
            DialogControllerFactory.info("Preview", "This PDF has no pages.", owner);
            return;
        }
        PDFRenderer renderer = new PDFRenderer(document);

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);

        Label loading = new Label("Rendering page…");
        loading.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        StackPane center = new StackPane(loading, imageView);

        final int[] currentPage = {0};

        Button prevBtn = new Button("Previous");
        Button nextBtn = new Button("Next");
        Label pageLbl = new Label();
        pageLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");

        Runnable loadPage = () -> {
            loading.setVisible(true);
            imageView.setImage(null);
            int page = currentPage[0];
            Task<Image> task = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    BufferedImage bi = renderer.renderImageWithDPI(page, 110);
                    File tmp = Files.createTempFile("cv_preview_", ".png").toFile();
                    tmp.deleteOnExit();
                    ImageIO.write(bi, "png", tmp);
                    return new Image(tmp.toURI().toString());
                }
            };
            task.setOnSucceeded(ev -> {
                imageView.setImage(task.getValue());
                loading.setVisible(false);
                pageLbl.setText((page + 1) + " / " + numPages);
                prevBtn.setDisable(page <= 0);
                nextBtn.setDisable(page >= numPages - 1);
            });
            task.setOnFailed(ev -> {
                loading.setVisible(false);
                Throwable t = task.getException();
                String msg = t != null ? t.getMessage() : "Unknown error";
                DialogControllerFactory.operationFailed("Preview", msg, owner);
            });
            new Thread(task, "cv-pdf-render").start();
        };

        prevBtn.setOnAction(e -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                loadPage.run();
            }
        });
        nextBtn.setOnAction(e -> {
            if (currentPage[0] < numPages - 1) {
                currentPage[0]++;
                loadPage.run();
            }
        });

        if (numPages <= 1) {
            prevBtn.setVisible(false);
            nextBtn.setVisible(false);
            pageLbl.setVisible(false);
        }

        Button openExt = new Button("Open with system viewer");
        openExt.getStyleClass().add("secondary-button");
        openExt.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(pdfPath.toFile());
            } catch (IOException ex) {
                DialogControllerFactory.operationFailed("Open failed", ex.getMessage(), owner);
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("primary-button");

        HBox nav = new HBox(16, prevBtn, pageLbl, nextBtn);
        nav.setAlignment(Pos.CENTER);

        HBox actions = new HBox(12, openExt, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(820);
        scroll.setPrefViewportHeight(620);
        scroll.setStyle("-fx-background: #e2e8f0;");

        VBox root = new VBox(16, scroll, nav, actions);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #f8fafc;");

        Stage stage = new Stage();
        closeBtn.setOnAction(e -> stage.close());
        Scene scene = new Scene(root, 860, 750);
        stage.setTitle("CV Preview — " + pdfPath.getFileName());
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(scene);
        stage.setOnHidden(e -> {
            try {
                document.close();
            } catch (IOException ignored) {
            }
        });
        stage.show();
        loadPage.run();
    }

    private void refreshCvPageBody() {
        ApplicantProfile profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        ResumeInfo resume = services.resumeService().getOrCreateResume(profile.getApplicantId());
        int completion = services.resumeService().calculateResumeCompletion(profile.getApplicantId());
        if (pageRoot != null && pageRoot.getChildren().size() >= 3) {
            pageRoot.getChildren().set(1, buildBasicInfoCard(profile, resume, completion));
            pageRoot.getChildren().set(2, buildActionColumns(profile, resume, completion));
        }
    }

    private VBox buildStatusCard(ApplicantProfile profile, ResumeInfo resume, int resumeCompletion) {
        VBox card = new VBox(20);
        card.getStyleClass().add("cv-card");
        card.getStyleClass().add("cv-status-card");
        card.setPadding(new Insets(32));
        card.setPrefHeight(414);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox statusHeader = new HBox(16);
        statusHeader.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = iconBubble("✓", "cv-status-badge", "cv-status-badge-label");
        badge.setPrefSize(48, 48);
        badge.setMinSize(48, 48);
        badge.setMaxSize(48, 48);

        VBox headerCopy = new VBox(2);
        Label statusTitle = new Label(resumeCompletion >= 80 ? "CV Uploaded Successfully" : "CV In Progress");
        statusTitle.getStyleClass().add("cv-status-title");

        Label statusSubtitle = new Label(resumeCompletion >= 80 ? "Verification Complete" : "Awaiting Completion");
        statusSubtitle.getStyleClass().add("cv-status-subtitle");

        headerCopy.getChildren().addAll(statusTitle, statusSubtitle);
        statusHeader.getChildren().addAll(badge, headerCopy);

        HBox fileRow = new HBox(12);
        fileRow.getStyleClass().add("cv-file-row");
        fileRow.setAlignment(Pos.CENTER_LEFT);
        fileRow.setPadding(new Insets(21));

        String ext = resume.getCvStoredExtension();
        String iconText = "docx".equalsIgnoreCase(ext) ? "DOC" : "PDF";
        StackPane fileIcon = iconBubble(iconText, "cv-file-icon", "cv-file-icon-label");
        fileIcon.setPrefSize(40, 40);
        fileIcon.setMinSize(40, 40);
        fileIcon.setMaxSize(40, 40);

        VBox fileCopy = new VBox(2);
        fileCopy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fileCopy, Priority.ALWAYS);
        Label fileTitle = new Label(buildResumeLabel(profile, resume));
        fileTitle.getStyleClass().add("cv-file-title");

        Label fileMeta = new Label(buildResumeMeta(resume, resumeCompletion));
        fileMeta.getStyleClass().add("cv-file-meta");

        fileCopy.getChildren().addAll(fileTitle, fileMeta);

        Button previewCv = new Button("Preview CV");
        previewCv.getStyleClass().add("secondary-button");
        previewCv.setMinWidth(120);
        if (services.resumeService().hasCvFileOnDisk(profile.getApplicantId())) {
            previewCv.setOnAction(e -> openCvPreview(profile.getApplicantId()));
        } else {
            previewCv.setDisable(true);
        }

        fileRow.getChildren().addAll(fileIcon, fileCopy, previewCv);

        VBox nextSteps = new VBox(16);

        Label nextStepsTitle = new Label("NEXT STEPS");
        nextStepsTitle.getStyleClass().add("cv-step-section-title");

        VBox stepList = new VBox(12);
        stepList.getChildren().addAll(
                stepButton("Browse Available Positions", "↗", () -> browseJobsAction.run()),
                stepButton("Complete Profile Details", "✎", this::showProfileEditor)
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

        StackPane icon = iconBubble("i", "cv-guideline-icon", "cv-guideline-icon-label");
        icon.setPrefSize(24, 24);
        icon.setMinSize(24, 24);
        icon.setMaxSize(24, 24);

        VBox copy = new VBox(8);
        copy.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label title = new Label("CV Guidelines for Applicants");
        title.getStyleClass().add("cv-guideline-title");

        VBox lines = new VBox(8);
        lines.setMaxWidth(Double.MAX_VALUE);
        List<String> guidance = List.of(
                "Ensure your GPA and relevant course grades are clearly visible.",
                "List any previous teaching assistant or research assistant experience.",
                "Include your proficiency in English and any other required languages for the specific course.",
                "Keep the file size under 10MB to ensure smooth processing by our automated system."
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

    private VBox infoCell(String label, String value) {
        VBox cell = new VBox(4);
        cell.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cell, Priority.ALWAYS);

        Label name = new Label(label);
        name.getStyleClass().add("cv-meta-label");

        Label data = new Label(value);
        data.getStyleClass().add("cv-meta-value");
        data.setWrapText(true);

        cell.getChildren().addAll(name, data);
        return cell;
    }

    private Button stepButton(String text, String glyph, Runnable action) {
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

        StackPane icon = iconBubble(glyph, "cv-step-icon", "cv-step-icon-label");
        icon.setPrefSize(20, 20);
        icon.setMinSize(20, 20);
        icon.setMaxSize(20, 20);

        Label label = new Label(text);
        label.getStyleClass().add("cv-step-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label arrow = new Label(">");
        arrow.getStyleClass().add("cv-step-arrow");

        HBox row = new HBox(12, icon, label, spacer, arrow);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        button.setGraphic(row);
        return button;
    }

    private StackPane iconBubble(String glyph, String bubbleStyleClass, String glyphStyleClass) {
        StackPane bubble = new StackPane();
        bubble.getStyleClass().add(bubbleStyleClass);

        Label label = new Label(glyph);
        label.getStyleClass().add(glyphStyleClass);

        bubble.getChildren().add(label);
        return bubble;
    }

    private void showResumeEditor() {
        showEditor("Resume Information", new ResumeInfoController(services, user).getView());
    }

    private void showProfileEditor() {
        showEditor("Edit Basic Information", new ApplicantProfileController(services, user).getView());
    }

    private void showEditor(String titleText, Parent editorView) {
        editorSection.getChildren().clear();
        editorSection.setManaged(true);
        editorSection.setVisible(true);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("Close");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(event -> hideEditor());

        header.getChildren().addAll(title, spacer, close);

        editorSection.getChildren().addAll(header, editorView);
        scrollEditorIntoView();
    }

    private void hideEditor() {
        editorSection.getChildren().clear();
        editorSection.setVisible(false);
        editorSection.setManaged(false);
    }

    private void scrollEditorIntoView() {
        Platform.runLater(() -> {
            if (pageRoot == null || scrollPane.getContent() == null || !editorSection.isVisible()) {
                return;
            }
            Bounds viewportBounds = scrollPane.getViewportBounds();
            double viewportHeight = viewportBounds == null ? 0 : viewportBounds.getHeight();
            double contentHeight = pageRoot.getBoundsInLocal().getHeight();
            double maxScroll = Math.max(1, contentHeight - viewportHeight);
            double targetY = editorSection.getBoundsInParent().getMinY();
            scrollPane.setVvalue(Math.max(0, Math.min(1, targetY / maxScroll)));
        });
    }

    private String buildResumeLabel(ApplicantProfile profile, ResumeInfo resume) {
        if (resume.getCvOriginalFileName() != null && !resume.getCvOriginalFileName().isBlank()) {
            return resume.getCvOriginalFileName();
        }
        String name = safe(profile.getFullName()).replaceAll("\\s+", "_");
        if (name.equals("-")) {
            return "No CV file yet — upload PDF or DOCX";
        }
        return name + "_CV.pdf";
    }

    private String buildResumeMeta(ResumeInfo resume, int resumeCompletion) {
        LocalDateTime cvAt = resume.getCvFileUploadedAt();
        String cvPart = (cvAt != null && services.resumeService().hasCvFileOnDisk(resume.getApplicantId()))
                ? "CV file • " + formatUpdated(cvAt)
                : "No CV file uploaded";
        LocalDateTime updated = resume.getLastUpdated();
        String formPart = updated == null ? "Form not updated" : formatUpdated(updated);
        return resumeCompletion + "% complete • " + cvPart + " • " + formPart;
    }

    private String formatUpdated(LocalDateTime updated) {
        long minutes = ChronoUnit.MINUTES.between(updated, DateTimeUtils.now());
        if (minutes < 1) {
            return "Updated just now";
        }
        if (minutes < 60) {
            return "Updated " + minutes + " mins ago";
        }
        if (minutes < 24 * 60) {
            long hours = ChronoUnit.HOURS.between(updated, DateTimeUtils.now());
            if (hours <= 1) {
                return "Updated 1 hour ago";
            }
            return "Updated " + hours + " hours ago";
        }
        return "Updated " + UPDATED_FORMAT.format(updated);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
