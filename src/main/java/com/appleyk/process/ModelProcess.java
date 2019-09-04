package com.appleyk.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

/**
 * Spark贝叶斯分类器 + HanLP分词器 + 实现问题语句的抽象+模板匹配+关键性语句还原
 */
public class ModelProcess {

	
	/**
	 * 分类标签号和问句模板对应表
	 */
	Map<Double, String> questionsPattern; 
	
	/**
	 * Spark贝叶斯分类器
	 */
	NaiveBayesModel nbModel;
	
	/**
	 * 词语和下标的对应表   == 词汇表
	 */
	Map<String, Integer> vocabulary; 
	
	/**
	 * 关键字与其词性的map键值对集合 == 句子抽象
	 */
	Map<String, String> abstractMap;
	
	/**
	 * 指定问题question及字典的txt模板所在的根目录
	 */
    String rootDirPath = "D:/HanLP/data";
    
    /**
     * 分类模板索引
     */
    int modelIndex = 0;

	public ModelProcess() throws Exception{
		questionsPattern = loadQuestionsPattern();
		vocabulary = loadVocabulary();
		nbModel = loadClassifierModel();
	}	
	
	
	public ModelProcess(String rootDirPath) throws Exception{
		this.rootDirPath = rootDirPath+'/';
		questionsPattern = loadQuestionsPattern();
		vocabulary = loadVocabulary();
		nbModel = loadClassifierModel();
	}
	
	public ArrayList<String> analyQuery(String queryString) throws Exception {
		
		/**
		 * 打印问句
		 */
		System.out.println("原始句子："+queryString);
		System.out.println("========HanLP开始分词========");
		
		/**
		 * 抽象句子，利用HanPL分词，将关键字进行词性抽象
		 */
		String abstr = queryAbstract(queryString);	
		System.out.println("句子抽象化结果："+abstr);
		
		/**
		 * 将抽象的句子与spark训练集中的模板进行匹配，拿到句子对应的模板
		 */
		String strPatt = queryClassify(abstr);
		System.out.println("句子套用模板结果："+strPatt); 
		
		
		/**
		 * 模板还原成句子
		 */
		String finalPattern = queryExtenstion(strPatt);
		System.out.println("原始句子替换成系统可识别的结果："+finalPattern);
		
		
		ArrayList<String> resultList = new ArrayList<String>();
		resultList.add(String.valueOf(modelIndex)); //问句编号，语句单词
		String[] finalPattArray = finalPattern.split(" ");
		for (String word : finalPattArray)
			resultList.add(word);
		return resultList;
	}

	public  String queryAbstract(String querySentence) {
											
		// 句子抽象化
		Segment segment = HanLP.newSegment().enableCustomDictionary(true);							
		List<Term> terms = segment.seg(querySentence);
		String abstractQuery = "";
		abstractMap = new HashMap<String, String>();
		for (Term term : terms) {
			String word = term.word;
			String termStr = term.toString();
			System.out.println(termStr);
			if (termStr.contains("nf")) {        //nf 食物
				abstractQuery += "nf ";
				abstractMap.put("nf", word);
			} else if (termStr.contains("na")) { //na 添加剂
				abstractQuery += "na ";
				abstractMap.put("na", word);
				
			} 	
			else {
				abstractQuery += word + " ";
			}
		}
		System.out.println("========HanLP分词结束========");
		return abstractQuery;
	}

