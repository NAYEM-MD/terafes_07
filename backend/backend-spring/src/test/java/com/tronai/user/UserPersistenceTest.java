package com.tronai.user;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Flyway migrations load successfully")
    void flywayMigrationLoadsSuccessfully() {
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(Integer.parseInt(current.getVersion().getVersion())).isGreaterThanOrEqualTo(10);
        assertThat(current.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("UserRepository persists and retrieves a user by email")
    void userPersistenceWorks() {
        User user = User.builder()
                .email("test.engineer@tronai.dev")
                .displayName("Tron Engineer")
                .passwordHash("hashed_secret_123")
                .build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(userRepository.existsByEmail("test.engineer@tronai.dev")).isTrue();

        Optional<User> found = userRepository.findByEmail("test.engineer@tronai.dev");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Tron Engineer");
    }

    @Test
    @DisplayName("UserSettings relation cascades and persists settings correctly")
    void userSettingsRelationWorks() {
        User user = User.builder()
                .email("settings.test@tronai.dev")
                .displayName("Settings User")
                .build();

        UserSettings settings = UserSettings.builder()
                .preferredLanguage("ja")
                .preferredModel("qwen3:8b")
                .build();

        user.setSettings(settings);

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getSettings()).isNotNull();
        assertThat(savedUser.getSettings().getId()).isNotNull();
        assertThat(savedUser.getSettings().getPreferredLanguage()).isEqualTo("ja");
        assertThat(savedUser.getSettings().getPreferredModel()).isEqualTo("qwen3:8b");

        Optional<UserSettings> foundSettings = userSettingsRepository.findByUserId(savedUser.getId());
        assertThat(foundSettings).isPresent();
        assertThat(foundSettings.get().getUser().getEmail()).isEqualTo("settings.test@tronai.dev");
    }
}
