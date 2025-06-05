package com.boot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.boot.dto.testDTO;
import com.boot.repository.testRepository;

@Service
public class testService {

	private final testRepository testRepository;

	public testService(testRepository testRepository) {
		this.testRepository = testRepository;
	}

	public List<testDTO> findAllTests() {
		return testRepository.findAll();
	}

	public void save(String name, int age) {
		testDTO dto = new testDTO();
//		dto.setName(name);
//		dto.setAge(age);
		testRepository.save(dto);
	}

	public List<testDTO> findByName(String name) {
		return testRepository.findByName(name);
	}
}
