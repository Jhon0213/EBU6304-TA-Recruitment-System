package edu.bupt.ta.controller;

import edu.bupt.ta.dto.LoginResult;
import edu.bupt.ta.enums.Role;
import edu.bupt.ta.model.ApplicantProfile;
import edu.bupt.ta.model.Job;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.ui.IconFactory;
import edu.bupt.ta.util.DateTimeUtils;
import edu.bupt.ta.util.IdGenerator;
import edu.bupt.ta.util.I18n;
import edu.bupt.ta.util.PasswordUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class LoginController {

    private static final String RESET_VERIFICATION_CODE = "1234";

    private final ServiceRegistry services;
    private final Consumer<User> onLoginSuccess;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField visiblePasswordField = new TextField();
    private boolean passwordVisible;

    private final BorderPane view = new BorderPane();

    public LoginController(ServiceRegistry services, Consumer<User> onLoginSuccess) {
        this.services = services;
        this.onLoginSuccess = onLoginSuccess;
        I18n.initTranslations();
        initialize();
    }

    public Parent getView() {
        return view;
    }

    public void prefillUsername(String username) {
        usernameField.setText(username);
        passwordField.requestFocus();
    }

    private void initialize() {
        view.getStyleClass().add("login-root");

        HBox root = new HBox();
        root.getStyleClass().add("login-shell");
        root.setFillHeight(true);
        root.setMaxWidth(1400);
        root.setPrefHeight(760);
        root.setMinHeight(680);

        VBox leftPane = buildLeftPane();
        VBox rightPane = buildRightPane();

        leftPane.setMinWidth(520);
        rightPane.setMinWidth(520);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        root.getChildren().addAll(leftPane, rightPane);
        view.setCenter(root);
    }

    private VBox buildLeftPane() {
        VBox left = new VBox();
        left.getStyleClass().add("login-left");
        left.setPadding(new Insets(64));

        HBox brandRow = new HBox(12);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        StackPane brandIcon = IconFactory.badge(
                IconFactory.IconType.GRADUATION_CAP,
                36,
                Color.rgb(255, 255, 255, 0.12),
                Color.WHITE
        );
        brandIcon.getStyleClass().add("login-brand-icon");

        Label brandTitle = new Label(I18n.t("bupt_title"));
        brandTitle.getStyleClass().add("login-brand-title");

        brandRow.getChildren().addAll(brandIcon, brandTitle);

        Label hero = new Label(I18n.t("ta_recruitment_title"));
        hero.getStyleClass().add("login-hero-title");

        VBox topBlock = new VBox(48, brandRow, hero);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox secureRow = new HBox(16);
        secureRow.setAlignment(Pos.CENTER_LEFT);

        StackPane secureIcon = IconFactory.glyph(
                IconFactory.IconType.SHIELD,
                13,
                Color.web("#cbd5e1")
        );
        secureIcon.getStyleClass().add("login-left-meta-icon");

        Label secure = new Label(I18n.t("secure_portal"));
        secure.getStyleClass().add("login-left-meta");
        secureRow.getChildren().addAll(secureIcon, secure);

        Label copyright = new Label(I18n.t("copyright"));
        copyright.getStyleClass().add("login-left-footer");

        VBox footer = new VBox(12, secureRow, copyright);

        left.getChildren().addAll(topBlock, spacer, footer);
        return left;
    }

    private VBox buildRightPane() {
        VBox right = new VBox();
        right.getStyleClass().add("login-right");
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(48));

        VBox content = new VBox(32);
        content.setMaxWidth(448);
        content.setPrefWidth(448);
        content.getStyleClass().add("login-content");

        Label heading = new Label(I18n.t("portal_login"));
        heading.getStyleClass().add("login-heading");

        Label subtitle = new Label(I18n.t("enter_credentials"));
        subtitle.getStyleClass().add("login-subheading");

        VBox titleBlock = new VBox(8, heading, subtitle);

        Label userLabel = new Label(I18n.t("university_id"));
        userLabel.getStyleClass().add("login-field-label");
        usernameField.setPromptText(I18n.t("eg_2023211000"));
        usernameField.getStyleClass().add("login-input-field");
        HBox usernameInput = buildInputShell(
                IconFactory.glyph(IconFactory.IconType.USER, 13, Color.web("#94a3b8")),
                usernameField,
                null
        );
        VBox usernameBlock = new VBox(8, userLabel, usernameInput);

        Label passLabel = new Label(I18n.t("current_password"));
        passLabel.getStyleClass().add("login-field-label");

        Button forgotButton = new Button(I18n.t("forgot_password"));
        forgotButton.getStyleClass().add("login-forgot-link");
        forgotButton.setFocusTraversable(false);
        forgotButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        forgotButton.setOnAction(event -> showResetPasswordDialog());

        HBox passwordHeader = new HBox();
        passwordHeader.setAlignment(Pos.CENTER_LEFT);
        Region passwordSpacer = new Region();
        HBox.setHgrow(passwordSpacer, Priority.ALWAYS);
        passwordHeader.getChildren().addAll(passLabel, passwordSpacer, forgotButton);

        passwordField.setPromptText("••••••••");
        passwordField.getStyleClass().add("login-input-field");
        visiblePasswordField.setPromptText(passwordField.getPromptText());
        visiblePasswordField.getStyleClass().add("login-input-field");
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);

        Button eyeButton = new Button();
        eyeButton.setFocusTraversable(false);
        eyeButton.getStyleClass().add("login-eye-button");
        eyeButton.setGraphic(IconFactory.glyph(IconFactory.IconType.EYE, 13, Color.web("#94a3b8")));
        eyeButton.setOnAction(event -> togglePasswordVisibility(eyeButton));
        HBox passwordInput = buildPasswordInputShell(
                IconFactory.glyph(IconFactory.IconType.LOCK, 13, Color.web("#94a3b8")),
                eyeButton
        );
        VBox passwordBlock = new VBox(8, passwordHeader, passwordInput);

        VBox fieldsBlock = new VBox(20, usernameBlock, passwordBlock);

        Button loginButton = new Button(I18n.t("login_to_portal"));
        loginButton.getStyleClass().add("login-primary-button");
        loginButton.setPrefHeight(44);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> doLogin());

        Button resetButton = new Button(I18n.t("reset"));
        resetButton.getStyleClass().add("login-secondary-button");
        resetButton.setPrefHeight(44);
        resetButton.setPrefWidth(96);
        resetButton.setOnAction(event -> {
            usernameField.clear();
            passwordField.clear();
        });

        HBox actions = new HBox(12, loginButton, resetButton);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        actions.setMaxWidth(Double.MAX_VALUE);

        VBox form = new VBox(24, fieldsBlock, actions);

        Region divider = new Region();
        divider.getStyleClass().add("login-divider");
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);

        HBox footerLinks = new HBox();
        footerLinks.setAlignment(Pos.CENTER_LEFT);
        footerLinks.setMaxWidth(Double.MAX_VALUE);

        Button registerLink = footerLink(I18n.t("register"));
        registerLink.setOnAction(event -> showRegistrationDialog());

        Button support = footerLink(I18n.t("help_center_link"));
        support.setOnAction(event -> showDocument("Help Center", Path.of("docs", "User-Manual.md"), defaultHelpDocument()));

        Button privacy = footerLink(I18n.t("privacy_link"));
        privacy.setOnAction(event -> showDocument("Privacy", Path.of("docs", "Privacy.md"), defaultPrivacyDocument()));

        HBox footerSpacer = new HBox();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox rightLinks = new HBox(16, support, privacy);
        footerLinks.getChildren().addAll(registerLink, footerSpacer, rightLinks);

        content.getChildren().addAll(
                titleBlock,
                form,
                divider,
                footerLinks
        );

        right.getChildren().add(content);
        return right;
    }

    private HBox buildInputShell(Node leftIcon, TextField field, Node rightNode) {
        HBox shell = new HBox(10);
        shell.getStyleClass().add("login-input-shell");
        shell.setAlignment(Pos.CENTER_LEFT);
        shell.setMaxWidth(Double.MAX_VALUE);

        if (leftIcon != null) {
            StackPane leftIconBox = new StackPane(leftIcon);
            leftIconBox.getStyleClass().add("login-input-icon-left");
            shell.getChildren().add(leftIconBox);
        }

        field.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(field, Priority.ALWAYS);
        shell.getChildren().add(field);

        if (rightNode != null) {
            StackPane rightIconBox = new StackPane(rightNode);
            rightIconBox.getStyleClass().add("login-input-icon-right");
            shell.getChildren().add(rightIconBox);
        }

        return shell;
    }

    private Button footerLink(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("login-footer-links");
        button.setFocusTraversable(false);
        button.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        return button;
    }

    private HBox buildPasswordInputShell(Node leftIcon, Button eyeButton) {
        HBox shell = new HBox(10);
        shell.getStyleClass().add("login-input-shell");
        shell.setAlignment(Pos.CENTER_LEFT);
        shell.setMaxWidth(Double.MAX_VALUE);

        if (leftIcon != null) {
            StackPane leftIconBox = new StackPane(leftIcon);
            leftIconBox.getStyleClass().add("login-input-icon-left");
            shell.getChildren().add(leftIconBox);
        }

        StackPane fieldStack = new StackPane(passwordField, visiblePasswordField);
        fieldStack.setMaxWidth(Double.MAX_VALUE);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        visiblePasswordField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fieldStack, Priority.ALWAYS);
        shell.getChildren().add(fieldStack);

        StackPane rightIconBox = new StackPane(eyeButton);
        rightIconBox.getStyleClass().add("login-input-icon-right");
        shell.getChildren().add(rightIconBox);

        return shell;
    }

    private void togglePasswordVisibility(Button eyeButton) {
        passwordVisible = !passwordVisible;
        passwordField.setVisible(!passwordVisible);
        passwordField.setManaged(!passwordVisible);
        visiblePasswordField.setVisible(passwordVisible);
        visiblePasswordField.setManaged(passwordVisible);
        eyeButton.setStyle(passwordVisible ? "-fx-opacity: 1;" : "");

        TextField activeField = passwordVisible ? visiblePasswordField : passwordField;
        activeField.requestFocus();
        activeField.positionCaret(activeField.getText().length());
    }

    private void showDocument(String title, Path documentPath, String fallbackContent) {
        String content = formatDocumentForDisplay(readDocument(documentPath, fallbackContent));

        Label heading = new Label(title);
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        TextArea document = new TextArea(content);
        document.setEditable(false);
        document.setWrapText(true);
        document.setPrefWidth(720);
        document.setPrefHeight(460);
        document.setStyle("-fx-font-size: 14px; -fx-text-fill: #0f172a;");

        Button close = new Button(I18n.t("ok"));
        close.getStyleClass().add("login-primary-button");
        close.setPrefWidth(110);

        HBox actions = new HBox(close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(18, heading, document, actions);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff;");

        Stage dialog = new Stage();
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null && view.getScene().getWindow() != null) {
            dialog.initOwner(view.getScene().getWindow());
        }
        close.setOnAction(event -> dialog.close());

        Scene scene = new Scene(root, 760, 580);
        if (LoginController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(LoginController.class.getResource("/styles/app.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private String readDocument(Path path, String fallbackContent) {
        try {
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
        return fallbackContent;
    }

    private String formatDocumentForDisplay(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.lines()
                .map(line -> line
                        .replaceFirst("^#{1,6}\\s*", "")
                        .replace("`", "")
                        .replace("**", "")
                        .replaceAll("^[-*]\\s+", "• "))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private void showResetPasswordDialog() {
        TextField resetUsername = registrationField(I18n.t("university_id"));
        resetUsername.setText(usernameField.getText());
        TextField verificationCode = registrationField(I18n.t("verification_code"));
        PasswordField newPassword = registrationPasswordField(I18n.t("new_password"));
        PasswordField confirmPassword = registrationPasswordField(I18n.t("confirm_password"));

        VBox fields = new VBox(14,
                requiredLabeledControl(I18n.t("university_id"), buildInputShell(null, resetUsername, null)),
                requiredLabeledControl(I18n.t("verification_code"), buildInputShell(null, verificationCode, null)),
                requiredLabeledControl(I18n.t("new_password"), buildInputShell(null, newPassword, null)),
                requiredLabeledControl(I18n.t("confirm_password"), buildInputShell(null, confirmPassword, null))
        );

        Label heading = new Label(I18n.t("reset_password_title"));
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Button cancel = new Button(I18n.t("cancel"));
        cancel.getStyleClass().add("login-secondary-button");
        cancel.setPrefWidth(110);

        Button reset = new Button(I18n.t("reset"));
        reset.getStyleClass().add("login-primary-button");
        reset.setPrefWidth(130);

        HBox actions = new HBox(12, cancel, reset);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(22, heading, fields, actions);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff;");

        Stage dialog = new Stage();
        dialog.setTitle(I18n.t("reset_password_title"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null && view.getScene().getWindow() != null) {
            dialog.initOwner(view.getScene().getWindow());
        }
        cancel.setOnAction(event -> dialog.close());
        reset.setOnAction(event -> resetPassword(resetUsername, verificationCode, newPassword, confirmPassword, dialog));

        Scene scene = new Scene(root, 460, 520);
        if (LoginController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(LoginController.class.getResource("/styles/app.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void resetPassword(TextField resetUsername,
                               TextField verificationCode,
                               PasswordField newPassword,
                               PasswordField confirmPassword,
                               Stage dialog) {
        String username = trim(resetUsername.getText());
        String code = trim(verificationCode.getText());
        String password = trim(newPassword.getText());
        String confirm = trim(confirmPassword.getText());

        if (username.isBlank()) {
            DialogControllerFactory.validationError(I18n.t("username_required_msg"), dialog);
            return;
        }
        User user = services.userRepository().findByUsername(username).orElse(null);
        if (user == null) {
            DialogControllerFactory.validationError(I18n.t("username_not_found"), dialog);
            return;
        }
        if (code.isBlank()) {
            DialogControllerFactory.validationError(I18n.t("verification_required"), dialog);
            return;
        }
        if (!RESET_VERIFICATION_CODE.equals(code)) {
            DialogControllerFactory.validationError(I18n.t("verification_incorrect"), dialog);
            return;
        }
        if (password.isBlank()) {
            DialogControllerFactory.validationError(I18n.t("new_password_required"), dialog);
            return;
        }
        String passwordError = validatePasswordStrength(password);
        if (passwordError != null) {
            DialogControllerFactory.validationError(passwordError, dialog);
            return;
        }
        if (!password.equals(confirm)) {
            DialogControllerFactory.validationError(I18n.t("password_mismatch_msg"), dialog);
            return;
        }

        user.setPasswordHash(PasswordUtils.sha256(password));
        services.userRepository().save(user);
        usernameField.setText(username);
        passwordField.clear();
        dialog.close();
        DialogControllerFactory.success(
                I18n.t("reset_password_title"),
                I18n.t("password_reset_success"),
                view.getScene() == null ? null : view.getScene().getWindow()
        );
    }

    private void showRegistrationDialog() {
        TextField registerUsername = registrationField(I18n.t("university_id"));
        PasswordField registerPassword = registrationPasswordField(I18n.t("current_password"));
        PasswordField confirmPassword = registrationPasswordField(I18n.t("confirm_password"));
        TextField displayName = registrationField(I18n.t("full_name"));
        TextField studentId = registrationField(I18n.t("student_id"));
        TextField programme = registrationField(I18n.t("major_programme"));
        TextField email = registrationField(I18n.t("email_address"));
        TextField phone = registrationField(I18n.t("phone_number"));
        ComboBox<String> campus = new ComboBox<>();
        campus.getItems().setAll(Job.ALLOWED_CAMPUSES);
        campus.setPromptText(I18n.t("select_campus"));
        campus.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Integer> academicYear = new ComboBox<>();
        academicYear.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8);
        academicYear.setPromptText(I18n.t("select_academic_year"));
        academicYear.setMaxWidth(Double.MAX_VALUE);

        TextField title = registrationField(I18n.t("title_label"));
        TextField department = registrationField(I18n.t("department_label"));
        TextField contactEmail = registrationField(I18n.t("contact_email"));

        ChoiceBox<Role> roleChoice = new ChoiceBox<>();
        roleChoice.getItems().setAll(Role.TA, Role.MO);
        roleChoice.setValue(Role.TA);
        roleChoice.getStyleClass().add("status-selector");
        roleChoice.setMaxWidth(Double.MAX_VALUE);

        VBox taFields = new VBox(14,
                requiredLabeledControl(I18n.t("student_id"), buildInputShell(null, studentId, null)),
                requiredLabeledControl(I18n.t("email_address"), buildInputShell(null, email, null)),
                requiredLabeledControl(I18n.t("select_academic_year"), academicYear),
                requiredLabeledControl(I18n.t("major_programme"), buildInputShell(null, programme, null)),
                requiredLabeledControl(I18n.t("select_campus"), campus),
                labeledControl(I18n.t("phone_number"), buildInputShell(null, phone, null))
        );

        VBox moFields = new VBox(14,
                requiredLabeledControl(I18n.t("title_label"), buildInputShell(null, title, null)),
                requiredLabeledControl(I18n.t("department_label"), buildInputShell(null, department, null)),
                requiredLabeledControl(I18n.t("contact_email"), buildInputShell(null, contactEmail, null))
        );
        moFields.setVisible(false);
        moFields.setManaged(false);
        roleChoice.valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean isTa = newValue == Role.TA;
            taFields.setVisible(isTa);
            taFields.setManaged(isTa);
            moFields.setVisible(!isTa);
            moFields.setManaged(!isTa);
        });

        VBox fields = new VBox(14,
                requiredLabeledControl(I18n.t("university_id"), buildInputShell(null, registerUsername, null)),
                requiredLabeledControl(I18n.t("current_password"), buildInputShell(null, registerPassword, null)),
                requiredLabeledControl(I18n.t("confirm_password"), buildInputShell(null, confirmPassword, null)),
                requiredLabeledControl(I18n.t("full_name"), buildInputShell(null, displayName, null)),
                requiredLabeledControl(I18n.t("select_role"), roleChoice),
                taFields,
                moFields
        );

        Label heading = new Label(I18n.t("create_account_title"));
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Button cancel = new Button(I18n.t("cancel"));
        cancel.getStyleClass().add("login-secondary-button");
        cancel.setPrefWidth(110);

        Button create = new Button(I18n.t("register"));
        create.getStyleClass().add("login-primary-button");
        create.setPrefWidth(130);

        HBox actions = new HBox(12, cancel, create);
        actions.setAlignment(Pos.CENTER_RIGHT);

        ScrollPane scroll = new ScrollPane(fields);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox root = new VBox(22, heading, scroll, actions);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff;");

        Stage dialog = new Stage();
        dialog.setTitle(I18n.t("register"));
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (view.getScene() != null && view.getScene().getWindow() != null) {
            dialog.initOwner(view.getScene().getWindow());
        }
        cancel.setOnAction(event -> dialog.close());
        create.setOnAction(event -> registerUser(
                registerUsername,
                registerPassword,
                confirmPassword,
                displayName,
                roleChoice,
                studentId,
                programme,
                email,
                phone,
                campus,
                academicYear,
                title,
                department,
                contactEmail,
                dialog
        ));

        Scene scene = new Scene(root, 520, 720);
        if (LoginController.class.getResource("/styles/app.css") != null) {
            scene.getStylesheets().add(LoginController.class.getResource("/styles/app.css").toExternalForm());
        }
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private TextField registrationField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("login-input-field");
        field.setStyle("-fx-prompt-text-fill: #64748b; -fx-text-fill: #0f172a;");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private PasswordField registrationPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().add("login-input-field");
        field.setStyle("-fx-prompt-text-fill: #64748b; -fx-text-fill: #0f172a;");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private VBox labeledControl(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("login-field-label");
        return new VBox(7, label, control);
    }

    private VBox requiredLabeledControl(String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("login-field-label");
        Label required = new Label("*");
        required.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #dc2626;");
        HBox row = new HBox(4, label, required);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(7, row, control);
    }

    private void registerUser(TextField registerUsername,
                              PasswordField registerPassword,
                              PasswordField confirmPassword,
                              TextField displayName,
                              ChoiceBox<Role> roleChoice,
                              TextField studentId,
                              TextField programme,
                              TextField email,
                              TextField phone,
                              ComboBox<String> campus,
                              ComboBox<Integer> academicYear,
                              TextField title,
                              TextField department,
                              TextField contactEmail,
                              Stage dialog) {
        String username = trim(registerUsername.getText());
        String password = trim(registerPassword.getText());
        String confirm = trim(confirmPassword.getText());
        String name = trim(displayName.getText());
        Role role = roleChoice.getValue();

        String validationError = validateRegistration(
                username,
                password,
                confirm,
                name,
                role,
                studentId,
                programme,
                email,
                phone,
                campus,
                academicYear,
                title,
                department,
                contactEmail
        );
        if (validationError != null) {
            DialogControllerFactory.validationError(validationError, dialog);
            return;
        }

        User user = new User();
        user.setUserId(nextUserId(role));
        user.setUsername(username);
        user.setPasswordHash(PasswordUtils.sha256(password));
        user.setRole(role);
        user.setDisplayName(name);
        user.setActive(true);
        if (role == Role.MO) {
            user.setTitle(trim(title.getText()));
            user.setDepartment(trim(department.getText()));
            user.setContactEmail(trim(contactEmail.getText()));
        }
        services.userRepository().save(user);

        if (role == Role.TA) {
            ApplicantProfile profile = new ApplicantProfile();
            profile.setApplicantId(nextApplicantId());
            profile.setUserId(user.getUserId());
            profile.setFullName(name);
            profile.setStudentId(trim(studentId.getText()));
            profile.setProgramme(trim(programme.getText()));
            profile.setEmail(trim(email.getText()));
            profile.setPhone(trim(phone.getText()));
            profile.setCampus(campus.getValue());
            profile.setYear(academicYear.getValue());
            profile.setLastUpdated(DateTimeUtils.now());
            services.applicantProfileRepository().save(profile);
        }

        usernameField.setText(username);
        passwordField.clear();
        dialog.close();
        DialogControllerFactory.success(
                I18n.t("registration_success"),
                I18n.t("registration_success_msg"),
                view.getScene() == null ? null : view.getScene().getWindow()
        );
    }

    private String validateRegistration(String username,
                                        String password,
                                        String confirm,
                                        String displayName,
                                        Role role,
                                        TextField studentId,
                                        TextField programme,
                                        TextField email,
                                        TextField phone,
                                        ComboBox<String> campus,
                                        ComboBox<Integer> academicYear,
                                        TextField title,
                                        TextField department,
                                        TextField contactEmail) {
        if (username.isBlank()) {
            return I18n.t("username_required_v");
        }
        if (!username.matches("^[A-Za-z0-9_]{3,30}$")) {
            return I18n.t("username_format");
        }
        if (services.userRepository().findByUsername(username).isPresent()) {
            return I18n.t("username_exists");
        }
        if (password.isBlank()) {
            return I18n.t("password_required_v");
        }
        String passwordError = validatePasswordStrength(password);
        if (passwordError != null) {
            return passwordError;
        }
        if (!password.equals(confirm)) {
            return I18n.t("password_mismatch_msg");
        }
        if (displayName.isBlank()) {
            return I18n.t("fullname_required");
        }
        if (displayName.length() < 2 || displayName.length() > 60) {
            return I18n.t("fullname_format");
        }
        if (!displayName.matches("^[A-Za-z\\p{IsHan} .'-]{2,60}$")) {
            return I18n.t("fullname_format");
        }
        if (role != Role.TA && role != Role.MO) {
            return I18n.t("select_role");
        }
        if (role == Role.TA) {
            String studentIdValue = trim(studentId.getText());
            String emailValue = trim(email.getText());
            String programmeValue = trim(programme.getText());
            String phoneValue = trim(phone.getText());
            if (studentIdValue.isBlank()) {
                return I18n.t("studentid_required");
            }
            if (!studentIdValue.matches("^\\d{6,20}$")) {
                return I18n.t("studentid_format");
            }
            if (isStudentIdTaken(studentIdValue)) {
                return I18n.t("studentid_required");
            }
            if (emailValue.isBlank()) {
                return I18n.t("email_required");
            }
            if (!isValidEmail(emailValue)) {
                return I18n.t("email_invalid");
            }
            if (isEmailTaken(emailValue)) {
                return I18n.t("email_required");
            }
            if (academicYear.getValue() == null) {
                return I18n.t("academic_year_required");
            }
            if (programmeValue.isBlank()) {
                return I18n.t("major_required");
            }
            if (programmeValue.length() < 2 || programmeValue.length() > 80) {
                return I18n.t("major_required");
            }
            if (campus.getValue() == null || campus.getValue().isBlank()) {
                return I18n.t("campus_required");
            }
            if (!phoneValue.isBlank() && !phoneValue.matches("^[0-9+\\-]{6,20}$")) {
                return I18n.t("phone_number");
            }
        }
        if (role == Role.MO) {
            String titleValue = trim(title.getText());
            String departmentValue = trim(department.getText());
            String contactEmailValue = trim(contactEmail.getText());
            if (titleValue.isBlank()) {
                return I18n.t("title_required");
            }
            if (titleValue.length() < 2 || titleValue.length() > 60) {
                return I18n.t("title_required");
            }
            if (departmentValue.isBlank()) {
                return I18n.t("dept_required");
            }
            if (departmentValue.length() < 2 || departmentValue.length() > 80) {
                return I18n.t("dept_required");
            }
            if (contactEmailValue.isBlank()) {
                return I18n.t("contact_email_required");
            }
            if (!isValidEmail(contactEmailValue)) {
                return I18n.t("email_invalid");
            }
            if (isEmailTaken(contactEmailValue)) {
                return I18n.t("contact_email_required");
            }
        }
        return null;
    }

    private boolean isValidEmail(String value) {
        return value != null && value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isStudentIdTaken(String studentId) {
        return services.applicantProfileRepository().findAll().stream()
                .anyMatch(profile -> profile.getStudentId() != null
                        && profile.getStudentId().equalsIgnoreCase(studentId));
    }

    private boolean isEmailTaken(String email) {
        boolean profileEmailTaken = services.applicantProfileRepository().findAll().stream()
                .anyMatch(profile -> profile.getEmail() != null
                        && profile.getEmail().equalsIgnoreCase(email));
        boolean userEmailTaken = services.userRepository().findAll().stream()
                .anyMatch(user -> user.getContactEmail() != null
                        && user.getContactEmail().equalsIgnoreCase(email));
        return profileEmailTaken || userEmailTaken;
    }

    private String validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return I18n.t("password_length");
        }
        if (!password.matches(".*[A-Z].*")) {
            return I18n.t("password_uppercase");
        }
        if (!password.matches(".*[a-z].*")) {
            return I18n.t("password_lowercase");
        }
        if (!password.matches(".*\\d.*")) {
            return I18n.t("password_number");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return I18n.t("password_special");
        }
        return null;
    }

    private String nextUserId(Role role) {
        int base = role == Role.MO ? 100 : 0;
        int ceiling = role == Role.MO ? 899 : 99;
        int max = services.userRepository().findAll().stream()
                .map(User::getUserId)
                .mapToInt(this::parseUserNumber)
                .filter(number -> number > base && number <= ceiling)
                .max()
                .orElse(base);
        return String.format("U%03d", max + 1);
    }

    private int parseUserNumber(String userId) {
        if (userId == null || !userId.matches("U\\d{3}")) {
            return -1;
        }
        return Integer.parseInt(userId.substring(1));
    }

    private String nextApplicantId() {
        return IdGenerator.next(
                "A",
                services.applicantProfileRepository().findAll().stream()
                        .map(ApplicantProfile::getApplicantId)
                        .toList(),
                3
        );
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultHelpDocument() {
        return """
                BUPT International School TA Recruitment System

                Login
                - Enter your university username and password.
                - Use the eye button to show or hide the password.
                - Successful login routes you to the role-specific homepage.

                Sample accounts
                - TA: ta001 / ta002 / ta003 / ta004 / ta005
                - MO: mo001 / mo002
                - Admin: admin
                - Password: Password123!
                """;
    }

    private String defaultPrivacyDocument() {
        return """
                Privacy Notice

                This desktop prototype stores user, job, application, profile, and CV metadata locally in JSON files under the application data directory.

                The system uses account credentials only for local authentication in this prototype. Passwords are stored as SHA-256 hashes, not as plain text.

                Uploaded CV files and application records are used only for TA recruitment workflows inside this application.
                """;
    }

    private void doLogin() {
        LoginResult result = services.authenticationService().login(usernameField.getText(), passwordField.getText());
        if (!result.success()) {
            DialogControllerFactory.operationFailed(I18n.t("login_to_portal"), result.message(), view.getScene() == null
                    ? null : view.getScene().getWindow());
            return;
        }
        onLoginSuccess.accept(result.user());
    }
}
