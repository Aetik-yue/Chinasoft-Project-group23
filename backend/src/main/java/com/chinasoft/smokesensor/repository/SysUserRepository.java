package com.chinasoft.smokesensor.repository;

import com.chinasoft.smokesensor.entity.SysUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 系统用户表 sys_user 的数据库访问层。
 */
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /**
     * 登录时按用户名查询账号。
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 按手机号查询账号，用于手机号 + 密码登录。
     */
    Optional<SysUser> findByPhone(String phone);
}
