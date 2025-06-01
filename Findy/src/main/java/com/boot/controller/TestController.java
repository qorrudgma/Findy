package com.boot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.dto.testDTO;
import com.boot.service.testService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class TestController {

	private final testService testService;

	public TestController(testService testService) {
		this.testService = testService;
	}

	@GetMapping("/test")
	public List<testDTO> getAllTests() {
		log.info("!@#$ => " + testService.findAllTests());

		return testService.findAllTests();
	}

	@GetMapping("/save")
	public String save(@RequestParam("name") String name, @RequestParam("age") int age) {
		log.info("save()");
		testService.save(name, age);

		return "saveOk";
	}

	@GetMapping("/findByName")
	public List<testDTO> findByName(@RequestParam String name) {
//    	http://localhost:8485/findByName?name=test1
		log.info("!@#$ => " + testService.findByName(name));

		return testService.findByName(name);
	}
}