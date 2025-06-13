//package com.boot.elasticsearch;
//
//import java.io.IOException;
//
//import org.springframework.stereotype.Component;
//
//import com.nikialeksey.hunspell.Hunspell;
//
//@Component
//public class EnglishDictionary {
//	private final Hunspell hunspell;
//
//	public EnglishDictionary() throws IOException {
//		String dicPath = getClass().getResource("/dict/en_US/en_US.dic").getPath();
//		String affPath = getClass().getResource("/dict/en_US/en_US.aff").getPath();
//		// aff → dic 순으로 전달
//		this.hunspell = new Hunspell(affPath, dicPath);
//	}
//
//	public boolean exists(String word) {
//		return hunspell.spell(word);
//	}
//}