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
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SteamService steamService;

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        String returnUrl = request.getScheme() + "://" + request.getServerName() + 
                          ":" + request.getServerPort() + "/auth/callback";
        
        String loginUrl = steamService.getOpenIdLoginUrl(returnUrl);
        
        return "redirect:" + loginUrl;
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