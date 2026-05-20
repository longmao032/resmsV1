package com.guang.house.service;

import com.guang.house.domain.dto.HouseImageSaveDTO;
import com.guang.house.entity.HouseImage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 房源图片表 服务类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
public interface HouseImageService extends IService<HouseImage> {

    /**
     * 批量保存图片
     */
    Boolean batchSave(List<HouseImageSaveDTO> imageDTOs);

    /**
     * 设置为封面
     */
    Boolean setAsCover(Integer imageId);

    /**
     * 图片排序
     */
    Boolean sortImages(List<Integer> imageIds);

    /**
     * 查找磁盘上存在的但在数据库中无记录的图片（孤儿图片）
     */
    List<String> findOrphanedImages();
}
