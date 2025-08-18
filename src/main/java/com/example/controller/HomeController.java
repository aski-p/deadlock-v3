package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, HttpSession session,
                      @RequestParam(required = false) String error) {
        
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        Map<String, Object> user = (Map<String, Object>) session.getAttribute("user");
        
        model.addAttribute("title", "Deadlock Stats Tracker");
        model.addAttribute("message", "Steam 계정으로 로그인하여 Deadlock 게임 통계를 확인하세요!");
        model.addAttribute("isLoggedIn", isLoggedIn != null && isLoggedIn);
        model.addAttribute("user", user);
        
        if ("login_failed".equals(error)) {
            model.addAttribute("error", "Steam 로그인에 실패했습니다. 다시 시도해주세요.");
        }
        
        return "home";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "소개");
        model.addAttribute("content", "이 애플리케이션은 Spring Framework와 JSP를 사용하여 구성되었습니다.");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "연락처");
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String message,
                               Model model) {
        model.addAttribute("title", "연락처");
        model.addAttribute("success", "메시지가 성공적으로 전송되었습니다!");
        model.addAttribute("submittedName", name);
        return "contact";
    }

    @GetMapping("/api/test")
    @ResponseBody
    public Map<String, Object> apiTest() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "API 테스트 성공");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}