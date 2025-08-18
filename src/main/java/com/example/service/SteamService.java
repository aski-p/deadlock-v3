package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class SteamService {
    
    private static final String STEAM_API_KEY = "52BA45A6297B3E697EB6CC84A8D02BEA";
    private static final String STEAM_API_BASE_URL = "http://api.steampowered.com";
    private static final String STEAM_OPENID_URL = "https://steamcommunity.com/openid";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Steam ID로 사용자 정보 조회
     */
    public Map<String, Object> getUserInfo(String steamId) {
        String url = String.format("%s/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
                STEAM_API_BASE_URL, STEAM_API_KEY, steamId);
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode players = root.get("response").get("players");
                
                if (players.isArray() && players.size() > 0) {
                    JsonNode player = players.get(0);
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("steamId", player.get("steamid").asText());
                    userInfo.put("personaName", player.get("personaname").asText());
                    userInfo.put("avatarFull", player.get("avatarfull").asText());
                    userInfo.put("avatarMedium", player.get("avatarmedium").asText());
                    userInfo.put("avatar", player.get("avatar").asText());
                    userInfo.put("profileUrl", player.get("profileurl").asText());
                    
                    return userInfo;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Steam OpenID 로그인 URL 생성
     */
    public String getOpenIdLoginUrl(String returnUrl) {
        StringBuilder loginUrl = new StringBuilder();
        loginUrl.append(STEAM_OPENID_URL)
                .append("?openid.ns=http://specs.openid.net/auth/2.0")
                .append("&openid.mode=checkid_setup")
                .append("&openid.return_to=").append(returnUrl)
                .append("&openid.realm=").append(returnUrl.substring(0, returnUrl.lastIndexOf("/")))
                .append("&openid.identity=http://specs.openid.net/auth/2.0/identifier_select")
                .append("&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select");
        
        return loginUrl.toString();
    }
    
    /**
     * OpenID 응답에서 Steam ID 추출
     */
    public String extractSteamId(String openIdIdentity) {
        if (openIdIdentity != null && openIdIdentity.startsWith("https://steamcommunity.com/openid/id/")) {
            return openIdIdentity.substring("https://steamcommunity.com/openid/id/".length());
        }
        return null;
    }
    
    /**
     * Steam ID 유효성 검증
     */
    public boolean isValidSteamId(String steamId) {
        return steamId != null && steamId.matches("\\d{17}");
    }
}