package com.guang.portal.service;

import com.guang.portal.domain.dto.UpdatePasswordDTO;
import com.guang.portal.domain.dto.UpdateProfileDTO;
import com.guang.portal.domain.vo.UserProfileVO;

public interface ClientUserService {

    /**
     * 获取当前C端用户的个人资料（含统计数据）
     */
    UserProfileVO getProfile();

    /**
     * 更新个人资料
     */
    void updateProfile(UpdateProfileDTO dto);

    /**
     * 修改密码
     */
    void updatePassword(UpdatePasswordDTO dto);
}
