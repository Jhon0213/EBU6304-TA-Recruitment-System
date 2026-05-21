package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ApplicantProfileController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();

    private ApplicantProfile profile;

    private final TextField fullName = new TextField();
    private final TextField studentId = new TextField();
    private final TextField programme = new TextField();
    private final ComboBox<Integer> year = new ComboBox<>();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();
    private final ComboBox<String> campus = new ComboBox<>();
    private final ComboBox<String> crossCampus = new ComboBox<>();

    public ApplicantProfileController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());

        VBox root = new VBox(14);
        root.getStyleClass().add("app-surface");
        root.setFillWidth(true);
        root.setPadding(new Insets(4, 0, 0, 0));

        HBox header = new HBox();
        header.setSpacing(14);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button reset = new Button(I18n.t("reset"));
        reset.getStyleClass().add("secondary-button");
        reset.setOnAction(event -> loadFromModel());

        Button save = new Button(I18n.t("save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveProfile());

        header.getChildren().addAll(spacer, reset, save);

        VBox formCard = new VBox(14);
        formCard.getStyleClass().add("panel-card");
        formCard.setPadding(new Insets(16));
        formCard.setFillWidth(true);

        Label formTitle = new Label(I18n.t("personal_information"));
        formTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);

        form.add(field(I18n.t("full_name_upper"), fullName), 0, 0);
        form.add(field(I18n.t("student_id_upper"), studentId), 1, 0);
        form.add(field(I18n.t("email_upper"), email), 0, 1);
        form.add(yearField(I18n.t("academic_year_upper")), 1, 1);
        form.add(field(I18n.t("phone_upper"), phone), 0, 2);
        form.add(field(I18n.t("degree_program_upper"), programme), 1, 2);
        form.add(campusField(I18n.t("select_campus")), 0, 3);
        form.add(crossCampusField(I18n.t("accept_cross_campus_upper")), 1, 3);

        formCard.getChildren().addAll(formTitle, form);

        VBox tipCard = new VBox(6);
        tipCard.getStyleClass().add("soft-card");
        tipCard.setPadding(new Insets(12));

        Label tipTitle = new Label(I18n.t("pro_tip"));
        tipTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #b45309;");

        Label tipBody = new Label(I18n.t("complete_profile_unlock_apply"));
        tipBody.setWrapText(true);
        tipBody.setStyle("-fx-font-size: 12px; -fx-text-fill: #92400e;");

        tipCard.getChildren().addAll(tipTitle, tipBody);

        root.getChildren().addAll(header, formCard, tipCard);
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

    private VBox yearField(String title) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        year.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8);
        year.setPromptText(I18n.t("select_year"));
        year.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, year);
        return box;
    }

    private VBox campusField(String title) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        campus.getItems().setAll(I18n.t("haidian_campus"), I18n.t("shahe_campus_cvc"));
        campus.setPromptText(I18n.t("select_campus"));
        campus.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, campus);
        return box;
    }

    private VBox crossCampusField(String title) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        crossCampus.getItems().setAll(I18n.t("yes_option"), I18n.t("no_option"));
        crossCampus.setPromptText(I18n.t("select"));
        crossCampus.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, crossCampus);
        return box;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void loadFromModel() {
        fullName.setText(nullToEmpty(profile.getFullName()));
        studentId.setText(nullToEmpty(profile.getStudentId()));
        programme.setText(nullToEmpty(profile.getProgramme()));
        year.setValue(profile.getYear() > 0 ? profile.getYear() : null);
        email.setText(nullToEmpty(profile.getEmail()));
        phone.setText(nullToEmpty(profile.getPhone()));
        campus.setValue(notBlank(profile.getCampus()) ? profile.getCampus() : null);
        crossCampus.setValue(profile.isAcceptCrossCampus() ? I18n.t("yes_option") : I18n.t("no_option"));
    }

    private void saveProfile() {
        profile.setFullName(fullName.getText());
        profile.setStudentId(studentId.getText());
        profile.setProgramme(programme.getText());
        profile.setEmail(email.getText());
        profile.setPhone(phone.getText());
        Integer selectedYear = year.getValue();
        profile.setYear(selectedYear == null ? 0 : selectedYear);
        profile.setCampus(campus.getValue());
        profile.setAcceptCrossCampus(I18n.t("yes_option").equals(crossCampus.getValue()));

        ValidationResult result = services.applicantProfileService().saveProfile(profile);
        if (!result.isValid()) {
            DialogControllerFactory.validationError(String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success(I18n.t("profile_saved_label"), I18n.t("profile_saved_success"),
                view.getScene() == null ? null : view.getScene().getWindow());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
