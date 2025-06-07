package com.boot.repository;

import com.boot.dto.NewsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends MongoRepository<NewsDTO, String> {
    
    // 제목이나 본문에서 키워드 검색 (대소문자 구분 없음)
    @Query("{'$or': [{'headline': {'$regex': ?0, '$options': 'i'}}, {'content': {'$regex': ?0, '$options': 'i'}}]}")
    Page<NewsDTO> findByKeyword(String keyword, Pageable pageable);
    
    // 카테고리별 검색
    Page<NewsDTO> findByCategory(String category, Pageable pageable);
    
    // 키워드 + 카테고리 검색
    @Query("{'$and': [{'category': ?1}, {'$or': [{'headline': {'$regex': ?0, '$options': 'i'}}, {'content': {'$regex': ?0, '$options': 'i'}}]}]}")
    Page<NewsDTO> findByKeywordAndCategory(String keyword, String category, Pageable pageable);
    
    // 출처별 검색
    Page<NewsDTO> findBySource(String source, Pageable pageable);
    
    // 전체 뉴스 수 조회
    long count();
    
    // 카테고리별 뉴스 수 조회
    long countByCategory(String category);
    
    // 최근 뉴스 조회 (시간 순으로 정렬)
    @Query("{}")
    Page<NewsDTO> findAllOrderByTimeDesc(Pageable pageable);
} 