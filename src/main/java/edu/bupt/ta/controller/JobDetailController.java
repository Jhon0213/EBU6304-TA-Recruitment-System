package edu.bupt.ta.controller;

import edu.bupt.ta.dto.MatchExplanationDTO;
import edu.bupt.ta.enums.JobStatus;
import edu.bupt.ta.model.Job;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.function.Consumer;

public class JobDetailController {

    private final VBox view = new VBox(16);
    private final TextFlow titleFlow = new TextFlow();
    private final Text titleText = new Text("Select a job");
    private final Label moduleLabel = new Label("-");
    private final Label metaHours = chip("Hours: -");
    private final Label metaPositions = chip("Seats: -");
    private final Label metaDeadline = chip("Deadline: -");
    private final Label descLabel = new Label("Choose a job card on the left to preview details.");
    private final Label matchLabel = new Label("AI Match: -");
    private final Label missingLabel = new Label("Missing Skills: -");
    private final Label workloadLabel = new Label("Projected Workload: -");
    private final TextArea statementArea = new TextArea();
    private final Button applyButton = new Button("APPLY NOW");

    private Job currentJob;
    private Consumer<String> onApply;
    private Consumer<String> onCancel;
    private boolean hasApplied = false;
    private boolean isAccepted = false;
    private boolean isJobCancelled = false;
    private String currentApplicantId;

    public JobDetailController() {
        initialize();
    }

    public Parent getView() {
        return view;
    }

    public void setOnApply(Consumer<String> onApply) {
        this.onApply = onApply;
    }

    public void setOnCancel(Consumer<String> onCancel) {
        this.onCancel = onCancel;
    }

    public void setHasApplied(boolean hasApplied, String applicantId) {
        this.hasApplied = hasApplied;
        this.currentApplicantId = applicantId;
        updateButtonState();
    }

    public void setIsAccepted(boolean isAccepted) {
        this.isAccepted = isAccepted;
        updateButtonState();
    }

    public void setJobCancelled(boolean isJobCancelled) {
        this.isJobCancelled = isJobCancelled;
        updateButtonState();
    }

    public void setJob(Job job) {
        this.currentJob = job;
        if (job == null) {
            titleText.setText("Select a job");
            moduleLabel.setText("-");
            metaHours.setText("Hours: -");
            metaPositions.setText("Seats: -");
            metaDeadline.setText("Deadline: -");
            descLabel.setText("Choose a job card on the left to preview details.");
            setMatchExplanation(null);
            applyButton.setDisable(true);
            applyButton.setText("APPLY NOW");
            statementArea.setDisable(true);
            statementArea.clear();
            return;
        }

        titleText.setText(job.getTitle());
        moduleLabel.setText(job.getModuleCode() + "  |  " + job.getModuleName());
        metaHours.setText("Hours: " + job.getWeeklyHours() + "h/week");
        metaPositions.setText("Seats: " + job.getPositions());
        metaDeadline.setText("Deadline: " + job.getDeadline());
        descLabel.setText(job.getDescription());

        boolean isOpen = job.getStatus() == JobStatus.OPEN;
        applyButton.setDisable(!isOpen);
        statementArea.setDisable(!isOpen);

        updateButtonState();
    }

