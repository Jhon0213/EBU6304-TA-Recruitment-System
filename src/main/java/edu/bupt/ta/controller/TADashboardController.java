package edu.bupt.ta.controller;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.dto.AiJobMatchDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ApplicantProfileService;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.ValidationResult;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TADashboardController {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private ApplicantProfile profile;
    private ResumeInfo resume;
    private String applicantId;
    private List<Application> applications = List.of();

    public TADashboardController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        I18n.initTranslations();
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        reloadData();

        VBox page = new VBox(22);
        page.setPadding(new Insets(24));
        page.getChildren().addAll(
                buildWelcome(),
                buildStatRow(),
                buildMainArea()
        );

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setVvalue(0.0);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        view.setCenter(scrollPane);
    }

    private void reloadData() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        applicantId = profile.getApplicantId();
        resume = services.resumeService().getOrCreateResume(applicantId);
        applications = services.applicationService().getApplicationsByApplicant(applicantId);
    }

    private HBox buildWelcome() {
        int activeApplications = (int) applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED
                        || app.getStatus() == ApplicationStatus.UNDER_REVIEW
                        || app.getStatus() == ApplicationStatus.ACCEPTED)
                .count();

        Label title = new Label(I18n.t("welcome_back").replace("{name}", resolveApplicantDisplayName() + " \uD83D\uDC4B"));
        title.getStyleClass().add("page-title");

        Label subtitle = new Label(I18n.t("active_applications").replace("{n}", String.valueOf(activeApplications)));
        subtitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        VBox left = new VBox(4, title, subtitle);

        Button viewSchedule = new Button(I18n.t("view_schedule"));
        viewSchedule.getStyleClass().add("secondary-button");
        viewSchedule.setOnAction(event -> openApplicationsModal());

        Button browseJobs = new Button(I18n.t("browse_new_jobs"));
        browseJobs.getStyleClass().add("primary-button");
        browseJobs.setOnAction(event -> openJobBrowserModal());

        HBox right = new HBox(12, viewSchedule, browseJobs);
        right.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(18, left, spacer, right);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildStatRow() {
        int profileFilled = services.applicantProfileService().countFilledProfileFields(applicantId);
        int profileTotal = ApplicantProfileService.PROFILE_TOTAL_FIELDS;
        int resumeCompletion = services.resumeService().calculateResumeCompletion(applicantId);
        int applicationCount = applications.size();
        boolean hasCv = resume.getCvFileName() != null && !resume.getCvFileName().isBlank();

        HBox row = new HBox(14,
                buildProfileCard(profileFilled, profileTotal),
                buildResumeCard(resumeCompletion, hasCv),
                buildApplicationCard(applicationCount)
        );
        return row;
    }

    private VBox buildProfileCard(int filled, int total) {
        VBox card = baseStatCard();

        Label pill = buildMiniPill(filled >= total ? I18n.t("complete") : I18n.t("incomplete"), filled >= total ? "success" : "warning");
        HBox header = metricHeader("PROFILE STATUS", pill);

        HBox valueRow = new HBox(4);
        Label filledLabel = new Label(String.valueOf(filled));
        filledLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label totalLabel = new Label(I18n.t("fields_filled").replace("{n}", String.valueOf(total)));
        totalLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
        valueRow.getChildren().addAll(filledLabel, totalLabel);
        valueRow.setAlignment(Pos.BASELINE_LEFT);

        Label sub = new Label(filled >= total ? I18n.t("all_fields_filled") : I18n.t("fields_remaining").replace("{n}", String.valueOf(total - filled)));
        sub.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        card.getChildren().addAll(header, valueRow, sub);
        return card;
    }

    private VBox buildResumeCard(int completion, boolean hasCv) {
        VBox card = baseStatCard();

        Label pill = buildMiniPill(hasCv ? I18n.t("uploaded") : I18n.t("incomplete"), hasCv ? "success" : "warning");
        HBox header = metricHeader("CV STATUS", pill);

        Label file = new Label(hasCv ? resume.getCvFileName() : I18n.t("add_cv"));
        file.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        file.setWrapText(true);

        Label meta = new Label(I18n.t("completion_percent").replace("{n}", String.valueOf(completion)));
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        card.getChildren().addAll(header, file, meta);
        return card;
    }

    private VBox buildWorkloadCard(int currentHours, int maxHours) {
        VBox card = baseStatCard();
        double ratio = Math.min(1.0, currentHours / (double) maxHours);

        Label pill = buildMiniPill(currentHours >= maxHours ? "FULL" : "BALANCED", currentHours >= maxHours ? "danger" : "success");
        HBox header = metricHeader("CURRENT WORKLOAD", pill);

        HBox valueRow = new HBox(4);
        Label current = new Label(String.valueOf(currentHours));
        current.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label max = new Label("/ " + maxHours + " hrs/wk");
        max.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;");
        valueRow.getChildren().addAll(current, max);
        valueRow.setAlignment(Pos.BASELINE_LEFT);

        Rectangle track = new Rectangle(110, 6);
        track.setArcWidth(6);
        track.setArcHeight(6);
        track.setFill(Color.web("#dfe7f1"));
        Rectangle fill = new Rectangle(Math.max(10, 110 * ratio), 6);
        fill.setArcWidth(6);
        fill.setArcHeight(6);
        fill.setFill(Color.web(currentHours >= maxHours ? "#ef4444" : "#10bfa4"));
        StackPane progress = new StackPane(track);
        progress.setAlignment(Pos.CENTER_LEFT);
        progress.getChildren().add(fill);

        card.getChildren().addAll(header, valueRow, progress);
        return card;
    }

    private VBox buildApplicationCard(int applicationCount) {
        VBox card = baseStatCard();

        Label pill = buildMiniPill(I18n.t("active"), "neutral");
        HBox header = metricHeader("MY APPLICATIONS", pill);

        Label count = new Label(String.format("%02d", applicationCount));
        count.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label meta = new Label(applicationCount == 0 ? I18n.t("no_submissions_yet") : I18n.t("track_progress"));
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        card.getChildren().addAll(header, count, meta);
        return card;
    }

    private VBox baseStatCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setMinWidth(0);
        card.setPrefWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox metricHeader(String titleText, Label pill) {
        Label kicker = new Label(titleText);
        kicker.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #64748b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, kicker, spacer, pill);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMinHeight(24);
        return header;
    }

    private HBox buildMainArea() {
        VBox left = new VBox(18, buildRecentJobsCard(), buildRecommendedCard());
        VBox right = new VBox(18, buildQuickActionsCard(), buildDeadlineCard());

        HBox.setHgrow(left, Priority.ALWAYS);
        right.setMinWidth(260);
        right.setPrefWidth(260);

        return new HBox(18, left, right);
    }

    private VBox buildRecentJobsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label title = new Label(I18n.t("recent_open_jobs"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Button viewAll = new Button(I18n.t("view_all"));
        viewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: 600;");
        viewAll.setOnAction(event -> openJobBrowserModal());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, viewAll);
        header.setAlignment(Pos.CENTER_LEFT);

        TableView<JobSummaryRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(-1);
        table.setPrefHeight(280);
        table.setPlaceholder(new Label(I18n.t("no_recent_jobs")));

        TableColumn<JobSummaryRow, JobSummaryRow> titleCol = new TableColumn<>("JOB TITLE");
        titleCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        titleCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(JobSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label title = new Label(item.job().getTitle());
                title.setWrapText(true);
                title.setMinWidth(0);
                title.setMaxWidth(Double.MAX_VALUE);
                title.prefWidthProperty().bind(getTableColumn().widthProperty().subtract(26));
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #1f2937;");

                Label meta = new Label(fallback(item.job().getModuleCode(), "-"));
                meta.setWrapText(true);
                meta.setMinWidth(0);
                meta.setMaxWidth(Double.MAX_VALUE);
                meta.prefWidthProperty().bind(getTableColumn().widthProperty().subtract(26));
                meta.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

                VBox box = new VBox(4, title, meta);
                box.setFillWidth(true);
                setGraphic(box);
                setText(null);
            }
        });

        TableColumn<JobSummaryRow, String> deptCol = new TableColumn<>("DEPARTMENT");
        deptCol.setCellValueFactory(cell -> new SimpleStringProperty(fallback(cell.getValue().job().getModuleName(), "-")));
        deptCol.setCellFactory(column -> mutedTableCell());

        TableColumn<JobSummaryRow, JobSummaryRow> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(JobSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = buildMiniPill(item.statusLabel(), item.statusTone());
                setGraphic(chip);
                setText(null);
            }
        });

        TableColumn<JobSummaryRow, JobSummaryRow> actionCol = new TableColumn<>("ACTION");
        actionCol.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        actionCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(JobSummaryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Button action = new Button(item.actionLabel());
                action.setStyle(item.primaryAction()
                        ? "-fx-background-color: #354a5f; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 7 14 7 14;"
                        : "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-text-fill: #334155; -fx-font-size: 12px; -fx-font-weight: 600; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 7 14 7 14;");
                action.setOnAction(event -> handleRecentJobAction(item));
                setGraphic(action);
                setText(null);
            }
        });

        titleCol.setPrefWidth(420);
        deptCol.setPrefWidth(260);
        statusCol.setPrefWidth(120);
        actionCol.setPrefWidth(140);
        statusCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().setAll(titleCol, deptCol, statusCol, actionCol);

        List<JobSummaryRow> rows = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .sorted(Comparator.comparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(this::toJobSummaryRow)
                .toList();
        table.setItems(FXCollections.observableArrayList(rows));

        card.getChildren().addAll(header, table);
        return card;
    }

    private VBox buildRecommendedCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        HBox header = new HBox(8);
        Label title = new Label(I18n.t("recommended_for_you"));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label aiTag = new Label(I18n.t("ai_auto_match"));
        aiTag.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: white;"
                + "-fx-background-color: #6366f1; -fx-background-radius: 999;"
                + "-fx-padding: 3 10 3 10;");

        Button refreshAi = new Button(I18n.t("ai_refresh_match", "Refresh"));
        refreshAi.getStyleClass().add("secondary-button");
        refreshAi.setGraphic(IconFactory.glyph(IconFactory.IconType.REFRESH, 12, Color.web("#6366f1")));
        refreshAi.setGraphicTextGap(6);
        refreshAi.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10 4 10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, refreshAi, aiTag);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox row = buildRecommendationRow(buildLocalRecommendations());
        refreshAi.setOnAction(event -> refreshAiRecommendations(row, refreshAi));

        card.getChildren().addAll(header, row);
        return card;
    }

    private HBox buildRecommendationRow(List<DashboardRecommendation> recommendations) {
        HBox row = new HBox(14);
        row.setFillHeight(true);
        HBox.setHgrow(row, Priority.ALWAYS);
        row.setMaxWidth(Double.MAX_VALUE);

        if (recommendations.size() >= 2) {
            VBox card1 = recommendationCard(recommendations.get(0).job(), recommendations.get(0).score());
            VBox card2 = recommendationCard(recommendations.get(1).job(), recommendations.get(1).score());
            HBox.setHgrow(card1, Priority.ALWAYS);
            HBox.setHgrow(card2, Priority.ALWAYS);
            row.getChildren().addAll(card1, card2);
        } else if (recommendations.size() == 1) {
            VBox card1 = recommendationCard(recommendations.get(0).job(), recommendations.get(0).score());
            VBox card2 = placeholderRecommendCard(87);
            HBox.setHgrow(card1, Priority.ALWAYS);
            HBox.setHgrow(card2, Priority.ALWAYS);
            row.getChildren().addAll(card1, card2);
        } else {
            VBox card1 = placeholderRecommendCard(90);
            VBox card2 = placeholderRecommendCard(83);
            HBox.setHgrow(card1, Priority.ALWAYS);
            HBox.setHgrow(card2, Priority.ALWAYS);
            row.getChildren().addAll(card1, card2);
        }

        return row;
    }

    private void refreshAiRecommendations(HBox row, Button refreshAi) {
        refreshAi.setDisable(true);
        refreshAi.setText(I18n.t("ai_matching", "Matching..."));

        Task<List<DashboardRecommendation>> task = new Task<>() {
            @Override
            protected List<DashboardRecommendation> call() {
                List<DashboardRecommendation> recommendations = buildAiRecommendations();
                return recommendations.isEmpty() ? buildLocalRecommendations() : recommendations;
            }
        };

        task.setOnSucceeded(event -> {
            row.getChildren().setAll(buildRecommendationRow(task.getValue()).getChildren());
            refreshAi.setText(I18n.t("ai_refresh_match", "Refresh"));
            refreshAi.setDisable(false);
        });
        task.setOnFailed(event -> {
            row.getChildren().setAll(buildRecommendationRow(buildLocalRecommendations()).getChildren());
            refreshAi.setText(I18n.t("ai_refresh_match", "Refresh"));
            refreshAi.setDisable(false);
        });

        Thread worker = new Thread(task, "deepseek-job-match");
        worker.setDaemon(true);
        worker.start();
    }

    private List<DashboardRecommendation> buildAiRecommendations() {
        List<Job> openJobs = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN && job.getDeadline() != null)
                .sorted(Comparator.comparing(Job::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (openJobs.isEmpty()) {
            return List.of();
        }

        List<AiJobMatchDTO> aiMatches = services.deepSeekJobMatchService().rankJobs(profile, resume, openJobs);
        if (!aiMatches.isEmpty()) {
            Map<String, Job> jobsById = openJobs.stream()
                    .collect(Collectors.toMap(Job::getJobId, Function.identity(), (a, b) -> a));
            List<DashboardRecommendation> aiRecommendations = aiMatches.stream()
                    .map(match -> {
                        Job job = jobsById.get(match.jobId());
                        return job == null ? null : new DashboardRecommendation(job, match.score());
                    })
                    .filter(item -> item != null)
                    .limit(2)
                    .toList();
            if (!aiRecommendations.isEmpty()) {
                return aiRecommendations;
            }
        }

        return scoreLocally(openJobs);
    }

    private List<DashboardRecommendation> buildLocalRecommendations() {
        List<Job> openJobs = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN && job.getDeadline() != null)
                .sorted(Comparator.comparing(Job::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return scoreLocally(openJobs);
    }

    private List<DashboardRecommendation> scoreLocally(List<Job> openJobs) {
        return openJobs.stream()
                .map(job -> new DashboardRecommendation(job, recommendationScore(job)))
                .sorted(Comparator.comparingInt(DashboardRecommendation::score).reversed())
                .limit(2)
                .toList();
    }

    private VBox placeholderRecommendCard(int matchScore) {
        VBox box = new VBox(10);
        box.getStyleClass().add("soft-info-card");
        box.setPadding(new Insets(14));
        HBox.setHgrow(box, Priority.ALWAYS);

        Region tagBar = skeletonBar(60, 16);
        Region titleBar1 = skeletonBar(Double.MAX_VALUE, 14);
        Region titleBar2 = skeletonBar(160, 14);
        Region descBar1 = skeletonBar(Double.MAX_VALUE, 10);
        Region descBar2 = skeletonBar(Double.MAX_VALUE, 10);
        Region descBar3 = skeletonBar(120, 10);
        Region hoursBar = skeletonBar(60, 10);
        Region deadlineBar = skeletonBar(80, 10);
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(hoursBar, footerSpacer, deadlineBar);
        footer.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(tagBar, titleBar1, titleBar2, descBar1, descBar2, descBar3, footer);
        return box;
    }

    private Region skeletonBar(double width, double height) {
        Region bar = new Region();
        bar.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 4;");
        bar.setPrefHeight(height);
        bar.setMinHeight(height);
        bar.setMaxHeight(height);
        if (width == Double.MAX_VALUE) {
            bar.setMaxWidth(Double.MAX_VALUE);
        } else {
            bar.setPrefWidth(width);
            bar.setMaxWidth(width);
        }
        return bar;
    }

    private VBox recommendationCard(Job job, int matchScore) {
        VBox box = new VBox(10);
        box.getStyleClass().add("soft-info-card");
        box.setPadding(new Insets(14));
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setCursor(javafx.scene.Cursor.HAND);
        box.setOnMouseClicked(event -> openJobDetailModal(job));

        Label tag = new Label(fallback(job.getModuleCode(), "BUPT"));
        tag.getStyleClass().addAll("mini-pill", "mini-pill-neutral");

        Label title = new Label(job.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");

        Label desc = new Label(fallback(job.getDescription(), I18n.t("recommended_skill_match")));
        desc.setWrapText(true);
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label hours = new Label(job.getWeeklyHours() + I18n.t("hours_per_week"));
        hours.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        Label deadline = new Label(I18n.t("deadline_label") + formatDate(job.getDeadline()));
        deadline.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label matchRate = new Label(I18n.tl("match_rate_percent", matchScore));
        matchRate.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #6366f1;");

        footer.getChildren().addAll(hours, spacer, matchRate, deadline);
        box.getChildren().addAll(tag, title, desc, footer);
        return box;
    }

    private VBox buildQuickActionsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("accent-panel");
        card.setPadding(new Insets(18));

        Label title = new Label(I18n.t("quick_actions"));
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: white;");

        Button profileButton = quickActionButton(I18n.t("complete_profile"), IconFactory.IconType.PENCIL);
        profileButton.setOnAction(event -> openProfileModal());

        HBox profileRow = new HBox(profileButton);
        HBox.setHgrow(profileButton, Priority.ALWAYS);
        profileRow.setAlignment(Pos.CENTER_LEFT);

        Button uploadCv = quickActionButton(I18n.t("upload_cv_portfolio"), IconFactory.IconType.UPLOAD);
        uploadCv.setOnAction(event -> openCvModal());

        Button browseJobs = quickActionButton(I18n.t("browse_jobs"), IconFactory.IconType.SEARCH);
        browseJobs.setOnAction(event -> openJobBrowserModal());

        Button viewApplications = quickActionButton(I18n.t("my_applications"), IconFactory.IconType.CLIPBOARD);
        viewApplications.setOnAction(event -> openApplicationsModal());

        card.getChildren().addAll(title, profileRow, uploadCv, browseJobs, viewApplications);
        return card;
    }

    private Button quickActionButton(String text, IconFactory.IconType iconType) {
        Button button = new Button(text);
        button.getStyleClass().add("dashboard-action-button");
        button.setGraphic(IconFactory.glyph(iconType, 16, Color.WHITE));
        button.setGraphicTextGap(10);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private VBox buildStatusCheckCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label title = new Label(I18n.t("status_check"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        VBox body = new VBox(10);
        boolean hasCv = resume.getCvFileName() != null && !resume.getCvFileName().isBlank();
        List<String> missingSections = services.resumeService().getMissingResumeSections(applicantId);
        int profileCompletion = services.applicantProfileService().calculateProfileCompletion(applicantId);
        int currentHours = services.workloadService().getWorkload(applicantId).currentHours();
        int maxHours = Math.max(services.workloadService().getWorkload(applicantId).maxWeeklyHours(), 1);

        if (!hasCv) {
            body.getChildren().add(alertBox(I18n.t("missing_documents"), I18n.t("missing_cv_msg"), "#fff1f2", "#ef4444"));
        }
        if (!missingSections.isEmpty()) {
            body.getChildren().add(alertBox(I18n.t("profile_incomplete"), I18n.t("profile_missing_sections").replace("{sections}", String.join(", ", missingSections)), "#fff7ed", "#f59e0b"));
        } else if (profileCompletion < 100) {
            body.getChildren().add(alertBox(I18n.t("profile_incomplete"), I18n.t("profile_incomplete_msg"), "#fff7ed", "#f59e0b"));
        }
        if (currentHours >= maxHours || currentHours * 1.0 / maxHours > 0.8) {
            body.getChildren().add(alertBox(I18n.t("workload_warning"), I18n.t("workload_warning_msg"), "#eff6ff", "#2563eb"));
        }
        if (body.getChildren().isEmpty()) {
            body.getChildren().add(alertBox(I18n.t("all_set"), I18n.t("all_set_msg"), "#ecfdf3", "#10b981"));
        }

        card.getChildren().addAll(title, body);
        return card;
    }

    private VBox alertBox(String title, String body, String background, String accent) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: " + background + "; -fx-border-color: transparent; -fx-background-radius: 10; -fx-border-radius: 10;");

        IconFactory.IconType iconType = switch (title) {
            case "All Set" -> IconFactory.IconType.CHECK_CIRCLE;
            case "Workload Warning" -> IconFactory.IconType.ALERT_TRIANGLE;
            case "Missing Documents" -> IconFactory.IconType.ALERT_TRIANGLE;
            default -> IconFactory.IconType.INFO_CIRCLE;
        };

        HBox titleRow = new HBox(6,
                IconFactory.glyph(iconType, 14, Color.web(accent)),
                new Label(title)
        );
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleNode = (Label) titleRow.getChildren().get(1);
        titleNode.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";");

        Label bodyNode = new Label(body);
        bodyNode.setWrapText(true);
        bodyNode.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: " + accent + ";");

        box.getChildren().addAll(titleRow, bodyNode);
        return box;
    }

    private VBox buildDeadlineCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));

        Label title = new Label(I18n.t("recruitment_deadlines"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        VBox body = new VBox(10);
        List<Job> deadlines = services.jobService().searchJobs(null).stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN && job.getDeadline() != null)
                .sorted(Comparator.comparing(Job::getDeadline))
                .limit(2)
                .toList();

        if (deadlines.isEmpty()) {
            Label empty = new Label(I18n.t("no_upcoming_deadlines"));
            empty.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");
            body.getChildren().add(empty);
        } else {
            deadlines.forEach(job -> body.getChildren().add(deadlineRow(job)));
        }

        card.getChildren().addAll(title, body);
        return card;
    }

    private HBox deadlineRow(Job job) {
        String day = job.getDeadline() == null ? "--" : String.format("%02d", job.getDeadline().getDayOfMonth());
        String month = job.getDeadline() == null ? "---" : job.getDeadline().getMonth().name().substring(0, 3);

        VBox dateBox = new VBox(
                styledLabel(day, "-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;"),
                styledLabel(month, "-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #94a3b8;")
        );
        dateBox.setAlignment(Pos.CENTER);
        dateBox.setMinWidth(42);

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        title.setWrapText(true);

        Label meta = new Label(fallback(job.getModuleCode(), "-") + " \u2022 " + formatDate(job.getDeadline()));
        meta.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

        VBox body = new VBox(2, title, meta);
        return new HBox(12, dateBox, body);
    }

    private StackPane progressRing(int percent, String accent) {
        Circle track = new Circle(24);
        track.setFill(Color.TRANSPARENT);
        track.setStroke(Color.web("#dfe7f1"));
        track.setStrokeWidth(5);

        Arc progress = new Arc(0, 0, 24, 24, 90, -360 * (percent / 100.0));
        progress.setType(ArcType.OPEN);
        progress.setFill(Color.TRANSPARENT);
        progress.setStroke(Color.web(accent));
        progress.setStrokeWidth(5);
        progress.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        Label value = new Label(percent + "%");
        value.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        return new StackPane(track, progress, value);
    }

    private VBox profileTextBlock(String kickerText, String titleText, String subtitleText) {
        Label kicker = new Label(kickerText);
        kicker.getStyleClass().add("tiny-kicker");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #94a3b8;");

        return new VBox(3, kicker, title, subtitle);
    }

    private StackPane statIconBox(String text) {
        StackPane icon = new StackPane(new Label(text));
        icon.setMinSize(36, 36);
        icon.setPrefSize(36, 36);
        icon.setMaxSize(36, 36);
        icon.setStyle("-fx-background-color: #f3f7fb; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #64748b;");
        return icon;
    }

    private Label buildMiniPill(String text, String tone) {
        Label label = new Label(text);
        label.getStyleClass().add("mini-pill");
        switch (tone) {
            case "success" -> label.getStyleClass().add("mini-pill-success");
            case "warning" -> label.getStyleClass().add("mini-pill-warning");
            case "danger" -> label.getStyleClass().add("mini-pill-danger");
            case "info" -> label.getStyleClass().add("mini-pill-info");
            default -> label.getStyleClass().add("mini-pill-neutral");
        }
        return label;
    }

    private TableCell<JobSummaryRow, String> mutedTableCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (!empty) {
                    setStyle("-fx-text-fill: #64748b; -fx-font-weight: 600;");
                } else {
                    setStyle("");
                }
                setGraphic(null);
            }
        };
    }

    private JobSummaryRow toJobSummaryRow(Job job) {
        Optional<ApplicationStatus> statusOpt = services.applicationService().getApplicationStatus(applicantId, job.getJobId());
        if (statusOpt.isPresent()) {
            ApplicationStatus status = statusOpt.get();
            String label = switch (status) {
                case SUBMITTED, UNDER_REVIEW -> "APPLIED";
                case ACCEPTED -> "ACCEPTED";
                case REJECTED -> "REJECTED";
                case CANCELLED -> "OPEN";
            };
            String tone = switch (status) {
                case ACCEPTED -> "success";
                case REJECTED -> "danger";
                case SUBMITTED, UNDER_REVIEW -> "info";
                case CANCELLED -> "neutral";
            };
            String action = status == ApplicationStatus.CANCELLED ? "Apply" : "Details";
            boolean primary = status == ApplicationStatus.CANCELLED;
            return new JobSummaryRow(job, label, tone, action, primary);
        }
        return new JobSummaryRow(job, "OPEN", "neutral", "Apply", true);
    }

    private void handleRecentJobAction(JobSummaryRow row) {
        if (row.primaryAction()) {
            applyToJob(row.job());
        } else {
            openJobDetailModal(row.job());
        }
    }

    private void applyToJob(Job job) {
        Optional<String> statementOpt = JobApplyDialog.showAndWait(
                job,
                profile,
                services.resumeService(),
                applicantId,
                view.getScene() == null ? null : view.getScene().getWindow()
        );
        if (statementOpt.isEmpty()) {
            return;
        }

        ValidationResult result = services.applicationService().apply(applicantId, job.getJobId(), statementOpt.get());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(I18n.t("apply_failed"), String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success(I18n.t("apply_success"), I18n.t("apply_success_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        initialize();
    }

    private int recommendationScore(Job job) {
        try {
            MatchExplanationDTO match = services.matchingService().evaluateMatch(applicantId, job.getJobId());
            return match == null ? 0 : match.score();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void openJobBrowserModal() {
        showModal("Browse Jobs", new JobBrowserController(services, user).getView(), 1320, 860);
    }

    private void openJobDetailModal(Job job) {
        JobDetailController detailCtrl = new JobDetailController(services);
        ApplicationStatus appStatus = services.applicationService().getApplicationStatus(applicantId, job.getJobId()).orElse(null);
        detailCtrl.setJobWithApplicationStatus(job, applicantId, appStatus);

        detailCtrl.setOnApply(() -> {
            applyToJob(job);
        });
        detailCtrl.setOnCancel(cancelledApplicantId -> {
            cancelApplication(job);
        });
        detailCtrl.setOnFavouriteToggle(jobId -> {
            toggleFavourite(jobId);
            detailCtrl.updateHeartIconDirectly(jobId,
                    services.favouriteJobRepository().findByApplicantId(applicantId)
                            .map(f -> f.isFavourite(jobId)).orElse(false));
        });

        showModal("Job Detail", detailCtrl.getView(), 800, 820);
    }

    private void cancelApplication(Job job) {
        ValidationResult result = services.applicationService().cancelApplication(applicantId, job.getJobId());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(I18n.t("cancel_failed"), String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }
        DialogControllerFactory.success(I18n.t("cancel_success"), I18n.t("cancel_success_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        initialize();
    }

    private void toggleFavourite(String jobId) {
        if (applicantId == null) return;
        var favouriteOpt = services.favouriteJobRepository().findByApplicantId(applicantId);
        var favourite = favouriteOpt.orElseGet(() -> new edu.bupt.ta.model.FavouriteJob(applicantId, new java.util.ArrayList<>()));
        favourite.toggleFavourite(jobId);
        services.favouriteJobRepository().saveForApplicant(favourite);
    }

    private void openApplicationsModal() {
        showModal("My Applications", new MyApplicationsController(services, user, job -> {
            showModal("Job Details", new JobDetailController(services).getView(), 900, 700);
        }).getView(), 1180, 820);
    }

    private void openProfileModal() {
        showModal("Profile", new ApplicantProfileController(services, user).getView(), 1100, 760);
    }

    private void openCvModal() {
        showModal("My CV", new MyCvController(services, user).getView(), 1160, 860);
    }

    private void showModal(String title, Parent content, double width, double height) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle(title);
        BorderPane modalRoot = new BorderPane();
        modalRoot.getStyleClass().add("app-surface");
        modalRoot.setCenter(content);
        Scene scene = new Scene(modalRoot, width, height);
        if (TADashboardController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(TADashboardController.class.getResource("/styles/app.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        initialize();
    }

    private Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        return label;
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DEADLINE_FORMAT).toUpperCase(Locale.ROOT);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveApplicantDisplayName() {
        if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
            return profile.getFullName().trim();
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName().trim();
        }
        return "Student";
    }

    private record JobSummaryRow(Job job, String statusLabel, String statusTone, String actionLabel, boolean primaryAction) {
    }

    private record DashboardRecommendation(Job job, int score) {
    }
}
