package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.UserPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * user_preference 表的数据访问层。
 */
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /**
     * 查询某个用户的全部偏好，供页面初始化时一次性恢复设置。
     */
    List<UserPreference> findByUserIdOrderByPrefGroupAscPrefKeyAsc(Long userId);

    /**
     * 查询某个用户的单项偏好，保存时用于按唯一键 user_id + pref_key 更新。
     */
    Optional<UserPreference> findByUserIdAndPrefKey(Long userId, String prefKey);

    /**
     * 删除某个用户的全部偏好；注销账号时级联清理。
     */
    void deleteByUserId(Long userId);

    /**
     * 按 Key 和 Value 查询偏好，用于通过绑定的 QQ 号查询对应的系统用户 ID。
     */
    Optional<UserPreference> findByPrefKeyAndPrefValue(String prefKey, String prefValue);
}
