package edu.bupt.ta.repository;

import edu.bupt.ta.config.AppPaths;
import edu.bupt.ta.model.UserSettings;

import java.util.Optional;

public class UserSettingsRepository extends AbstractJsonRepository<UserSettings, String> {

    public UserSettingsRepository() {
        super(AppPaths.userSettingsJson(), UserSettings.class);
    }

    public Optional<UserSettings> findGlobal() {
        return findById(UserSettings.DEFAULT_ID);
    }

    public UserSettings getOrCreateGlobal() {
        return findGlobal().orElseGet(() -> {
            UserSettings settings = new UserSettings();
            save(settings);
            return settings;
        });
    }
}
