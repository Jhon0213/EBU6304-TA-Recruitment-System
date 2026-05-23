package edu.bupt.ta.controller;

import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class MODashboardController {
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final double KPI_CARD_HEIGHT = 154;
    private static final int RECENT_JOB_MONTHS = 3;
    private static final double RECENT_JOBS_HEIGHT = 450;
    private static final double DRAFT_JOBS_HEIGHT = 330;
    private static final double RIGHT_CARD_HEIGHT = 252;

    private final ServiceRegistry services;
    private final User user;
    private final Consumer<Job> openJobManagement;
    private final Runnable openApplicantList;
    private final Runnable openProfile;

    private final BorderPane view = new BorderPane();

    public MODashboardController(ServiceRegistry services,
                                 User user,
                                 Consumer<Job> openJobManagement,
                                 Runnable openApplicantList,
                                 Runnable openProfile) {
        this.services = services;
        this.user = user;
        this.openJobManagement = openJobManagement;
        this.openApplicantList = openApplicantList;
        this.openProfile = openProfile;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        refresh();
    }

    private void refresh() {
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId()).stream()
                .sorted(Comparator.comparing(Job::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Application> applications = jobs.stream()
                .flatMap(job -> services.applicationService().getApplicationsByJob(job.getJobId()).stream())
                .toList();

        long openJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.OPEN).count();
        VBox page = new VBox(22);
        page.getStyleClass().add("app-surface");
        page.setPadding(new Insets(24));
        page.getChildren().setAll(
                buildHero(jobs.size(), applications.size(), openJobs),
                buildKpis(jobs, applications, openJobs),
                buildBody(jobs)
        );

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        view.setCenter(scrollPane);
    }

    private HBox buildHero(int jobCount, int applicationCount, long openJobs) {
        VBox copy = new VBox(6);
        copy.setMinWidth(0);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Label title = new Label(I18n.tl("welcome_back_mo", displayName()));
        title.getStyleClass().add("page-title");
        title.setWrapText(true);

        Label subtitle = new Label(jobCount == 0
                ? I18n.t("no_job_posts_created")
                : I18n.tl("managing_job_posts", jobCount, openJobs, applicationCount));
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        copy.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button manageJobs = new Button(I18n.t("job_management_sidebar"));
        manageJobs.getStyleClass().add("secondary-button");
        manageJobs.setOnAction(event -> openJobManagement.accept(null));

        Button browseApplicants = new Button(I18n.t("applicant_list_sidebar"));
        browseApplicants.getStyleClass().add("primary-button");
        browseApplicants.setOnAction(event -> openApplicantList.run());

        HBox row = new HBox(12, copy, spacer, manageJobs, browseApplicants);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildKpis(List<Job> jobs, List<Application> applications, long openJobs) {
        long draftJobs = jobs.stream().filter(job -> job.getStatus() == JobStatus.DRAFT).count();
        List<Job> activePipelineJobs = jobs.stream()
                .filter(this::isInActiveApplicationScope)
                .toList();
        int activeApplications = activePipelineJobs.stream()
                .mapToInt(this::countNonCancelledApplications)
                .sum();
        long underReviewApplications = activePipelineJobs.stream()
                .flatMap(job -> services.applicationService().getApplicationsByJob(job.getJobId()).stream())
                .filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW)
                .count();
        int reviewedRate = activeApplications == 0 ? 0
                : (int) Math.round(underReviewApplications * 100.0 / activeApplications);

        HBox row = new HBox(16,
                kpiCard(I18n.t("managed_jobs"), String.valueOf(jobs.size()),
                        jobs.isEmpty() ? I18n.t("no_postings_created_yet") : I18n.tl("currently_open", openJobs),
                        IconFactory.IconType.BRIEFCASE, "#e0f2fe", "#0284c7"),
                kpiCard(I18n.t("active_applications"), String.valueOf(activeApplications),
                        I18n.t("open_unfilled_expired"),
                        IconFactory.IconType.USERS, "#ecfdf3", "#10b981"),
                kpiCard(I18n.t("draft_jobs"), String.valueOf(draftJobs),
                        draftJobs == 0 ? I18n.t("all_jobs_publishable") : I18n.t("not_visible_until_published"),
                        IconFactory.IconType.FILE, "#fff7ed", "#f59e0b"),
                kpiCard(I18n.t("under_review_count"), reviewedRate + "%",
                        activeApplications == 0
                                ? I18n.t("no_active_applications")
                                : I18n.tl("in_screening", underReviewApplications, activeApplications),
                        IconFactory.IconType.EYE, "#eef2ff", "#6366f1")
        );
        row.setMinWidth(0);
        row.setFillHeight(true);
        return row;
    }

    private VBox kpiCard(String titleText,
                         String valueText,
                         String metaText,
                         IconFactory.IconType iconType,
                         String iconBackground,
                         String iconColor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setMinWidth(0);
        card.setPrefWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(KPI_CARD_HEIGHT);
        card.setPrefHeight(KPI_CARD_HEIGHT);
        card.setMaxHeight(KPI_CARD_HEIGHT);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        StackPane iconBadge = IconFactory.badge(
                iconType,
                28,
                Color.web(iconBackground),
                Color.web(iconColor)
        );
        iconBadge.setStyle("-fx-background-color: " + iconBackground + "; -fx-background-radius: 999;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, iconBadge);
        header.setAlignment(Pos.CENTER_LEFT);

        Label value = new Label(valueText);
        value.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label meta = new Label(metaText);
        meta.setWrapText(true);
        meta.setMinHeight(Region.USE_PREF_SIZE);
        meta.setTextOverrun(OverrunStyle.CLIP);
        meta.setMaxWidth(Double.MAX_VALUE);
        meta.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #10b981;");
        meta.prefWidthProperty().bind(card.widthProperty().subtract(40));

        card.getChildren().addAll(header, value, meta);
        return card;
    }

    private boolean isInActiveApplicationScope(Job job) {
        if (job == null || job.getStatus() == null) {
            return false;
        }
        if (job.getStatus() == JobStatus.OPEN) {
            return true;
        }
        if (job.getStatus() != JobStatus.EXPIRED) {
            return false;
        }
        long acceptedCount = services.applicationService().getApplicationsByJob(job.getJobId()).stream()
                .filter(app -> app.getStatus() == ApplicationStatus.ACCEPTED)
                .count();
        return acceptedCount < Math.max(job.getPositions(), 0);
    }

    private int countNonCancelledApplications(Job job) {
        return (int) services.applicationService().getApplicationsByJob(job.getJobId()).stream()
                .filter(app -> app.getStatus() != ApplicationStatus.CANCELLED)
                .count();
    }

    private HBox buildBody(List<Job> jobs) {
        VBox left = new VBox(18, recentJobsCard(jobs), draftJobsCard(jobs));
        VBox right = new VBox(18, quickActionsCard(), deadlineCard(jobs), statusCheckCard(jobs));

        left.setMinWidth(0);
        left.setPrefHeight(RECENT_JOBS_HEIGHT + DRAFT_JOBS_HEIGHT + 18);
        HBox.setHgrow(left, Priority.ALWAYS);

        right.setMinWidth(320);
        right.setPrefWidth(320);
        right.setPrefHeight(RIGHT_CARD_HEIGHT * 3 + 36);

        return new HBox(18, left, right);
    }

    private VBox recentJobsCard(List<Job> jobs) {
        VBox card = shellCard(I18n.t("recent_jobs_3_months"));
        card.setPrefHeight(RECENT_JOBS_HEIGHT);
        card.setMinHeight(RECENT_JOBS_HEIGHT);
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(RECENT_JOB_MONTHS);

        List<Job> recent = jobs.stream()
                .filter(job -> job.getStatus() != JobStatus.DRAFT)
                .filter(job -> job.getCreatedAt() != null && !job.getCreatedAt().isBefore(cutoff))
                .toList();

        if (recent.isEmpty()) {
            card.getChildren().add(centeredEmptyState(
                    I18n.t("no_recent_jobs"),
                    I18n.t("created_last_3_months")
            ));
        } else {
            VBox rows = new VBox(10);
            for (Job job : recent) {
                int applicantCount = services.applicationService().getApplicationsByJob(job.getJobId()).size();
                rows.getChildren().add(jobRow(job, applicantCount));
            }
            card.getChildren().add(scrollContent(rows));
        }
        return card;
    }

    private VBox draftJobsCard(List<Job> jobs) {
        VBox card = shellCard(I18n.t("draft_jobs"));
        card.setPrefHeight(DRAFT_JOBS_HEIGHT);
        card.setMinHeight(DRAFT_JOBS_HEIGHT);

        List<Job> drafts = jobs.stream()
                .filter(job -> job.getStatus() == JobStatus.DRAFT)
                .toList();

        if (drafts.isEmpty()) {
            card.getChildren().add(centeredEmptyState(
                    I18n.t("no_draft_jobs_pending"),
                    I18n.t("unpublished_job_posts")
            ));
        } else {
            VBox rows = new VBox(10);
            for (Job job : drafts) {
                int applicantCount = services.applicationService().getApplicationsByJob(job.getJobId()).size();
                rows.getChildren().add(jobRow(job, applicantCount));
            }
            card.getChildren().add(scrollContent(rows));
        }
        return card;
    }

    private VBox quickActionsCard() {
        VBox card = shellCard(I18n.t("quick_actions_mo"));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #14b8a6, #10b981);"
                + "-fx-background-radius: 24; -fx-border-radius: 24;");
        card.setPrefHeight(RIGHT_CARD_HEIGHT);
        card.setMinHeight(RIGHT_CARD_HEIGHT);

        card.getChildren().clear();

        Label title = new Label(I18n.t("quick_actions_mo"));
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: white;");

        Label body = new Label(I18n.t("jump_workflows"));
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: rgba(255,255,255,0.88);");

        VBox actions = new VBox(10,
                actionButton(I18n.t("manage_jobs_btn"), () -> openJobManagement.accept(null)),
                actionButton(I18n.t("review_applicants_btn"), openApplicantList),
                actionButton(I18n.t("update_profile_btn"), openProfile)
        );

        VBox bodyBox = new VBox(14, body, actions);
        card.getChildren().addAll(title, bodyBox);
        return card;
    }

    private VBox deadlineCard(List<Job> jobs) {
        VBox card = shellCard(I18n.t("recruitment_deadlines_mo"));
        card.setPrefHeight(RIGHT_CARD_HEIGHT);
        card.setMinHeight(RIGHT_CARD_HEIGHT);

        List<Job> deadlines = jobs.stream()
                .filter(job -> job.getDeadline() != null)
                .filter(job -> job.getStatus() == JobStatus.OPEN || job.getStatus() == JobStatus.EXPIRED)
                .sorted(Comparator.comparing(Job::getDeadline))
                .toList();

        if (deadlines.isEmpty()) {
            card.getChildren().add(centeredEmptyState(
                    I18n.t("no_active_deadlines"),
                    I18n.t("only_open_expired")
            ));
        } else {
            VBox rows = new VBox(10);
            for (Job job : deadlines) {
                rows.getChildren().add(deadlineRow(job));
            }
            card.getChildren().add(scrollContent(rows));
        }
        return card;
    }

    private VBox statusCheckCard(List<Job> jobs) {
        VBox card = shellCard(I18n.t("status_check_mo"));
        card.setPrefHeight(RIGHT_CARD_HEIGHT);
        card.setMinHeight(RIGHT_CARD_HEIGHT);
        VBox body = new VBox(10);

        long draftCount = jobs.stream().filter(job -> job.getStatus() == JobStatus.DRAFT).count();
        long openWithoutApplicants = jobs.stream()
                .filter(job -> job.getStatus() == JobStatus.OPEN)
                .filter(job -> services.applicationService().getApplicationsByJob(job.getJobId()).isEmpty())
                .count();

        if (draftCount > 0) {
            body.getChildren().add(alertBox(
                    I18n.t("draft_jobs_pending"),
                    I18n.tl("draft_jobs_pending_desc", draftCount),
                    "#fff7ed",
                    "#f59e0b"
            ));
        }
        if (openWithoutApplicants > 0) {
            body.getChildren().add(alertBox(
                    I18n.t("open_jobs_without_applicants"),
                    I18n.tl("open_no_applicants_desc", openWithoutApplicants),
                    "#eff6ff",
                    "#2563eb"
            ));
        }
        if (body.getChildren().isEmpty()) {
            body.getChildren().add(alertBox(
                    I18n.t("all_clear_mo"),
                    I18n.t("no_management_issues"),
                    "#ecfdf3",
                    "#10b981"
            ));
        }

        card.getChildren().add(scrollContent(body));
        return card;
    }

    private VBox shellCard(String titleText) {
        VBox card = new VBox(16);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(18));
        card.setFillWidth(true);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        card.getChildren().add(title);
        return card;
    }

    private VBox alertBox(String titleText, String bodyText, String background, String accent) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: " + background + "; -fx-background-radius: 10; -fx-border-radius: 10;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 11px; -fx-font-weight: 400; -fx-text-fill: " + accent + ";");

        box.getChildren().addAll(title, body);
        return box;
    }

    private ScrollPane scrollContent(Node contentNode) {
        ScrollPane scrollPane = new ScrollPane(contentNode);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("detail-scroll-plain");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return scrollPane;
    }

    private VBox emptyStateBox(String titleText, String bodyText) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-radius: 10;");

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setStyle("-fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #64748b;");

        box.getChildren().addAll(title, body);
        return box;
    }

    private VBox centeredEmptyState(String titleText, String bodyText) {
        Region illustration = new Region();
        illustration.setMinSize(96, 96);
        illustration.setPrefSize(96, 96);
        illustration.setMaxSize(96, 96);
        illustration.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8fafc, #eef2f7);"
                + "-fx-background-radius: 999; -fx-border-radius: 999;");

        Region paper = new Region();
        paper.setMinSize(30, 40);
        paper.setPrefSize(30, 40);
        paper.setMaxSize(30, 40);
        paper.setStyle("-fx-background-color: rgba(203, 213, 225, 0.42);"
                + "-fx-background-radius: 12; -fx-border-radius: 12;");

        Region line1 = new Region();
        line1.setMinSize(15, 3);
        line1.setPrefSize(15, 3);
        line1.setMaxSize(15, 3);
        line1.setStyle("-fx-background-color: rgba(226, 232, 240, 0.95); -fx-background-radius: 999;");

        Region line2 = new Region();
        line2.setMinSize(15, 3);
        line2.setPrefSize(15, 3);
        line2.setMaxSize(15, 3);
        line2.setStyle("-fx-background-color: rgba(226, 232, 240, 0.95); -fx-background-radius: 999;");

        Region line3 = new Region();
        line3.setMinSize(11, 3);
        line3.setPrefSize(11, 3);
        line3.setMaxSize(11, 3);
        line3.setStyle("-fx-background-color: rgba(226, 232, 240, 0.95); -fx-background-radius: 999;");

        VBox paperLines = new VBox(6, line1, line2, line3);
        paperLines.setAlignment(Pos.CENTER_LEFT);
        StackPane paperIcon = new StackPane(paper, paperLines);
        paperIcon.setTranslateY(4);

        StackPane badge = IconFactory.badge(
                IconFactory.IconType.CLIPBOARD,
                34,
                Color.WHITE,
                Color.web("#64748b")
        );
        badge.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.10), 18, 0.18, 0, 4);");
        badge.setTranslateX(20);
        badge.setTranslateY(14);

        StackPane accent = IconFactory.badge(
                IconFactory.IconType.INFO_CIRCLE,
                26,
                Color.WHITE,
                Color.web("#cbd5e1")
        );
        accent.setStyle("-fx-background-color: white; -fx-background-radius: 999;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 12, 0.14, 0, 3);");
        accent.setTranslateX(-18);
        accent.setTranslateY(-14);

        StackPane illustrationGroup = new StackPane(illustration, paperIcon, badge, accent);
        illustrationGroup.setMinSize(110, 110);
        illustrationGroup.setPrefSize(110, 110);
        illustrationGroup.setMaxSize(110, 110);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.setMaxWidth(420);
        body.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        body.setAlignment(Pos.CENTER);

        VBox box = new VBox(8, illustrationGroup, title, body);
        box.setAlignment(Pos.CENTER);
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPadding(new Insets(4, 0, 4, 0));
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private HBox jobRow(Job job, int applicantCount) {
        VBox copy = new VBox(4);
        copy.setMinWidth(0);

        Label title = new Label(fallback(job.getTitle(), I18n.t("untitled_job")));
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label meta = new Label(fallback(job.getModuleCode(), "-") + " • " + fallback(job.getModuleName(), I18n.t("module_name")));
        meta.setWrapText(true);
        meta.setMaxWidth(Double.MAX_VALUE);
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        copy.getChildren().addAll(title, meta);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Label applicants = new Label(I18n.tl("applicants_label", applicantCount));
        applicants.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #475569;"
                + "-fx-background-color: #f1f5f9; -fx-background-radius: 999; -fx-padding: 6 10 6 10;");

        HBox row = new HBox(10, copy, applicants, buildStatusChip(job.getStatus()));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 10, 0));
        row.setStyle("-fx-border-color: transparent transparent #eef2f7 transparent;");
        row.setOnMouseClicked(event -> openJobManagement.accept(job));
        row.setOnMouseEntered(event -> row.setStyle("-fx-border-color: transparent transparent #eef2f7 transparent; -fx-background-color: #f8fafc; -fx-background-radius: 14;"));
        row.setOnMouseExited(event -> row.setStyle("-fx-border-color: transparent transparent #eef2f7 transparent;"));
        return row;
    }

    private Button actionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> action.run());
        button.setStyle("-fx-background-color: rgba(255,255,255,0.14); -fx-text-fill: white;"
                + "-fx-font-size: 14px; -fx-font-weight: 700; -fx-background-radius: 14; -fx-padding: 12 14 12 14;");
        return button;
    }

    private HBox deadlineRow(Job job) {
        DeadlineTone tone = deadlineTone(job.getDeadline());

        Label day = new Label(job.getDeadline().format(DEADLINE_FORMAT).toUpperCase(Locale.ENGLISH));
        day.setMinWidth(62);
        day.setAlignment(Pos.CENTER);
        day.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + tone.labelColor() + ";"
                + "-fx-background-color: " + tone.badgeBackground() + "; -fx-background-radius: 14; -fx-padding: 10 8 10 8;");

        VBox copy = new VBox(4);
        Label title = new Label(fallback(job.getTitle(), I18n.t("untitled_job")));
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label meta = new Label(deadlineMeta(job.getDeadline()));
        meta.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: " + tone.metaColor() + ";");

        copy.getChildren().addAll(title, meta);
        HBox.setHgrow(copy, Priority.ALWAYS);

        HBox row = new HBox(10, day, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 6, 4, 6));
        row.setOnMouseClicked(event -> openJobManagement.accept(job));
        row.setOnMouseEntered(event -> row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14;"));
        row.setOnMouseExited(event -> row.setStyle(""));
        return row;
    }

    private Label emptyLine(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        return label;
    }

    private Label buildStatusChip(JobStatus status) {
        String text = status == null ? "-" : I18n.t("job_status_" + status.name().toLowerCase());
        String background = status == null ? "#f1f5f9" : switch (status) {
            case OPEN -> "#dcfce7";
            case DRAFT -> "#e0e7ff";
            case CLOSED -> "#e2e8f0";
            case EXPIRED -> "#ffedd5";
        };
        String foreground = status == null ? "#64748b" : switch (status) {
            case OPEN -> "#16a34a";
            case DRAFT -> "#2563eb";
            case CLOSED -> "#64748b";
            case EXPIRED -> "#ea580c";
        };

        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + foreground + ";"
                + "-fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 6 10 6 10;");
        return chip;
    }

    private String displayName() {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName().trim();
        }
        return I18n.t("professor");
    }

    private String formatDeadline(LocalDateTime deadline) {
        return deadline == null ? "-" : deadline.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH));
    }

    private String deadlineMeta(LocalDateTime deadline) {
        if (deadline == null) {
            return I18n.t("no_deadline_set");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), deadline.toLocalDate());
        if (days < 0) {
            return I18n.tl("deadline_passed", formatDeadline(deadline));
        }
        if (days <= 3) {
            return I18n.tl("urgent_closes", formatDeadline(deadline));
        }
        if (days <= 10) {
            return I18n.tl("approaching_closes", formatDeadline(deadline));
        }
        return I18n.tl("deadline_closes", formatDeadline(deadline));
    }

    private DeadlineTone deadlineTone(LocalDateTime deadline) {
        if (deadline == null) {
            return new DeadlineTone("#f8fafc", "#0f172a", "#64748b");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now().toLocalDate(), deadline.toLocalDate());
        if (days < 0) {
            return new DeadlineTone("#fee2e2", "#b91c1c", "#dc2626");
        }
        if (days <= 3) {
            return new DeadlineTone("#ffedd5", "#c2410c", "#ea580c");
        }
        if (days <= 10) {
            return new DeadlineTone("#fef3c7", "#a16207", "#ca8a04");
        }
        return new DeadlineTone("#f8fafc", "#0f172a", "#64748b");
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record DeadlineTone(String badgeBackground, String labelColor, String metaColor) {
    }
}
