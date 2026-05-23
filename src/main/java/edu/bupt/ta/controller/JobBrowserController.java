package edu.bupt.ta.controller;

import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.JobType;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.ValidationResult;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class JobBrowserController {
    private static final DateTimeFormatter CARD_DEADLINE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final double OPEN_POSITIONS_PANEL_WIDTH = 392;


    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final TextField keywordField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> moduleCodeFilter = new ComboBox<>();
    private final ComboBox<String> jobTypeFilter = new ComboBox<>();
    private final ComboBox<String> skillsFilter = new ComboBox<>();
    private final ComboBox<String> deadlineFilter = new ComboBox<>();
    private final ListView<JobWithApplication> jobList = new ListView<>();
    private final JobDetailController jobDetailController;

    public JobBrowserController(ServiceRegistry services, User user) {
        I18n.initTranslations();
        this.services = services;
        this.user = user;
        this.jobDetailController = new JobDetailController(services);
        initialize();
        loadJobs();

        I18n.setOnLanguageChange(lang -> {
            jobDetailController.refreshI18n();
        });
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setTop(buildFilters());

        HBox content = new HBox(0);
        content.setPadding(Insets.EMPTY);

        VBox listPanel = new VBox(16);
        listPanel.setPrefWidth(OPEN_POSITIONS_PANEL_WIDTH);
        listPanel.setMinWidth(OPEN_POSITIONS_PANEL_WIDTH);
        listPanel.setMaxWidth(OPEN_POSITIONS_PANEL_WIDTH);
        listPanel.setPadding(new Insets(20, 16, 20, 20));
        listPanel.getStyleClass().add("open-positions-panel");

        Label listTitle = new Label(I18n.t("browse_opportunities"));
        listTitle.getStyleClass().add("section-title");

        jobList.getStyleClass().add("job-list");
        jobList.setCellFactory(param -> new JobCardCell(this));
        jobList.setPrefHeight(760);
        VBox.setVgrow(jobList, Priority.ALWAYS);

        listPanel.getChildren().addAll(listTitle, jobList);

        HBox.setHgrow(jobDetailController.getView(), Priority.ALWAYS);
        content.getChildren().addAll(listPanel, jobDetailController.getView());
        view.setCenter(content);

        jobList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                String applicantId = getCurrentApplicantId();
                jobDetailController.setJobWithApplicationStatus(newValue.job, applicantId, newValue.appStatus);
                updateMatchExplanation(newValue.job);
            } else {
                jobDetailController.setJob(null);
                updateMatchExplanation(null);
            }
        });

        jobDetailController.setOnFavouriteToggle(jobId -> {
            toggleFavourite(jobId);
            jobDetailController.updateHeartIconDirectly(jobId, services.favouriteJobRepository()
                    .findByApplicantId(getCurrentApplicantId())
                    .map(f -> f.isFavourite(jobId)).orElse(false));
        });

        jobDetailController.setOnApply(() -> {
            JobWithApplication selected = jobList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            Optional<String> applicantIdOpt = services.applicantProfileRepository()
                    .findByUserId(user.getUserId())
                    .map(profile -> profile.getApplicantId());
            if (applicantIdOpt.isEmpty()) {
                DialogControllerFactory.permissionDenied(
                        I18n.t("profile_not_found"),
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Optional<ApplicantProfile> profileOpt = services.applicantProfileRepository()
                    .findById(applicantIdOpt.get());
            if (profileOpt.isEmpty()) {
                DialogControllerFactory.permissionDenied(
                        I18n.t("profile_not_found"),
                        view.getScene() == null ? null : view.getScene().getWindow());
                return;
            }
            Optional<String> statementOpt = JobApplyDialog.showAndWait(
                    selected.job,
                    profileOpt.get(),
                    services.resumeService(),
                    applicantIdOpt.get(),
                    view.getScene() == null ? null : view.getScene().getWindow());
            statementOpt.ifPresent(statement -> applyToJob(selected.job, statement));
        });

        jobDetailController.setOnCancel(applicantId -> {
            JobWithApplication selected = jobList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                cancelApplication(selected.job);
            }
        });

        if (user.getRole() != Role.TA) {
            jobDetailController.setOnApply(() -> {
            });
            jobDetailController.setOnCancel(applicantId -> {
            });
            jobDetailController.setMatchExplanation(null);
        }
    }

    private String getCurrentApplicantId() {
        if (user.getRole() != Role.TA) return null;

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        return applicantIdOpt.orElse(null);
    }

    private String getJobStatusDisplayText(JobStatus status) {
        return switch (status) {
            case OPEN -> I18n.t("open_status");
            case CLOSED -> I18n.t("closed_status");
            case EXPIRED -> I18n.t("expired_status");
            case DRAFT -> I18n.t("draft_status", "Draft");
        };
    }

    private Parent buildFilters() {
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("job-browser-top");

        VBox headerSection = new VBox(14);
        headerSection.getStyleClass().add("job-browser-header");

        Label heading = new Label(I18n.t("browse_opportunities"));
        heading.getStyleClass().add("job-browser-title");

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        HBox keywordShell = new HBox(12);
        keywordShell.setAlignment(Pos.CENTER_LEFT);
        keywordShell.getStyleClass().add("job-browser-search-shell");

        keywordField.setPromptText(I18n.t("search_keyword"));
        keywordField.getStyleClass().add("job-browser-keyword-field");
        keywordField.setOnAction(event -> loadJobs());
        HBox.setHgrow(keywordField, Priority.ALWAYS);
        keywordShell.getChildren().addAll(
                IconFactory.glyph(IconFactory.IconType.SEARCH, 18, Color.web("#94a3b8")),
                keywordField);

        Button searchButton = new Button(I18n.t("search_btn"));
        searchButton.getStyleClass().add("job-browser-search-button");
        searchButton.setOnAction(event -> loadJobs());

        HBox.setHgrow(keywordShell, Priority.ALWAYS);
        searchRow.getChildren().addAll(keywordShell, searchButton);

        // --- Status Filter ---
        statusFilter.getItems().setAll(
                I18n.t("status_filter"),
                I18n.t("open_status"),
                I18n.t("closed_status"),
                I18n.t("expired_status"),
                I18n.t("accepted_status"));
        statusFilter.setValue(I18n.t("status_filter"));
        statusFilter.getStyleClass().add("job-browser-filter-pill");
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Job Type Filter ---
        jobTypeFilter.getItems().add(I18n.t("job_type_filter"));
        for (JobType type : JobType.values()) {
            jobTypeFilter.getItems().add(type.name());
        }
        jobTypeFilter.setValue(I18n.t("job_type_filter"));
        jobTypeFilter.getStyleClass().add("job-browser-filter-pill");
        jobTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Module Code Filter (dynamic) ---
        moduleCodeFilter.getItems().add(I18n.t("module_code_filter"));
        services.jobService().searchJobs(new JobSearchCriteria()).stream()
                .map(Job::getModuleCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .sorted()
                .forEach(code -> moduleCodeFilter.getItems().add(code));
        moduleCodeFilter.setValue(I18n.t("module_code_filter"));
        moduleCodeFilter.getStyleClass().add("job-browser-filter-pill");
        moduleCodeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Skills Filter (dynamic) ---
        skillsFilter.getItems().add(I18n.t("skills_filter"));
        services.jobService().searchJobs(new JobSearchCriteria()).stream()
                .flatMap(job -> {
                    List<String> combined = new ArrayList<>();
                    if (job.getRequiredSkills() != null) combined.addAll(job.getRequiredSkills());
                    if (job.getPreferredSkills() != null) combined.addAll(job.getPreferredSkills());
                    return combined.stream();
                })
                .filter(skill -> skill != null && !skill.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .forEach(skill -> skillsFilter.getItems().add(skill));
        skillsFilter.setValue(I18n.t("skills_filter"));
        skillsFilter.getStyleClass().add("job-browser-filter-pill");
        skillsFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- Deadline Filter ---
        deadlineFilter.getItems().setAll(
                I18n.t("deadline_filter"),
                I18n.t("within_7_days"),
                I18n.t("within_30_days"),
                I18n.t("within_90_days"));
        deadlineFilter.setValue(I18n.t("deadline_filter"));
        deadlineFilter.getStyleClass().add("job-browser-filter-pill");
        deadlineFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) loadJobs();
        });

        // --- CLEAR FILTERS ---
        Button clear = new Button(I18n.t("clear_filters"));
        clear.getStyleClass().add("job-browser-clear-filters");
        clear.setGraphic(IconFactory.glyph(IconFactory.IconType.FILTER, 11, Color.web("#ef4444")));
        clear.setContentDisplay(ContentDisplay.LEFT);
        clear.setGraphicTextGap(6);
        clear.setOnAction(event -> clearFilters());

        Region divider = new Region();
        divider.getStyleClass().add("job-browser-filter-divider");
        divider.setMinSize(1, 20);
        divider.setPrefSize(1, 20);
        divider.setMaxSize(1, 20);

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("job-browser-filter-row");
        filters.getChildren().addAll(
                moduleCodeFilter, jobTypeFilter,
                skillsFilter, deadlineFilter, statusFilter, divider, clear);

        headerSection.getChildren().addAll(heading, searchRow);
        wrapper.getChildren().addAll(headerSection, filters);
        return wrapper;
    }

    private void loadJobs() {
        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setKeyword(keywordField.getText());

        // --- Module Code ---
        String selectedModuleCode = moduleCodeFilter.getValue();
        if (selectedModuleCode != null && !I18n.t("module_code_filter").equals(selectedModuleCode)) {
            criteria.setModuleCode(selectedModuleCode);
        }

        // --- Job Type ---
        String selectedJobType = jobTypeFilter.getValue();
        if (selectedJobType != null && !I18n.t("job_type_filter").equals(selectedJobType)) {
            criteria.setType(JobType.valueOf(selectedJobType));
        }

        // --- Skills ---
        String selectedSkill = skillsFilter.getValue();
        if (selectedSkill != null && !I18n.t("skills_filter").equals(selectedSkill)) {
            criteria.setRequiredSkill(selectedSkill);
        }

        // --- Status ---
        String selectedStatus = statusFilter.getValue();
        if (selectedStatus != null && !I18n.t("status_filter").equals(selectedStatus)) {
            if (I18n.t("accepted_status").equals(selectedStatus)) {
                // ACCEPTED is application status, handled in post-filtering
            } else if (I18n.t("open_status").equals(selectedStatus)) {
                criteria.setStatus(JobStatus.OPEN);
            } else if (I18n.t("closed_status").equals(selectedStatus)) {
                criteria.setStatus(JobStatus.CLOSED);
            } else if (I18n.t("expired_status").equals(selectedStatus)) {
                criteria.setStatus(JobStatus.EXPIRED);
            }
        }

        // Base search
        List<Job> jobs = services.jobService().searchJobs(criteria);

        // MO role only shows their own posted jobs
        if (user.getRole() == Role.MO) {
            jobs = jobs.stream().filter(job -> user.getUserId().equals(job.getOrganiserId())).toList();
        }

        // Always filter out DRAFT status
        jobs = jobs.stream()
                .filter(job -> job.getStatus() != JobStatus.DRAFT)
                .collect(java.util.stream.Collectors.toList());

        // --- Deadline post-filtering ---
        String selectedDeadline = deadlineFilter.getValue();
        if (selectedDeadline != null && !I18n.t("deadline_filter").equals(selectedDeadline)) {
            LocalDateTime now = LocalDateTime.now();
            jobs = jobs.stream().filter(job -> {
                LocalDateTime dl = job.getDeadline();
                if (dl == null) return false;
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(now, dl);
                if (selectedDeadline.equals(I18n.t("within_7_days"))) {
                    return daysUntil >= 0 && daysUntil <= 7;
                } else if (selectedDeadline.equals(I18n.t("within_30_days"))) {
                    return daysUntil >= 0 && daysUntil <= 30;
                } else if (selectedDeadline.equals(I18n.t("within_90_days"))) {
                    return daysUntil >= 0 && daysUntil <= 90;
                }
                return true;
            }).collect(java.util.stream.Collectors.toList());
        }

        // --- ACCEPTED post-filtering: only keep jobs where current TA's application status is ACCEPTED ---
        if (I18n.t("accepted_status").equals(selectedStatus)) {
            jobs = filterJobsByAcceptedApplication(jobs);
        }

        // Get application status and favourite status
        List<JobWithApplication> jobsWithApp = new ArrayList<>();
        for (Job job : jobs) {
            ApplicationStatus appStatus = getApplicationStatusForJob(job);
            boolean favourite = isJobFavourite(job.getJobId());
            jobsWithApp.add(new JobWithApplication(job, appStatus, favourite));
        }

        // Sort by priority: Favourite > Accepted > Apply > Rejected > Expired > Closed
        jobsWithApp.sort(
                Comparator.comparingInt((JobWithApplication jwa) -> getSortOrder(jwa.job(), jwa.appStatus(), jwa.favourite()))
                        .thenComparing(jwa -> jwa.job().getTitle(), String.CASE_INSENSITIVE_ORDER));

        jobList.setItems(FXCollections.observableArrayList(jobsWithApp));

        if (!jobs.isEmpty()) {
            Platform.runLater(() -> {
                jobList.getSelectionModel().selectFirst();
                JobWithApplication first = jobList.getSelectionModel().getSelectedItem();
                if (first != null) {
                    jobDetailController.setJobWithApplicationStatus(first.job, getCurrentApplicantId(), first.appStatus);
                    updateMatchExplanation(first.job);
                }
            });
        } else {
            jobDetailController.setJob(null);
            jobDetailController.setMatchExplanation(null);
        }
    }

    private List<Job> filterJobsByAcceptedApplication(List<Job> jobs) {
        if (user.getRole() != Role.TA) return new ArrayList<>();
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());
        if (applicantIdOpt.isEmpty()) return new ArrayList<>();
        String applicantId = applicantIdOpt.get();
        return jobs.stream()
                .filter(job -> services.applicationService()
                        .getApplicationStatus(applicantId, job.getJobId())
                        .map(s -> s == ApplicationStatus.ACCEPTED)
                        .orElse(false))
                .collect(java.util.stream.Collectors.toList());
    }

    private void clearFilters() {
        keywordField.clear();
        moduleCodeFilter.setValue(I18n.t("module_code_filter"));
        jobTypeFilter.setValue(I18n.t("job_type_filter"));
        skillsFilter.setValue(I18n.t("skills_filter"));
        deadlineFilter.setValue(I18n.t("deadline_filter"));
        statusFilter.setValue(I18n.t("status_filter"));
        loadJobs();
    }

    private record JobWithApplication(Job job, ApplicationStatus appStatus, boolean favourite) {}

    private ApplicationStatus getApplicationStatusForJob(Job job) {
        if (user.getRole() != Role.TA) return null;

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) return null;

        return services.applicationService().getApplicationStatus(applicantIdOpt.get(), job.getJobId())
                .orElse(null);
    }

    private boolean isJobFavourite(String jobId) {
        if (user.getRole() != Role.TA) return false;

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) return false;

        return services.favouriteJobRepository()
                .findByApplicantId(applicantIdOpt.get())
                .map(f -> f.isFavourite(jobId))
                .orElse(false);
    }

    private void toggleFavourite(String jobId) {
        if (user.getRole() != Role.TA) return;

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) return;

        String applicantId = applicantIdOpt.get();
        var favouriteOpt = services.favouriteJobRepository().findByApplicantId(applicantId);

        var favourite = favouriteOpt.orElseGet(() -> new edu.bupt.ta.model.FavouriteJob(applicantId, new java.util.ArrayList<>()));
        favourite.toggleFavourite(jobId);
        services.favouriteJobRepository().saveForApplicant(favourite);

        loadJobs();
    }

    private int getSortOrder(Job job, ApplicationStatus appStatus, boolean favourite) {
        if (favourite) return 0;
        if (appStatus == ApplicationStatus.ACCEPTED) {
            return 1;
        }
        if (appStatus == ApplicationStatus.REJECTED) {
            return 3;
        }
        if (appStatus == ApplicationStatus.SUBMITTED
                || appStatus == ApplicationStatus.UNDER_REVIEW
                || appStatus == ApplicationStatus.CANCELLED) {
            return 2;
        }
        // No application record: sort by job status
        return switch (job.getStatus()) {
            case OPEN -> 2;
            case EXPIRED -> 4;
            case CLOSED -> 5;
            case DRAFT -> 6;
        };
    }

    private void applyToJob(Job job, String statement) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            DialogControllerFactory.permissionDenied(
                    I18n.t("profile_not_found"),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        ValidationResult result = services.applicationService().apply(applicantIdOpt.get(), job.getJobId(), statement);
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(I18n.t("apply_fail_title"), String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success(I18n.t("apply_success"), I18n.t("apply_success_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        loadJobs();
    }

    private void cancelApplication(Job job) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            return;
        }

        ValidationResult result = services.applicationService().cancelApplication(applicantIdOpt.get(), job.getJobId());
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed(I18n.t("cancel_failed"), String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success(I18n.t("cancel_success"), I18n.t("cancel_success_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
        loadJobs();
    }

    private void updateMatchExplanation(Job selectedJob) {
        if (selectedJob == null || user.getRole() != Role.TA) {
            jobDetailController.setMatchExplanation(null);
            return;
        }

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());
        if (applicantIdOpt.isEmpty()) {
            jobDetailController.setMatchExplanation(null);
            return;
        }

        MatchExplanationDTO explanation = services.matchingService().evaluateMatch(applicantIdOpt.get(), selectedJob.getJobId());
        jobDetailController.setMatchExplanation(explanation);
    }

    private static class JobCardCell extends ListCell<JobWithApplication> {
        private static final String TITLE_SELECTED_STYLE = "-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a;";
        private static final String TITLE_DEFAULT_STYLE = "-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0f172a;";
        private static final String MODULE_SELECTED_STYLE = "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #00c29f;";
        private static final String MODULE_DEFAULT_STYLE = "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #64748b;";
        private static final String FOOTER_TEXT_STYLE = "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;";

        private final VBox card = new VBox(0);
        private final HBox heading = new HBox(10);
        private final Label title = new Label();
        private final Label status = new Label();
        private final StackPane favouriteBtn = new StackPane();
        private final Label module = new Label();
        private final HBox chipRow = new HBox(8);
        private final Region footerDivider = new Region();
        private final HBox footerRow = new HBox(8);
        private final HBox postsGroup = new HBox(8);
        private final HBox deadlineGroup = new HBox(8);
        private final Label posts = new Label();
        private final Label deadline = new Label();
        private final Region footerSpacer = new Region();

        private String fullTitle = "";
        private String statusText = "";
        private boolean isFavourite = false;
        private final JobBrowserController controller;

        private JobCardCell(JobBrowserController controller) {
            this.controller = controller;
            setText(null);
            setPrefWidth(0);
            setMinWidth(0);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setStyle("-fx-background-color: transparent; -fx-padding: 0 0 16 0; -fx-background-insets: 0; -fx-border-color: transparent; -fx-border-width: 0;");

            card.getStyleClass().add("open-position-card");
            card.setPadding(new Insets(14, 18, 14, 18));
            card.setMinWidth(0);
            card.prefWidthProperty().bind(Bindings.max(0, widthProperty().subtract(14)));
            card.minWidthProperty().bind(card.prefWidthProperty());
            card.maxWidthProperty().bind(card.prefWidthProperty());

            title.setWrapText(true);
            title.setTextOverrun(OverrunStyle.CLIP);
            title.setEllipsisString("");
            title.setMinWidth(0);
            title.setMaxWidth(Double.MAX_VALUE);
            title.setMaxHeight(Double.MAX_VALUE);

            status.setVisible(true);
            status.setManaged(true);
            status.setMinWidth(Region.USE_PREF_SIZE);
            status.setMaxWidth(Region.USE_PREF_SIZE);

            FontIcon starEmpty = FontIcon.of(FontAwesomeSolid.STAR, 14);
            starEmpty.setIconColor(Color.web("#94a3b8"));
            FontIcon starFilled = FontIcon.of(FontAwesomeSolid.STAR, 14);
            starFilled.setIconColor(Color.web("#facc15"));
            starFilled.setVisible(false);
            starEmpty.setVisible(true);
            favouriteBtn.getChildren().setAll(starEmpty, starFilled);
            favouriteBtn.setCursor(javafx.scene.Cursor.HAND);
            favouriteBtn.setOnMouseClicked(event -> {
                if (getItem() != null) {
                    controller.toggleFavourite(getItem().job().getJobId());
                }
            });

            Region headingSpacer = new Region();
            HBox.setHgrow(headingSpacer, Priority.ALWAYS);

            heading.setMaxWidth(Double.MAX_VALUE);
            heading.setAlignment(Pos.TOP_LEFT);
            heading.getChildren().setAll(title, headingSpacer, status, favouriteBtn);

            module.setWrapText(true);
            module.setTextOverrun(OverrunStyle.CLIP);
            module.setEllipsisString("");
            module.setMinWidth(0);
            module.setMaxWidth(Double.MAX_VALUE);
            module.setMaxHeight(Double.MAX_VALUE);
            VBox.setMargin(module, new Insets(4, 0, 0, 0));

            chipRow.setAlignment(Pos.CENTER_LEFT);
            chipRow.setPadding(new Insets(6, 0, 0, 0));

            footerDivider.setPrefHeight(1);
            footerDivider.setMinHeight(1);
            footerDivider.setMaxHeight(1);
            footerDivider.setStyle("-fx-background-color: #f8fafc;");
            footerDivider.setVisible(true);
            footerDivider.setManaged(true);

            posts.setStyle(FOOTER_TEXT_STYLE);
            deadline.setStyle(FOOTER_TEXT_STYLE);

            postsGroup.setAlignment(Pos.CENTER_LEFT);
            deadlineGroup.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(footerSpacer, Priority.ALWAYS);

            footerRow.setPadding(new Insets(8, 0, 0, 0));
            footerRow.setAlignment(Pos.CENTER_LEFT);
            footerRow.getChildren().addAll(postsGroup, footerSpacer, deadlineGroup);

            card.getChildren().addAll(heading, module, chipRow, footerDivider, footerRow);
        }

        @Override
        protected void updateItem(JobWithApplication item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                fullTitle = "";
                statusText = "";
                return;
            }

            Job job = item.job();
            ApplicationStatus appStatus = item.appStatus();
            isFavourite = item.favourite();

            statusText = getDynamicStatusText(job, appStatus);
            status.setText(statusText);
            status.setStyle(getDynamicStatusStyle(statusText));
            status.setVisible(true);
            status.setManaged(true);

            FontIcon starEmpty = (FontIcon) favouriteBtn.getChildren().get(0);
            FontIcon starFilled = (FontIcon) favouriteBtn.getChildren().get(1);
            starEmpty.setVisible(!isFavourite);
            starFilled.setVisible(isFavourite);

            fullTitle = job.getTitle() == null ? "-" : job.getTitle();
            title.setText(fullTitle);

            boolean selected = isSelected();
            title.setStyle(selected ? TITLE_SELECTED_STYLE : TITLE_DEFAULT_STYLE);
            module.setStyle(selected ? MODULE_SELECTED_STYLE : MODULE_DEFAULT_STYLE);
            module.setText(moduleText(job));

            refreshSkillChips(job, selected);
            refreshFooter(job, selected);
            applyCardSelection(selected);

            setGraphic(card);
            setText(null);
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (getItem() == null) {
                return;
            }
            status.setVisible(true);
            status.setManaged(true);
            title.setStyle(selected ? TITLE_SELECTED_STYLE : TITLE_DEFAULT_STYLE);
            module.setStyle(selected ? MODULE_SELECTED_STYLE : MODULE_DEFAULT_STYLE);
            module.setText(moduleText(getItem().job()));
            refreshSkillChips(getItem().job(), selected);
            refreshFooter(getItem().job(), selected);
            applyCardSelection(selected);
        }

        private void applyCardSelection(boolean selected) {
            card.getStyleClass().setAll("open-position-card");
            if (selected) {
                card.getStyleClass().add("open-position-card-selected");
                card.setPadding(new Insets(13, 17, 13, 17));
            } else {
                card.setPadding(new Insets(14, 18, 14, 18));
            }
        }

        private void refreshSkillChips(Job job, boolean selected) {
            chipRow.getChildren().clear();
            List<String> skillTags = collectSkillTags(job);
            if (skillTags.isEmpty()) {
                chipRow.setVisible(false);
                chipRow.setManaged(false);
                return;
            }
            chipRow.setVisible(true);
            chipRow.setManaged(true);
            for (String skill : skillTags) {
                Label chip = new Label(skill.toUpperCase(Locale.ENGLISH));
                chip.setStyle(selected
                        ? "-fx-background-color: #f1f5f9; -fx-border-color: #f1f5f9; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5 11 5 11; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #475569;"
                        : "-fx-background-color: #f8fafc; -fx-border-color: #f1f5f9; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5 11 5 11; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
                chipRow.getChildren().add(chip);
            }
        }

        private void refreshFooter(Job job, boolean selected) {
            int positions = job.getPositions();
            String postsText = positions == 1
                    ? I18n.t("post_single")
                    : I18n.t("posts_plural").replace("{n}", String.valueOf(positions));
            posts.setText(postsText);
            deadline.setText(formatCardDeadline(job.getDeadline()));

            Color postsIconColor = selected ? Color.web("#00c29f") : Color.web("#94a3b8");
            Color dateIconColor = Color.web("#94a3b8");

            postsGroup.getChildren().setAll(
                    IconFactory.glyph(IconFactory.IconType.USERS, 13, postsIconColor),
                    posts
            );
            deadlineGroup.getChildren().setAll(
                    IconFactory.glyph(IconFactory.IconType.CALENDAR, 13, dateIconColor),
                    deadline
            );
        }

        private static String moduleText(Job job) {
            return fallback(job.getModuleCode()) + " • " + fallback(job.getModuleName());
        }

        private static String fallback(String value) {
            return value == null || value.isBlank() ? "-" : value;
        }

        private static List<String> collectSkillTags(Job job) {
            Set<String> skills = new LinkedHashSet<>();
            if (job.getRequiredSkills() != null) {
                for (String skill : job.getRequiredSkills()) {
                    if (skill != null && !skill.isBlank()) {
                        skills.add(skill.trim());
                    }
                }
            }
            if (job.getPreferredSkills() != null) {
                for (String skill : job.getPreferredSkills()) {
                    if (skill != null && !skill.isBlank()) {
                        skills.add(skill.trim());
                    }
                }
            }
            return skills.stream().limit(2).toList();
        }

        private String getDynamicStatusText(Job job, ApplicationStatus appStatus) {
            if (appStatus != null) {
                return switch (appStatus) {
                    case ACCEPTED -> I18n.t("accepted_status");
                    case SUBMITTED -> I18n.t("applied_status");
                    case CANCELLED -> I18n.t("cancelled_status");
                    case UNDER_REVIEW -> I18n.t("under_review_status");
                    case REJECTED -> I18n.t("rejected_status");
                };
            }
            // No application: show job status
            return switch (job.getStatus()) {
                case OPEN -> I18n.t("open_status");
                case CLOSED -> I18n.t("closed_status");
                case EXPIRED -> I18n.t("expired_status");
                case DRAFT -> I18n.t("draft_status", "Draft");
            };
        }

        private String getDynamicStatusStyle(String statusText) {
            String color;
            if (statusText.equals(I18n.t("accepted_status"))) {
                color = "#047857;#ecfdf5";
            } else if (statusText.equals(I18n.t("applied_status"))) {
                color = "#1d4ed8;#eff6ff";
            } else if (statusText.equals(I18n.t("under_review_status"))) {
                color = "#1d4ed8;#eff6ff";
            } else if (statusText.equals(I18n.t("rejected_status"))) {
                color = "#b91c1c;#fef2f2";
            } else if (statusText.equals(I18n.t("cancelled_status"))) {
                color = "#b45309;#fffbeb";
            } else if (statusText.equals(I18n.t("open_status"))) {
                color = "#ffffff;#00c29f";
            } else if (statusText.equals(I18n.t("closed_status"))) {
                color = "#64748b;#f1f5f9";
            } else if (statusText.equals(I18n.t("expired_status"))) {
                color = "#64748b;#f1f5f9";
            } else {
                color = "#64748b;#f1f5f9";
            }
            String[] parts = color.split(";");
            return String.format("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: %s; -fx-background-color: %s; -fx-background-radius: 4; -fx-padding: 2 8 2 8; -fx-letter-spacing: 0.4px;", parts[0], parts[1]);
        }
    }

    private static String formatCardDeadline(LocalDateTime deadline) {
        return deadline == null ? "-" : deadline.format(CARD_DEADLINE_FORMAT);
    }
}
