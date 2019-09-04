package com.appleyk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class PageIndexController {

	@RequestMapping("/")
	@ResponseBody
	public String index() {
		return "index";
	}

}
