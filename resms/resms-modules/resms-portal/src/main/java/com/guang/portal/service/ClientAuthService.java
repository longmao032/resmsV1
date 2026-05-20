package com.guang.portal.service;

import com.guang.portal.domain.dto.ClientLoginDTO;
import com.guang.portal.domain.dto.ClientRegisterDTO;
import com.guang.portal.domain.vo.ClientLoginVO;

public interface ClientAuthService {

    /**
     * C端用户注册（已存在则直接登录）
     */
    ClientLoginVO register(ClientRegisterDTO dto);

    /**
     * C端用户登录
     */
    ClientLoginVO login(ClientLoginDTO dto);

    /**
     * C端用户登出，清除 Redis 缓存
     */
    void logout();
}
