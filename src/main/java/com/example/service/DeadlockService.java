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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DeadlockService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeadlockService.class);
    
    @Value("${deadlock.api.base.url}")
    private String deadlockApiBaseUrl;
    
    @Value("${http.client.connection.timeout:30000}")
    private int connectionTimeout;
    
    @Value("${http.client.socket.timeout:30000}")
    private int socketTimeout;
    
    @Value("${cache.match.data.ttl:180}")
    private int cacheMatchDataTtl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CloseableHttpClient httpClient;
    private final Map<String, CacheEntry> matchCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry> statsCache = new ConcurrentHashMap<>();
    
    private static class CacheEntry {
        final Object data;
        final long expireTime;
        
        CacheEntry(Object data, long ttlSeconds) {
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
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(10);
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.info("DeadlockService initialized with HTTP client");
    }
    
    @PreDestroy
    public void destroy() {
        if (httpClient != null) {
            try {
                httpClient.close();
                logger.info("DeadlockService HTTP client closed");
            } catch (IOException e) {
                logger.error("Error closing HTTP client", e);
            }
        }
    }
    
    /**
     * 플레이어 매치 데이터 조회 (캐시 지원)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPlayerMatches(String steamId) {
        if (steamId == null || steamId.trim().isEmpty()) {
            logger.warn("Invalid Steam ID provided for match data: {}", steamId);
            return createEmptyResponse();
        }
        
        // 캐시 확인 (API가 불안정하므로 캐시 사용 중단)
        // CacheEntry cached = matchCache.get(steamId);
        // if (cached != null && !cached.isExpired()) {
        //     logger.debug("Cache hit for match data: {}", steamId);
        //     return (Map<String, Object>) cached.data;
        // }
        
        // 실제 Deadlock JSON API 사용 - 2단계 프로세스
        try {
            // Step 1: 매치 히스토리 가져오기
            String matchHistoryUrl = String.format("https://api.deadlock-api.com/v1/players/%s/match-history", steamId);
            logger.info("Fetching match history from: {}", matchHistoryUrl);
            
            HttpGet historyRequest = new HttpGet(matchHistoryUrl);
            historyRequest.setHeader("User-Agent", "Mozilla/5.0 (Deadlock-Stats-Tracker/1.0)");
            historyRequest.setHeader("Accept", "application/json");
            
            String historyResponse;
            try (CloseableHttpResponse response = httpClient.execute(historyRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();
                historyResponse = EntityUtils.toString(response.getEntity());
                
                if (statusCode != 200) {
                    logger.warn("Match history API returned status: {} for Steam ID: {}", statusCode, steamId);
                    return createEmptyResponse();
                }
            }
            
            // 매치 히스토리 파싱하여 매치 ID들 추출
            logger.info("History response length: {}", historyResponse.length());
            logger.info("First 500 chars of history response: {}", historyResponse.substring(0, Math.min(500, historyResponse.length())));
            
            JsonNode historyArray = objectMapper.readTree(historyResponse);
            logger.info("Parsed JSON - isArray: {}, size: {}", historyArray.isArray(), historyArray.size());
            
            if (!historyArray.isArray() || historyArray.size() == 0) {
                logger.warn("No matches found for Steam ID: {} - isArray: {}, size: {}", steamId, historyArray.isArray(), historyArray.size());
                return createEmptyResponse();
            }
            
            // Step 2: 상세 매치 데이터 가져오기 (최근 20개만)
            List<String> matchIds = new ArrayList<>();
            logger.info("Processing match history with {} total matches", historyArray.size());
            
            for (int i = 0; i < Math.min(20, historyArray.size()); i++) {
                JsonNode match = historyArray.get(i);
                if (match.has("match_id")) {
                    String matchId = match.get("match_id").asText();
                    matchIds.add(matchId);
                    logger.info("Added match ID {}: {}", i+1, matchId);
                }
            }
            
            if (matchIds.isEmpty()) {
                return createEmptyResponse();
            }
            
            logger.info("Extracted match IDs: [{}]", String.join(", ", matchIds));
            
            String matchIdsParam = String.join(",", matchIds);
            String metadataUrl = String.format(
                "https://api.deadlock-api.com/v1/matches/metadata?include_info=true&include_player_info=true&include_player_items=true&match_ids=%s",
                matchIdsParam);
            
            logger.info("Fetching match metadata for {} matches from: {}", matchIds.size(), metadataUrl);
            
            HttpGet metadataRequest = new HttpGet(metadataUrl);
            metadataRequest.setHeader("User-Agent", "Mozilla/5.0 (Deadlock-Stats-Tracker/1.0)");
            metadataRequest.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(metadataRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String metadataResponse = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    logger.info("Metadata API response: {}", metadataResponse.substring(0, Math.min(500, metadataResponse.length())));
                    JsonNode metadataRoot = objectMapper.readTree(metadataResponse);
                    
                    Map<String, Object> result = new HashMap<>();
                    List<Map<String, Object>> parsedMatches = parseDeadlockMetadataResponseWithOrder(metadataRoot, historyArray, steamId);
                    
                    result.put("matches", parsedMatches);
                    result.put("totalMatches", parsedMatches.size());
                    
                    logger.info("Successfully fetched {} matches for Steam ID: {}", parsedMatches.size(), steamId);
                    return result;
                } else {
                    logger.warn("Match metadata API returned status: {} for Steam ID: {}", statusCode, steamId);
                    return createEmptyResponse();
                }
            }
            
        } catch (IOException e) {
            logger.error("Error fetching match data from Deadlock API for Steam ID: " + steamId, e);
            return createEmptyResponse();
        }
    }
    
    /**
     * 날짜 범위별 플레이어 매치 데이터 조회
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPlayerMatchesWithDateRange(String steamId, String startDate, String endDate) {
        if (steamId == null || steamId.trim().isEmpty()) {
            logger.warn("Invalid Steam ID provided for match data: {}", steamId);
            return createEmptyResponse();
        }
        
        // 기본 매치 데이터 조회
        Map<String, Object> matchData = getPlayerMatches(steamId);
        List<Map<String, Object>> allMatches = (List<Map<String, Object>>) matchData.get("matches");
        
        // 날짜 필터링
        if (startDate != null && endDate != null) {
            List<Map<String, Object>> filteredMatches = filterMatchesByDateRange(allMatches, startDate, endDate);
            Map<String, Object> result = new HashMap<>();
            result.put("matches", filteredMatches);
            result.put("totalMatches", filteredMatches.size());
            return result;
        }
        
        return matchData;
    }
    
    /**
     * 패치별 플레이어 데이터 조회 (패치 날짜 기반)
     */
    public Map<String, Object> getPlayerDataByPatch(String steamId, String patchStartDate, String patchEndDate) {
        logger.info("Fetching player data for Steam ID: {} from {} to {}", steamId, patchStartDate, patchEndDate);
        
        // 실제 Deadlock API 호출 - 날짜 범위 포함
        String dateRange = patchStartDate + "_" + patchEndDate;
        String url = String.format("https://deadlock-api.com/player?pd-picker-tab=patch&steam_id=%s&tab=matches&date_range=%s", 
                                   steamId, dateRange);
        
        logger.info("Calling Deadlock API with date range: {}", url);
        
        try {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0 (Deadlock-Stats-Tracker/1.0)");
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String jsonResponse = EntityUtils.toString(response.getEntity());
                
                logger.info("Patch data API response - Status: {}, Body length: {}", statusCode, jsonResponse.length());
                
                Map<String, Object> result = new HashMap<>();
                
                if (statusCode == 200) {
                    // HTML 응답 체크 (실제 API가 아직 사용 불가능한 경우)
                    if (jsonResponse.trim().startsWith("<!DOCTYPE html") || jsonResponse.trim().startsWith("<html")) {
                        logger.warn("Deadlock API returned HTML instead of JSON for patch data - API not available yet, returning empty data");
                        result.put("matches", new ArrayList<>());
                        result.put("totalMatches", 0);
                        result.put("stats", createEmptyStats());
                        result.put("patchPeriod", Map.of("start", patchStartDate, "end", patchEndDate));
                        
                        return result;
                    }
                    
                    JsonNode root = objectMapper.readTree(jsonResponse);
                    List<Map<String, Object>> matches = parseDeadlockMetadataResponse(root, steamId);
                    Map<String, Object> stats = calculateStatsFromMatches(matches);
                    
                    result.put("matches", matches);
                    result.put("totalMatches", matches.size());
                    result.put("stats", stats);
                    result.put("patchPeriod", Map.of("start", patchStartDate, "end", patchEndDate));
                    
                    logger.info("Successfully fetched {} matches for patch period", matches.size());
                } else {
                    logger.warn("Patch data API failed with status: {}, returning empty data", statusCode);
                    result.put("matches", new ArrayList<>());
                    result.put("totalMatches", 0);
                    result.put("stats", createEmptyStats());
                    result.put("patchPeriod", Map.of("start", patchStartDate, "end", patchEndDate));
                }
                
                return result;
            }
        } catch (IOException e) {
            logger.error("Error fetching patch data from Deadlock API", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("matches", new ArrayList<>());
            result.put("totalMatches", 0);
            result.put("stats", createEmptyStats());
            result.put("patchPeriod", Map.of("start", patchStartDate, "end", patchEndDate));
            
            return result;
        }
    }
    
    /**
     * 플레이어 통계 정보 조회
     */
    public Map<String, Object> getPlayerStats(String steamId) {
        // 현재 API가 사용 불가능하므로 매치 데이터 기반으로 통계 생성
        logger.info("Generating stats from match data for Steam ID: {}", steamId);
        
        try {
            Map<String, Object> matchData = getPlayerMatches(steamId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matches = (List<Map<String, Object>>) matchData.get("matches");
            return calculateStatsFromMatches(matches);
        } catch (Exception e) {
            logger.error("Error generating stats for Steam ID: " + steamId, e);
            return createEmptyStats();
        }
    }
    
    /**
     * 플레이어 프로필 정보 조회
     */
    public Map<String, Object> getPlayerProfile(String steamId) {
        Map<String, Object> profile = new HashMap<>();
        
        // 기본 통계 정보
        Map<String, Object> stats = getPlayerStats(steamId);
        profile.putAll(stats);
        
        // 최근 매치 정보
        Map<String, Object> matchData = getPlayerMatches(steamId);
        profile.put("recentMatches", matchData.get("matches"));
        profile.put("totalMatches", matchData.get("totalMatches"));
        
        // 랭킹 정보는 API에서 제공되지 않으므로 생략
        
        return profile;
    }
    
    private List<Map<String, Object>> parseDeadlockMatches(JsonNode matchesNode) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        if (matchesNode.isArray()) {
            for (JsonNode match : matchesNode) {
                Map<String, Object> matchData = new HashMap<>();
                matchData.put("matchId", match.has("match_id") ? match.get("match_id").asText() : "");
                matchData.put("result", match.has("win") ? (match.get("win").asBoolean() ? "WIN" : "LOSS") : "UNKNOWN");
                matchData.put("hero", match.has("hero_name") ? match.get("hero_name").asText() : "Unknown");
                matchData.put("kills", match.has("player_kills") ? match.get("player_kills").asInt() : 0);
                matchData.put("deaths", match.has("player_deaths") ? match.get("player_deaths").asInt() : 0);
                matchData.put("assists", match.has("player_assists") ? match.get("player_assists").asInt() : 0);
                matchData.put("duration", match.has("match_duration_s") ? formatDuration(match.get("match_duration_s").asInt()) : "00:00");
                matchData.put("startTime", match.has("start_time") ? match.get("start_time").asLong() : 0);
                matchData.put("netWorth", match.has("net_worth") ? match.get("net_worth").asInt() : 0);
                matchData.put("heroImage", getHeroImagePath(match.has("hero_name") ? match.get("hero_name").asText() : ""));
                
                matches.add(matchData);
            }
        }
        
        return matches;
    }

    /**
     * match-history 순서를 유지하면서 메타데이터 파싱
     */
    private List<Map<String, Object>> parseDeadlockMetadataResponseWithOrder(JsonNode metadataRoot, JsonNode historyArray, String targetSteamId) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        try {
            // 메타데이터를 match_id로 매핑
            Map<String, JsonNode> metadataMap = new HashMap<>();
            if (metadataRoot.isArray()) {
                for (JsonNode metadata : metadataRoot) {
                    String matchId = metadata.get("match_id").asText();
                    metadataMap.put(matchId, metadata);
                }
            }
            
            // match-history 순서대로 처리 (최신 순서 유지)
            for (JsonNode historyMatch : historyArray) {
                String matchId = historyMatch.get("match_id").asText();
                JsonNode metadata = metadataMap.get(matchId);
                
                if (metadata != null) {
                    Map<String, Object> matchData = parseMatchMetadataWithHistory(metadata, historyMatch, targetSteamId);
                    if (matchData != null) {
                        matches.add(matchData);
                    }
                }
            }
            
            logger.info("Parsed {} matches in history order", matches.size());
            
        } catch (Exception e) {
            logger.error("Error parsing Deadlock metadata with order", e);
        }
        
        return matches;
    }
    
    /**
     * Deadlock API v1 메타데이터 응답 파싱
     */
    private List<Map<String, Object>> parseDeadlockMetadataResponse(JsonNode root, String targetSteamId) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        try {
            // API v1 메타데이터 응답 구조: 배열 또는 객체
            JsonNode matchesData = root.isArray() ? root : root.get("matches");
            if (matchesData == null) {
                matchesData = root; // 전체가 매치 데이터일 수 있음
            }
            
            if (matchesData.isArray()) {
                for (JsonNode matchNode : matchesData) {
                    Map<String, Object> matchData = parseMatchMetadata(matchNode, targetSteamId);
                    if (matchData != null) {
                        matches.add(matchData);
                    }
                }
            } else if (matchesData.isObject()) {
                // 단일 매치 데이터
                Map<String, Object> matchData = parseMatchMetadata(matchesData, targetSteamId);
                if (matchData != null) {
                    matches.add(matchData);
                }
            }
            
            logger.info("Parsed {} matches from Deadlock metadata API", matches.size());
            
        } catch (Exception e) {
            logger.error("Error parsing Deadlock metadata API response", e);
        }
        
        return matches;
    }
    
    /**
     * match-history와 metadata를 결합하여 파싱 (승패 정보 포함)
     */
    private Map<String, Object> parseMatchMetadataWithHistory(JsonNode metadataNode, JsonNode historyNode, String targetSteamId) {
        try {
            Map<String, Object> matchData = new HashMap<>();
            
            // match-history에서 기본 정보 가져오기
            matchData.put("matchId", historyNode.get("match_id").asText());
            matchData.put("startTime", historyNode.get("start_time").asLong() * 1000); // 초를 밀리초로
            
            // match-history에서 승패 정보 가져오기 (가장 정확함)
            int matchResult = historyNode.get("match_result").asInt();
            matchData.put("result", matchResult == 1 ? "WIN" : "LOSS");
            
            // 게임 시간
            if (metadataNode.has("duration_s")) {
                matchData.put("duration", formatDuration(metadataNode.get("duration_s").asInt()));
            } else if (historyNode.has("match_duration_s")) {
                matchData.put("duration", formatDuration(historyNode.get("match_duration_s").asInt()));
            }
            
            // 영웅 정보 (match-history에서)
            int heroId = historyNode.get("hero_id").asInt();
            String heroName = getHeroNameById(heroId);
            matchData.put("hero", heroName);
            matchData.put("heroImage", getHeroImagePath(heroName));
            
            // KDA 정보 (match-history에서)
            matchData.put("kills", historyNode.get("player_kills").asInt());
            matchData.put("deaths", historyNode.get("player_deaths").asInt());
            matchData.put("assists", historyNode.get("player_assists").asInt());
            matchData.put("netWorth", historyNode.get("net_worth").asInt());
            
            // Final Items 파싱 (metadata에서)
            List<Map<String, Object>> finalItems = new ArrayList<>();
            if (metadataNode.has("players") && metadataNode.get("players").isArray()) {
                JsonNode playersNode = metadataNode.get("players");
                for (JsonNode playerNode : playersNode) {
                    long playerAccountId = playerNode.has("account_id") ? playerNode.get("account_id").asLong() : 0;
                    
                    // Steam ID 매칭 (다양한 형식 지원)
                    if (isMatchingSteamId(String.valueOf(playerAccountId), targetSteamId)) {
                        logger.debug("Found target player in metadata for match {}", matchData.get("matchId"));
                        finalItems = extractFinalItemsFromPlayer(playerNode);
                        break;
                    }
                }
            }
            matchData.put("finalItems", finalItems);
            
            logger.debug("Match {} - Final items count: {}", matchData.get("matchId"), finalItems.size());
            
            return matchData;
            
        } catch (Exception e) {
            logger.error("Error parsing match metadata with history", e);
            return null;
        }
    }
    
    /**
     * 개별 매치 메타데이터 파싱
     */
    private Map<String, Object> parseMatchMetadata(JsonNode matchNode, String targetSteamId) {
        try {
            Map<String, Object> matchData = new HashMap<>();
            
            // 매치 ID
            String matchId = matchNode.has("match_id") ? matchNode.get("match_id").asText() : 
                           matchNode.has("id") ? matchNode.get("id").asText() : "unknown";
            matchData.put("matchId", matchId);
            
            // 매치 기본 정보
            if (matchNode.has("start_time")) {
                matchData.put("startTime", matchNode.get("start_time").asLong() * 1000); // 초를 밀리초로
            }
            
            if (matchNode.has("duration_s")) {
                matchData.put("duration", formatDuration(matchNode.get("duration_s").asInt()));
            }
            
            // 플레이어 정보 찾기 (메타데이터 API는 바로 최상위에 players가 있음)
            JsonNode playersNode = matchNode.get("players");
            if (playersNode != null && playersNode.isArray()) {
                logger.info("Match {} has {} players, looking for Steam ID: {}", matchData.get("matchId"), playersNode.size(), targetSteamId);
                
                // 모든 플레이어의 account_id를 로그로 출력
                StringBuilder allPlayers = new StringBuilder();
                for (JsonNode playerNode : playersNode) {
                    long playerAccountId = playerNode.has("account_id") ? playerNode.get("account_id").asLong() : 0;
                    allPlayers.append(playerAccountId).append(", ");
                }
                logger.info("All players in match {}: [{}]", matchData.get("matchId"), allPlayers.toString());
                
                for (JsonNode playerNode : playersNode) {
                    long playerAccountId = playerNode.has("account_id") ? playerNode.get("account_id").asLong() : 0;
                    logger.info("Checking player account_id: {} vs target Steam ID: {}", playerAccountId, targetSteamId);
                    
                    // 다양한 Steam ID 형식 매칭 시도
                    boolean isMatch = false;
                    
                    // 1. 직접 문자열 매칭
                    if (String.valueOf(playerAccountId).equals(targetSteamId)) {
                        isMatch = true;
                        logger.info("Match found: Direct string match");
                    }
                    
                    // 2. 64비트 Steam ID에서 32비트로 변환 시도
                    if (!isMatch) {
                        try {
                            long targetId64 = Long.parseLong(targetSteamId);
                            if (targetId64 > 76561197960265728L) {
                                long converted32 = targetId64 - 76561197960265728L;
                                if (converted32 == playerAccountId) {
                                    isMatch = true;
                                    logger.info("Match found: 64bit to 32bit conversion ({} -> {})", targetId64, converted32);
                                }
                            }
                        } catch (NumberFormatException e) {
                            // 무시
                        }
                    }
                    
                    // 3. 32비트를 64비트로 변환 시도
                    if (!isMatch) {
                        try {
                            long targetId32 = Long.parseLong(targetSteamId);
                            long converted64 = targetId32 + 76561197960265728L;
                            if (converted64 == playerAccountId) {
                                isMatch = true;
                                logger.info("Match found: 32bit to 64bit conversion ({} -> {})", targetId32, converted64);
                            }
                        } catch (NumberFormatException e) {
                            // 무시
                        }
                    }
                    
                    if (isMatch) {
                        logger.info("Found target player {} in match {}", targetSteamId, matchData.get("matchId"));
                        // 대상 플레이어 데이터 추출
                        extractPlayerStatsFromMetadata(playerNode, matchData);
                        
                        // 팀 승패 결정
                        int playerTeam = playerNode.has("player_slot") ? 
                            (playerNode.get("player_slot").asInt() < 6 ? 0 : 1) : 0;
                        if (matchNode.has("winning_team")) {
                            int winningTeam = matchNode.get("winning_team").asInt();
                            matchData.put("result", playerTeam == winningTeam ? "WIN" : "LOSS");
                        } else {
                            matchData.put("result", "UNKNOWN");
                        }
                        
                        break;
                    }
                }
            }
            
            // 필수 필드가 없으면 null 반환
            if (!matchData.containsKey("hero") || !matchData.containsKey("kills")) {
                logger.warn("Player {} not found in match {} or missing required data", targetSteamId, matchData.get("matchId"));
                return null;
            }
            
            return matchData;
            
        } catch (Exception e) {
            logger.error("Error parsing individual match metadata", e);
            return null;
        }
    }
    
    /**
     * 매치 메타데이터에서 플레이어 통계 추출 
     */
    private void extractPlayerStatsFromMetadata(JsonNode playerNode, Map<String, Object> matchData) {
        // Hero ID를 hero name으로 변환
        int heroId = playerNode.has("hero_id") ? playerNode.get("hero_id").asInt() : 0;
        String heroName = getHeroNameById(heroId);
        matchData.put("hero", heroName);
        matchData.put("heroImage", getHeroImagePath(heroName));
        
        // KDA 정보 (메타데이터 API는 직접 제공)
        matchData.put("kills", playerNode.has("kills") ? playerNode.get("kills").asInt() : 0);
        matchData.put("deaths", playerNode.has("deaths") ? playerNode.get("deaths").asInt() : 0);
        matchData.put("assists", playerNode.has("assists") ? playerNode.get("assists").asInt() : 0);
        matchData.put("netWorth", playerNode.has("net_worth") ? playerNode.get("net_worth").asInt() : 0);
        
        // Final Items 정보 추출 - 최종 12개 아이템만
        List<Map<String, Object>> finalItems = extractFinalItemsFromPlayer(playerNode);
        matchData.put("finalItems", finalItems);
    }
    
    /**
     * 플레이어 통계 추출 (기존 방식)
     */
    private void extractPlayerStats(JsonNode playerNode, Map<String, Object> matchData) {
        // Hero ID를 hero name으로 변환 (나중에 매핑 테이블 추가 가능)
        int heroId = playerNode.has("hero_id") ? playerNode.get("hero_id").asInt() : 0;
        String heroName = getHeroNameById(heroId);
        matchData.put("hero", heroName);
        matchData.put("heroImage", getHeroImagePath(heroName));
        
        // KDA 정보
        matchData.put("kills", playerNode.has("kills") ? playerNode.get("kills").asInt() : 0);
        matchData.put("deaths", playerNode.has("deaths") ? playerNode.get("deaths").asInt() : 0);
        matchData.put("assists", playerNode.has("assists") ? playerNode.get("assists").asInt() : 0);
        
        // 네트워스
        matchData.put("netWorth", playerNode.has("net_worth") ? playerNode.get("net_worth").asInt() : 0);
        
        // Final Items 정보 추출
        List<Map<String, Object>> finalItems = new ArrayList<>();
        if (playerNode.has("items")) {
            JsonNode itemsNode = playerNode.get("items");
            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", itemNode.has("item_id") ? itemNode.get("item_id").asInt() : 0);
                    item.put("name", itemNode.has("name") ? itemNode.get("name").asText() : "Unknown Item");
                    item.put("image", getItemImagePath(itemNode.has("item_id") ? itemNode.get("item_id").asInt() : 0));
                    finalItems.add(item);
                }
            }
        }
        matchData.put("finalItems", finalItems);
    }
    
    /**
     * Hero ID를 name으로 변환 (임시 구현)
     */
    private Map<Integer, String> heroNameCache = new HashMap<>();
    private boolean heroNamesLoaded = false;
    
    private String getHeroNameById(int heroId) {
        if (!heroNamesLoaded) {
            loadHeroNamesFromAPI();
        }
        return heroNameCache.getOrDefault(heroId, "Unknown Hero");
    }
    
    private void loadHeroNamesFromAPI() {
        try {
            String heroesUrl = "https://assets.deadlock-api.com/v2/heroes?only_active=true";
            logger.info("Loading hero names and images from API: {}", heroesUrl);
            
            HttpGet request = new HttpGet(heroesUrl);
            request.setHeader("User-Agent", "Mozilla/5.0 (Deadlock-Stats-Tracker/1.0)");
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JsonNode heroesArray = objectMapper.readTree(responseBody);
                    
                    if (heroesArray.isArray()) {
                        for (JsonNode hero : heroesArray) {
                            int id = hero.get("id").asInt();
                            String name = hero.get("name").asText();
                            heroNameCache.put(id, name);
                            
                            // 이미지 URL도 함께 캐시
                            if (hero.has("images")) {
                                JsonNode images = hero.get("images");
                                if (images.has("selection_image")) {
                                    String imageUrl = images.get("selection_image").asText();
                                    heroImageCache.put(id, imageUrl);
                                }
                            }
                        }
                        heroNamesLoaded = true;
                        logger.info("Successfully loaded {} hero names and {} images", heroNameCache.size(), heroImageCache.size());
                    }
                } else {
                    logger.warn("Failed to load heroes API, status: {}", statusCode);
                    fallbackToDefaultHeroNames();
                }
            }
        } catch (Exception e) {
            logger.error("Error loading heroes from API, using fallback", e);
            fallbackToDefaultHeroNames();
        }
    }
    
    private void fallbackToDefaultHeroNames() {
        // 최신 Deadlock 영웅 목록 (실제 API에서 확인된 ID들)
        heroNameCache.put(1, "Infernus");
        heroNameCache.put(2, "Seven");
        heroNameCache.put(3, "Vindicta"); 
        heroNameCache.put(4, "Lady Geist");
        heroNameCache.put(6, "Abrams");
        heroNameCache.put(7, "Wraith");
        heroNameCache.put(8, "McGinnis");
        heroNameCache.put(9, "Paradox");
        heroNameCache.put(10, "Dynamo");
        heroNameCache.put(11, "Kelvin");
        heroNameCache.put(12, "Haze");
        heroNameCache.put(13, "Mirage");
        heroNameCache.put(14, "Lash");
        heroNameCache.put(15, "Bebop");
        heroNameCache.put(16, "Viscous");
        heroNameCache.put(17, "Pocket");
        heroNameCache.put(18, "Ivy");
        heroNameCache.put(19, "Grey Talon");
        heroNameCache.put(20, "Mo & Krill");
        heroNameCache.put(21, "Shiv");
        heroNameCache.put(22, "Yamato");
        heroNameCache.put(27, "Warden");
        heroNameCache.put(28, "Calico"); // 새 영웅들 추가
        heroNameCache.put(29, "Magician");
        
        heroNamesLoaded = true;
        logger.info("Loaded {} fallback hero names", heroNameCache.size());
    }
    
    
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
    
    private String getHeroImagePath(String heroName) {
        // 실제 Deadlock heroes API에서 이미지 URL을 가져오도록 수정
        if (!heroNamesLoaded) {
            loadHeroNamesFromAPI();
        }
        
        // WebP 이미지를 사용하는 특별 히어로들 처리
        if ("Lash".equalsIgnoreCase(heroName)) {
            return "/resources/images/heroes/lash.webp";
        } else if ("Holliday".equalsIgnoreCase(heroName)) {
            return "/resources/images/heroes/holliday.webp";
        } else if ("Mirage".equalsIgnoreCase(heroName)) {
            return "/resources/images/heroes/mirage.jpg";
        } else if ("Vyper".equalsIgnoreCase(heroName)) {
            return "/resources/images/heroes/vyper.jpg";
        }
        
        // heroImageCache에서 이미지 URL 찾기
        for (Map.Entry<Integer, String> entry : heroNameCache.entrySet()) {
            if (entry.getValue().equals(heroName)) {
                return getHeroImageById(entry.getKey());
            }
        }
        
        // 기본 이미지 반환
        return "/resources/images/heroes/default.jpg";
    }
    
    private Map<Integer, String> heroImageCache = new HashMap<>();
    
    // 아이템 관련 캐시
    private final Map<Long, String> itemNameCache = new ConcurrentHashMap<>();
    private final Map<Long, String> itemImageCache = new ConcurrentHashMap<>();
    private boolean itemsLoaded = false;
    
    private String getHeroImageById(int heroId) {
        // 캐시에서 이미지 URL 찾기
        if (heroImageCache.containsKey(heroId)) {
            return heroImageCache.get(heroId);
        }
        
        // Heroes API가 아직 로드되지 않았다면 로드하기
        if (!heroNamesLoaded) {
            loadHeroNamesFromAPI();
        }
        
        // 캐시에서 다시 확인
        if (heroImageCache.containsKey(heroId)) {
            return heroImageCache.get(heroId);
        }
        
        // 영웅 이름 기반 이미지 URL 생성 (실제 API가 안될 때 대안)
        String heroName = getHeroNameById(heroId);
        if (!"Unknown Hero".equals(heroName)) {
            // 영웅 이름을 기반으로 로컬 이미지 경로 생성
            String heroImagePath = "/resources/images/heroes/" + heroName.toLowerCase().replace(" ", "_").replace("&", "and") + ".jpg";
            heroImageCache.put(heroId, heroImagePath);
            return heroImagePath;
        }
        
        // 최종 기본 이미지
        String defaultImage = "/resources/images/heroes/default.jpg";
        heroImageCache.put(heroId, defaultImage);
        return defaultImage;
    }
    
    private String getItemImagePath(int itemId) {
        // int를 long으로 변환하여 공통 메서드 호출
        return getItemImagePath((long) itemId);
    }
    
    private String getItemImagePath(long itemId) {
        // Long 타입 아이템 ID용 오버로드
        if (itemId == 0) {
            return "/resources/images/items/default.svg";
        }
        
        // 아이템 이름과 이미지 정보 로드 (필요시)
        if (!itemsLoaded) {
            loadItemsFromAPI();
        }
        
        // 아이템 이미지 캐시에서 경로 확인
        String imagePath = itemImageCache.get(itemId);
        if (imagePath != null && !imagePath.isEmpty()) {
            logger.debug("Found image path for item {}: {}", itemId, imagePath);
            return imagePath;
        }
        
        // 캐시에 없으면 기본 이미지 반환
        logger.debug("No image found for item {}, using default", itemId);
        return "/resources/images/items/default.svg";
    }
    
    private String getItemName(int itemId) {
        // 주요 아이템들의 이름 매핑 (실제 게임 데이터 기반)
        Map<Integer, String> itemNames = Map.of(
            539192269, "Basic Magazine",
            1548066885, "Sprint Boots", 
            1074714947, "Ammo Scavenger",
            1065103387, "Extra Stamina",
            395867183, "Restorative Shot",
            968099481, "High-Velocity Mag"
        );
        
        // 큰 아이템 ID들을 위한 추가 맵핑
        Map<Long, String> largeItemNames = Map.of(
            2356412290L, "Monster Rounds",
            3970837787L, "Headshot Booster", 
            2061574352L, "Extra Health",
            3403085434L, "Divine Barrier"
        );
        
        String name = itemNames.get(itemId);
        if (name != null) {
            return name;
        }
        
        name = largeItemNames.get((long) itemId);
        if (name != null) {
            return name;
        }
        
        return "Item #" + itemId;
    }
    
    private String getItemName(long itemId) {
        // 아이템 데이터가 로드되지 않았다면 로드
        if (!itemsLoaded) {
            loadItemsFromAPI();
        }
        
        // 캐시에서 아이템 이름 찾기
        String cachedName = itemNameCache.get(itemId);
        if (cachedName != null) {
            return cachedName;
        }
        
        // Long 타입 아이템 ID용 오버로드 (fallback)
        if (itemId <= Integer.MAX_VALUE) {
            return getItemName((int) itemId);
        }
        
        // 큰 아이템 ID들을 위한 기본 매핑 (fallback)
        Map<Long, String> largeItemNames = Map.of(
            2356412290L, "Monster Rounds",
            3970837787L, "Headshot Booster", 
            2061574352L, "Extra Health",
            3403085434L, "Divine Barrier",
            4179229681L, "Basic Magazine",
            2839006491L, "Spirit Strike"
        );
        
        String name = largeItemNames.get(itemId);
        if (name != null) {
            return name;
        }
        
        return "Item #" + itemId;
    }
    
    private Map<String, Object> createEmptyResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("matches", new ArrayList<>());
        response.put("totalMatches", 0);
        return response;
    }
    
    private Map<String, Object> createEmptyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalKills", 0);
        stats.put("totalDeaths", 0);
        stats.put("totalAssists", 0);
        stats.put("winRate", 0.0);
        stats.put("avgKDA", 0.0);
        stats.put("favoriteHero", "Unknown");
        return stats;
    }
    
    /**
     * 날짜 범위로 매치 필터링
     */
    private List<Map<String, Object>> filterMatchesByDateRange(List<Map<String, Object>> matches, String startDate, String endDate) {
        try {
            long startTimestamp = parseISODate(startDate);
            long endTimestamp = parseISODate(endDate);
            
            return matches.stream()
                    .filter(match -> {
                        Long matchTime = (Long) match.get("startTime");
                        if (matchTime == null) return false;
                        return matchTime >= startTimestamp && matchTime <= endTimestamp;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error filtering matches by date range: {} - {}", startDate, endDate, e);
            return matches; // 필터링 실패 시 전체 매치 반환
        }
    }
    
    /**
     * ISO 날짜 문자열을 타임스탬프로 변환
     */
    private long parseISODate(String dateStr) {
        try {
            // "2025-05-08T19:43:20.000Z" 형식 파싱
            Instant instant = Instant.parse(dateStr);
            return instant.toEpochMilli();
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}, using current time", dateStr);
            return System.currentTimeMillis();
        }
    }
    
    /**
     * 매치 리스트에서 통계 계산
     */
    private Map<String, Object> calculateStatsFromMatches(List<Map<String, Object>> matches) {
        Map<String, Object> stats = new HashMap<>();
        
        if (matches.isEmpty()) {
            return createEmptyStats();
        }
        
        int totalKills = 0;
        int totalDeaths = 0;
        int totalAssists = 0;
        int wins = 0;
        Map<String, Integer> heroCount = new HashMap<>();
        
        for (Map<String, Object> match : matches) {
            totalKills += (Integer) match.getOrDefault("kills", 0);
            totalDeaths += (Integer) match.getOrDefault("deaths", 0);
            totalAssists += (Integer) match.getOrDefault("assists", 0);
            
            if ("WIN".equals(match.get("result"))) {
                wins++;
            }
            
            String hero = (String) match.getOrDefault("hero", "Unknown");
            heroCount.put(hero, heroCount.getOrDefault(hero, 0) + 1);
        }
        
        // 가장 많이 플레이한 영웅
        String favoriteHero = heroCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
        
        // 주 캐릭터 이미지
        String favoriteHeroImage = getHeroImagePath(favoriteHero);
        
        double winRate = matches.size() > 0 ? (double) wins / matches.size() * 100 : 0.0;
        double avgKDA = totalDeaths > 0 ? (double) (totalKills + totalAssists) / totalDeaths : 
                        (totalKills + totalAssists > 0 ? 999.0 : 0.0);
        
        stats.put("totalKills", totalKills);
        stats.put("totalDeaths", totalDeaths);
        stats.put("totalAssists", totalAssists);
        stats.put("winRate", Math.round(winRate * 100.0) / 100.0);
        stats.put("avgKDA", Math.round(avgKDA * 100.0) / 100.0);
        stats.put("favoriteHero", favoriteHero);
        stats.put("favoriteHeroImage", favoriteHeroImage);
        stats.put("totalMatches", matches.size());
        stats.put("wins", wins);
        stats.put("losses", matches.size() - wins);
        
        return stats;
    }
    
    /**
     * Steam ID 매칭 여부 확인 (다양한 형식 지원)
     */
    private boolean isMatchingSteamId(String playerAccountId, String targetSteamId) {
        // 1. 직접 문자열 매칭
        if (playerAccountId.equals(targetSteamId)) {
            return true;
        }
        
        // 2. 64비트 Steam ID에서 32비트로 변환 시도
        try {
            long targetId64 = Long.parseLong(targetSteamId);
            if (targetId64 > 76561197960265728L) {
                long converted32 = targetId64 - 76561197960265728L;
                if (converted32 == Long.parseLong(playerAccountId)) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            // 무시
        }
        
        // 3. 32비트에서 64비트로 변환 시도
        try {
            long playerId32 = Long.parseLong(playerAccountId);
            long converted64 = playerId32 + 76561197960265728L;
            if (String.valueOf(converted64).equals(targetSteamId)) {
                return true;
            }
        } catch (NumberFormatException e) {
            // 무시
        }
        
        return false;
    }
    
    /**
     * 플레이어 노드에서 Final Items 추출
     */
    private List<Map<String, Object>> extractFinalItemsFromPlayer(JsonNode playerNode) {
        List<Map<String, Object>> finalItems = new ArrayList<>();
        
        if (playerNode.has("items") && playerNode.get("items").isArray()) {
            JsonNode itemsArray = playerNode.get("items");
            logger.debug("Found {} total items for player", itemsArray.size());
            
            // 아이템 슬롯 관리 (최대 12개)
            Map<Long, Map<String, Object>> currentItems = new HashMap<>();
            
            for (JsonNode itemNode : itemsArray) {
                long originalItemId = itemNode.has("item_id") ? itemNode.get("item_id").asLong() : 0;
                
                // sold_time_s가 있으면 해당 아이템을 인벤토리에서 제거
                if (itemNode.has("sold_time_s")) {
                    int soldTime = itemNode.get("sold_time_s").asInt();
                    if (soldTime > 0) {
                        currentItems.remove(originalItemId);
                        logger.debug("Removed sold item: {}", originalItemId);
                        continue;
                    }
                }
                
                // flags가 0이 아니면 스킵 (특수 아이템)
                int flags = itemNode.has("flags") ? itemNode.get("flags").asInt() : -1;
                if (flags != 0) {
                    logger.debug("Skipping item with flags: {}", flags);
                    continue;
                }
                
                if (originalItemId > 0) {
                    // 업그레이드 처리
                    long displayItemId = originalItemId;
                    if (itemNode.has("upgrade_id") && itemNode.get("upgrade_id").asLong() != 0) {
                        displayItemId = itemNode.get("upgrade_id").asLong();
                        logger.debug("Item {} upgraded to {}", originalItemId, displayItemId);
                    }
                    
                    // 최대 12개까지만 허용
                    if (currentItems.size() < 12 || currentItems.containsKey(originalItemId)) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", displayItemId);
                        item.put("name", getItemName(displayItemId));
                        item.put("image", getItemImagePath(displayItemId));
                        
                        // 원본 아이템 ID를 키로 사용 (업그레이드 추적용)
                        currentItems.put(originalItemId, item);
                        logger.debug("Added/Updated item: {} ({})", item.get("name"), displayItemId);
                    } else {
                        logger.debug("Item inventory full (12 max), skipping: {}", displayItemId);
                    }
                }
            }
            
            // 최종 아이템 리스트 (최대 12개)
            finalItems.addAll(currentItems.values());
            
            // 정확히 12개로 제한
            if (finalItems.size() > 12) {
                finalItems = finalItems.subList(0, 12);
            }
            
            logger.info("Final items count: {} (max 12)", finalItems.size());
        } else {
            logger.debug("No items array found for player");
        }
        
        return finalItems;
    }
    
    /**
     * Deadlock API에서 아이템 데이터를 로드하고 캐시에 저장
     */
    private void loadItemsFromAPI() {
        try {
            String itemsUrl = "https://assets.deadlock-api.com/v2/items";
            logger.info("Loading items from API: {}", itemsUrl);
            
            HttpGet request = new HttpGet(itemsUrl);
            request.setHeader("User-Agent", "Mozilla/5.0 (Deadlock-Stats-Tracker/1.0)");
            request.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String jsonResponse = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    JsonNode itemsArray = objectMapper.readTree(jsonResponse);
                    
                    if (itemsArray.isArray()) {
                        int loadedItems = 0;
                        for (JsonNode itemNode : itemsArray) {
                            if (itemNode.has("id") && itemNode.has("name")) {
                                long itemId = itemNode.get("id").asLong();
                                String itemName = itemNode.get("name").asText();
                                
                                // 아이템 이름이 의미있는 경우에만 캐시 (citadel_로 시작하는 내부 이름 제외)
                                if (!itemName.startsWith("citadel_") && !itemName.equals("")) {
                                    itemNameCache.put(itemId, itemName);
                                    
                                    // 이미지 URL도 캐시
                                    if (itemNode.has("image_webp")) {
                                        String imageWebp = itemNode.get("image_webp").asText();
                                        // 로컬 이미지 경로로 변환 (추후 다운로드할 예정)
                                        String localImagePath = convertToLocalImagePath(itemId, imageWebp);
                                        itemImageCache.put(itemId, localImagePath);
                                    }
                                    
                                    loadedItems++;
                                }
                            }
                        }
                        
                        itemsLoaded = true;
                        logger.info("Successfully loaded {} item names and images from API", loadedItems);
                    }
                } else {
                    logger.warn("Items API returned status: {}", statusCode);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to load items from API", e);
            loadFallbackItems();
        }
    }
    
    /**
     * 외부 이미지 URL을 로컬 이미지 경로로 변환
     */
    private String convertToLocalImagePath(long itemId, String originalUrl) {
        // 다운로드한 모든 이미지가 other 디렉토리에 있으므로, 
        // 원본 URL에서 파일명만 추출하여 other 카테고리 사용
        String fileName = itemId + ".webp";
        
        if (originalUrl != null) {
            String[] parts = originalUrl.split("/");
            if (parts.length > 0) {
                fileName = parts[parts.length - 1];
            }
        }
        
        // 모든 아이템 이미지를 other 디렉토리에서 찾음
        return "/resources/images/items/other/" + fileName;
    }
    
    /**
     * API 로드 실패 시 기본 아이템들 로드
     */
    private void loadFallbackItems() {
        // 다운로드된 실제 아이템 이미지들과 매핑
        Map<Long, ItemInfo> fallbackItems = new HashMap<>();
        
        // 실제 서버 로그에서 확인된 아이템들 - 다운로드된 이미지와 매핑
        fallbackItems.put(2366960452L, new ItemInfo("Crimson Slash", "other", "yamato_crimson_slash.webp"));
        fallbackItems.put(2566573207L, new ItemInfo("Flying Strike", "other", "yamato_flying_strike.webp"));
        fallbackItems.put(1758361060L, new ItemInfo("Power Slash", "other", "yamato_power_slash.webp"));
        fallbackItems.put(3255651252L, new ItemInfo("Titanic Magazine", "other", "titanic_magazine.webp"));
        fallbackItems.put(778852085L, new ItemInfo("Shadow Transformation", "other", "yamato_blinding_steel.webp"));
        fallbackItems.put(2356412290L, new ItemInfo("Burst Fire", "other", "fire_rate_plus_plus.webp"));
        fallbackItems.put(1808328943L, new ItemInfo("Kinetic Dash", "other", "kinetic_sash.webp"));
        fallbackItems.put(3319782965L, new ItemInfo("Berserker", "other", "berserker.webp"));
        fallbackItems.put(287109927L, new ItemInfo("Hollow Point", "other", "hollow_point.webp"));
        fallbackItems.put(1360608436L, new ItemInfo("Glass Cannon", "other", "glass_cannon.webp"));
        fallbackItems.put(2739107182L, new ItemInfo("Fortitude", "other", "revitalizer.webp"));
        fallbackItems.put(1471955600L, new ItemInfo("Vampiric Burst", "other", "vampiric_burst.webp"));
        fallbackItems.put(690458959L, new ItemInfo("Superior Duration", "other", "arcane_persistance.webp"));
        fallbackItems.put(800799385L, new ItemInfo("Flog", "other", "lash_flog.webp"));
        fallbackItems.put(3977876567L, new ItemInfo("Ground Strike", "other", "lash_death_slam.webp"));
        fallbackItems.put(1414319208L, new ItemInfo("Grapple", "other", "lash_lash.webp"));
        fallbackItems.put(1078869312L, new ItemInfo("Death Slam", "other", "lash_counter_lash.webp"));
        fallbackItems.put(2678489038L, new ItemInfo("Arcane Surge", "other", "arcane_surge.webp"));
        fallbackItems.put(2055232442L, new ItemInfo("Cold Front", "other", "ice_blast.webp"));
        fallbackItems.put(365620721L, new ItemInfo("Enchanter's Emblem", "other", "tech_shield_pulse.webp"));
        fallbackItems.put(3585132399L, new ItemInfo("Mystic Shot", "other", "explosive_bullets.webp"));
        fallbackItems.put(377372174L, new ItemInfo("Greater Expansion", "other", "spiritual_dominion.webp"));
        fallbackItems.put(1055679805L, new ItemInfo("Rapid Recharge", "other", "rapid_recharge.webp"));
        fallbackItems.put(2717651715L, new ItemInfo("Tankbuster", "other", "magic_shock.webp"));
        fallbackItems.put(519124136L, new ItemInfo("Trophy Collector", "other", "sprint_booster.webp"));
        fallbackItems.put(1039061940L, new ItemInfo("Debuff Remover", "other", "debuff_remover.webp"));
        fallbackItems.put(3561817145L, new ItemInfo("Boundless Spirit", "other", "boundless_spirit.webp"));
        
        fallbackItems.forEach((id, info) -> {
            itemNameCache.put(id, info.name);
            itemImageCache.put(id, "/resources/images/items/" + info.category + "/" + info.fileName);
        });
        
        itemsLoaded = true;
        logger.info("Loaded {} fallback item names", fallbackItems.size());
    }
    
    /**
     * 아이템 정보를 저장하는 내부 클래스
     */
    private static class ItemInfo {
        final String name;
        final String category;
        final String fileName;
        
        ItemInfo(String name, String category, String fileName) {
            this.name = name;
            this.category = category;
            this.fileName = fileName;
        }
    }
}