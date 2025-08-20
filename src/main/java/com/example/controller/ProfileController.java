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
            return "redirect:/auth/steam";
        }
        
        String steamId;
        Map<String, Object> userInfo;
        
        steamId = (String) session.getAttribute("steamId");
        userInfo = (Map<String, Object>) session.getAttribute("user");
        
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
        
        String steamId;
        
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return error;
        }
        steamId = (String) session.getAttribute("steamId");
        
        return deadlockService.getPlayerMatches(steamId);
    }
    
    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getStats(HttpSession session) {
        String steamId;
        
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return error;
        }
        steamId = (String) session.getAttribute("steamId");
        
        return deadlockService.getPlayerStats(steamId);
    }
    
    /**
     * 날짜 범위별 매치 데이터 조회 API
     */
    @GetMapping("/api/matches/daterange")
    @ResponseBody
    public Map<String, Object> getMatchesByDateRange(HttpSession session,
                                                   @RequestParam String startDate,
                                                   @RequestParam String endDate,
                                                   @RequestParam(required = false) String demo) {
        String steamId;
        
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return error;
        }
        steamId = (String) session.getAttribute("steamId");
        
        return deadlockService.getPlayerMatchesWithDateRange(steamId, startDate, endDate);
    }
    
    /**
     * 패치별 플레이어 데이터 조회 API (Deadlock API와 동일한 형식)
     */
    @GetMapping("/api/patch-data")
    @ResponseBody
    public Map<String, Object> getPatchData(HttpSession session,
                                          @RequestParam(required = false) String patchTab,
                                          @RequestParam(defaultValue = "matches") String tab,
                                          @RequestParam(required = false) String dateRange,
                                          @RequestParam(required = false) String demo) {
        
        String steamId;
        
        Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        if (isLoggedIn == null || !isLoggedIn) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not authenticated");
            return error;
        }
        steamId = (String) session.getAttribute("steamId");
        
        // 날짜 범위 파싱 (2025-05-08T19:43:20.000Z_2025-08-19T23:59:59.999Z 형식)
        String startDate = null;
        String endDate = null;
        
        if (dateRange != null && dateRange.contains("_")) {
            String[] dates = dateRange.split("_");
            if (dates.length == 2) {
                startDate = dates[0];
                endDate = dates[1];
            }
        }
        
        // 기본값 설정 (최근 3개월)
        if (startDate == null || endDate == null) {
            endDate = "2025-08-19T23:59:59.999Z";
            startDate = "2025-05-08T19:43:20.000Z";
        }
        
        Map<String, Object> result = deadlockService.getPlayerDataByPatch(steamId, startDate, endDate);
        
        // Deadlock API와 호환되는 응답 형식으로 변환
        Map<String, Object> response = new HashMap<>();
        response.put("steamId", steamId);
        response.put("tab", tab);
        response.put("patchTab", patchTab != null ? patchTab : "patch");
        response.put("dateRange", dateRange != null ? dateRange : (startDate + "_" + endDate));
        response.put("data", result);
        
        return response;
    }
}