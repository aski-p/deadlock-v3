package com.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SteamService {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamService.class);
    
    @Value("${steam.api.key}")
    private String steamApiKey;
    
    @Value("${steam.api.base.url}")
    private String steamApiBaseUrl;
    
    @Value("${steam.openid.url}")
    private String steamOpenIdUrl;
    
    @Value("${http.client.connection.timeout:30000}")
    private int connectionTimeout;
    
    @Value("${http.client.socket.timeout:30000}")
    private int socketTimeout;
    
    @Value("${http.client.max.connections:100}")
    private int maxConnections;
    
    @Value("${http.client.max.per.route:20}")
    private int maxPerRoute;
    
    @Value("${cache.player.stats.ttl:300}")
    private int cachePlayerStatsTtl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CloseableHttpClient httpClient;
    private final Map<String, CacheEntry> userInfoCache = new ConcurrentHashMap<>();
    
    private static class CacheEntry {
        final Map<String, Object> data;
        final long expireTime;
        
        CacheEntry(Map<String, Object> data, long ttlSeconds) {
            this.data = data;
            this.expireTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
    
    @PostConstruct
    public void init() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxConnections);
        cm.setDefaultMaxPerRoute(maxPerRoute);
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.info("SteamService initialized with connection pool: max={}, maxPerRoute={}", 
                   maxConnections, maxPerRoute);
    }
    
    @PreDestroy
    public void destroy() {
        if (httpClient != null) {
            try {
                httpClient.close();
                logger.info("SteamService HTTP client closed");
            } catch (IOException e) {
                logger.error("Error closing HTTP client", e);
            }
        }
    }
    
    /**
     * Steam ID로 사용자 정보 조회 (캐시 지원)
     */
    public Map<String, Object> getUserInfo(String steamId) {
        if (steamId == null || !isValidSteamId(steamId)) {
            logger.warn("Invalid Steam ID provided: {}", steamId);
            return null;
        }
        
        // 캐시에서 확인
        CacheEntry cached = userInfoCache.get(steamId);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Cache hit for Steam ID: {}", steamId);
            return cached.data;
        }
        
        String url = String.format("%s/ISteamUser/GetPlayerSummaries/v0002/?key=%s&steamids=%s",
                steamApiBaseUrl, steamApiKey, steamId);
        
        logger.debug("Fetching user info for Steam ID: {}", steamId);
        
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Deadlock-Stats-Tracker/1.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    logger.error("Steam API returned status code: {} for Steam ID: {}", statusCode, steamId);
                    return null;
                }
                
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode players = root.get("response").get("players");
                
                if (players.isArray() && players.size() > 0) {
                    JsonNode player = players.get(0);
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("steamId", getJsonValue(player, "steamid", ""));
                    userInfo.put("personaName", getJsonValue(player, "personaname", "Unknown"));
                    userInfo.put("avatarFull", getJsonValue(player, "avatarfull", ""));
                    userInfo.put("avatarMedium", getJsonValue(player, "avatarmedium", ""));
                    userInfo.put("avatar", getJsonValue(player, "avatar", ""));
                    userInfo.put("profileUrl", getJsonValue(player, "profileurl", ""));
                    
                    // 캐시에 저장
                    userInfoCache.put(steamId, new CacheEntry(userInfo, cachePlayerStatsTtl));
                    logger.debug("User info cached for Steam ID: {}", steamId);
                    
                    return userInfo;
                } else {
                    logger.warn("No player data found for Steam ID: {}", steamId);
                }
            }
        } catch (IOException e) {
            logger.error("Error fetching user info for Steam ID: " + steamId, e);
        }
        
        return null;
    }
    
    private String getJsonValue(JsonNode node, String fieldName, String defaultValue) {
        return node.has(fieldName) ? node.get(fieldName).asText() : defaultValue;
    }
    
    /**
     * Steam OpenID 로그인 URL 생성
     */
    public String getOpenIdLoginUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            logger.error("Return URL cannot be null or empty");
            return null;
        }
        
        try {
            StringBuilder loginUrl = new StringBuilder();
            loginUrl.append(steamOpenIdUrl)
                    .append("?openid.ns=http://specs.openid.net/auth/2.0")
                    .append("&openid.mode=checkid_setup")
                    .append("&openid.return_to=").append(returnUrl)
                    .append("&openid.realm=").append(returnUrl.substring(0, returnUrl.lastIndexOf("/")))
                    .append("&openid.identity=http://specs.openid.net/auth/2.0/identifier_select")
                    .append("&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select");
            
            logger.debug("Generated Steam OpenID login URL for return URL: {}", returnUrl);
            return loginUrl.toString();
        } catch (Exception e) {
            logger.error("Error generating Steam OpenID login URL for: " + returnUrl, e);
            return null;
        }
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