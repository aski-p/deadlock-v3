package com.example.controller;

import com.example.service.DeadlockService;
import com.example.service.SteamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private DeadlockService deadlockService;
    
    @Autowired
    private SteamService steamService;

    @GetMapping("")
    public String profile(HttpSession session, Model model,
                         @RequestParam(defaultValue = "matches") String tab) {
        
        // 로그인 체크
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            return "redirect:/auth/login";
        }
        
        String steamId = (String) session.getAttribute("steamId");
        Map<String, Object> userInfo = (Map<String, Object>) session.getAttribute("user");
        
        // Deadlock 프로필 데이터 조회
        Map<String, Object> profileData = deadlockService.getPlayerProfile(steamId);
        
        model.addAttribute("user", userInfo);
        model.addAttribute("steamId", steamId);
        model.addAttribute("profileData", profileData);
        model.addAttribute("currentTab", tab);
        model.addAttribute("title", userInfo.get("personaName") + " - Deadlock Profile");
        
        return "profile";
    }
    
    @GetMapping("/api/matches")
    @ResponseBody
    public Map<String, Object> getMatches(HttpSession session,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return error;
        }
        
        String steamId = (String) session.getAttribute("steamId");
        return deadlockService.getPlayerMatches(steamId);
    }
    
    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getStats(HttpSession session) {
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return error;
        }
        
        String steamId = (String) session.getAttribute("steamId");
        return deadlockService.getPlayerStats(steamId);
    }
}