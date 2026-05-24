package edu.bupt.ta.controller;

import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.ValidationResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ApplicantProfileController {

    private final ServiceRegistry services;
    private final User user;
    private final BorderPane view = new BorderPane();
    private ApplicantProfile profile;

    // Edit fields
    private final TextField fullNameField = new TextField();
    private final TextField studentIdField = new TextField();
    private final TextField programmeField = new TextField();
    private final ComboBox<Integer> yearCombo = new ComboBox<>();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final ComboBox<String> campusCombo = new ComboBox<>();
    private final ComboBox<String> crossCampusCombo = new ComboBox<>();

    // Read-only labels
    private final Label fullNameValue = readOnlyValue();
    private final Label studentIdValue = readOnlyValue();
    private final Label programmeValue = readOnlyValue();
    private final Label yearValue = readOnlyValue();
    private final Label emailValue = readOnlyValue();
    private final Label phoneValue = readOnlyValue();
    private final Label campusValue = readOnlyValue();
    private final Label crossCampusValue = readOnlyValue();

    // Avatar labels (updated on save)
    private final Label avatarNameLabel = new Label();
    private final Label avatarRoleLabel = new Label();

    // Card body — swapped between read/edit mode
    private final VBox cardBody = new VBox(20);

    // Header buttons
    private final Button editBtn = new Button();
    private final Button resetBtn = new Button();
    private final Button saveBtn = new Button();

    private boolean editing = false;

    public ApplicantProfileController(ServiceRegistry services, User user) {
        this.services = services;
        this.user = user;
        initialize();
    }

    public Parent getView() {
        return view;
    }

    /** Programmatically enter edit mode (called from parent views). */
    public void enterEditMode() {
        if (!editing) {
            enterEditModeInternal();
        }
    }

    private void initialize() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());

        VBox page = new VBox(20);
        page.setFillWidth(true);

        page.getChildren().addAll(
                buildTitleBlock(),
                buildInfoCard()
        );

        view.setCenter(page);
    }

    private VBox buildTitleBlock() {
        Label heading = new Label(I18n.t("my_profile"));
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label(I18n.t("manage_personal_info"));
        subtitle.getStyleClass().add("body-muted");
        subtitle.setStyle("-fx-font-size: 15px;");

        return new VBox(4, heading, subtitle);
    }

    private VBox buildInfoCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("cv-card");
        card.setPadding(new Insets(24));

        card.getChildren().addAll(buildCardHeader(), buildAvatarRow(), cardBody);

        loadFields();
        renderReadMode();
        return card;
    }

    private HBox buildCardHeader() {
        Label cardTitle = new Label(I18n.t("basic_information"));
        cardTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        editBtn.setText(I18n.t("edit"));
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> enterEditModeInternal());

        resetBtn.setText(I18n.t("reset"));
        resetBtn.getStyleClass().add("secondary-button");
        resetBtn.setOnAction(e -> loadFields());

        saveBtn.setText(I18n.t("save"));
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> saveProfile());

        HBox header = new HBox(8, cardTitle, spacer, editBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox buildAvatarRow() {
        Label avatar = new Label(initials());
        avatar.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: white;"
                + "-fx-background-color: #354a5f; -fx-background-radius: 999;"
                + "-fx-min-width: 72; -fx-min-height: 72; -fx-pref-width: 72; -fx-pref-height: 72;"
                + "-fx-alignment: center;");

        avatarNameLabel.setText(safe(profile.getFullName(), "TA Applicant"));
        avatarNameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        String roleText = I18n.t("ta_applicant_label");
        if (profile.getYear() > 0) {
            roleText += "  ·  " + I18n.t("year_label").replace("{n}", String.valueOf(profile.getYear()));
        }
        avatarRoleLabel.setText(roleText);
        avatarRoleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        VBox meta = new VBox(4, avatarNameLabel, avatarRoleLabel);
        HBox row = new HBox(16, avatar, meta);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void refreshAvatarRow() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        avatarNameLabel.setText(safe(profile.getFullName(), "TA Applicant"));
        String roleText = I18n.t("ta_applicant_label");
        if (profile.getYear() > 0) {
            roleText += "  ·  " + I18n.t("year_label").replace("{n}", String.valueOf(profile.getYear()));
        }
        avatarRoleLabel.setText(roleText);
    }

    // ── Read mode ──────────────────────────────────────────────────────────

    private void renderReadMode() {
        editing = false;
        refreshReadLabels();
        refreshAvatarRow();

        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(16);

        grid.add(infoCell(I18n.t("full_name_upper"), fullNameValue), 0, 0);
        grid.add(infoCell(I18n.t("student_id_upper"), studentIdValue), 1, 0);
        grid.add(infoCell(I18n.t("degree_program_upper"), programmeValue), 2, 0);
        grid.add(infoCell(I18n.t("email_upper"), emailValue), 0, 1);
        grid.add(infoCell(I18n.t("academic_year_upper"), yearValue), 1, 1);
        grid.add(infoCell(I18n.t("phone_upper"), phoneValue), 2, 1);
        grid.add(infoCell(I18n.t("select_campus"), campusValue), 0, 2);
        grid.add(infoCell(I18n.t("accept_cross_campus_upper"), crossCampusValue), 1, 2);

        cardBody.getChildren().setAll(grid);

        HBox header = (HBox) ((VBox) cardBody.getParent()).getChildren().get(0);
        header.getChildren().setAll(
                header.getChildren().get(0), // cardTitle
                header.getChildren().get(1), // spacer
                editBtn
        );
    }

    // ── Edit mode ──────────────────────────────────────────────────────────

    private void enterEditModeInternal() {
        editing = true;
        loadFields();

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(14);

        form.add(formField(I18n.t("full_name_upper"), fullNameField), 0, 0);
        form.add(formField(I18n.t("student_id_upper"), studentIdField), 1, 0);
        form.add(formField(I18n.t("degree_program_upper"), programmeField), 2, 0);
        form.add(formField(I18n.t("email_upper"), emailField), 0, 1);
        form.add(yearField(I18n.t("academic_year_upper")), 1, 1);
        form.add(formField(I18n.t("phone_upper"), phoneField), 2, 1);
        form.add(campusField(I18n.t("select_campus")), 0, 2);
        form.add(crossCampusField(I18n.t("accept_cross_campus_upper")), 1, 2);

        cardBody.getChildren().setAll(form);

        HBox header = (HBox) ((VBox) cardBody.getParent()).getChildren().get(0);
        header.getChildren().setAll(
                header.getChildren().get(0), // cardTitle
                header.getChildren().get(1), // spacer
                resetBtn,
                saveBtn
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private VBox infoCell(String label, Label value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        VBox cell = new VBox(4, lbl, value);
        cell.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private VBox formField(String label, TextField input) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("field-label");
        input.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(6, lbl, input);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox yearField(String title) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        yearCombo.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8);
        yearCombo.setPromptText(I18n.t("select_year"));
        yearCombo.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, yearCombo);
        return box;
    }

    private VBox campusField(String title) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        campusCombo.getItems().setAll(I18n.t("haidian_campus"), I18n.t("shahe_campus_cvc"));
        campusCombo.setPromptText(I18n.t("select_campus"));
        campusCombo.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, campusCombo);
        return box;
    }

    private VBox crossCampusField(String title) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(box, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        crossCampusCombo.getItems().setAll(I18n.t("yes_option"), I18n.t("no_option"));
        crossCampusCombo.setPromptText(I18n.t("select"));
        crossCampusCombo.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, crossCampusCombo);
        return box;
    }

    private static Label readOnlyValue() {
        Label lbl = new Label("-");
        lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #0f172a;");
        lbl.setWrapText(true);
        return lbl;
    }

    private void refreshReadLabels() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        fullNameValue.setText(safe(profile.getFullName(), "-"));
        studentIdValue.setText(safe(profile.getStudentId(), "-"));
        programmeValue.setText(safe(profile.getProgramme(), "-"));
        yearValue.setText(profile.getYear() > 0 ? String.valueOf(profile.getYear()) : "-");
        emailValue.setText(safe(profile.getEmail(), "-"));
        phoneValue.setText(safe(profile.getPhone(), "-"));
        campusValue.setText(safe(profile.getCampus(), "-"));
        crossCampusValue.setText(profile.isAcceptCrossCampus() ? I18n.t("yes_option") : I18n.t("no_option"));
    }

    private void loadFields() {
        profile = services.applicantProfileService().getOrCreateProfile(user.getUserId());
        fullNameField.setText(nullToEmpty(profile.getFullName()));
        studentIdField.setText(nullToEmpty(profile.getStudentId()));
        programmeField.setText(nullToEmpty(profile.getProgramme()));
        yearCombo.setValue(profile.getYear() > 0 ? profile.getYear() : null);
        emailField.setText(nullToEmpty(profile.getEmail()));
        phoneField.setText(nullToEmpty(profile.getPhone()));
        campusCombo.setValue(notBlank(profile.getCampus()) ? profile.getCampus() : null);
        crossCampusCombo.setValue(profile.isAcceptCrossCampus() ? I18n.t("yes_option") : I18n.t("no_option"));
    }

    private void saveProfile() {
        profile.setFullName(fullNameField.getText());
        profile.setStudentId(studentIdField.getText());
        profile.setProgramme(programmeField.getText());
        profile.setEmail(emailField.getText());
        profile.setPhone(phoneField.getText());
        Integer selectedYear = yearCombo.getValue();
        profile.setYear(selectedYear == null ? 0 : selectedYear);
        profile.setCampus(campusCombo.getValue());
        profile.setAcceptCrossCampus(I18n.t("yes_option").equals(crossCampusCombo.getValue()));

        ValidationResult result = services.applicantProfileService().saveProfile(profile);
        if (!result.isValid()) {
            DialogControllerFactory.validationError(String.join("\n", result.getErrors()),
                    view.getScene() == null ? null : view.getScene().getWindow());
            return;
        }

        DialogControllerFactory.success(I18n.t("profile_saved_label"), I18n.t("profile_saved_success"),
                view.getScene() == null ? null : view.getScene().getWindow());
        renderReadMode();
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private String initials() {
        String name = profile.getFullName();
        if (name == null || name.isBlank()) return "TA";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
