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
        initialize();
    }

    public Parent getView() {
        return view;
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

        Label brandTitle = new Label("BUPT International School");
        brandTitle.getStyleClass().add("login-brand-title");

        brandRow.getChildren().addAll(brandIcon, brandTitle);

        Label hero = new Label("Teaching Assistant\nRecruitment System");
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

        Label secure = new Label("Secure Academic Portal for Students & Faculty");
        secure.getStyleClass().add("login-left-meta");
        secureRow.getChildren().addAll(secureIcon, secure);

        Label copyright = new Label("© 2026 Beijing University of Posts and Telecommunications. All rights reserved.");
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

        Label heading = new Label("Portal Login");
        heading.getStyleClass().add("login-heading");

        Label subtitle = new Label("Enter your university credentials to continue");
        subtitle.getStyleClass().add("login-subheading");

        VBox titleBlock = new VBox(8, heading, subtitle);

        Label userLabel = new Label("University ID / Username");
        userLabel.getStyleClass().add("login-field-label");
        usernameField.setPromptText("e.g. 2023211000");
        usernameField.getStyleClass().add("login-input-field");
        HBox usernameInput = buildInputShell(
                IconFactory.glyph(IconFactory.IconType.USER, 13, Color.web("#94a3b8")),
                usernameField,
                null
        );
        VBox usernameBlock = new VBox(8, userLabel, usernameInput);

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("login-field-label");

        Button forgotButton = new Button("Forgot password?");
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

        Button loginButton = new Button("LOGIN TO PORTAL");
        loginButton.getStyleClass().add("login-primary-button");
        loginButton.setPrefHeight(44);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> doLogin());

        Button resetButton = new Button("Reset");
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

        Button registerLink = footerLink("REGISTER");
        registerLink.setOnAction(event -> showRegistrationDialog());

        Button support = footerLink("HELP CENTER");
        support.setOnAction(event -> showDocument("Help Center", Path.of("docs", "User-Manual.md"), defaultHelpDocument()));

        Button privacy = footerLink("PRIVACY");
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

        Button close = new Button("OK");
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
            // Fall back to the bundled summary below when the local document is unavailable.
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
        TextField resetUsername = registrationField("Username");
        resetUsername.setText(usernameField.getText());
        TextField verificationCode = registrationField("Verification code");
        PasswordField newPassword = registrationPasswordField("New password");
        PasswordField confirmPassword = registrationPasswordField("Confirm password");

        VBox fields = new VBox(14,
                requiredLabeledControl("Username", buildInputShell(null, resetUsername, null)),
                requiredLabeledControl("Verification Code", buildInputShell(null, verificationCode, null)),
                requiredLabeledControl("New Password", buildInputShell(null, newPassword, null)),
                requiredLabeledControl("Confirm Password", buildInputShell(null, confirmPassword, null))
        );

        Label heading = new Label("Reset Password");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("login-secondary-button");
        cancel.setPrefWidth(110);

        Button reset = new Button("Reset");
        reset.getStyleClass().add("login-primary-button");
        reset.setPrefWidth(130);

        HBox actions = new HBox(12, cancel, reset);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(22, heading, fields, actions);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff;");

        Stage dialog = new Stage();
        dialog.setTitle("Reset Password");
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
            DialogControllerFactory.validationError("Username is required.", dialog);
            return;
        }
        User user = services.userRepository().findByUsername(username).orElse(null);
        if (user == null) {
            DialogControllerFactory.validationError("Username does not exist.", dialog);
            return;
        }
        if (code.isBlank()) {
            DialogControllerFactory.validationError("Verification code is required.", dialog);
            return;
        }
        if (!RESET_VERIFICATION_CODE.equals(code)) {
            DialogControllerFactory.validationError("Verification code is incorrect.", dialog);
            return;
        }
        if (password.isBlank()) {
            DialogControllerFactory.validationError("New password is required.", dialog);
            return;
        }
        String passwordError = validatePasswordStrength(password);
        if (passwordError != null) {
            DialogControllerFactory.validationError(passwordError, dialog);
            return;
        }
        if (!password.equals(confirm)) {
            DialogControllerFactory.validationError("Password and confirmation do not match.", dialog);
            return;
        }

        user.setPasswordHash(PasswordUtils.sha256(password));
        services.userRepository().save(user);
        usernameField.setText(username);
        passwordField.clear();
        dialog.close();
        DialogControllerFactory.success(
                "Password Reset",
                "Password has been updated. Please log in with the new password.",
                view.getScene() == null ? null : view.getScene().getWindow()
        );
    }

    private void showRegistrationDialog() {
        TextField registerUsername = registrationField("Username");
        PasswordField registerPassword = registrationPasswordField("Password");
        PasswordField confirmPassword = registrationPasswordField("Confirm password");
        TextField displayName = registrationField("Full name");
        TextField studentId = registrationField("Student ID");
        TextField programme = registrationField("Major / programme");
        TextField email = registrationField("Email address");
        TextField phone = registrationField("Phone number");
        ComboBox<String> campus = new ComboBox<>();
        campus.getItems().setAll(Job.ALLOWED_CAMPUSES);
        campus.setPromptText("Select campus");
        campus.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Integer> academicYear = new ComboBox<>();
        academicYear.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8);
        academicYear.setPromptText("Select academic year");
        academicYear.setMaxWidth(Double.MAX_VALUE);

        TextField title = registrationField("Title");
        TextField department = registrationField("Department");
        TextField contactEmail = registrationField("Contact email");

        ChoiceBox<Role> roleChoice = new ChoiceBox<>();
        roleChoice.getItems().setAll(Role.TA, Role.MO);
        roleChoice.setValue(Role.TA);
        roleChoice.getStyleClass().add("status-selector");
        roleChoice.setMaxWidth(Double.MAX_VALUE);

        VBox taFields = new VBox(14,
                requiredLabeledControl("Student ID", buildInputShell(null, studentId, null)),
                requiredLabeledControl("Email Address", buildInputShell(null, email, null)),
                requiredLabeledControl("Academic Year", academicYear),
                requiredLabeledControl("Major", buildInputShell(null, programme, null)),
                requiredLabeledControl("Campus", campus),
                labeledControl("Phone Number", buildInputShell(null, phone, null))
        );

        VBox moFields = new VBox(14,
                requiredLabeledControl("Title", buildInputShell(null, title, null)),
                requiredLabeledControl("Department", buildInputShell(null, department, null)),
                requiredLabeledControl("Contact Email", buildInputShell(null, contactEmail, null))
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
                requiredLabeledControl("Username", buildInputShell(null, registerUsername, null)),
                requiredLabeledControl("Password", buildInputShell(null, registerPassword, null)),
                requiredLabeledControl("Confirm Password", buildInputShell(null, confirmPassword, null)),
                requiredLabeledControl("Full Name", buildInputShell(null, displayName, null)),
                requiredLabeledControl("Role", roleChoice),
                taFields,
                moFields
        );

        Label heading = new Label("Create Account");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("login-secondary-button");
        cancel.setPrefWidth(110);

        Button create = new Button("Register");
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
        dialog.setTitle("Register");
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
                "Registration Successful",
                "Account created. Please log in with your new credentials.",
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
            return "Username is required.";
        }
        if (!username.matches("^[A-Za-z0-9_]{3,30}$")) {
            return "Username must be 3-30 characters and contain only letters, numbers, or underscores.";
        }
        if (services.userRepository().findByUsername(username).isPresent()) {
            return "Username already exists.";
        }
        if (password.isBlank()) {
            return "Password is required.";
        }
        String passwordError = validatePasswordStrength(password);
        if (passwordError != null) {
            return passwordError;
        }
        if (!password.equals(confirm)) {
            return "Password and confirmation do not match.";
        }
        if (displayName.isBlank()) {
            return "Full name is required.";
        }
        if (displayName.length() < 2 || displayName.length() > 60) {
            return "Full name must be 2-60 characters.";
        }
        if (!displayName.matches("^[A-Za-z\\p{IsHan} .'-]{2,60}$")) {
            return "Full name can contain only letters, Chinese characters, spaces, dots, hyphens, or apostrophes.";
        }
        if (role != Role.TA && role != Role.MO) {
            return "Please select TA or MO.";
        }
        if (role == Role.TA) {
            String studentIdValue = trim(studentId.getText());
            String emailValue = trim(email.getText());
            String programmeValue = trim(programme.getText());
            String phoneValue = trim(phone.getText());
            if (studentIdValue.isBlank()) {
                return "Student ID is required.";
            }
            if (!studentIdValue.matches("^\\d{6,20}$")) {
                return "Student ID must contain only digits and be 6-20 characters.";
            }
            if (isStudentIdTaken(studentIdValue)) {
                return "Student ID already exists.";
            }
            if (emailValue.isBlank()) {
                return "Email address is required.";
            }
            if (!isValidEmail(emailValue)) {
                return "Email format is invalid.";
            }
            if (isEmailTaken(emailValue)) {
                return "Email address already exists.";
            }
            if (academicYear.getValue() == null) {
                return "Academic year is required.";
            }
            if (programmeValue.isBlank()) {
                return "Major is required.";
            }
            if (programmeValue.length() < 2 || programmeValue.length() > 80) {
                return "Major must be 2-80 characters.";
            }
            if (campus.getValue() == null || campus.getValue().isBlank()) {
                return "Campus is required.";
            }
            if (!phoneValue.isBlank() && !phoneValue.matches("^[0-9+\\-]{6,20}$")) {
                return "Phone number must be 6-20 characters and contain only digits, +, or -.";
            }
        }
        if (role == Role.MO) {
            String titleValue = trim(title.getText());
            String departmentValue = trim(department.getText());
            String contactEmailValue = trim(contactEmail.getText());
            if (titleValue.isBlank()) {
                return "Title is required.";
            }
            if (titleValue.length() < 2 || titleValue.length() > 60) {
                return "Title must be 2-60 characters.";
            }
            if (departmentValue.isBlank()) {
                return "Department is required.";
            }
            if (departmentValue.length() < 2 || departmentValue.length() > 80) {
                return "Department must be 2-80 characters.";
            }
            if (contactEmailValue.isBlank()) {
                return "Contact email is required.";
            }
            if (!isValidEmail(contactEmailValue)) {
                return "Contact email format is invalid.";
            }
            if (isEmailTaken(contactEmailValue)) {
                return "Contact email already exists.";
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
            return "Password must be at least 8 characters.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number.";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Password must contain at least one special character.";
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
            DialogControllerFactory.operationFailed("Login Failed", result.message(), view.getScene() == null
                    ? null : view.getScene().getWindow());
            return;
        }
        onLoginSuccess.accept(result.user());
    }
}
