package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewsCountDto {
	private String newsId;
	private String url;
	private long count;
}