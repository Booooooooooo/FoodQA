package com.appleyk.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


public interface QuestionRepository extends GraphRepository<Long> {
	
	/**
	 *0 nf 保质期 
	 *@param name 食物名称
	 *@return 保质期
	 */
	@Query("match(n:Food) where n.fname={name} return n.period")
	String getFoodPeriod(@Param("name") String name); 
	
	/**
	 *1 nf 添加剂 
	 *@param name 食物名称
	 *@return 添加剂
	 */
	@Query("match(n:Food)-[:contain]-(m:Add) where n.fname={name} return m.aname")
	List<String> getFoodAdd(@Param("name") String name); 
	
	/**
	 *2 nf 防腐剂 
	 *@param name 食物名称
	 *@return 保质期
	 */
	@Query("match(n:Food)-[:contain]-(m:Add) where n.fname={name} and m.type=\"防腐剂\" return m.aname")
	List<String> getFoodAddF(@Param("name") String name); 
	

	/**
	 *3 nf 主要成分 
	 *@param name 主要成分
	 *@return 成分
	 */
	@Query("match(n:Food) where n.fname={name} return n.ingredient")
	String getFoodIngre(@Param("name") String name); 
	
	/**
	 *4 na 参与构成 
	 *@param name 添加剂名称
	 *@return 食物名称
	 */
	@Query("match(n:Food)-[:contain]-(m:Add) where m.aname={name} return n.fname")
	List<String> getAddFood(@Param("name") String name); 
  
	
	
}
