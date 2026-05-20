package com.guang.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 聊天响应")
public class AiChatVO {
    @Schema(description = "AI 回复内容")
    private String reply;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "结构化房源推荐列表")
    private List<HouseItemVO> recommendations;

    @Schema(description = "分块渲染内容")
    private List<ChatBlock> blocks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 聊天分块内容")
    public static class ChatBlock {
        @Schema(description = "块类型: text (文本) / house_card (单张房源卡片) / house_cards (房源卡片组)")
        private String type;

        @Schema(description = "文本内容 (当type为text时有效)")
        private String content;

        @Schema(description = "房源ID列表 (当type为house_card/house_cards时有效)")
        private List<String> houseIds;
    }
}
