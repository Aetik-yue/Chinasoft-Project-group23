package com.chinasoft.smokesensor.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chinasoft.smokesensor.common.UserContext;
import com.chinasoft.smokesensor.dto.UserPreferencesRequest;
import com.chinasoft.smokesensor.dto.UserPreferencesResponse;
import com.chinasoft.smokesensor.entity.PetMediaRecord;
import com.chinasoft.smokesensor.entity.UserPreference;
import com.chinasoft.smokesensor.repository.PetMediaRecordRepository;
import com.chinasoft.smokesensor.repository.PetProfileRepository;
import com.chinasoft.smokesensor.repository.UserPreferenceRepository;
import com.chinasoft.smokesensor.service.UserPreferenceService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
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
 * <p>以 {@link UserContext#requireUserId()} 取当前登录用户 ID 做隔离：
 * 每个用户的偏好独立存储与读取，互不可见。
 */
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";
    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_FONT_COLOR = "font_color";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_PERMISSION_ENABLED = "permission_enabled";
    private static final String KEY_AVATAR_PARROT_ID = "avatar_parrot_id";
    private static final String KEY_PET_AVATAR_MEDIA_MAP = "pet_avatar_media_map";
    private static final int MAX_PET_AVATARS = 10;

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
    private final PetProfileRepository petProfileRepository;
    private final PetMediaRecordRepository petMediaRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesResponse getCurrentUserPreferences() {
        Long userId = UserContext.requireUserId();
        return buildResponse(userId, loadPreferenceMap(userId).values());
    }

    @Override
    @Transactional
    public UserPreferencesResponse updateCurrentUserPreferences(UserPreferencesRequest request) {
        UserPreferencesRequest safeRequest = request == null ? new UserPreferencesRequest() : request;
        Long userId = UserContext.requireUserId();
        Map<String, UserPreference> preferences = loadPreferenceMap(userId);

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
        if (safeRequest.getPetAvatarMediaMap() != null) {
            Map<String, String> avatarMap = validatePetAvatarMediaMap(safeRequest.getPetAvatarMediaMap(), userId);
            savePreference(preferences, KEY_PET_AVATAR_MEDIA_MAP, writeAvatarMediaMap(avatarMap));
        }

        return buildResponse(userId, preferences.values());
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
        Long userId = UserContext.requireUserId();
        PreferenceMeta meta = PREFERENCE_META.get(key);
        UserPreference preference = preferences.get(key);
        if (preference == null) {
            preference = userPreferenceRepository.findByUserIdAndPrefKey(userId, key)
                    .orElseGet(() -> UserPreference.builder()
                            .userId(userId)
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
                .petAvatarMediaMap(readAvatarMediaMap(values.get(KEY_PET_AVATAR_MEDIA_MAP)))
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * 头像映射只允许引用当前用户拥有的宠物及其成长相册照片，阻止客户端串宠或越权引用媒体。
     */
    private Map<String, String> validatePetAvatarMediaMap(Map<String, String> input, Long userId) {
        if (input.size() > MAX_PET_AVATARS) {
            throw new IllegalArgumentException("宠物头像数量不能超过 " + MAX_PET_AVATARS);
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String petId = requiredText(entry.getKey(), "petAvatarMediaMap petId");
            String mediaId = requiredText(entry.getValue(), "petAvatarMediaMap mediaId");
            if (!petProfileRepository.existsByPetIdAndUserId(petId, userId)) {
                throw new IllegalArgumentException("宠物不存在或不属于当前用户: " + petId);
            }
            PetMediaRecord media = petMediaRecordRepository.findByMediaIdAndPetId(mediaId, petId)
                    .orElseThrow(() -> new IllegalArgumentException("头像照片不存在或不属于该宠物: " + mediaId));
            if (!"photo".equals(media.getMediaType()) && !"screenshot".equals(media.getMediaType())) {
                throw new IllegalArgumentException("头像必须使用成长相册中的照片");
            }
            normalized.put(petId, mediaId);
        }
        return normalized;
    }

    private String writeAvatarMediaMap(Map<String, String> avatarMap) {
        try {
            String json = objectMapper.writeValueAsString(avatarMap);
            if (json.length() > 512) throw new IllegalArgumentException("宠物头像配置过长");
            return json;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("宠物头像配置格式无效", ex);
        }
    }

    private Map<String, String> readAvatarMediaMap(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            Map<String, String> parsed = objectMapper.readValue(value, new TypeReference<LinkedHashMap<String, String>>() {});
            if (parsed == null) return Map.of();
            Map<String, String> safeMap = new LinkedHashMap<>();
            parsed.forEach((petId, mediaId) -> {
                if (petId != null && !petId.isBlank() && mediaId != null && !mediaId.isBlank()) {
                    safeMap.put(petId, mediaId);
                }
            });
            return safeMap;
        } catch (JsonProcessingException ex) {
            // 历史异常偏好不影响用户正常进入系统，前端会使用默认头像。
            return Map.of();
        }
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
                KEY_AVATAR_PARROT_ID, new PreferenceMeta("profile", "设置页头像鹦鹉 ID"),
                KEY_PET_AVATAR_MEDIA_MAP, new PreferenceMeta("profile", "宠物成长相册头像映射"));
    }

    private record PreferenceMeta(String group, String description) {
    }
}
