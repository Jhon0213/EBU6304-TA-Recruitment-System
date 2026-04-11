package edu.bupt.ta.controller;

import edu.bupt.ta.model.Application;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ApplicantListController {

    private static final String ALL_JOBS_JOB_ID = "__ALL_JOBS__";

    private final ServiceRegistry services;
    private final User user;
    private final String preferredJobId;
    private final BorderPane view = new BorderPane();

    private final ComboBox<Job> jobSelector = new ComboBox<>();
    private final ListView<Row> listView = new ListView<>();

    private final Button allTab = new Button();
    private final Button underReviewTab = new Button();
    private final Button rejectedTab = new Button();
    private final Button acceptedTab = new Button();

    private final List<Row> allRows = new ArrayList<>();
    private StatusFilter statusFilter = StatusFilter.ALL;

    private final Job allJobsOption = buildAllJobsOption();

    private final Label detailSkills = new Label("-");
    private final Label detailAcademic = new Label("-");
    private Row selectedRow;

    private enum StatusFilter {
        ALL,
        UNDER_REVIEW,
        REJECTED,
        ACCEPTED
    }

    public ApplicantListController(ServiceRegistry services, User user) {
        this(services, user, null);
    }

    public ApplicantListController(ServiceRegistry services, User user, String preferredJobId) {
        this.services = services;
        this.user = user;
        this.preferredJobId = preferredJobId;
        initialize();
        refreshJobs();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");

        VBox page = new VBox(16);
        page.setPadding(new Insets(24));

        page.getChildren().add(buildHeader());
        page.getChildren().add(buildBody());

        view.setCenter(page);
    }

    private HBox buildHeader() {
        Label title = new Label("Applicant List");
        title.getStyleClass().add("page-title");

        jobSelector.setPrefWidth(320);
        jobSelector.setPromptText("Select your job");
        jobSelector.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        jobSelector.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        jobSelector.valueProperty().addListener((obs, oldV, newV) -> refreshApplications());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button filter = new Button("Filter");
        filter.getStyleClass().add("secondary-button");

        Button export = new Button("Export Report");
        export.getStyleClass().add("secondary-button");

        HBox row = new HBox(10, title, jobSelector, filter, export, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildBody() {
        VBox left = new VBox(10);
        left.getStyleClass().add("panel-card");
        left.setPadding(new Insets(12));
        left.setMaxHeight(Double.MAX_VALUE);

        HBox tabs = buildStatusTabs();

        listView.setCellFactory(param -> new ApplicantRowCell());
        listView.setPrefHeight(700);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateDetail(newV));
        VBox.setVgrow(listView, Priority.ALWAYS);

        left.getChildren().addAll(tabs, listView);

        VBox right = new VBox(12);
        right.getStyleClass().add("panel-card");
        right.setPadding(new Insets(16));
        right.setPrefWidth(360);
        right.setMaxHeight(Double.MAX_VALUE);

        Label title = new Label("Applicant Details");
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Button accept = new Button("Accept");
        accept.getStyleClass().add("primary-button");
        accept.setMaxWidth(Double.MAX_VALUE);
        accept.setOnAction(event -> openReview());

        Button viewFull = new Button("View Full Application");
        viewFull.getStyleClass().add("secondary-button");
        viewFull.setMaxWidth(Double.MAX_VALUE);
        viewFull.setOnAction(event -> openReview());

        right.getChildren().addAll(
                title,
                detailField("Skills Overlap", detailSkills),
                detailField("Academic Record", detailAcademic),
                accept,
                viewFull
        );

        HBox body = new HBox(16, left, right);
        body.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);
        return body;
    }

    private HBox buildStatusTabs() {
        allTab.getStyleClass().addAll("applicant-status-tab", "applicant-status-tab-active");
        underReviewTab.getStyleClass().add("applicant-status-tab");
        rejectedTab.getStyleClass().add("applicant-status-tab");
        acceptedTab.getStyleClass().add("applicant-status-tab");

        allTab.setOnAction(e -> setStatusFilter(StatusFilter.ALL));
        underReviewTab.setOnAction(e -> setStatusFilter(StatusFilter.UNDER_REVIEW));
        rejectedTab.setOnAction(e -> setStatusFilter(StatusFilter.REJECTED));
        acceptedTab.setOnAction(e -> setStatusFilter(StatusFilter.ACCEPTED));

        HBox tabs = new HBox(10, allTab, underReviewTab, rejectedTab, acceptedTab);
        tabs.getStyleClass().add("applicant-status-tabs");
        tabs.setAlignment(Pos.CENTER_LEFT);
        updateTabCounts();
        return tabs;
    }

    private void setStatusFilter(StatusFilter filter) {
        this.statusFilter = filter == null ? StatusFilter.ALL : filter;
        updateTabStyles();
        applyStatusFilter();
    }

    private void updateTabStyles() {
        setActiveTab(allTab, statusFilter == StatusFilter.ALL);
        setActiveTab(underReviewTab, statusFilter == StatusFilter.UNDER_REVIEW);
        setActiveTab(rejectedTab, statusFilter == StatusFilter.REJECTED);
        setActiveTab(acceptedTab, statusFilter == StatusFilter.ACCEPTED);
    }

    private void setActiveTab(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            if (!button.getStyleClass().contains("applicant-status-tab-active")) {
                button.getStyleClass().add("applicant-status-tab-active");
            }
        } else {
            button.getStyleClass().remove("applicant-status-tab-active");
        }
    }

    private VBox detailField(String title, Label value) {
        VBox box = new VBox(8);

        Label label = new Label(title);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.8px;");

        value.setWrapText(true);
        value.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        VBox content = new VBox(value);
        content.getStyleClass().add("soft-card");
        content.setPadding(new Insets(12));

        box.getChildren().addAll(label, content);
        return box;
    }

    private void refreshJobs() {
        List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
        List<Job> selectorItems = new ArrayList<>();
        if (!jobs.isEmpty()) {
            selectorItems.add(allJobsOption);
        }
        selectorItems.addAll(jobs);

        jobSelector.setItems(FXCollections.observableArrayList(selectorItems));
        if (jobs.isEmpty()) {
            return;
        }

        if (preferredJobId != null) {
            for (Job job : jobs) {
                if (preferredJobId.equals(job.getJobId())) {
                    jobSelector.setValue(job);
                    return;
                }
            }
        }

        jobSelector.setValue(jobs.get(0));
    }

    private void refreshApplications() {
        Job selectedJob = jobSelector.getValue();
        if (selectedJob == null) {
            listView.setItems(FXCollections.observableArrayList());
            allRows.clear();
            updateTabCounts();
            updateDetail(null);
            return;
        }

        List<Application> apps;
        if (isAllJobsOption(selectedJob)) {
            apps = new ArrayList<>();
            List<Job> jobs = services.jobService().getJobsByOrganiser(user.getUserId());
            for (Job job : jobs) {
                apps.addAll(services.applicationService().getApplicationsByJob(job.getJobId()));
            }
        } else {
            apps = services.applicationService().getApplicationsByJob(selectedJob.getJobId());
        }

        List<Row> rows = new ArrayList<>();
        for (Application app : apps) {
            String applicantName = services.applicantProfileRepository().findById(app.getApplicantId())
                    .map(profile -> profile.getFullName())
                    .orElse(app.getApplicantId());
            String jobTitle = services.jobRepository().findById(app.getJobId())
                    .map(Job::getTitle)
                    .orElse(app.getJobId());
            rows.add(new Row(app.getApplicationId(), applicantName, app.getStatus().name(), app.getMatchScore(), jobTitle));
        }

        allRows.clear();
        allRows.addAll(rows);
        updateTabCounts();
        applyStatusFilter();
    }

    private void updateTabCounts() {
        int allCount = allRows.size();
        int underReviewCount = 0;
        int rejectedCount = 0;
        int acceptedCount = 0;

        for (Row row : allRows) {
            if (row == null || row.status == null) {
                continue;
            }
            String status = row.status;
            if ("SUBMITTED".equals(status) || "UNDER_REVIEW".equals(status)) {
                underReviewCount++;
            }
            if ("REJECTED".equals(status)) {
                rejectedCount++;
            }
            if ("ACCEPTED".equals(status)) {
                acceptedCount++;
            }
        }

        allTab.setText("All (" + allCount + ")");
        underReviewTab.setText("Under Review (" + underReviewCount + ")");
        rejectedTab.setText("Rejected (" + rejectedCount + ")");
        acceptedTab.setText("Accepted (" + acceptedCount + ")");
    }

    private void applyStatusFilter() {
        String selectedId = selectedRow == null ? null : selectedRow.applicationId;

        List<Row> filtered = new ArrayList<>();
        for (Row row : allRows) {
            if (row == null) {
                continue;
            }
            if (matchesStatusFilter(row)) {
                filtered.add(row);
            }
        }

        listView.setItems(FXCollections.observableArrayList(filtered));

        if (selectedId != null) {
            for (Row row : filtered) {
                if (selectedId.equals(row.applicationId)) {
                    listView.getSelectionModel().select(row);
                    return;
                }
            }
        }

        if (!filtered.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        } else {
            updateDetail(null);
        }
    }

    private boolean matchesStatusFilter(Row row) {
        if (row == null) {
            return false;
        }
        if (statusFilter == StatusFilter.ALL) {
            return true;
        }
        String status = row.status;
        if (status == null) {
            return false;
        }
        return switch (statusFilter) {
            case UNDER_REVIEW -> "SUBMITTED".equals(status) || "UNDER_REVIEW".equals(status);
            case REJECTED -> "REJECTED".equals(status);
            case ACCEPTED -> "ACCEPTED".equals(status);
            case ALL -> true;
        };
    }

    private void updateDetail(Row row) {
        selectedRow = row;
        if (row == null) {
            detailSkills.setText("-");
            detailAcademic.setText("-");
            return;
        }

        detailSkills.setText("Match score " + row.matchScore + "% based on skills and availability.");
        detailAcademic.setText("Applicant: " + row.applicantName + "\nCurrent status: " + row.status);
    }

    private void openReview() {
        if (selectedRow == null) {
            DialogControllerFactory.validationError("Please select one applicant first.",
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null) {
            stage.initOwner(view.getScene().getWindow());
        }
        stage.setTitle("Applicant Review");

        Parent reviewView = new ApplicantReviewController(services, user, selectedRow.applicationId).getView();
        Scene scene = new Scene(reviewView, 920, 760);
        if (ApplicantListController.class.getResource("/styles/app.css") != null) {
            String stylesheet = ApplicantListController.class.getResource("/styles/app.css").toExternalForm();
            scene.getStylesheets().add(stylesheet);
        }
        stage.setScene(scene);
        stage.showAndWait();
        refreshApplications();
    }

    private static class ApplicantRowCell extends ListCell<Row> {
        @Override
        protected void updateItem(Row item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox row = new VBox(6);
            row.setPadding(new Insets(12));
            row.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10;");

            HBox top = new HBox();
            Label name = new Label(item.applicantName);
            name.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label status = new Label(item.status.replace('_', ' '));
            status.getStyleClass().add(statusPillClass(item.status));

            top.getChildren().addAll(name, spacer, status);

            String metaText = (item.jobTitle == null || item.jobTitle.isBlank())
                    ? ("Application " + item.applicationId + "   |   Score " + item.matchScore + "%")
                    : ("Job " + item.jobTitle + "   |   Application " + item.applicationId + "   |   Score " + item.matchScore + "%");

            Label meta = new Label(metaText);
            meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            row.getChildren().addAll(top, meta);
            setGraphic(row);
        }

        private static String statusPillClass(String status) {
            if (status == null) {
                return "status-pill-draft";
            }
            return switch (status) {
                case "ACCEPTED" -> "status-pill-active";
                case "REJECTED" -> "status-pill-danger";
                case "SUBMITTED", "UNDER_REVIEW" -> "status-pill-draft";
                default -> "status-pill-draft";
            };
        }
    }

    public static class Row {
        private final String applicationId;
        private final String applicantName;
        private final String status;
        private final int matchScore;
        private final String jobTitle;

        public Row(String applicationId, String applicantName, String status, int matchScore, String jobTitle) {
            this.applicationId = applicationId;
            this.applicantName = applicantName;
            this.status = status;
            this.matchScore = matchScore;
            this.jobTitle = jobTitle;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public String getApplicantName() {
            return applicantName;
        }

        public String getStatus() {
            return status;
        }

        public int getMatchScore() {
            return matchScore;
        }

        public String getJobTitle() {
            return jobTitle;
        }
    }

    private static Job buildAllJobsOption() {
        Job job = new Job();
        job.setJobId(ALL_JOBS_JOB_ID);
        job.setTitle("All Jobs");
        return job;
    }

    private boolean isAllJobsOption(Job job) {
        return job != null && ALL_JOBS_JOB_ID.equals(job.getJobId());
    }
}