	public  String queryExtenstion(String queryPattern) {
		// 句子还原
		Set<String> set = abstractMap.keySet();
		for (String key : set) {
			/**
			 * 如果句子模板中含有抽象的词性
			 */
			if (queryPattern.contains(key)) {
				
				/**
				 * 则替换抽象词性为具体的值 
				 */
				String value = abstractMap.get(key);
				queryPattern = queryPattern.replace(key, value);
			}
		}
		String extendedQuery = queryPattern;
		/**
		 * 当前句子处理完，抽象map清空释放空间并置空，等待下一个句子的处理
		 */
		abstractMap.clear();
		abstractMap = null;
		return extendedQuery;
	}

	
	/**
	 * 加载词汇表 == 关键特征 == 与HanLP分词后的单词进行匹配
	 */
	public  Map<String, Integer> loadVocabulary() {
		Map<String, Integer> vocabulary = new HashMap<String, Integer>();
		File file = new File(rootDirPath + "question2/vocabulary.txt");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line;
		int i=0;
		try {
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(":");
				int index = i;;
				String word = tokens[1];
				vocabulary.put(word, index);
			//	System.out.println(index+word);
				i=i+1;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return vocabulary;
	}

	/**
	 * 加载文件，并读取内容返回
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public  String loadFile(String filename) throws IOException {
		File file = new File(rootDirPath + filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String content = "";
		String line;
		while ((line = br.readLine()) != null) {
			/**
			 * 文本的换行符暂定用"`"代替
			 */
			content += line + "`";
		}
		/**
		 * 关闭资源
		 */
		br.close();
		return content;
	}

	/**
	 * 句子分词后与词汇表进行key匹配转换为double向量数组
	 * @param sentence
	 * @return
	 * @throws Exception
	 */
	public  double[] sentenceToArrays(String sentence) throws Exception {
		
		double[] vector = new double[vocabulary.size()];
		/**
		 * 模板对照词汇表的大小进行初始化，全部为0.0
		 */
		for (int i = 0; i < vocabulary.size(); i++) {
			vector[i] = 0;
		}
		
		/**
		 * HanLP分词，拿分词的结果和词汇表里面的关键特征进行匹配
		 */
		Segment segment = HanLP.newSegment();
		List<Term> terms = segment.seg(sentence);
		for (Term term : terms) {
			String word = term.word;
		//	System.out.println(word);
			/**
			 * 如果命中，0.0 改为 1.0
			 */
			if (vocabulary.containsKey(word)) {
				int index = vocabulary.get(word);
				vector[index] = 1;
			}
		}
		return vector;
	}

	/**
	 * Spark朴素贝叶斯(naiveBayes)
	 * 对特定的模板进行加载并分类
	 * @return
	 * @throws Exception
	 */
	public  NaiveBayesModel loadClassifierModel() throws Exception {
	
			/**
			 * 本地模式，*表示启用多个线程并行计算
			 */
			SparkConf conf = new SparkConf().setAppName("NaiveBayesTest").setMaster("local[*]");
			JavaSparkContext sc = new JavaSparkContext(conf);

			/**
			 * 训练集生成
			 * labeled point 是一个局部向量，要么是密集型的要么是稀疏型的
			 * 用一个label/response进行关联。在MLlib里，labeled points 被用来监督学习算法
			 * 我们使用一个double数来存储一个label，因此我们能够使用labeled points进行回归和分类
			 */
			List<LabeledPoint> train_list = new LinkedList<LabeledPoint>();
			String[] sentences = null;
			

			
			String dateQuestions = loadFile("question2/【0】保质期.txt");
			sentences = dateQuestions.split("`");
			for (String sentence : sentences) {
				
		//		System.out.println(sentence);
				
				double[] array = sentenceToArrays(sentence);
				LabeledPoint train_one = new LabeledPoint(0.0, Vectors.dense(array));  //编号，稠密向量
				train_list.add(train_one);
			}

			
			String addQuestions = loadFile("question2/【1】添加剂.txt");
			sentences = addQuestions.split("`");
			for (String sentence : sentences) {
				
				double[] array = sentenceToArrays(sentence);
				LabeledPoint train_one = new LabeledPoint(1.0, Vectors.dense(array));
				train_list.add(train_one);
			}


			
			String styleQuestions = loadFile("question2/【2】防腐剂.txt");
			sentences = styleQuestions.split("`");
			for (String sentence : sentences) {
				double[] array = sentenceToArrays(sentence);
				LabeledPoint train_one = new LabeledPoint(2.0, Vectors.dense(array));
				train_list.add(train_one);
			}


			
			String ingreQuestions = loadFile("question2/【3】主要成分.txt");
			sentences = ingreQuestions.split("`");
			for (String sentence : sentences) {
				double[] array = sentenceToArrays(sentence);
				LabeledPoint train_one = new LabeledPoint(3.0, Vectors.dense(array));
				train_list.add(train_one);
			}

			
			String foodQuestion = loadFile("question2/【4】哪些食物含某种添加剂.txt");
			sentences = foodQuestion.split("`");
			for (String sentence : sentences) {
				double[] array = sentenceToArrays(sentence);
				LabeledPoint train_one = new LabeledPoint(4.0, Vectors.dense(array));
				train_list.add(train_one);
			}
	
			
			JavaRDD<LabeledPoint> trainingRDD = sc.parallelize(train_list); //转化为rdd
			NaiveBayesModel nb_model = NaiveBayes.train(trainingRDD.rdd());
			
			/**
			 * 关闭资源
			 */
			sc.close();
			
			/**
			 * 返回贝叶斯分类器
			 */
			return nb_model;		
		
	}

	/**
	 * 加载问题模板 == 分类器标签
	 * @return
	 */
	public  Map<Double, String> loadQuestionsPattern() {
		Map<Double, String> questionsPattern = new HashMap<Double, String>();
		File file = new File(rootDirPath + "question2/question_classification.txt");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String line;
		int i=0;
		try {
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(":");
				double index = i;
				String pattern = tokens[1];
				questionsPattern.put(index, pattern);
		//		System.out.println(index+pattern);
				i=i+1;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return questionsPattern;
	}

	/**
	 * 贝叶斯分类器分类的结果，拿到匹配的分类标签号，并根据标签号返回问题的模板
	 * @param sentence
	 * @return
	 * @throws Exception
	 */
	public  String queryClassify(String sentence) throws Exception {
		
		double[] testArray = sentenceToArrays(sentence);
		Vector v = Vectors.dense(testArray);
		
		/**
		 * 对数据进行预测predict
		 * 句子模板在 spark贝叶斯分类器中的索引【位置】
		 * 根据词汇使用的频率推断出句子对应哪一个模板
		 */
		double index = nbModel.predict(v);
		modelIndex = (int)index;
		System.out.println("the model index is " + index);	
		return questionsPattern.get(index);
	}

	public static void main(String[] agrs) throws Exception {
		ModelProcess mp = new ModelProcess("D:/HanLP/data");
		//mp.analyQuery("火腿肠的保质期");
		System.out.println("Hello World !");
	}
}
