package com.guang.house.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guang.common.config.FileProperties;
import com.guang.common.exception.ApiException;
import com.guang.common.util.SecurityUtils;
import com.guang.house.domain.dto.HouseImageSaveDTO;
import com.guang.house.entity.HouseImage;
import com.guang.house.mapper.HouseImageMapper;
import com.guang.house.service.HouseImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 房源图片表 服务实现类
 * </p>
 *
 * @author blackDuck
 * @since 2026-05-07
 */
@Service
@RequiredArgsConstructor
public class HouseImageServiceImpl extends ServiceImpl<HouseImageMapper, HouseImage> implements HouseImageService {

    private final FileProperties fileProperties;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSave(List<HouseImageSaveDTO> imageDTOs) {
        if (CollUtil.isEmpty(imageDTOs)) {
            return true;
        }

        // 获取当前上下文中的用户 ID
        Integer currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null) {
            throw new ApiException("未获取到当前登录用户信息，图片上传失败");
        }

        List<HouseImage> images = imageDTOs.stream().map(dto -> {
            HouseImage image = cn.hutool.core.bean.BeanUtil.copyProperties(dto, HouseImage.class);

            // 1. 强制赋值上传人
            image.setUploadUserId(currentUserId);
            image.setAuditStatus((byte) 1); // 自动通过演示

            // 2. 严格限制 is_default 逻辑（绝对不能写入 0）
            if (image.getIsDefault() != null && image.getIsDefault() != 1) {
                // 只要不是明确的 1，全部刷为 null，利用 MySQL 允许多个 NULL 规避唯一索引冲突
                image.setIsDefault(null);
            }

            // 3. 补充必填字段 image_type 的默认值逻辑
            if (image.getImageType() == null) {
                // 默认策略：如果明确是默认首图，类型兜底为 1(封面图)，否则兜底为 5(其他)
                image.setImageType((byte) (image.getIsDefault() != null && image.getIsDefault() == 1 ? 1 : 5));
            }

            return image;
        }).collect(Collectors.toList());

        return this.saveBatch(images);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setAsCover(Integer imageId) {
        HouseImage image = this.getById(imageId);
        if (image == null) {
            throw new ApiException("图片不存在");
        }

        // 1. 取消该房源原有的封面
        // 【关键修复】：将原来的 0 改为 null，避免在唯一索引（house_id, is_default）上引发冲突
        this.update(new LambdaUpdateWrapper<HouseImage>()
                .eq(HouseImage::getHouseId, image.getHouseId())
                .isNotNull(HouseImage::getIsDefault) // 仅更新之前是封面的记录，提升性能
                .set(HouseImage::getIsDefault, null));

        // 2. 设置新封面
        image.setIsDefault((byte) 1);
        return this.updateById(image);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean sortImages(List<Integer> imageIds) {
        if (CollUtil.isEmpty(imageIds)) return true;

        List<HouseImage> images = new ArrayList<>();
        for (int i = 0; i < imageIds.size(); i++) {
            HouseImage img = new HouseImage();
            img.setId(imageIds.get(i));
            img.setSortOrder(i + 1);
            images.add(img);
        }
        return this.updateBatchById(images);
    }

    @Override
    public List<String> findOrphanedImages() {
        String houseSubDir = "house"; // 与 FileCategory.HOUSE.getSubDir() 一致
        String basePath = fileProperties.getPath() + File.separator + houseSubDir;
        File dir = new File(basePath);
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();

        // 1. 递归扫描物理文件
        List<File> allFiles = new ArrayList<>();
        scanRecursive(dir, allFiles);

        // 2. 获取数据库中所有房源图片的 file_key
        Set<String> dbFileKeys = this.list(new LambdaQueryWrapper<HouseImage>()
                        .select(HouseImage::getFileKey))
                .stream()
                .map(HouseImage::getFileKey)
                .collect(Collectors.toSet());

        // 3. 找出在磁盘上但不在数据库中的
        String rootPath = new File(fileProperties.getPath()).getAbsolutePath();
        return allFiles.stream()
                .map(file -> {
                    String absPath = file.getAbsolutePath();
                    // 转换为相对路径 key: house/yyyy/MM/dd/uuid.jpg (注意分隔符转换)
                    String relative = absPath.substring(rootPath.length() + 1).replace("\\", "/");
                    return relative;
                })
                .filter(key -> !dbFileKeys.contains(key))
                .collect(Collectors.toList());
    }

    private void scanRecursive(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanRecursive(f, result);
            } else {
                result.add(f);
            }
        }
    }
}
