package edu.bupt.ta.controller;

import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.Notification;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MainShellController {

    private static final double SIDEBAR_WIDTH = 220;
    private static final Insets SIDEBAR_BRAND_PADDING = new Insets(18, 14, 16, 14);
    private static final Insets SIDEBAR_NAV_PADDING = new Insets(8, 8, 8, 8);
    private static final Insets SIDEBAR_FOOTER_PADDING = new Insets(10, 8, 10, 8);

    private final ServiceRegistry services;
    private final User user;
    private final Runnable onLogout;
    private final BorderPane view = new BorderPane();
    private final StackPane contentPane = new StackPane();
    private final Label breadcrumbBase = new Label();
    private final Label breadcrumbCurrent = new Label();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private String preferredApplicantJobId;
    private String preferredManagementJobId;
    private String currentPageId = "dashboard";
    private String currentPageIdBackup = "dashboard";

    private Popup notificationPopup;
    private StackPane bellContainer;
    private StackPane badge;

    private Stage getStage() {
        if (view.getScene() != null && view.getScene().getWindow() instanceof Stage s) {
            return s;
        }
        return null;
    }

    public MainShellController(ServiceRegistry services, User user, Runnable onLogout) {
        this.services = services;
        this.user = user;
        this.onLogout = onLogout;
        I18n.initTranslations();
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        breadcrumbBase.setText(I18n.t("recruitment_system"));
        view.getStyleClass().add("shell-root");
        buildShell();

        I18n.setOnLanguageChange(lang -> {
            currentPageIdBackup = currentPageId;
            buildShell();
            navigateTo(currentPageIdBackup);
        });
    }

    private void buildShell() {
        view.setLeft(buildSidebar());

        BorderPane mainArea = new BorderPane();
        mainArea.getStyleClass().add("shell-main-area");
        mainArea.setTop(buildTopBar());

        BorderPane center = new BorderPane();
        center.getStyleClass().add("shell-content");
        center.setCenter(contentPane);
        mainArea.setCenter(center);
        view.setCenter(mainArea);

        notificationPopup = createNotificationPopup();

        if (currentPageId == null || currentPageId.isBlank()) {
            currentPageId = "dashboard";
        }
        navigateTo(currentPageId);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("shell-sidebar");
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMinWidth(SIDEBAR_WIDTH);
        sidebar.setMaxWidth(SIDEBAR_WIDTH);

        VBox brandSection = new VBox();
        brandSection.setPadding(SIDEBAR_BRAND_PADDING);

        StackPane brandIcon = IconFactory.badge(
                IconFactory.IconType.GRADUATION_CAP,
                46,
                Color.web("#354a5f"),
                Color.WHITE
        );
        brandIcon.setStyle("-fx-background-color: #354a5f; -fx-background-radius: 999;");

        Label brandTitle = new Label(I18n.t("bupt_is_recruitment"));
        brandTitle.getStyleClass().add("shell-brand-title");
        brandTitle.setWrapText(true);
        brandTitle.setTextOverrun(OverrunStyle.CLIP);
        brandTitle.setMaxWidth(Double.MAX_VALUE);

        Label brandSub = new Label(roleEditionText());
        brandSub.getStyleClass().add("shell-brand-sub");
        brandSub.setMaxWidth(Double.MAX_VALUE);

        VBox brandText = new VBox(2, brandTitle, brandSub);
        brandText.setFillWidth(true);
        brandText.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(brandText, Priority.ALWAYS);

        HBox brandRow = new HBox(8, brandIcon, brandText);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        brandSection.getChildren().add(brandRow);

        VBox navArea = new VBox(8);
        navArea.setPadding(SIDEBAR_NAV_PADDING);
        navButtons.clear();

        if (user.getRole() == Role.TA) {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry(I18n.t("dashboard"), "dashboard", IconFactory.IconType.DASHBOARD),
                    new NavEntry(I18n.t("browse_jobs"), "browseJobs", IconFactory.IconType.SEARCH),
                    new NavEntry(I18n.t("my_applications"), "myApplications", IconFactory.IconType.CLIPBOARD),
                    new NavEntry(I18n.t("my_cv"), "myCv", IconFactory.IconType.FILE)
            )));
        } else if (user.getRole() == Role.MO) {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry(I18n.t("dashboard"), "moDashboard", IconFactory.IconType.DASHBOARD),
                    new NavEntry(I18n.t("job_management"), "jobManagement", IconFactory.IconType.BRIEFCASE),
                    new NavEntry(I18n.t("applicant_list"), "applicantList", IconFactory.IconType.USERS),
                    new NavEntry(I18n.t("profile"), "moProfile", IconFactory.IconType.USER)
            )));
        } else {
            navArea.getChildren().add(buildSection("RECRUITMENT", List.of(
                    new NavEntry(I18n.t("dashboard"), "adminDashboard", IconFactory.IconType.DASHBOARD),
                    new NavEntry(I18n.t("jobs"), "adminJobs", IconFactory.IconType.BRIEFCASE),
                    new NavEntry(I18n.t("applications"), "adminApplications", IconFactory.IconType.CLIPBOARD)
            )));
        }

        navArea.getChildren().add(buildSection("SUPPORT", List.of(
                new NavEntry(I18n.t("help_center"), "helpCenter", IconFactory.IconType.HELP),
                new NavEntry(I18n.t("settings"), "settings", IconFactory.IconType.SETTINGS)
        )));

        VBox footer = new VBox(12);
        footer.setPadding(SIDEBAR_FOOTER_PADDING);

        VBox profileCard = new VBox(10);
        profileCard.getStyleClass().add("shell-profile-card");
        profileCard.setPadding(new Insets(10));

        StackPane avatar = IconFactory.badge(
                IconFactory.IconType.USER,
                40,
                Color.web("#eef2f7"),
                Color.web("#64748b")
        );

        Label profileName = new Label(resolveProfileCardName());
        profileName.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");

        Label profileId = new Label("ID: " + user.getUserId());
        profileId.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");

        VBox profileText = new VBox(2, profileName, profileId);
        HBox profileRow = new HBox(10, avatar, profileText);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        profileCard.getChildren().add(profileRow);

        Button logout = new Button(I18n.t("logout"));
        logout.getStyleClass().add("secondary-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setGraphic(IconFactory.glyph(IconFactory.IconType.LOGOUT, 14, Color.web("#64748b")));
        logout.setGraphicTextGap(8);
        logout.setContentDisplay(ContentDisplay.RIGHT);
        logout.setOnAction(event -> onLogout.run());

        footer.getChildren().addAll(profileCard, logout);

        VBox.setVgrow(navArea, Priority.ALWAYS);
        sidebar.getChildren().addAll(brandSection, navArea, footer);
        return sidebar;
    }

    private VBox buildSection(String title, List<NavEntry> items) {
        VBox section = new VBox(8);

        Label header = new Label(title);
        header.getStyleClass().add("shell-nav-header");
        header.setPadding(new Insets(10, 8, 0, 8));

        VBox links = new VBox(6);
        for (NavEntry item : items) {
            links.getChildren().add(navButton(item));
        }

        section.getChildren().addAll(header, links);
        return section;
    }

    private Button navButton(NavEntry entry) {
        Button button = new Button(entry.label());
        button.getStyleClass().add("shell-nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(36);
        button.setGraphic(IconFactory.glyph(entry.icon(), 18, Color.web("#475569")));
        button.setGraphicTextGap(7);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setOnAction(event -> navigateTo(entry.pageId()));
        navButtons.put(entry.pageId(), button);
        return button;
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("shell-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 24, 16, 24));

        breadcrumbBase.getStyleClass().add("shell-breadcrumb-base");
        breadcrumbCurrent.getStyleClass().add("shell-breadcrumb-current");

        StackPane chevron = IconFactory.glyph(IconFactory.IconType.CHEVRON_RIGHT, 14, Color.web("#64748b"));

        HBox breadcrumb = new HBox(8, breadcrumbBase, chevron, breadcrumbCurrent);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox termInfo = new VBox(1);
        termInfo.setAlignment(Pos.CENTER_RIGHT);

        Label termMain = new Label(I18n.t("spring_semester_2026"));
        termMain.getStyleClass().add("shell-term-main");

        Label termSub = new Label(I18n.t("bupt_international_school"));
        termSub.getStyleClass().add("shell-term-sub");

        termInfo.getChildren().addAll(termMain, termSub);

        StackPane notificationIcon = IconFactory.glyph(IconFactory.IconType.BELL, 22, Color.web("#64748b"));

        Circle badgeDot = new Circle(6, Color.web("#ef4444"));
        badge = new StackPane(badgeDot);
        badge.setVisible(false);
        StackPane.setMargin(badge, new Insets(-8, 0, 0, 14));

        bellContainer = new StackPane(notificationIcon, badge);
        bellContainer.setCursor(javafx.scene.Cursor.HAND);
        bellContainer.setOnMouseClicked(e -> showNotificationPopup());

        Region divider = new Region();
        divider.getStyleClass().add("shell-topbar-divider");
        divider.setMinWidth(1);
        divider.setPrefWidth(1);
        divider.setMaxWidth(1);
        divider.setPrefHeight(34);

        HBox right = new HBox(16, bellContainer, divider, termInfo);
        right.setAlignment(Pos.CENTER_RIGHT);

        topBar.getChildren().addAll(breadcrumb, spacer, right);

        refreshNotificationBadge();
        return topBar;
    }

    private Popup createNotificationPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox panel = new VBox();
        panel.setPrefWidth(320);
        panel.setMaxWidth(320);
        panel.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-border-color: #e2e8f0; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 1, 3);"
        );

        HBox header = new HBox();
        header.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10 10 0 0;");
        header.setPadding(new Insets(10, 12, 10, 12));

        Label title = new Label(I18n.t("notifications"));
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllBtn = new Button(I18n.t("mark_all_read"));
        markAllBtn.setStyle("-fx-font-size: 10px; -fx-text-fill: #3b82f6; -fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
        markAllBtn.setOnAction(e -> {
            String recipientId = getNotificationRecipientId();
            services.notificationService().markAllAsRead(recipientId);
            refreshNotificationBadge();
            refreshNotificationList();
        });

        header.getChildren().addAll(title, spacer, markAllBtn);

        HBox footer = new HBox();
        footer.setId("notificationFooter");
        footer.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 0 0 10 10; -fx-padding: 8 12;");
        footer.setPrefWidth(320);
        footer.setMaxWidth(320);

        Button clearAllBtn = new Button(I18n.t("clear_all"));
        clearAllBtn.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #ef4444; " +
                "-fx-background-color: transparent; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-underline: false;"
        );
        clearAllBtn.setOnAction(e -> {
            String recipientId = getNotificationRecipientId();
            services.notificationService().clearAllNotifications(recipientId);
            refreshNotificationBadge();
            refreshNotificationList();
        });
        footer.getChildren().add(clearAllBtn);

        Label emptyLabel = new Label(I18n.t("no_notifications"));
        emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-padding: 30 0;");
        emptyLabel.setAlignment(Pos.CENTER);
        emptyLabel.setId("emptyLabel");

        VBox body = new VBox();
        body.setId("notificationBody");
        body.setMaxWidth(320);
        body.setStyle("-fx-background-color: #ffffff;");

        panel.getChildren().addAll(header, body, emptyLabel, footer);

        popup.getContent().add(panel);

        return popup;
    }

    private void showNotificationPopup() {
        refreshNotificationList();

        String recipientId = getNotificationRecipientId();
        int unreadCount = services.notificationService().getUnreadCount(recipientId);
        if (unreadCount > 0) {
            services.notificationService().markAllAsRead(recipientId);
            refreshNotificationBadge();
        }

        Window ownerWindow = view.getScene().getWindow();
        double bellScreenX = bellContainer.localToScreen(bellContainer.getBoundsInLocal()).getMaxX();
        double bellScreenY = bellContainer.localToScreen(bellContainer.getBoundsInLocal()).getMaxY();

        notificationPopup.show(ownerWindow, bellScreenX - 320, bellScreenY + 2);
    }

    private void refreshNotificationBadge() {
        String recipientId = getNotificationRecipientId();
        int unreadCount = services.notificationService().getUnreadCount(recipientId);
        badge.setVisible(unreadCount > 0);
    }

    private String getNotificationRecipientId() {
        if (user.getRole() == Role.TA) {
            return services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        }
        return user.getUserId();
    }

    private void refreshNotificationList() {
        String recipientId = getNotificationRecipientId();
        List<Notification> notifications = services.notificationService().getNotificationsForUser(recipientId);

        VBox body = (VBox) notificationPopup.getContent().get(0).lookup("#notificationBody");
        body.getChildren().clear();

        Label emptyLabel = (Label) notificationPopup.getContent().get(0).lookup("#emptyLabel");
        emptyLabel.setText(I18n.t("no_notifications"));
        emptyLabel.setVisible(notifications.isEmpty());

        if (notifications.isEmpty()) {
            return;
        }

        for (Notification notification : notifications) {
            VBox item = createNotificationItem(notification);
            body.getChildren().add(item);
        }

        double itemHeight = 60;
        double bodyHeight = Math.min(notifications.size(), 5) * itemHeight;
        body.setPrefHeight(bodyHeight);
    }

    private VBox createNotificationItem(Notification notification) {
        VBox container = new VBox();
        container.setSpacing(2);
        container.setPadding(new Insets(10, 14, 10, 14));
        container.setMaxWidth(320);
        container.setPrefWidth(320);

        String bgColor = notification.isRead() ? "#ffffff" : "#f0f9ff";
        container.setStyle("-fx-background-color: " + bgColor + ";");

        Region unreadDot = new Region();
        unreadDot.setPrefSize(7, 7);
        unreadDot.setMaxSize(7, 7);
        unreadDot.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 3.5;");
        unreadDot.setVisible(!notification.isRead());

        Label titleLabel = new Label();
        titleLabel.setText(notification.getTitle());
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(260);

        Label timeLabel = new Label();
        String timeText = notification.getCreatedAt() != null
                ? notification.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
                : "";
        timeLabel.setText(timeText);
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().add(unreadDot);
        HBox titleBox = new HBox(8, titleLabel);
        titleBox.setMaxWidth(260);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        topRow.getChildren().add(titleBox);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().add(spacer);
        topRow.getChildren().add(timeLabel);

        Label messageLabel = new Label();
        messageLabel.setText(notification.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(292);
        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 1;");
        messageLabel.setPadding(new Insets(2, 0, 0, 15));

        container.getChildren().addAll(topRow, messageLabel);
        return container;
    }

    private void navigateTo(String pageId) {
        currentPageId = pageId;
        currentPageIdBackup = pageId;
        navButtons.values().forEach(this::deactivateButton);
        Button active = navButtons.get(pageId);
        if (active != null) {
            if (!active.getStyleClass().contains("shell-nav-item-active")) {
                active.getStyleClass().add("shell-nav-item-active");
            }
        }

        Parent page;
        switch (pageId) {
            case "dashboard" -> {
                breadcrumbCurrent.setText(I18n.t("dashboard"));
                page = new TADashboardController(services, user).getView();
            }
            case "browseJobs" -> {
                breadcrumbCurrent.setText(I18n.t("browse_jobs"));
                page = new JobBrowserController(services, user).getView();
            }
            case "myApplications" -> {
                breadcrumbCurrent.setText(I18n.t("my_applications"));
                page = new MyApplicationsController(services, user, this::openJobBrowserForJob).getView();
            }
            case "myCv" -> {
                breadcrumbCurrent.setText(I18n.t("my_cv"));
                page = new MyCvController(services, user, () -> navigateTo("browseJobs")).getView();
            }
            case "jobManagement" -> {
                breadcrumbCurrent.setText(I18n.t("job_management"));
                page = new JobManagementController(services, user, this::openApplicantListForJob, preferredManagementJobId).getView();
                preferredManagementJobId = null;
            }
            case "moDashboard" -> {
                breadcrumbCurrent.setText(I18n.t("dashboard"));
                page = new MODashboardController(
                        services,
                        user,
                        this::openJobManagementForJob,
                        () -> navigateTo("applicantList"),
                        () -> navigateTo("moProfile")
                ).getView();
            }
            case "applicantList" -> {
                breadcrumbCurrent.setText(I18n.t("applicant_list"));
                page = new ApplicantListController(services, user, preferredApplicantJobId).getView();
            }
            case "adminDashboard" -> {
                breadcrumbCurrent.setText(I18n.t("applications"));
                page = new AdminDashboardController(services, user).getView();
            }
            case "adminJobs" -> {
                breadcrumbCurrent.setText(I18n.t("jobs"));
                page = new AdminJobsController(services, user).getView();
            }
            case "adminApplications" -> {
                breadcrumbCurrent.setText(I18n.t("applications"));
                page = new AdminApplicationsController(services, user).getView();
            }
            case "moProfile" -> {
                breadcrumbCurrent.setText(I18n.t("profile"));
                page = new MOProfileController(services, user, this::openJobManagementForJob).getView();
            }
            case "helpCenter" -> {
                breadcrumbCurrent.setText(I18n.t("help_center"));
                page = new HelpCenterController().getView();
            }
            case "settings" -> {
                breadcrumbCurrent.setText(I18n.t("settings"));
                page = new SettingsController(services, user, getStage()).getView();
            }
            default -> {
                breadcrumbCurrent.setText(I18n.t("dashboard"));
                page = PlaceholderPage.simple(I18n.t("dashboard"), I18n.t("error"));
            }
        }

        contentPane.getChildren().setAll(page);
    }

    private void openApplicantListForJob(Job job) {
        preferredApplicantJobId = job == null ? null : job.getJobId();
        navigateTo("applicantList");
    }

    private void openJobManagementForJob(Job job) {
        preferredManagementJobId = job == null ? null : job.getJobId();
        navigateTo("jobManagement");
    }

    private void openJobBrowserForJob(Job job) {
        preferredApplicantJobId = job == null ? null : job.getJobId();
        navigateTo("browseJobs");
    }

    private void deactivateButton(Button button) {
        button.getStyleClass().remove("shell-nav-item-active");
    }

    private String roleEditionText() {
        if (user.getRole() == Role.TA) {
            return I18n.t("ta_edition");
        }
        if (user.getRole() == Role.MO) {
            return I18n.t("mo_edition");
        }
        return I18n.t("admin_edition");
    }

    private String resolveProfileCardName() {
        if (user.getRole() == Role.TA) {
            ApplicantProfile profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
            if (profile.getFullName() != null && !profile.getFullName().isBlank()) {
                return profile.getFullName().trim();
            }
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName().trim();
        }
        return "User";
    }

    private record NavEntry(String label, String pageId, IconFactory.IconType icon) {
    }
}
