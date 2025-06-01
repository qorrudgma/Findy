package com.boot.repository;

import com.boot.dto.testDTO;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface testRepository extends MongoRepository<testDTO, String> {
	// 기본 메서드 findAll, findById, save, deleteById, delete, existsById, count
	// 기본적인 메서드 외 커스텀 메서드 추가하기
	List<testDTO> findByName(String name);
	
	List<testDTO> findByNameAndAge(String name, int age);
}