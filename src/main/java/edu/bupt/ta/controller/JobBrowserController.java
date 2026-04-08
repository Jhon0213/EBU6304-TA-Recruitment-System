package edu.bupt.ta.controller;

import edu.bupt.ta.dto.JobSearchCriteria;
import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.ApplicationStatus;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.ValidationResult;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JobBrowserController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private final TextField keywordField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ListView<Job> jobList = new ListView<>();
    private final JobDetailController jobDetailController = new JobDetailController();

    public JobBrowserController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
        loadJobs();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setTop(buildFilters());

        HBox content = new HBox(20);
        content.setPadding(new Insets(20));

        VBox listPanel = new VBox(12);
        listPanel.setPrefWidth(560);
        listPanel.setMinWidth(500);

        Label listTitle = new Label("Open Positions");
        listTitle.getStyleClass().add("section-title");

        jobList.getStyleClass().add("job-browse-list");
        jobList.setCellFactory(param -> new JobCardCell(services, user));
        jobList.setPrefHeight(760);
        VBox.setVgrow(jobList, Priority.ALWAYS);

        listPanel.getChildren().addAll(listTitle, jobList);

        HBox.setHgrow(jobDetailController.getView(), Priority.ALWAYS);
        content.getChildren().addAll(listPanel, jobDetailController.getView());
        view.setCenter(content);

        jobList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            jobDetailController.setJob(newValue);
            updateMatchExplanation(newValue);
            updateApplicationStatus(newValue);
        });

        jobDetailController.setOnApply(statement -> {
            Job selected = jobList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                applyToJob(selected, statement);
            }
        });

        jobDetailController.setOnCancel(jobId -> {
            cancelApplication(jobId);
        });

        if (user.getRole() != Role.TA) {
            jobDetailController.setOnApply(statement -> {
            });
            jobDetailController.setOnCancel(jobId -> {
            });
            jobDetailController.setMatchExplanation(null);
        }
    }

    private void updateApplicationStatus(Job job) {
        if (user.getRole() != Role.TA || job == null) {
            jobDetailController.setHasApplied(false, null);
            jobDetailController.setIsAccepted(false);
            jobDetailController.setJobCancelled(false);
            return;
        }

        // 检查岗位是否已取消（ApplicationStatus.CANCELLED）
        boolean isJobCancelled = false;

        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            jobDetailController.setHasApplied(false, null);
            jobDetailController.setIsAccepted(false);
            jobDetailController.setJobCancelled(false);
            return;
        }

        String applicantId = applicantIdOpt.get();
        Optional<Application> appOpt = services.applicationService().getApplication(applicantId, job.getJobId());

        if (appOpt.isEmpty()) {
            jobDetailController.setHasApplied(false, applicantId);
            jobDetailController.setIsAccepted(false);
            jobDetailController.setJobCancelled(false);
            return;
        }

        Application app = appOpt.get();
        boolean isAccepted = (app.getStatus() == ApplicationStatus.ACCEPTED);
        isJobCancelled = (app.getStatus() == ApplicationStatus.CANCELLED);

        jobDetailController.setHasApplied(true, applicantId);
        jobDetailController.setIsAccepted(isAccepted);
        jobDetailController.setJobCancelled(isJobCancelled);
    }

    private Parent buildFilters() {
        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(24, 20, 14, 20));
        wrapper.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        Label heading = new Label("Browse Opportunities");
        heading.getStyleClass().add("page-title");

        HBox searchRow = new HBox(12);
        keywordField.setPromptText("Search by Keyword (e.g. CS101, Python, Web Dev)");
        keywordField.setPrefHeight(42);

        Button searchButton = new Button("SEARCH");
        searchButton.getStyleClass().add("primary-button");
        searchButton.setOnAction(event -> loadJobs());

        HBox.setHgrow(keywordField, Priority.ALWAYS);
        searchRow.getChildren().addAll(keywordField, searchButton);

        statusFilter.getItems().addAll("ALL", "OPEN", "CLOSED", "EXPIRED", "DRAFT");
        statusFilter.setValue("ALL");

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> moduleCode = new ComboBox<>();
        moduleCode.getItems().addAll("Module Code");
        moduleCode.setValue("Module Code");

        ComboBox<String> jobType = new ComboBox<>();
        jobType.getItems().addAll("Job Type");
        jobType.setValue("Job Type");

        ComboBox<String> weeklyHours = new ComboBox<>();
        weeklyHours.getItems().addAll("Weekly Hours");
        weeklyHours.setValue("Weekly Hours");

        Button clear = new Button("CLEAR FILTERS");
        clear.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: 700;");
        clear.setOnAction(event -> {
            keywordField.clear();
            statusFilter.setValue("ALL");
            loadJobs();
        });

        filters.getChildren().addAll(moduleCode, jobType, weeklyHours, statusFilter, clear);

        wrapper.getChildren().addAll(heading, searchRow, filters);
        return wrapper;
    }

    private void loadJobs() {
        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setKeyword(keywordField.getText());
        if (!"ALL".equals(statusFilter.getValue())) {
            criteria.setStatus(JobStatus.valueOf(statusFilter.getValue()));
        }

        List<Job> jobs = services.jobService().searchJobs(criteria);
        if (user.getRole() == Role.MO) {
            jobs = jobs.stream().filter(job -> user.getUserId().equals(job.getOrganiserId())).toList();
        }

        // 过滤掉 DRAFT 状态的岗位（不再显示岗位未开放）
        jobs = jobs.stream()
                .filter(job -> job.getStatus() != JobStatus.DRAFT)
                .collect(java.util.stream.Collectors.toList());

        // TA：按「已录取 > 已申请 > 申请取消 > 岗位开放 > 关闭 > 过期」排序，同档内按截止日期、岗位 ID
        if (user.getRole() == Role.TA) {
            jobs = sortJobsForTaBrowse(jobs);
        }

        jobList.setItems(FXCollections.observableArrayList(jobs));

        if (!jobs.isEmpty()) {
            jobList.getSelectionModel().selectFirst();
        } else {
            jobDetailController.setJob(null);
            jobDetailController.setMatchExplanation(null);
            jobDetailController.setHasApplied(false, null);
        }

        // 更新当前选中岗位的申请状态
        Job selected = jobList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            updateApplicationStatus(selected);
        }
    }

    /**
     * TA 浏览列表排序（与您要求的顺序一致）：
     * 已录取 &gt; 已申请(审核中/已提交) &gt; 申请已取消 &gt; 仍开放岗位上曾被拒 &gt; 可投递的开放岗 &gt; 已关闭 &gt; 已过期。
     * Closed/expired jobs with rejected applications sort with closed/expired, not among open jobs.
     * 同档内按截止日期、jobId。
     */
    private List<Job> sortJobsForTaBrowse(List<Job> jobs) {
        Map<String, ApplicationStatus> appByJobId = new HashMap<>();
        services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .ifPresent(profile -> {
                    for (Application app : services.applicationService().getApplicationsByApplicant(profile.getApplicantId())) {
                        appByJobId.putIfAbsent(app.getJobId(), app.getStatus());
                    }
                });

        List<Job> out = new ArrayList<>(jobs);
        out.sort(Comparator
                .comparingInt((Job j) -> -taBrowseSortKey(appByJobId.get(j.getJobId()), j.getStatus()))
                .thenComparing(Job::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Job::getJobId));
        return out;
    }

    /** 数值越大越靠前 */
    private static int taBrowseSortKey(ApplicationStatus appStatus, JobStatus jobStatus) {
        if (appStatus == ApplicationStatus.ACCEPTED) {
            return 60;
        }
        if (appStatus == ApplicationStatus.UNDER_REVIEW || appStatus == ApplicationStatus.SUBMITTED) {
            return 50;
        }
        if (appStatus == ApplicationStatus.CANCELLED) {
            return 40;
        }
        // 仅在「岗位仍开放」时，被拒单独排在取消之后、其余开放岗之前
        if (appStatus == ApplicationStatus.REJECTED && jobStatus == JobStatus.OPEN) {
            return 35;
        }
        if (jobStatus == JobStatus.OPEN) {
            return 30;
        }
        if (jobStatus == JobStatus.CLOSED) {
            return 20;
        }
        if (jobStatus == JobStatus.EXPIRED) {
            return 10;
        }
        return 0;
    }

    private void applyToJob(Job job, String statement) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            DialogControllerFactory.permissionDenied(
                    "Profile not found for current TA account. Please complete your profile first.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        ValidationResult result = services.applicationService().apply(applicantIdOpt.get(), job.getJobId(), statement);
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Apply Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success("Apply Success", "Application submitted successfully.",
                view.getScene() == null ? null : view.getScene().getWindow());
        loadJobs();
    }

    private void cancelApplication(String jobId) {
        Optional<String> applicantIdOpt = services.applicantProfileRepository()
                .findByUserId(user.getUserId())
                .map(profile -> profile.getApplicantId());

        if (applicantIdOpt.isEmpty()) {
            return;
        }

        ValidationResult result = services.applicationService().cancelApplication(applicantIdOpt.get(), jobId);
        if (!result.isValid()) {
            DialogControllerFactory.operationFailed("Cancel Failed", String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success("Cancel Success", "Application cancelled successfully.",
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

    private static class JobCardCell extends ListCell<Job> {
        private final ServiceRegistry services;
        private final User user;

        JobCardCell(ServiceRegistry services, User user) {
            this.services = services;
            this.user = user;
        }

        @Override
        protected void updateItem(Job item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox card = new VBox(6);
            card.setPadding(new Insets(14, 16, 14, 14));
            card.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");

            BorderPane header = new BorderPane();
            Label title = new Label(item.getTitle());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
            title.setWrapText(true);
            title.setMaxWidth(Double.MAX_VALUE);
            BorderPane.setAlignment(title, Pos.CENTER_LEFT);

            Label status = new Label(cardStatusText(item));
            status.setStyle(cardStatusStyle(item));
            status.setMinWidth(Label.USE_PREF_SIZE);
            BorderPane.setAlignment(status, Pos.CENTER_RIGHT);
            header.setCenter(title);
            header.setRight(status);

            Label module = new Label(item.getModuleCode() + " | " + item.getModuleName());
            module.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #00a58a;");

            Label meta = new Label(item.getWeeklyHours() + "h/week   |   Deadline: " + item.getDeadline());
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            card.getChildren().addAll(header, module, meta);
            setGraphic(card);
        }

        private String cardStatusText(Job job) {
            if (user.getRole() == Role.TA) {
                Optional<ApplicationStatus> appOpt = services.applicantProfileRepository()
                        .findByUserId(user.getUserId())
                        .flatMap(p -> services.applicationService().getApplication(p.getApplicantId(), job.getJobId()))
                        .map(Application::getStatus);
                return taBrowseCardStatusLabel(appOpt.orElse(null), job.getStatus());
            }
            return moJobStatusLabel(job.getStatus());
        }

        private String cardStatusStyle(Job job) {
            if (user.getRole() == Role.TA) {
                Optional<ApplicationStatus> appOpt = services.applicantProfileRepository()
                        .findByUserId(user.getUserId())
                        .flatMap(p -> services.applicationService().getApplication(p.getApplicantId(), job.getJobId()))
                        .map(Application::getStatus);
                return taBrowseCardStatusStyle(appOpt.orElse(null), job.getStatus());
            }
            return moJobStatusStyle(job.getStatus());
        }
    }

    private static String taBrowseCardStatusLabel(ApplicationStatus app, JobStatus job) {
        if (app == ApplicationStatus.ACCEPTED) {
            return "Accepted";
        }
        if (app == ApplicationStatus.UNDER_REVIEW || app == ApplicationStatus.SUBMITTED) {
            return "Applied";
        }
        if (app == ApplicationStatus.CANCELLED) {
            return "Position Cancelled";
        }
        if (app == ApplicationStatus.REJECTED) {
            return "Rejected";
        }
        if (job == JobStatus.OPEN) {
            return "Open";
        }
        if (job == JobStatus.CLOSED) {
            return "Closed";
        }
        if (job == JobStatus.EXPIRED) {
            return "Expired";
        }
        return "Not Open";
    }

    private static String taBrowseCardStatusStyle(ApplicationStatus app, JobStatus job) {
        String base = "-fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 2 8 2 8;";
        if (app == ApplicationStatus.ACCEPTED) {
            return base + "-fx-text-fill: #047857; -fx-background-color: #ecfdf5;";
        }
        if (app == ApplicationStatus.UNDER_REVIEW || app == ApplicationStatus.SUBMITTED) {
            return base + "-fx-text-fill: #1d4ed8; -fx-background-color: #eff6ff;";
        }
        if (app == ApplicationStatus.CANCELLED) {
            return base + "-fx-text-fill: #64748b; -fx-background-color: #f1f5f9;";
        }
        if (app == ApplicationStatus.REJECTED) {
            return base + "-fx-text-fill: #b91c1c; -fx-background-color: #fef2f2;";
        }
        if (job == JobStatus.OPEN) {
            return base + "-fx-text-fill: #047857; -fx-background-color: #ecfdf5;";
        }
        if (job == JobStatus.CLOSED) {
            return base + "-fx-text-fill: #475569; -fx-background-color: #f1f5f9;";
        }
        if (job == JobStatus.EXPIRED) {
            return base + "-fx-text-fill: #b45309; -fx-background-color: #fffbeb;";
        }
        return base + "-fx-text-fill: #0f766e; -fx-background-color: #ccfbf1;";
    }

    private static String moJobStatusLabel(JobStatus job) {
        return switch (job) {
            case OPEN -> "Open";
            case CLOSED -> "Closed";
            case EXPIRED -> "Expired";
            case DRAFT -> "Draft";
        };
    }

    private static String moJobStatusStyle(JobStatus job) {
        String base = "-fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 999; -fx-padding: 2 8 2 8;";
        return switch (job) {
            case OPEN -> base + "-fx-text-fill: #047857; -fx-background-color: #ecfdf5;";
            case CLOSED -> base + "-fx-text-fill: #475569; -fx-background-color: #f1f5f9;";
            case EXPIRED -> base + "-fx-text-fill: #b45309; -fx-background-color: #fffbeb;";
            case DRAFT -> base + "-fx-text-fill: #0f766e; -fx-background-color: #ccfbf1;";
        };
    }
}
