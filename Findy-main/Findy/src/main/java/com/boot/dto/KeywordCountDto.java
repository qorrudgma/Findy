package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KeywordCountDto {
	private String keyword;
	private long count;
}