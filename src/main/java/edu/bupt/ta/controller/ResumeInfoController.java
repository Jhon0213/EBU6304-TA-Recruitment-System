package edu.bupt.ta.controller;

import edu.bupt.ta.model.ResumeInfo;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResumeInfoController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private ResumeInfo resume;

    private final TextField relevantModules = new TextField();
    private final TextField technicalSkills = new TextField();
    private final TextField languageSkills = new TextField();
    private final TextField availability = new TextField();
    private final TextField maxWeeklyHours = new TextField();
    private final TextArea experience = new TextArea();
    private final TextArea personalStatement = new TextArea();

    public ResumeInfoController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        I18n.initTranslations();
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        String applicantId = services.applicantProfileService().getOrCreateProfile(user.getUserId()).getApplicantId();
        resume = services.resumeService().getOrCreateResume(applicantId);

        VBox root = new VBox(14);
        root.getStyleClass().add("app-surface");
        root.setFillWidth(true);

        HBox header = new HBox();

        Label heading = new Label(I18n.t("resume_information"));
        heading.getStyleClass().add("section-title");

        Label subtitle = new Label(I18n.t("maintain_resume_data"));
        subtitle.getStyleClass().add("body-muted");

        VBox titleBlock = new VBox(4, heading, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button reset = new Button(I18n.t("reset"));
        reset.getStyleClass().add("secondary-button");
        reset.setOnAction(event -> loadFromModel());

        Button save = new Button(I18n.t("save_resume"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveResume());

        header.getChildren().addAll(titleBlock, spacer, reset, save);

        VBox formCard = new VBox(12);
        formCard.getStyleClass().add("panel-card");
        formCard.setPadding(new Insets(16));
        formCard.setFillWidth(true);

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);

        form.add(field(I18n.t("relevant_modules"), relevantModules), 0, 0, 2, 1);
        form.add(field(I18n.t("technical_skills"), technicalSkills), 0, 1);
        form.add(field(I18n.t("language_skills"), languageSkills), 1, 1);
        form.add(field(I18n.t("availability_label"), availability), 0, 2);
        form.add(field(I18n.t("max_weekly_hours"), maxWeeklyHours), 1, 2);
        form.add(areaField(I18n.t("experience_label"), experience, 4), 0, 3, 2, 1);
        form.add(areaField(I18n.t("personal_statement_label"), personalStatement, 5), 0, 4, 2, 1);

        formCard.getChildren().add(form);

        VBox hintCard = new VBox(6);
        hintCard.getStyleClass().add("soft-card");
        hintCard.setPadding(new Insets(12));

        Label hintTitle = new Label(I18n.t("matching_quality_hint"));
        hintTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f766e;");

        Label hintText = new Label(I18n.t("improve_match_hint"));
        hintText.setWrapText(true);
        hintText.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f766e;");

        hintCard.getChildren().addAll(hintTitle, hintText);

        root.getChildren().addAll(header, formCard, hintCard);
        view.setCenter(root);

        loadFromModel();
    }

    private VBox field(String title, TextField input) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        input.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, input);
        return box;
    }

    private VBox areaField(String title, TextArea input, int rows) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        input.setPrefRowCount(rows);
        input.setWrapText(true);
        input.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, input);
        return box;
    }

    private void loadFromModel() {
        relevantModules.setText(String.join(", ", resume.getRelevantModules()));
        technicalSkills.setText(String.join(", ", resume.getTechnicalSkills()));
        languageSkills.setText(String.join(", ", resume.getLanguageSkills()));
        availability.setText(String.join(", ", resume.getAvailability()));
        maxWeeklyHours.setText(resume.getMaxWeeklyHours() > 0 ? String.valueOf(resume.getMaxWeeklyHours()) : "");
        experience.setText(resume.getExperienceText() == null ? "" : resume.getExperienceText());
        personalStatement.setText(resume.getPersonalStatement() == null ? "" : resume.getPersonalStatement());
    }

    private void saveResume() {
        resume.setRelevantModules(split(relevantModules.getText()));
        resume.setTechnicalSkills(split(technicalSkills.getText()));
        resume.setLanguageSkills(split(languageSkills.getText()));
        resume.setAvailability(split(availability.getText()));
        resume.setExperienceText(experience.getText());
        resume.setPersonalStatement(personalStatement.getText());

        try {
            resume.setMaxWeeklyHours(Integer.parseInt(maxWeeklyHours.getText()));
        } catch (NumberFormatException e) {
            resume.setMaxWeeklyHours(0);
        }

        ValidationResult result = services.resumeService().saveResume(resume);
        if (!result.isValid()) {
            DialogControllerFactory.validationError(String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success(I18n.t("resume_saved"), I18n.t("resume_saved_msg"),
                view.getScene() == null ? null : view.getScene().getWindow());
    }

    private List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }
}
