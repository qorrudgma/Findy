package com.boot.controller;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import com.boot.dto.NewsDTO;
import com.boot.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchNews(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category) {
        
        log.info("뉴스 검색 API 호출 - 검색어: {}, 페이지: {}, 크기: {}, 카테고리: {}", q, page, size, category);
        
        try {
            // 실제 MongoDB에서 뉴스 검색
            Page<NewsDTO> newsPage = newsService.searchNews(q, category, page, size);
            
            // DTO를 API 응답 형식으로 변환
            List<Map<String, Object>> articles = new ArrayList<>();
            
            for (NewsDTO news : newsPage.getContent()) {
                Map<String, Object> article = new HashMap<>();
                article.put("id", news.getId());
                article.put("title", news.getHeadline());  // headline -> title
                article.put("content", truncateContent(news.getContent(), 200)); // 요약본만
                article.put("category", convertActualCategoryToDisplay(news.getCategory()));
                article.put("source", news.getSource());
                article.put("publishedAt", formatPublishedDate(news.getTime()));  // 날짜 포맷팅
                article.put("url", news.getUrl());
                article.put("score", news.getScore() != null ? news.getScore() : 0.95);
                
                // tags 배열 추가
                List<String> tags = new ArrayList<>();
                if (news.getTags() != null) {
                    tags.addAll(news.getTags());
                } else {
                    // 기본 태그 생성 (중복 방지)
                    if (q != null && !q.trim().isEmpty() && !q.equals("뉴스")) {
                        tags.add(q);
                    }
                    if (news.getCategory() != null) {
                        String categoryTag = convertActualCategoryToDisplay(news.getCategory());
                        tags.add(categoryTag);
                    }
                    // 태그가 비어있으면 기본 태그 추가
                    if (tags.isEmpty()) {
                        tags.add("뉴스");
                    }
                }
                article.put("tags", tags);
                
                articles.add(article);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", articles);
            
            Map<String, Object> pageable = new HashMap<>();
            pageable.put("pageNumber", newsPage.getNumber());
            pageable.put("pageSize", newsPage.getSize());
            response.put("pageable", pageable);
            
            response.put("totalElements", newsPage.getTotalElements());
            response.put("totalPages", newsPage.getTotalPages());
            response.put("number", newsPage.getNumber());
            response.put("size", newsPage.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("뉴스 검색 중 오류 발생", e);
            
            // 오류 발생 시 빈 결과 반환
            Map<String, Object> response = new HashMap<>();
            response.put("content", new ArrayList<>());
            response.put("totalElements", 0);
            response.put("totalPages", 0);
            response.put("number", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 텍스트를 지정된 길이로 자르기
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
    
    /**
     * 실제 카테고리를 표시용 카테고리로 변환
     */
    private String convertActualCategoryToDisplay(String actualCategory) {
        if (actualCategory == null) return "기타";
        
        Map<String, String> mapping = new HashMap<>();
        mapping.put("economy", "경제");
        mapping.put("politics", "정치");
        mapping.put("society", "사회");
        mapping.put("sports", "스포츠");
        mapping.put("culture", "연예/문화");
        mapping.put("opinion", "오피니언");
        mapping.put("health", "건강");
        
        return mapping.getOrDefault(actualCategory, actualCategory);
    }
    
    /**
     * 날짜 포맷팅
     */
    private String formatPublishedDate(String timeStr) {
        if (timeStr == null || timeStr.equals("시간 없음")) {
            return "방금 전";
        }
        
        // 이미 ISO 8601 형식이면 그대로 반환
        if (timeStr.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
            return timeStr;
        }
        
        // 날짜 형식 변환 시도
        try {
            if (timeStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return timeStr + "T09:00:00Z";
            }
            if (timeStr.matches("\\d{4}\\.\\d{2}\\.\\d{2}")) {
                return timeStr.replace(".", "-") + "T09:00:00Z";
            }
            if (timeStr.matches("\\d{2}:\\d{2}")) {
                java.time.LocalDate today = java.time.LocalDate.now();
                return today.toString() + "T" + timeStr + ":00Z";
            }
        } catch (Exception e) {
            log.warn("날짜 포맷팅 실패: {}", timeStr);
        }
        
        return timeStr;
    }
    
    @GetMapping("/search/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String q) {
        log.info("자동완성 API 호출 - 검색어: {}", q);
        
        List<String> suggestions = newsService.getAutocompleteSuggestions(q);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/search/popular")
    public ResponseEntity<List<String>> getPopularQueries() {
        log.info("인기 검색어 API 호출");
        
        List<String> popular = newsService.getPopularQueries();
        return ResponseEntity.ok(popular);
    }
    
    // 크롤러 상태 확인 API
    @GetMapping("/crawler/status")
    public ResponseEntity<Map<String, Object>> getCrawlerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 실제 MongoDB 통계 조회
        long totalNews = newsService.getTotalNewsCount();
        Map<String, Long> categoryStats = newsService.getNewsByCategory();
        
        status.put("isRunning", false);
        status.put("lastRun", "2025-01-06T09:00:00Z");
        status.put("totalArticles", totalNews);
        status.put("sources", List.of("조선일보", "동아일보", "한겨레", "경향신문", "연합뉴스", "이데일리"));
        status.put("categoryStats", categoryStats);
        
        return ResponseEntity.ok(status);
    }
    
    // 크롤러 실행 API
    @PostMapping("/crawler/run")
    public ResponseEntity<Map<String, String>> runCrawler() {
        log.info("크롤러 실행 요청");
        
        try {
            // Python 크롤러 실행
            String pythonPath = "python";
            String scriptPath = "../findy-crawler/main.py";
            
            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            processBuilder.directory(new java.io.File("."));
            
            Process process = processBuilder.start();
            
            // 백그라운드에서 실행되도록 처리
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    log.info("크롤러 실행 완료. 종료 코드: {}", exitCode);
                } catch (InterruptedException e) {
                    log.error("크롤러 실행 중 인터럽트 발생", e);
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "크롤러 실행이 요청되었습니다.");
            response.put("status", "started");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("크롤러 실행 중 오류", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "크롤러 실행 중 오류가 발생했습니다: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.ok(response);
        }
    }
} 