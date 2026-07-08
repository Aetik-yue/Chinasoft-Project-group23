package com.chinasoft.smokesensor.service.impl;

import com.chinasoft.smokesensor.dto.UserPreferencesRequest;
import com.chinasoft.smokesensor.dto.UserPreferencesResponse;
import com.chinasoft.smokesensor.entity.UserPreference;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import com.chinasoft.smokesensor.service.UserPreferenceService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户偏好业务实现。
 *
 * <p>当前项目尚未接入登录鉴权，因此统一使用 userId=1 作为占位用户；
 * 后续接入鉴权时，只需要将 DEFAULT_USER_ID 替换为当前登录用户 ID 来源。
 */
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final long DEFAULT_USER_ID = 1L;

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";
    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_FONT_COLOR = "font_color";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_PERMISSION_ENABLED = "permission_enabled";
    private static final String KEY_AVATAR_PARROT_ID = "avatar_parrot_id";

    private static final String DEFAULT_LANGUAGE = "zh";
    private static final String DEFAULT_THEME = "light";
    private static final String DEFAULT_FONT_FAMILY = "default";
    private static final int DEFAULT_FONT_SIZE = 16;
    private static final String DEFAULT_FONT_COLOR = "black";
    private static final boolean DEFAULT_NOTIFICATION_ENABLED = true;
    private static final boolean DEFAULT_PERMISSION_ENABLED = true;

    private static final int MIN_FONT_SIZE = 12;
    private static final int MAX_FONT_SIZE = 28;
    private static final Set<String> ALLOWED_LANGUAGES = Set.of("zh", "en", "es", "ja");
    private static final Set<String> ALLOWED_THEMES = Set.of("light", "dark");

    private static final Map<String, PreferenceMeta> PREFERENCE_META = buildPreferenceMeta();

    private final UserPreferenceRepository userPreferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesResponse getCurrentUserPreferences() {
        return buildResponse(DEFAULT_USER_ID, loadPreferenceMap(DEFAULT_USER_ID).values());
    }

    @Override
    @Transactional
    public UserPreferencesResponse updateCurrentUserPreferences(UserPreferencesRequest request) {
        UserPreferencesRequest safeRequest = request == null ? new UserPreferencesRequest() : request;
        Map<String, UserPreference> preferences = loadPreferenceMap(DEFAULT_USER_ID);

        if (safeRequest.getLanguage() != null) {
            savePreference(preferences, KEY_LANGUAGE, normalizeLanguage(safeRequest.getLanguage()));
        }
        if (safeRequest.getTheme() != null) {
            savePreference(preferences, KEY_THEME, normalizeTheme(safeRequest.getTheme()));
        }
        if (safeRequest.getFontFamily() != null) {
            savePreference(preferences, KEY_FONT_FAMILY, requiredText(safeRequest.getFontFamily(), "fontFamily"));
        }
        if (safeRequest.getFontSize() != null) {
            savePreference(preferences, KEY_FONT_SIZE, String.valueOf(normalizeFontSize(safeRequest.getFontSize())));
        }
        if (safeRequest.getFontColor() != null) {
            savePreference(preferences, KEY_FONT_COLOR, requiredText(safeRequest.getFontColor(), "fontColor"));
        }
        if (safeRequest.getNotificationEnabled() != null) {
            savePreference(preferences, KEY_NOTIFICATION_ENABLED, String.valueOf(safeRequest.getNotificationEnabled()));
        }
        if (safeRequest.getPermissionEnabled() != null) {
            savePreference(preferences, KEY_PERMISSION_ENABLED, String.valueOf(safeRequest.getPermissionEnabled()));
        }
        if (safeRequest.getAvatarParrotId() != null) {
            // avatarParrotId 允许传空字符串，用于前端清空头像鹦鹉选择。
            savePreference(preferences, KEY_AVATAR_PARROT_ID, safeRequest.getAvatarParrotId().trim());
        }

        return buildResponse(DEFAULT_USER_ID, preferences.values());
    }

    private Map<String, UserPreference> loadPreferenceMap(Long userId) {
        return userPreferenceRepository.findByUserIdOrderByPrefGroupAscPrefKeyAsc(userId)
                .stream()
                .collect(Collectors.toMap(
                        UserPreference::getPrefKey,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    /**
     * 按现有唯一键 user_id + pref_key 更新；如果记录不存在则新增。
     */
    private void savePreference(Map<String, UserPreference> preferences, String key, String value) {
        PreferenceMeta meta = PREFERENCE_META.get(key);
        UserPreference preference = preferences.get(key);
        if (preference == null) {
            preference = userPreferenceRepository.findByUserIdAndPrefKey(DEFAULT_USER_ID, key)
                    .orElseGet(() -> UserPreference.builder()
                            .userId(DEFAULT_USER_ID)
                            .prefKey(key)
                            .prefGroup(meta.group())
                            .description(meta.description())
                            .build());
        }
        preference.setPrefValue(value);
        preference.setPrefGroup(meta.group());
        preference.setDescription(meta.description());
        UserPreference saved = userPreferenceRepository.save(preference);
        preferences.put(key, saved);
    }

    private UserPreferencesResponse buildResponse(Long userId, Collection<UserPreference> preferences) {
        Map<String, String> values = preferences.stream()
                .collect(Collectors.toMap(
                        UserPreference::getPrefKey,
                        preference -> preference.getPrefValue() == null ? "" : preference.getPrefValue(),
                        (first, ignored) -> first));
        LocalDateTime updatedAt = preferences.stream()
                .map(UserPreference::getUpdatedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return UserPreferencesResponse.builder()
                .userId(userId)
                .language(stringValue(values, KEY_LANGUAGE, DEFAULT_LANGUAGE))
                .theme(stringValue(values, KEY_THEME, DEFAULT_THEME))
                .fontFamily(stringValue(values, KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY))
                .fontSize(intValue(values, KEY_FONT_SIZE, DEFAULT_FONT_SIZE))
                .fontColor(stringValue(values, KEY_FONT_COLOR, DEFAULT_FONT_COLOR))
                .notificationEnabled(booleanValue(values, KEY_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_ENABLED))
                .permissionEnabled(booleanValue(values, KEY_PERMISSION_ENABLED, DEFAULT_PERMISSION_ENABLED))
                .avatarParrotId(optionalText(values.get(KEY_AVATAR_PARROT_ID)))
                .updatedAt(updatedAt)
                .build();
    }

    private String normalizeLanguage(String language) {
        String value = requiredText(language, "language");
        if (!ALLOWED_LANGUAGES.contains(value)) {
            throw new IllegalArgumentException("language 仅支持 zh/en/es/ja");
        }
        return value;
    }

    private String normalizeTheme(String theme) {
        String value = requiredText(theme, "theme");
        if (!ALLOWED_THEMES.contains(value)) {
            throw new IllegalArgumentException("theme 仅支持 light/dark");
        }
        return value;
    }

    private int normalizeFontSize(Integer fontSize) {
        if (fontSize < MIN_FONT_SIZE || fontSize > MAX_FONT_SIZE) {
            throw new IllegalArgumentException("fontSize 必须在 12-28 之间");
        }
        return fontSize;
    }

    private String requiredText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return trimmed;
    }

    private String stringValue(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int intValue(Map<String, String> values, String key, int defaultValue) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean booleanValue(Map<String, String> values, String key, boolean defaultValue) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase();
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    private static Map<String, PreferenceMeta> buildPreferenceMeta() {
        return Map.of(
                KEY_LANGUAGE, new PreferenceMeta("general", "语言选项"),
                KEY_THEME, new PreferenceMeta("theme", "主题模式：light/dark"),
                KEY_FONT_FAMILY, new PreferenceMeta("display", "字体"),
                KEY_FONT_SIZE, new PreferenceMeta("display", "字号，单位 pt"),
                KEY_FONT_COLOR, new PreferenceMeta("display", "字体颜色"),
                KEY_NOTIFICATION_ENABLED, new PreferenceMeta("notification", "通知开关：true/false"),
                KEY_PERMISSION_ENABLED, new PreferenceMeta("notification", "设备权限提示开关：true/false"),
                KEY_AVATAR_PARROT_ID, new PreferenceMeta("profile", "设置页头像鹦鹉 ID"));
    }

    private record PreferenceMeta(String group, String description) {
    }
}