    private void updateButtonState() {
        // 如果已经被录取，显示灰色 Accepted 按钮，不可点击
        if (isAccepted) {
            applyButton.setText("ACCEPTED");
            applyButton.setDisable(true);
            applyButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            statementArea.setDisable(true);
            return;
        }

        // 如果岗位已取消，显示灰色 Job Cancelled 按钮，不可点击
        if (isJobCancelled) {
            applyButton.setText("JOB CANCELLED");
            applyButton.setDisable(true);
            applyButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            statementArea.setDisable(true);
            return;
        }

        if (currentJob == null) {
            applyButton.setText("APPLY NOW");
            applyButton.setDisable(true);
            applyButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            return;
        }

        // 根据岗位状态显示不同的按钮文字
        if (currentJob.getStatus() == JobStatus.EXPIRED) {
            applyButton.setText("EXPIRED");
            applyButton.setDisable(true);
            applyButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            return;
        } else if (currentJob.getStatus() == JobStatus.CLOSED) {
            applyButton.setText("CLOSED");
            applyButton.setDisable(true);
            applyButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            return;
        } else if (currentJob.getStatus() == JobStatus.DRAFT) {
            applyButton.setText("NOT YET OPEN");
            applyButton.setDisable(true);
            applyButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            return;
        } else if (currentJob.getStatus() != JobStatus.OPEN) {
            applyButton.setText("UNAVAILABLE");
            applyButton.setDisable(true);
            applyButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            return;
        }

        if (hasApplied) {
            applyButton.setText("CANCEL APPLICATION");
            applyButton.setDisable(false);
            applyButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 14px; -fx-weight: 700;");
            statementArea.setDisable(true);
        } else {
            applyButton.setText("APPLY NOW");
            applyButton.setDisable(false);
            applyButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700;");
            statementArea.setDisable(false);
        }
    }

    public void setMatchExplanation(MatchExplanationDTO dto) {
        if (dto == null) {
            matchLabel.setText("AI Match: -");
            missingLabel.setText("Missing Skills: -");
            workloadLabel.setText("Projected Workload: -");
            matchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #334155;");
            return;
        }

        matchLabel.setText("AI Match: " + dto.score() + "%");
        missingLabel.setText("Missing Skills: " + (dto.missingSkills().isEmpty() ? "None" : String.join(", ", dto.missingSkills())));
        workloadLabel.setText("Projected Workload: " + dto.projectedWorkload() + "h/week");
        if (dto.score() >= 80) {
            matchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #059669;");
        } else {
            matchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #b45309;");
        }
    }

    private void initialize() {
        view.setPadding(new Insets(24));
        view.getStyleClass().add("panel-card");
        view.setMinWidth(430);

        titleText.setStyle("-fx-font-size: 32px; -fx-font-weight: 800; -fx-fill: #0f172a;");
        titleFlow.getChildren().setAll(titleText);
        titleFlow.setLineSpacing(4);
        titleFlow.maxWidthProperty().bind(view.widthProperty().subtract(48));

        moduleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #00c29f;");

        HBox metaRow = new HBox(10, metaHours, metaPositions, metaDeadline);

        Label sectionDesc = new Label("Job Description");
        sectionDesc.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #334155; -fx-letter-spacing: 0.5px;");

        descLabel.setWrapText(true);
        descLabel.prefWidthProperty().bind(view.widthProperty().subtract(48));
        descLabel.setMinWidth(0);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-line-spacing: 2px;");

        VBox matchPanel = new VBox(8, matchLabel, missingLabel, workloadLabel);
        matchPanel.setPadding(new Insets(14));
        matchPanel.getStyleClass().add("soft-card");

        Label statementLabel = new Label("Application Statement");
        statementLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #334155; -fx-letter-spacing: 0.5px;");

        statementArea.setPromptText("Why are you suitable for this role?");
        statementArea.setWrapText(true);
        statementArea.setPrefRowCount(5);

        applyButton.getStyleClass().add("primary-button");
        applyButton.setPrefHeight(44);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setOnAction(event -> {
            if (currentJob == null) {
                return;
            }
            if (hasApplied && onCancel != null) {
                // 已申请：取消申请
                onCancel.accept(currentJob.getJobId());
            } else if (onApply != null) {
                // 未申请：提交申请
                onApply.accept(statementArea.getText());
            }
        });

        VBox.setVgrow(descLabel, Priority.ALWAYS);
        view.getChildren().addAll(
                titleFlow,
                moduleLabel,
                metaRow,
                sectionDesc,
                descLabel,
                matchPanel,
                statementLabel,
                statementArea,
                applyButton
        );
    }

    private static Label chip(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569; -fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 10 8 10;");
        return label;
    }
}
