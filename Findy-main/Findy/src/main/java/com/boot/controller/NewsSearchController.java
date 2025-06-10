package com.boot.controller;

import com.boot.elasticsearch.ElasticTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NewsSearchController
{
    private final ElasticTestService elasticTestService;

    public NewsSearchController(ElasticTestService elasticTestService) {
        this.elasticTestService = elasticTestService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchNews() throws IOException
    {
        List<Map<String, Object>> newsList = elasticTestService.searchNews();
        return ResponseEntity.ok(newsList);
    }
}
