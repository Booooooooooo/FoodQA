package com.appleyk.service;

import org.springframework.stereotype.Service;

@Service
public interface QuestionService {

	  void showDictPath();
	  String answer(String question) throws Exception;
}
