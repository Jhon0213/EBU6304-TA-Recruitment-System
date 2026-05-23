package edu.bupt.ta.controller;

import edu.bupt.ta.util.I18n;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public class DialogControllerFactory {

    private static final String STYLESHEET = "/styles/app.css";

    static {
        I18n.initTranslations();
    }

    private DialogControllerFactory() {
    }

    public static void validationError(String message, Window owner) {
        Alert alert = create(Alert.AlertType.ERROR, I18n.t("validation_error"), I18n.t("correct_input"),
                message, owner, "ta-dialog-error");
        alert.showAndWait();
    }

    public static void operationFailed(String title, String message, Window owner) {
        Alert alert = create(Alert.AlertType.ERROR, title, I18n.t("operation_failed"), message, owner, "ta-dialog-error");
        alert.showAndWait();
    }

    public static void success(String title, String message, Window owner) {
        Alert alert = create(Alert.AlertType.INFORMATION, title, I18n.t("operation_completed_dlg"), message, owner,
                "ta-dialog-success");
        alert.showAndWait();
    }

    public static void info(String title, String message, Window owner) {
        Alert alert = create(Alert.AlertType.INFORMATION, title, null, message, owner, "ta-dialog-info");
        alert.showAndWait();
    }

    public static void permissionDenied(String message, Window owner) {
        Alert alert = create(Alert.AlertType.WARNING, I18n.t("permission_denied"),
                I18n.t("no_permission_action"), message, owner, "ta-dialog-warning");
        alert.showAndWait();
    }

    public static void workloadWarning(String message, Window owner) {
        Alert alert = create(Alert.AlertType.WARNING, I18n.t("workload_warn_title"), I18n.t("workload_warn_msg"),
                message, owner, "ta-dialog-warning");
        alert.showAndWait();
    }

    public static boolean confirmAction(String title, String message, Window owner) {
        ButtonType confirm = new ButtonType(I18n.t("confirm"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.t("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, cancel);
        setup(alert, title, I18n.t("confirm_action"), owner, "ta-dialog-info");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    private static Alert create(Alert.AlertType type, String title, String header, String message, Window owner,
                                String styleClass) {
        ButtonType ok = new ButtonType(I18n.t("ok"), ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(type, message, ok);
        setup(alert, title, header, owner, styleClass);
        return alert;
    }

    private static void setup(Alert alert, String title, String header, Window owner, String styleClass) {
        alert.setTitle(title == null ? I18n.t("notification_title") : title);
        alert.setHeaderText(header);
        if (owner != null) {
            alert.initOwner(owner);
        }
        if (DialogControllerFactory.class.getResource(STYLESHEET) != null) {
            String stylesheet = DialogControllerFactory.class.getResource(STYLESHEET).toExternalForm();
            if (!alert.getDialogPane().getStylesheets().contains(stylesheet)) {
                alert.getDialogPane().getStylesheets().add(stylesheet);
            }
        }
        alert.getDialogPane().getStyleClass().add("ta-dialog");
        if (styleClass != null && !styleClass.isBlank()) {
            alert.getDialogPane().getStyleClass().add(styleClass);
        }
    }
}
