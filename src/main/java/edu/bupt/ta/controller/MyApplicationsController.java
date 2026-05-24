package edu.bupt.ta.controller;

import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MyApplicationsController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final VBox statusLinks = new VBox(4);
    private final Map<String, Button> statusButtons = new LinkedHashMap<>();
    private final Map<String, Label> statusCounters = new LinkedHashMap<>();
    private final VBox applicationCards = new VBox(12);
    private final ScrollPane applicationCardsScroll = new ScrollPane(applicationCards);

    private final StackPane detailStageIconShell = new StackPane();
    private final Label detailStage = new Label("-");
    private final Label detailStageDate = new Label("-");

    private final Label feedbackAvatar = new Label("--");
    private final Label feedbackReviewer = new Label("-");
    private final Label feedbackTime = new Label("-");
    private final Label feedbackBody = new Label("-");

    private final StackPane timelineSubmittedDot = new StackPane();
    private final StackPane timelineReviewDot = new StackPane();
    private final StackPane timelineFinalDot = new StackPane();
    private final Label timelineSubmittedDate = new Label("-");
    private final Label timelineReviewDate = new Label("-");
    private final Label timelineFinalTitle = new Label(I18n.t("final_decision"));
    private final Label timelineFinalDate = new Label("-");
    private final VBox timelineFinalBlock = new VBox(2);

    private Parent normalState;

    private String applicantId;
    private String profileProgramme = "Computer Science";
    private List<ApplicationRecord> allApplications = List.of();
    private List<ApplicationRecord> filteredApplications = List.of();
    private String activeFilter = "ALL";
    private String selectedApplicationId;

    private Consumer<Job> onViewJobDetails;

    private static final DateTimeFormatter CARD_APPLY_DATE = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
    private static final DateTimeFormatter STAGE_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter FEEDBACK_TIME = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIMELINE_TIME = DateTimeFormatter.ofPattern("MMM dd, yyyy · h:mm a", Locale.ENGLISH);
    private static final double CARD_RIGHT_INFO_WIDTH = 166;

    private final Button viewJobButton = new Button(I18n.t("view_job_details"));
    private final Button viewFullButton = new Button(I18n.t("view_full_application"));
    private final Button withdrawButton = new Button(I18n.t("withdraw_application"));

    public MyApplicationsController(ServiceRegistry services, User user, Consumer<Job> onViewJobDetails) {
        I18n.initTranslations();
        this.services = services;
        this.user = user;
        this.onViewJobDetails = onViewJobDetails;
        initialize();
        refresh();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().addAll("app-surface", "my-app-page");
        applicationCards.getStyleClass().add("my-app-cards");
        applicationCardsScroll.getStyleClass().add("my-app-list-scroll");
        applicationCardsScroll.setFitToWidth(true);
        applicationCardsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        viewJobButton.getStyleClass().add("my-app-action-neutral");
        viewJobButton.setMaxWidth(Double.MAX_VALUE);
        viewJobButton.setOnAction(event -> viewJobDetails());

        viewFullButton.getStyleClass().add("my-app-action-neutral");
        viewFullButton.setMaxWidth(Double.MAX_VALUE);
        viewFullButton.setOnAction(event -> showFullApplication());

        withdrawButton.getStyleClass().add("my-app-action-danger");
        withdrawButton.setMaxWidth(Double.MAX_VALUE);
        withdrawButton.setOnAction(event -> withdrawSelectedApplication());
    }

    private void refresh() {
        applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        profileProgramme = services.applicantProfileRepository().findById(applicantId)
                .map(profile -> profile.getProgramme())
                .filter(text -> !text.isBlank())
                .orElse("Computer Science");

        allApplications = new ArrayList<>(services.applicationService().getApplicationsByApplicant(applicantId))
                .stream()
                .filter(application -> application.getStatus() != ApplicationStatus.CANCELLED)
                .map(this::toRecord)
                .sorted(Comparator.comparing((ApplicationRecord row) -> row.application().getApplyDate()).reversed())
                .toList();

        if (allApplications.isEmpty()) {
            view.setCenter(buildEmptyState());
            return;
        }

        if (normalState == null) {
            normalState = buildNormalState();
        }

        view.setCenter(normalState);
        updateStatusRail();
        updateApplicationCards();
    }

    private ApplicationRecord toRecord(Application application) {
        Job job = services.jobService().getJob(application.getJobId()).orElse(null);
        String reviewer = I18n.t("module_organiser_label");
        if (job != null && job.getOrganiserId() != null) {
            reviewer = services.userRepository().findById(job.getOrganiserId())
                    .map(User::getDisplayName)
                    .filter(name -> !name.isBlank())
                    .orElse(I18n.t("module_organiser_label"));
        }
        return new ApplicationRecord(application, job, reviewer);
    }

    private Parent buildNormalState() {
        HBox layout = new HBox();
        layout.getStyleClass().add("my-app-layout");

        VBox statusRail = buildStatusRail();
        VBox mainListArea = buildMainListArea();
        VBox detailPanel = buildDetailPanel();

        HBox.setHgrow(mainListArea, Priority.ALWAYS);
        layout.getChildren().addAll(statusRail, mainListArea, detailPanel);
        return layout;
    }

    private VBox buildStatusRail() {
        VBox rail = new VBox(10);
        rail.getStyleClass().add("my-app-status-rail");
        rail.setPrefWidth(200);
        rail.setMinWidth(200);
        rail.setMaxWidth(200);

        Label heading = new Label(I18n.t("status_overview"));
        heading.getStyleClass().addAll("tiny-kicker", "my-app-status-overview-title");

        statusLinks.getStyleClass().add("my-app-status-links");
        statusLinks.getChildren().setAll(
                createStatusButton("ALL", I18n.t("all_applications"), IconFactory.IconType.CLIPBOARD, Color.web("#475569")),
                createStatusButton("UNDER_REVIEW", I18n.t("under_review_status"), IconFactory.IconType.SEARCH, Color.web("#f59e0b")),
                createStatusButton("SUBMITTED", I18n.t("all_applications"), IconFactory.IconType.FILE, Color.web("#475569")),
                createStatusButton("ACCEPTED", I18n.t("accepted_status"), IconFactory.IconType.CHECK_CIRCLE, Color.web("#10b981")),
                createStatusButton("REJECTED", I18n.t("rejected_status"), IconFactory.IconType.ALERT_TRIANGLE, Color.web("#ef4444"))
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        rail.getChildren().addAll(heading, statusLinks, spacer);
        return rail;
    }

    private Button createStatusButton(String key, String text, IconFactory.IconType icon, Color iconColor) {
        Button button = new Button();
        button.getStyleClass().add("my-app-status-link");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);

        StackPane iconNode = IconFactory.glyph(icon, 13, iconColor);
        iconNode.getStyleClass().add("my-app-status-icon");

        Label textNode = new Label(text);
        textNode.getStyleClass().add("my-app-status-text");

        Label countNode = new Label("0");
        countNode.getStyleClass().add("my-app-status-count");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox content = new HBox(10, iconNode, textNode, spacer, countNode);
        content.setAlignment(Pos.CENTER_LEFT);

        button.setGraphic(content);
        button.setOnAction(event -> {
            activeFilter = key;
            updateStatusRail();
            updateApplicationCards();
        });

        statusButtons.put(key, button);
        statusCounters.put(key, countNode);
        return button;
    }

    private VBox buildMainListArea() {
        VBox panel = new VBox();
        panel.getStyleClass().add("my-app-main-area");

        VBox filterBar = new VBox();
        filterBar.getStyleClass().add("my-app-filter-bar");

        VBox.setVgrow(applicationCardsScroll, Priority.ALWAYS);
        panel.getChildren().addAll(filterBar, applicationCardsScroll);
        return panel;
    }

    private VBox buildDetailPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("my-app-detail-panel");
        panel.setPrefWidth(360);
        panel.setMinWidth(340);
        panel.setMaxWidth(380);

        HBox header = new HBox();
        header.getStyleClass().add("my-app-detail-header");
        Label title = new Label(I18n.t("application_details"));
        title.getStyleClass().add("my-app-detail-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer);

        Label currentStageKicker = new Label(I18n.t("current_stage"));
        currentStageKicker.getStyleClass().addAll("tiny-kicker", "my-app-stage-kicker");

        detailStageIconShell.getStyleClass().add("my-app-stage-icon-shell");
        detailStage.getStyleClass().add("my-app-stage-label");
        detailStageDate.getStyleClass().add("my-app-stage-date");

        VBox stageText = new VBox(2, detailStage, detailStageDate);
        HBox stageRow = new HBox(12, detailStageIconShell, stageText);
        stageRow.setAlignment(Pos.CENTER_LEFT);
        VBox stageCard = new VBox(16, currentStageKicker, stageRow);
        stageCard.getStyleClass().add("my-app-stage-card");

        HBox feedbackHeading = new HBox(8,
                IconFactory.glyph(IconFactory.IconType.INFO_CIRCLE, 12, Color.web("#475569")),
                sectionTitle(I18n.t("reviewer_feedback")));
        feedbackHeading.setAlignment(Pos.CENTER_LEFT);

        feedbackAvatar.getStyleClass().add("my-app-feedback-avatar");
        feedbackReviewer.getStyleClass().add("my-app-feedback-reviewer");
        feedbackTime.getStyleClass().add("my-app-feedback-time");
        feedbackBody.getStyleClass().add("my-app-feedback-body");
        feedbackBody.setWrapText(true);

        Region feedbackSpacer = new Region();
        HBox.setHgrow(feedbackSpacer, Priority.ALWAYS);
        HBox feedbackMeta = new HBox(8, feedbackAvatar, feedbackReviewer, feedbackSpacer, feedbackTime);
        feedbackMeta.setAlignment(Pos.CENTER_LEFT);

        VBox feedbackCard = new VBox(8, feedbackMeta, feedbackBody);
        feedbackCard.getStyleClass().add("my-app-feedback-card");

        VBox feedbackSection = new VBox(12, feedbackHeading, feedbackCard);

        Label timelineTitle = sectionTitle(I18n.t("timeline_label"));
        Label submittedTitle = timelineStepTitle(I18n.t("application_submitted"));
        Label reviewTitle = timelineStepTitle(I18n.t("application_under_review"));
        timelineFinalTitle.getStyleClass().add("my-app-timeline-step-title");

        timelineSubmittedDate.getStyleClass().add("my-app-timeline-step-date");
        timelineReviewDate.getStyleClass().add("my-app-timeline-step-date");
        timelineFinalDate.getStyleClass().add("my-app-timeline-step-date");

        HBox submittedStep = buildTimelineStep(timelineSubmittedDot, submittedTitle, timelineSubmittedDate);
        HBox reviewStep = buildTimelineStep(timelineReviewDot, reviewTitle, timelineReviewDate);
        timelineFinalBlock.getChildren().setAll(timelineFinalTitle, timelineFinalDate);
        HBox finalStep = buildTimelineStep(timelineFinalDot, timelineFinalBlock);

        VBox steps = new VBox(24, submittedStep, reviewStep, finalStep);
        steps.getStyleClass().add("my-app-timeline-steps");

        Region line = new Region();
        line.getStyleClass().add("my-app-timeline-line");
        line.setMaxWidth(2);
        line.setMinWidth(2);
        line.setPrefWidth(2);

        StackPane timelineStack = new StackPane(line, steps);
        StackPane.setAlignment(line, Pos.TOP_LEFT);
        StackPane.setMargin(line, new Insets(8, 0, 0, 5));

        VBox timelineSection = new VBox(16, timelineTitle, timelineStack);
        timelineSection.getStyleClass().add("my-app-timeline-section");

        VBox actionFooter = new VBox(8, viewJobButton, viewFullButton, withdrawButton);
        actionFooter.getStyleClass().add("my-app-action-footer");

        VBox detailContent = new VBox(24, stageCard, feedbackSection, timelineSection, actionFooter);
        detailContent.getStyleClass().add("my-app-detail-content");

        ScrollPane detailScroll = new ScrollPane(detailContent);
        detailScroll.getStyleClass().add("my-app-detail-scroll");
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(detailScroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, detailScroll);
        return panel;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("my-app-section-title");
        return label;
    }

    private Label timelineStepTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("my-app-timeline-step-title");
        return title;
    }

    private HBox buildTimelineStep(StackPane dot, Label title, Label date) {
        VBox text = new VBox(2, title, date);
        return buildTimelineStep(dot, text);
    }

    private HBox buildTimelineStep(StackPane dot, VBox textBlock) {
        dot.getStyleClass().add("my-app-timeline-dot");
        HBox row = new HBox(10, dot, textBlock);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private void updateStatusRail() {
        Map<ApplicationStatus, Long> counts = allApplications.stream()
                .collect(Collectors.groupingBy(row -> row.application().getStatus(), Collectors.counting()));

        statusCounters.get("ALL").setText(String.valueOf(allApplications.size()));
        statusCounters.get("UNDER_REVIEW").setText(String.valueOf(counts.getOrDefault(ApplicationStatus.UNDER_REVIEW, 0L)));
        statusCounters.get("SUBMITTED").setText(String.valueOf(counts.getOrDefault(ApplicationStatus.SUBMITTED, 0L)));
        statusCounters.get("ACCEPTED").setText(String.valueOf(counts.getOrDefault(ApplicationStatus.ACCEPTED, 0L)));
        statusCounters.get("REJECTED").setText(String.valueOf(counts.getOrDefault(ApplicationStatus.REJECTED, 0L)));

        statusButtons.forEach((key, button) -> {
            button.getStyleClass().remove("my-app-status-link-active");
            if (key.equals(activeFilter)) {
                button.getStyleClass().add("my-app-status-link-active");
            }
        });
    }

    private void updateApplicationCards() {
        filteredApplications = allApplications.stream()
                .filter(this::matchesFilter)
                .toList();

        if (filteredApplications.isEmpty()) {
            selectedApplicationId = null;
            applicationCards.getChildren().setAll(buildNoResultCard());
            updateDetail(null);
            return;
        }

        boolean selectedStillExists = selectedApplicationId != null && filteredApplications.stream()
                .anyMatch(row -> row.application().getApplicationId().equals(selectedApplicationId));
        if (!selectedStillExists) {
            selectedApplicationId = filteredApplications.get(0).application().getApplicationId();
        }

        renderApplicationCards();
        updateDetail(findSelectedApplication());
    }

    private boolean matchesFilter(ApplicationRecord record) {
        if ("ALL".equals(activeFilter)) {
            return true;
        }
        return record.application().getStatus().name().equals(activeFilter);
    }

    private Parent buildNoResultCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("my-app-no-results");
        Label title = new Label(I18n.t("no_applications_status"));
        title.getStyleClass().add("my-app-no-results-title");
        Label subtitle = new Label(I18n.t("try_switching"));
        subtitle.getStyleClass().add("my-app-no-results-subtitle");
        card.getChildren().addAll(title, subtitle);
        return card;
    }

    private void renderApplicationCards() {
        List<Parent> cards = filteredApplications.stream()
                .map(row -> buildApplicationCard(row,
                        row.application().getApplicationId().equals(selectedApplicationId)))
                .collect(Collectors.toList());
        applicationCards.getChildren().setAll(cards);
    }

    private Parent buildApplicationCard(ApplicationRecord row, boolean selected) {
        Application application = row.application();
        Job job = row.job();

        HBox card = new HBox(12);
        card.getStyleClass().add("my-app-card");
        if (selected) {
            card.getStyleClass().add("my-app-card-selected");
        }
        card.setOnMouseClicked(event -> {
            selectedApplicationId = application.getApplicationId();
            renderApplicationCards();
            updateDetail(row);
        });

        Label title = new Label(resolveCardTitle(job, application));
        title.getStyleClass().add("my-app-card-title");
        title.setWrapText(true);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setEllipsisString("…");
        title.setMaxHeight(40);
        title.setMinHeight(Region.USE_PREF_SIZE);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        Label statusTag = new Label(displayStatus(application.getStatus()));
        statusTag.getStyleClass().add("my-app-card-status-tag");
        statusTag.setStyle(statusTagStyle(application.getStatus()));

        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.TOP_LEFT);
        titleRow.setMaxWidth(Double.MAX_VALUE);

        FlowPane metaRow = new FlowPane();
        metaRow.setHgap(14);
        metaRow.setVgap(4);
        metaRow.setPrefWrapLength(320);
        metaRow.setMaxWidth(Double.MAX_VALUE);
        metaRow.getChildren().addAll(
                buildMetaItem(IconFactory.IconType.CALENDAR, resolveSemesterLabel(job, application)),
                buildMetaItem(IconFactory.IconType.INFO_CIRCLE, I18n.t("application_submitted") + " " + application.getApplyDate().format(CARD_APPLY_DATE)),
                buildMetaItem(IconFactory.IconType.USER, row.reviewer())
        );

        VBox content = new VBox(7, titleRow, metaRow);
        HBox.setHgrow(content, Priority.ALWAYS);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMinWidth(0);

        Label idLabel = new Label(I18n.t("application_detail_title") + ": " + application.getApplicationId());
        idLabel.getStyleClass().add("my-app-card-id");
        idLabel.setMaxWidth(Double.MAX_VALUE);
        idLabel.setAlignment(Pos.CENTER_RIGHT);

        Label updated = new Label(I18n.t("refresh_btn") + " " + toRelative(application.getApplyDate()));
        updated.getStyleClass().add("my-app-card-updated");
        updated.setMaxWidth(Double.MAX_VALUE);
        updated.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(6, statusTag, idLabel, updated);
        right.setAlignment(Pos.TOP_RIGHT);
        right.getStyleClass().add("my-app-card-right");
        right.setMinWidth(CARD_RIGHT_INFO_WIDTH);
        right.setPrefWidth(CARD_RIGHT_INFO_WIDTH);

        card.getChildren().addAll(content, right);
        return card;
    }

    private HBox buildMetaItem(IconFactory.IconType type, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("my-app-card-meta-text");
        HBox item = new HBox(4, IconFactory.glyph(type, 9.5, Color.web("#64748b")), label);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private ApplicationRecord findSelectedApplication() {
        if (selectedApplicationId == null) {
            return null;
        }
        return filteredApplications.stream()
                .filter(row -> row.application().getApplicationId().equals(selectedApplicationId))
                .findFirst()
                .orElse(null);
    }

    private void updateDetail(ApplicationRecord row) {
        if (row == null) {
            detailStage.setText("-");
            detailStageDate.setText("-");
            detailStageIconShell.getChildren().setAll();
            feedbackAvatar.setText("--");
            feedbackReviewer.setText("-");
            feedbackTime.setText("-");
            feedbackBody.setText("-");
            timelineSubmittedDate.setText("-");
            timelineReviewDate.setText("-");
            timelineFinalTitle.setText(I18n.t("final_decision"));
            timelineFinalDate.setText("-");
            timelineSubmittedDot.setStyle(dotStyle("#10b981"));
            timelineReviewDot.setStyle(dotStyle("#f59e0b"));
            timelineFinalDot.setStyle(dotStyle("#cbd5e1"));
            timelineFinalBlock.setOpacity(0.4);
            withdrawButton.setDisable(true);
            viewFullButton.setDisable(true);
            viewJobButton.setDisable(true);
            return;
        }

        Application application = row.application();
        ApplicationStatus status = application.getStatus();

        detailStage.setText(displayStatus(status));
        detailStageDate.setText(application.getApplyDate().format(STAGE_DATE));

        detailStageIconShell.getChildren().setAll(IconFactory.glyph(iconForStatus(status), 15, Color.WHITE));
        detailStageIconShell.setStyle(stageIconStyle(status));

        feedbackReviewer.setText(row.reviewer());
        feedbackAvatar.setText(initialsOf(row.reviewer()));
        feedbackTime.setText(application.getApplyDate().format(FEEDBACK_TIME));
        feedbackBody.setText(resolveFeedback(row));

        LocalDateTime submittedAt = application.getApplyDate();
        timelineSubmittedDate.setText(submittedAt.format(TIMELINE_TIME));
        timelineReviewDate.setText(submittedAt.plusDays(3).format(TIMELINE_TIME));

        boolean finalized = status == ApplicationStatus.ACCEPTED
                || status == ApplicationStatus.REJECTED
                || status == ApplicationStatus.CANCELLED;
        timelineFinalTitle.setText(I18n.t("final_decision"));
        timelineFinalDate.setText(finalized
                ? submittedAt.plusDays(7).format(TIMELINE_TIME)
                : I18n.t("final_decision_expected"));

        timelineSubmittedDot.setStyle(dotStyle("#10b981"));
        timelineReviewDot.setStyle(dotStyle("#f59e0b"));
        if (finalized) {
            timelineFinalDot.setStyle(dotStyle(status == ApplicationStatus.REJECTED ? "#ef4444" : "#10b981"));
            timelineFinalBlock.setOpacity(1);
        } else {
            timelineFinalDot.setStyle(dotStyle("#cbd5e1"));
            timelineFinalBlock.setOpacity(0.4);
        }

        viewFullButton.setDisable(false);
        viewJobButton.setDisable(row.job() == null);
        withdrawButton.setDisable(status != ApplicationStatus.SUBMITTED);
    }

    private String resolveFeedback(ApplicationRecord row) {
        Application application = row.application();
        if (application.getDecisionNote() != null && !application.getDecisionNote().isBlank()) {
            return "\"" + application.getDecisionNote().trim() + "\"";
        }
        return switch (application.getStatus()) {
            case SUBMITTED -> "\"" + I18n.t("application_submitted") + "\"";
            case UNDER_REVIEW -> "\"" + I18n.t("application_under_review") + "\"";
            case ACCEPTED -> "\"" + I18n.t("accepted_status") + "\"";
            case REJECTED -> "\"" + I18n.t("rejected_status") + "\"";
            case CANCELLED -> "\"" + I18n.t("application_withdrawn") + "\"";
        };
    }

    private String resolveCardTitle(Job job, Application application) {
        if (job == null) {
            return application.getJobId();
        }
        if (job.getModuleCode() != null && !job.getModuleCode().isBlank()
                && job.getModuleName() != null && !job.getModuleName().isBlank()) {
            return job.getModuleCode() + " " + job.getModuleName();
        }
        return job.getTitle();
    }

    private String resolveSemesterLabel(Job job, Application application) {
        if (job != null && job.getSemester() != null && !job.getSemester().isBlank()) {
            return job.getSemester();
        }
        Month month = application.getApplyDate().getMonth();
        String term = switch (month) {
            case SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER -> I18n.t("fall_term");
            default -> I18n.t("spring_term");
        };
        return term + " " + application.getApplyDate().getYear();
    }

    private IconFactory.IconType iconForStatus(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> IconFactory.IconType.FILE;
            case UNDER_REVIEW -> IconFactory.IconType.SEARCH;
            case ACCEPTED -> IconFactory.IconType.CHECK_CIRCLE;
            case REJECTED -> IconFactory.IconType.ALERT_TRIANGLE;
            case CANCELLED -> IconFactory.IconType.TRASH;
        };
    }

    private String displayStatus(ApplicationStatus status) {
        return switch (status) {
            case UNDER_REVIEW -> I18n.t("under_review_status").toUpperCase();
            case ACCEPTED -> I18n.t("accepted_status").toUpperCase();
            case REJECTED -> I18n.t("rejected_status").toUpperCase();
            case CANCELLED -> I18n.t("application_withdrawn").toUpperCase();
            case SUBMITTED -> I18n.t("all_applications").toUpperCase();
        };
    }

    private String statusTagStyle(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> "-fx-background-color: rgba(245,158,11,0.1); -fx-text-fill: #f59e0b;";
            case UNDER_REVIEW -> "-fx-background-color: rgba(53,74,95,0.1); -fx-text-fill: #354a5f;";
            case ACCEPTED -> "-fx-background-color: rgba(16,185,129,0.1); -fx-text-fill: #10b981;";
            case REJECTED -> "-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #ef4444;";
            case CANCELLED -> "-fx-background-color: rgba(100,116,139,0.12); -fx-text-fill: #64748b;";
        };
    }

    private String stageIconStyle(ApplicationStatus status) {
        return switch (status) {
            case SUBMITTED -> "-fx-background-color: #f59e0b; -fx-background-radius: 999;";
            case UNDER_REVIEW -> "-fx-background-color: #354a5f; -fx-background-radius: 999;";
            case ACCEPTED -> "-fx-background-color: #10b981; -fx-background-radius: 999;";
            case REJECTED -> "-fx-background-color: #ef4444; -fx-background-radius: 999;";
            case CANCELLED -> "-fx-background-color: #64748b; -fx-background-radius: 999;";
        };
    }

    private String dotStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-background-radius: 999;";
    }

    private String toRelative(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        if (timestamp.isAfter(now)) {
            return I18n.t("just_now");
        }
        long minutes = java.time.Duration.between(timestamp, now).toMinutes();
        if (minutes < 60) {
            return minutes <= 1 ? I18n.t("mins_ago_unit", 1) : I18n.t("mins_ago_unit", (int) minutes);
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return I18n.t("hours_ago_unit", (int) hours);
        }
        long days = hours / 24;
        if (days < 7) {
            if (days == 1) return I18n.t("day_singular");
            return I18n.t("days_ago_unit", (int) days);
        }
        long weeks = days / 7;
        if (weeks == 1) return I18n.t("week_singular");
        return I18n.t("weeks_ago_unit", (int) weeks);
    }

    private String initialsOf(String name) {
        if (name == null || name.isBlank()) {
            return "--";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }

    private void showFullApplication() {
        ApplicationRecord row = findSelectedApplication();
        if (row == null) {
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle(I18n.t("application_detail_title"));

        Parent reviewView = new ApplicantReviewController(
                services, user, row.application().getApplicationId(), true).getView();
        Scene scene = new Scene(reviewView, 980, 780);
        if (MyApplicationsController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(
                    MyApplicationsController.class.getResource("/styles/app.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void viewJobDetails() {
        ApplicationRecord row = findSelectedApplication();
        if (row == null || row.job() == null) {
            return;
        }

        if (onViewJobDetails != null) {
            onViewJobDetails.accept(row.job());
        }
    }

    private void withdrawSelectedApplication() {
        ApplicationRecord row = findSelectedApplication();
        if (row == null) {
            return;
        }
        if (row.application().getStatus() != ApplicationStatus.SUBMITTED) {
            DialogControllerFactory.operationFailed(I18n.t("unable_to_withdraw"),
                    I18n.t("only_submitted_withdraw"),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        boolean confirmed = DialogControllerFactory.confirmAction(
                I18n.t("withdraw_confirm_title"),
                I18n.t("withdraw_confirm_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        if (!confirmed) {
            return;
        }

        ValidationResult result = services.applicationService().cancelApplication(applicantId, row.application().getJobId());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(I18n.t("unable_to_withdraw"),
                    result.getErrors().isEmpty() ? I18n.t("unable_to_withdraw") : String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success(I18n.t("application_withdrawn"),
                I18n.t("withdraw_success_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        refresh();
    }

    private Parent buildEmptyState() {
        VBox root = new VBox(16);
        root.getStyleClass().add("my-app-empty-state");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(48));

        Label title = new Label(I18n.t("no_applications_yet"));
        title.getStyleClass().add("my-app-empty-title");

        Label subtitle = new Label(I18n.t("no_applications_subtitle"));
        subtitle.getStyleClass().add("my-app-empty-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(520);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        Button browse = new Button(I18n.t("browse_open_jobs"));
        browse.getStyleClass().add("primary-button");
        browse.setOnAction(event -> DialogControllerFactory.info(I18n.t("browse_jobs"),
                I18n.t("use_browse_page"),
                view.getScene() == null ? null : view.getScene().getWindow()));
        Button refreshButton = new Button(I18n.t("refresh_btn"));
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> refresh());
        actions.getChildren().addAll(browse, refreshButton);

        root.getChildren().addAll(title, subtitle, actions);
        return root;
    }

    private record ApplicationRecord(Application application, Job job, String reviewer) {
    }
}
