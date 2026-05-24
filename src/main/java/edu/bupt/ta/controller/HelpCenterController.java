package edu.bupt.ta.controller;

import edu.bupt.ta.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class HelpCenterController {

    private final VBox view = new VBox();

    public HelpCenterController() {
        I18n.initTranslations();
        initialize();
    }

    public Parent getView() {
        return view;
    }

    private void initialize() {
        view.getStyleClass().add("app-surface");
        view.setPadding(new Insets(32));
        view.setSpacing(0);
        view.setAlignment(Pos.TOP_LEFT);

        String content = I18n.t("bupt_is_recruitment") + "\n\n"
                + I18n.t("help_login") + "\n"
                + I18n.t("help_login_desc") + "\n\n"
                + I18n.t("help_sample_accounts") + "\n"
                + I18n.t("help_ta_accounts") + "\n"
                + I18n.t("help_mo_accounts") + "\n"
                + I18n.t("help_admin_account") + "\n"
                + I18n.t("help_password") + "\n\n"
                + I18n.t("help_ta_features") + "\n"
                + I18n.t("help_ta_browse") + "\n"
                + I18n.t("help_ta_heart") + "\n"
                + I18n.t("help_ta_apply") + "\n"
                + I18n.t("help_ta_track") + "\n"
                + I18n.t("help_ta_notifications") + "\n"
                + I18n.t("help_ta_profile") + "\n\n"
                + I18n.t("help_mo_features") + "\n"
                + I18n.t("help_mo_manage") + "\n"
                + I18n.t("help_mo_review") + "\n"
                + I18n.t("help_mo_decide") + "\n"
                + I18n.t("help_mo_workload") + "\n"
                + I18n.t("help_mo_notify") + "\n\n"
                + I18n.t("help_admin_features") + "\n"
                + I18n.t("help_admin_users") + "\n"
                + I18n.t("help_admin_stats") + "\n"
                + I18n.t("help_admin_export") + "\n\n"
                + I18n.t("help_privacy") + "\n"
                + I18n.t("help_privacy_data") + "\n"
                + I18n.t("help_privacy_hash") + "\n"
                + I18n.t("help_privacy_cv");

        Label heading = new Label(I18n.t("help_center"));
        heading.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        TextArea document = new TextArea(content);
        document.setEditable(false);
        document.setWrapText(true);
        document.setPrefHeight(600);
        document.setStyle(
                "-fx-font-size: 14px; " +
                "-fx-text-fill: #334155; " +
                "-fx-font-family: 'PingFang SC', 'Inter', 'Noto Sans SC', sans-serif; " +
                "-fx-background-color: #ffffff; " +
                "-fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 20; " +
                "-fx-control-inner-background: #ffffff;"
        );
        document.setFont(Font.font("PingFang SC", 14));

        VBox container = new VBox(24, heading, document);
        container.setFillWidth(true);
        container.setMaxWidth(800);
        container.setPrefWidth(800);

        view.getChildren().add(container);
    }
}
