package com.appleyk.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.appleyk.process.ModelProcess;
import com.appleyk.repository.QuestionRepository;
import com.appleyk.service.QuestionService;
import com.hankcs.hanlp.dictionary.CustomDictionary;

@Service
@Primary
public class QuestionServiceImpl implements QuestionService {

//	@Value("${rootDirPath}")
//	private String rootDictPath;
//	@Value("${HanLP.CustomDictionary.path.foodDict}")
//	private String foodDictPath;
//
//	@Value("${HanLP.CustomDictionary.path.addDict}")
//	private String addDictPath;

	private String rootDictPath = "D:/HanLP/";
	private String foodDictPath = "D:/HanLP/data/dictionary/custom/foodDict.txt";
	private String addDictPath = "D:/HanLP/data/dictionary/custom/addDict.txt";

	


	@Autowired
	private QuestionRepository questionRepository;

	@Override
	public void showDictPath() {
		System.out.println("HanLP分词字典及自定义问题模板根目录：" + rootDictPath);
	}

	@Override
	public String answer(String question) throws Exception {

		ModelProcess queryProcess = new ModelProcess(rootDictPath);

		
		loadFoodDict(foodDictPath);
		loadAddDict(addDictPath);
		
		
//		CustomDictionary.add("火腿肠", "nf 0");
		ArrayList<String> reStrings = queryProcess.analyQuery(question);
		int modelIndex = Integer.valueOf(reStrings.get(0));
		String answer = null;
		String name="";
		String period="";
		String ingre="";
		
		switch (modelIndex) {
		case 0:
			/**
			 * nf 保质期
			 */
			name = reStrings.get(1);
			System.out.println(name);
			period = questionRepository.getFoodPeriod(name);
			System.out.println(period);
			if (period != null) {
				answer = period;
				answer = answer+"天"; 
			} else {
				answer = null;
			}
			break;
		case 1:
			/**
			 * nf 添加剂
			 */
			name = reStrings.get(1);
			List<String> adds = questionRepository.getFoodAdd(name);
			if (adds.size()==0) {
				answer = null;
			} else {
				answer = adds.toString().replace("[", "").replace("]", "");
			}
			break;
		case 2:
			/**
			 * nf 防腐剂
			 */
			name = reStrings.get(1);
			List<String> addF = questionRepository.getFoodAddF(name);
			if (addF.size()==0) {
				answer = null;
			} else {
				answer = addF.toString().replace("[", "").replace("]", "");
			}
			break;
		case 3:
			/**
			 * nf 成分
			 */
			name = reStrings.get(1);
			ingre = questionRepository.getFoodIngre(name);
			if (ingre != null) {
				answer = ingre;
			} else {
				answer = null;
			}
			break;
		case 4:
			/**
			 * na 参与构成
			 */
			name = reStrings.get(1);
			List<String> foods = questionRepository.getAddFood(name);
			if (foods.size()==0) {
				answer = null;
			} else {
				answer = foods.toString().replace("[", "").replace("]", "");
			}
			break;
		default:
			break;
		}

	

		System.out.println(answer);
		if (answer != null && !answer.equals("") && !answer.equals("\\N")) {
			return answer;
		} else {
			return "sorry,我没有找到你要的答案";
		}
	}

	
	/**
	 * 加载自定义食物字典
	 * 
	 * @param path
	 */
	public void loadFoodDict(String path) {

		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 0);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

	}
	
	/**
	 * 加载自定义添加剂字典
	 * 
	 * @param path
	 */
	public void loadAddDict(String path) {

		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 1);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * 添加自定义分词及其词性，注意数字0表示频率，不能没有
	 * 
	 * @param br
	 * @param type
	 */
	public void addCustomDictionary(BufferedReader br, int type) {

		String word;
		try {
			while ((word = br.readLine()) != null) {
				switch (type) {
				/**
				 * 设置食物名称 词性 == nf 0
				 */
				case 0:
//					System.out.println(word.equals("火腿肠"));
					CustomDictionary.add(word, "nf 0");
					break;	
				case 1:
					CustomDictionary.add(word, "na 0");
					break;
				default:
					break;
				}
			}
			br.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
