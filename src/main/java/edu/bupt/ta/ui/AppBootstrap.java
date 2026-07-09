package edu.bupt.ta.ui;

import edu.bupt.ta.controller.LoginController;
import edu.bupt.ta.controller.MainShellController;
import edu.bupt.ta.model.User;
import edu.bupt.ta.service.ServiceRegistry;
import edu.bupt.ta.util.I18n;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.InputStream;

public class AppBootstrap {

    private static final double LOGIN_WIDTH = 1180;
    private static final double LOGIN_HEIGHT = 760;
    private static final double LOGIN_MIN_WIDTH = 1180;
    private static final double LOGIN_MIN_HEIGHT = 760;
    private static final double MAIN_WIDTH = 1280;
    private static final double MAIN_HEIGHT = 860;
    private static final double MAIN_MIN_WIDTH = 1280;
    private static final double MAIN_MIN_HEIGHT = 860;

    private final ServiceRegistry services = new ServiceRegistry();
    private final StackPane root = new StackPane();

    private String storedUsername;

    public Scene createInitialScene() {
        loadBundledFonts();
        loadUserSettings();
        showLogin();
        Scene scene = new Scene(root, LOGIN_WIDTH, LOGIN_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        applyFontSize(scene);
        Platform.runLater(() -> resizeWindow(LOGIN_WIDTH, LOGIN_HEIGHT, LOGIN_MIN_WIDTH, LOGIN_MIN_HEIGHT));
        return scene;
    }

    public void reloadWithLanguageChange(String username) {
        this.storedUsername = username;
        showLogin();
        Platform.runLater(() -> {
            if (root.getScene() != null) {
                applyFontSize(root.getScene());
                resizeWindow(LOGIN_WIDTH, LOGIN_HEIGHT, LOGIN_MIN_WIDTH, LOGIN_MIN_HEIGHT);
                LoginController loginController = new LoginController(services, this::showMainShell);
                loginController.prefillUsername(username);
            }
        });
    }

    private void loadBundledFonts() {
        loadBundledFont("/fonts/Inter-Regular.ttf");
        loadBundledFont("/fonts/Inter-SemiBold.ttf");
        loadBundledFont("/fonts/Inter-Black.ttf");
    }

    private void loadBundledFont(String resourcePath) {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream != null) {
                Font.loadFont(stream, 12);
            }
        } catch (Exception ignored) {
            // If loading fails, JavaFX falls back to the font stack in app.css.
        }
    }

    private void loadUserSettings() {
        I18n.initTranslations();
        var settings = services.userSettingsRepository().getOrCreateGlobal();
        String savedLang = settings.getLanguage();
        if (I18n.ZH.equals(savedLang)) {
            I18n.setLanguage(I18n.ZH);
        } else {
            I18n.setLanguage(I18n.EN);
        }
    }

    private void applyFontSize(Scene scene) {
        var settings = services.userSettingsRepository().getOrCreateGlobal();
        if (settings.getFontSize() > 0) {
            scene.getRoot().setStyle(String.format("-fx-font-size: %.0fpx;", settings.getFontSize()));
        }
    }

    private void showLogin() {
        LoginController loginController = new LoginController(services, this::showMainShell);
        root.getChildren().setAll(loginController.getView());
        resizeWindow(LOGIN_WIDTH, LOGIN_HEIGHT, LOGIN_MIN_WIDTH, LOGIN_MIN_HEIGHT);
    }

    private void showMainShell(User user) {
        MainShellController shellController = new MainShellController(services, user, () -> {
            services.authenticationService().logout();
            showLogin();
        }, this);
        root.getChildren().setAll(shellController.getView());
        if (root.getScene() != null) {
            applyFontSize(root.getScene());
        }
        resizeWindow(MAIN_WIDTH, MAIN_HEIGHT, MAIN_MIN_WIDTH, MAIN_MIN_HEIGHT);
    }

    private void resizeWindow(double width, double height, double minWidth, double minHeight) {
        if (root.getScene() == null || !(root.getScene().getWindow() instanceof Stage stage)) {
            return;
        }
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        }
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();
    }
}
