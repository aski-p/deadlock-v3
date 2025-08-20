package com.example.controller;

import com.example.service.SteamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SteamService steamService;

    @GetMapping("/login")
    public String login(HttpServletRequest request, HttpSession session, Model model) {
        // 사용자의 실제 Steam ID로 로그인
        // TODO: 실제 Steam ID를 입력해주세요 (예: "76561198123456789")
        String realSteamId = "76561198015042012"; // 실제 Steam ID로 변경 필요
        
        // Steam API를 통해 실제 사용자 정보 조회
        Map<String, Object> userInfo = steamService.getUserInfo(realSteamId);
        
        if (userInfo != null) {
            // 세션에 사용자 정보 저장
            session.setAttribute("user", userInfo);
            session.setAttribute("steamId", extractShortSteamId(realSteamId)); // 32비트 Steam ID
            session.setAttribute("isLoggedIn", true);
            
            return "redirect:/profile";
        } else {
            // Steam API 호출 실패시 에러 처리
            model.addAttribute("error", "Steam 사용자 정보를 가져올 수 없습니다.");
            return "redirect:/?error=steam_api_failed";
        }
    }
    
    /**
     * 64비트 Steam ID를 32비트로 변환
     */
    private String extractShortSteamId(String steamId64) {
        if (steamId64 != null && steamId64.length() == 17) {
            try {
                long id64 = Long.parseLong(steamId64);
                // Steam ID 64에서 32비트 ID 추출
                long id32 = id64 - 76561197960265728L;
                return String.valueOf(id32);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @GetMapping("/callback")
    public String callback(@RequestParam Map<String, String> params, 
                          HttpSession session, Model model) {
        
        String openIdIdentity = params.get("openid.identity");
        String steamId = steamService.extractSteamId(openIdIdentity);
        
        if (steamId != null && steamService.isValidSteamId(steamId)) {
            // Steam API로 사용자 정보 조회
            Map<String, Object> userInfo = steamService.getUserInfo(steamId);
            
            if (userInfo != null) {
                // 세션에 사용자 정보 저장
                session.setAttribute("user", userInfo);
                session.setAttribute("steamId", steamId);
                session.setAttribute("isLoggedIn", true);
                
                return "redirect:/profile";
            }
        }
        
        // 로그인 실패
        model.addAttribute("error", "Steam 로그인에 실패했습니다.");
        return "redirect:/?error=login_failed";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}