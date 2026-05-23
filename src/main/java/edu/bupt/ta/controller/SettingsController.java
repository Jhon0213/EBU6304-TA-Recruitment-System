package edu.bupt.ta.controller;

import edu.bupt.ta.model.User;
import edu.bupt.ta.model.UserSettings;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class SettingsController {

    private final ServiceRegistry services;
    private final User user;
    private final Stage ownerStage;
    private final Consumer<String> onLanguageChangeRequest;

    private final BorderPane view = new BorderPane();
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;
    private TextField newUsernameField;
    private RadioButton langEn;
    private RadioButton langZh;

    private boolean isZh;
    private String pendingLanguage;

    public SettingsController(ServiceRegistry services, User user, Stage ownerStage, Consumer<String> onLanguageChangeRequest) {
        this.services = services;
        this.user = user;
        this.ownerStage = ownerStage;
        this.onLanguageChangeRequest = onLanguageChangeRequest;
        I18n.initTranslations();
        isZh = I18n.getLanguage().equals(I18n.ZH);
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");

        VBox page = new VBox(24);
        page.setPadding(new Insets(24));
        page.setFillWidth(true);
        page.setMaxWidth(Double.MAX_VALUE);
        page.setMinWidth(0);

        VBox headerBlock = new VBox(4);
        Label heading = new Label(I18n.t("settings"));
        heading.getStyleClass().add("page-title");

        Label subtitle = new Label(I18n.t("settings_subtitle"));
        subtitle.getStyleClass().add("body-muted");
        subtitle.setStyle("-fx-font-size: 16px;");

        headerBlock.getChildren().addAll(heading, subtitle);

        VBox content = new VBox(20);
        content.getChildren().addAll(
                buildLanguageSection(),
                buildUsernameSection(),
                buildPasswordSection()
        );

        page.getChildren().addAll(headerBlock, content);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        view.setCenter(scrollPane);
    }

    private VBox buildSectionCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("cv-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(20));
        return card;
    }

    private VBox buildLanguageSection() {
        VBox card = buildSectionCard();

        Label title = new Label(I18n.t("language"));
        title.getStyleClass().add("cv-card-title");

        HBox options = new HBox(16);
        options.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup langGroup = new ToggleGroup();
        langEn = new RadioButton(I18n.t("english"));
        langZh = new RadioButton(I18n.t("chinese"));
        langEn.setToggleGroup(langGroup);
        langZh.setToggleGroup(langGroup);
        langEn.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
        langZh.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");

        if (I18n.getLanguage().equals(I18n.ZH)) {
            langZh.setSelected(true);
        } else {
            langEn.setSelected(true);
        }

        langEn.setOnAction(e -> {
            if (!isZh) return;
            pendingLanguage = I18n.EN;
            showLanguageChangeConfirmDialog();
        });

        langZh.setOnAction(e -> {
            if (isZh) return;
            pendingLanguage = I18n.ZH;
            showLanguageChangeConfirmDialog();
        });

        options.getChildren().addAll(langEn, langZh);
        card.getChildren().addAll(title, options);
        return card;
    }

    private void showLanguageChangeConfirmDialog() {
        Stage dialog = new Stage();
        dialog.setTitle(I18n.t("language_change_title"));
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label message = new Label(I18n.t("language_change_confirm"));
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155; -fx-wrap-text: true;");
        message.setMaxWidth(340);

        Button yesBtn = new Button(I18n.t("yes_option"));
        yesBtn.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 24; -fx-background-radius: 8; -fx-cursor: hand;");

        Button noBtn = new Button(I18n.t("no_option"));
        noBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 24; -fx-background-radius: 8; -fx-cursor: hand;");

        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.getChildren().addAll(yesBtn, noBtn);

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff;");
        root.setPrefWidth(400);
        root.getChildren().addAll(message, buttonRow);

        yesBtn.setOnAction(e -> {
            dialog.close();
            if (pendingLanguage != null) {
                UserSettings settings = services.userSettingsRepository().getOrCreateGlobal();
                settings.setLanguage(pendingLanguage);
                services.userSettingsRepository().save(settings);
                I18n.setLanguage(pendingLanguage);
                if (onLanguageChangeRequest != null) {
                    onLanguageChangeRequest.accept(pendingLanguage);
                }
            }
        });

        noBtn.setOnAction(e -> {
            dialog.close();
            // Restore radio button selection
            if (isZh) {
                langZh.setSelected(true);
            } else {
                langEn.setSelected(true);
            }
        });

        dialog.setScene(new javafx.scene.Scene(root, 400, 160));
        dialog.showAndWait();
    }

    private VBox buildUsernameSection() {
        VBox card = buildSectionCard();

        Label title = new Label(I18n.t("change_username"));
        title.getStyleClass().add("cv-card-title");

        Label currentLabel = new Label(I18n.t("current_username"));
        currentLabel.getStyleClass().add("cv-meta-label");

        Label currentValue = new Label(user.getUsername());
        currentValue.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");

        Label newUserLabel = new Label(I18n.t("new_username"));
        newUserLabel.getStyleClass().add("cv-meta-label");

        newUsernameField = new TextField();
        newUsernameField.setPromptText(I18n.t("new_username"));
        newUsernameField.setMaxWidth(400);
        newUsernameField.setPrefWidth(400);
        newUsernameField.getStyleClass().add("form-input");

        Button saveUsername = new Button(I18n.t("save"));
        saveUsername.getStyleClass().add("cv-primary-button");
        saveUsername.setOnAction(e -> saveUsername());

        HBox row = new HBox(12, newUsernameField, saveUsername);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox currentBlock = new VBox(4, currentLabel, currentValue);

        card.getChildren().addAll(title, currentBlock, newUserLabel, row);
        return card;
    }

    private VBox buildPasswordSection() {
        VBox card = buildSectionCard();

        Label title = new Label(I18n.t("change_password"));
        title.getStyleClass().add("cv-card-title");

        VBox curBlock = buildPasswordField(I18n.t("current_password"), currentPasswordField = new PasswordField());
        VBox newBlock = buildPasswordField(I18n.t("new_password"), newPasswordField = new PasswordField());
        VBox confirmBlock = buildPasswordField(I18n.t("confirm_password"), confirmPasswordField = new PasswordField());

        Button savePassword = new Button(I18n.t("save"));
        savePassword.getStyleClass().add("cv-primary-button");
        savePassword.setOnAction(e -> savePassword());

        card.getChildren().addAll(title, curBlock, newBlock, confirmBlock, savePassword);
        return card;
    }

    private VBox buildPasswordField(String labelText, PasswordField field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("cv-meta-label");

        field.setPromptText(labelText);
        field.setMaxWidth(400);
        field.setPrefWidth(400);
        field.getStyleClass().add("form-input");

        return new VBox(4, label, field);
    }

    private void saveUsername() {
        String newUsername = newUsernameField.getText();
        if (newUsername == null || newUsername.isBlank()) {
            showMsg(I18n.t("error"), I18n.t("username_required"));
            return;
        }
        String result = services.authenticationService().updateUsername(newUsername);
        if (result != null) {
            showMsg(I18n.t("error"), result);
        } else {
            showMsg(I18n.t("success"), I18n.t("update_success"));
            newUsernameField.clear();
        }
    }

    private void savePassword() {
        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (!newPass.equals(confirm)) {
            showMsg(I18n.t("error"), I18n.t("password_mismatch"));
            return;
        }
        String result = services.authenticationService().updatePassword(current, newPass);
        if (result != null) {
            showMsg(I18n.t("error"), result);
        } else {
            showMsg(I18n.t("success"), I18n.t("settings_saved"));
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        }
    }

    private void showMsg(String title, String msg) {
        Label msgLabel = new Label(msg);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
        Button ok = new Button("OK");
        ok.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 24; -fx-background-radius: 8; -fx-cursor: hand;");

        Stage dialog = new Stage();
        dialog.setTitle(title);
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(16, msgLabel, ok);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff;");
        root.setAlignment(Pos.CENTER);

        ok.setOnAction(e -> dialog.close());
        dialog.setScene(new javafx.scene.Scene(root, 380, 160));
        dialog.showAndWait();
    }
}
