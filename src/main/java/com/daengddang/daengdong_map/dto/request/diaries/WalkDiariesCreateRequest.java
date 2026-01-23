package com.daengddang.daengdong_map.dto.request.diaries;

import com.daengddang.daengdong_map.domain.diaries.WalkDiaries;
import com.daengddang.daengdong_map.domain.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WalkDiariesCreateRequest {

    private String memo;
    private String mapImageUrl;

    public WalkDiariesCreateRequest(String memo, String mapImageUrl) {
        this.memo = memo;
        this.mapImageUrl = mapImageUrl;
    }

    public static WalkDiaries of(WalkDiariesCreateRequest dto, User user) {
        return WalkDiaries.builder()
                .memo(dto.getMemo())
                .mapImageUrl(dto.getMapImageUrl())
                .user(user)
                .build();
    }

}
