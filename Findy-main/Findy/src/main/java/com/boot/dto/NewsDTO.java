package com.boot.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "newsdata")
public class NewsDTO {
    
    @Id
    private String id;
    
    private String headline;  // 제목
    private String url;       // URL (중복 방지 기준)
    private String content;   // 본문
    private String time;      // 발행시간
    private String category;  // 카테고리
    private String source;    // 출처 (chosun, donga 등)
    private List<String> tags; // 태그 배열
    private Double score;     // 검색 점수 (선택사항)
} 