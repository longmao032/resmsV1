package com.guang.aiassistant.controller;

import com.guang.aiassistant.model.dto.HouseSyncDTO;
import com.guang.aiassistant.model.dto.ProjectSyncDTO;
import com.guang.aiassistant.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final DataSyncService dataSyncService;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/house")
    public String syncHouse(@RequestBody HouseSyncDTO dto) {
        log.info("收到房源实时同步请求: houseId={}, action={}", dto.getHouseId(), dto.getAction());
        if (dto.getEventId() != null) {
            String redisKey = "sync:event:" + dto.getEventId();
            Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(success)) {
                log.warn("检测到重复请求，已忽略: eventId={}", dto.getEventId());
                return "DUPLICATE";
            }
        }

        if ("DELETE".equals(dto.getAction())) {
            dataSyncService.deleteHouse(dto.getHouseId());
        } else {
            dataSyncService.upsertHouse(dto);
        }
        return "SUCCESS";
    }

    @PostMapping("/house/batch")
    public String syncHouseBatch(@RequestBody List<HouseSyncDTO> dtos) {
        log.info("收到房源批量同步请求: size={}", dtos.size());
        if (dtos.isEmpty()) return "SUCCESS";

        String batchEventId = dtos.get(0).getEventId();
        if (batchEventId != null) {
            String redisKey = "sync:event:batch:" + batchEventId;
            Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(success)) {
                log.warn("检测到重复批量请求，已忽略: eventId={}", batchEventId);
                return "DUPLICATE";
            }
        }

        List<HouseSyncDTO> saves = dtos.stream().filter(d -> !"DELETE".equals(d.getAction())).toList();
        List<HouseSyncDTO> deletes = dtos.stream().filter(d -> "DELETE".equals(d.getAction())).toList();

        if (!saves.isEmpty()) {
            dataSyncService.upsertHouseBatch(saves);
        }
        for (HouseSyncDTO del : deletes) {
            dataSyncService.deleteHouse(del.getHouseId());
        }

        return "SUCCESS";
    }

    @PostMapping("/project")
    public String syncProject(@RequestBody ProjectSyncDTO dto) {
        log.info("收到楼盘实时同步请求: projectId={}, action={}", dto.getProjectId(), dto.getAction());
        if (dto.getEventId() != null) {
            String redisKey = "sync:event:" + dto.getEventId();
            Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(success)) {
                log.warn("检测到重复请求，已忽略: eventId={}", dto.getEventId());
                return "DUPLICATE";
            }
        }

        if ("DELETE".equals(dto.getAction())) {
            dataSyncService.deleteProject(dto.getProjectId());
        } else {
            dataSyncService.upsertProject(dto);
        }
        return "SUCCESS";
    }

    @org.springframework.web.bind.annotation.GetMapping("/house/ids")
    public List<Integer> getHouseIds() {
        return dataSyncService.getAllHouseIds();
    }
}
