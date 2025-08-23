<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <link rel="stylesheet" href="/resources/css/main.css">
    <link rel="stylesheet" href="/resources/css/profile.css">
</head>
<body>
    <c:set var="currentPage" value="profile" scope="request"/>
    <jsp:include page="layout/header.jsp" />
    
    <main class="main-content">
        <div class="container">
            <div class="profile-header">
                <div class="profile-info">
                    <div class="profile-avatar">
                        <img src="${user.avatarFull}" alt="${user.personaName}" class="avatar-large">
                        <div class="rank-badge">
                            <img src="${profileData.rankIcon}" alt="Rank" class="rank-icon">
                            <span class="rank-name">${profileData.currentRank}</span>
                        </div>
                    </div>
                    
                    <div class="profile-details">
                        <h1 class="profile-name">${user.personaName}</h1>
                        <p class="steam-id">Steam ID: ${steamId}</p>
                        
                        <div class="profile-stats-summary">
                            <div class="stat-item">
                                <span class="stat-value">${profileData.totalMatches}</span>
                                <span class="stat-label">총 게임</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value"><fmt:formatNumber value="${profileData.winRate}" pattern="0.0"/>%</span>
                                <span class="stat-label">승률</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value"><fmt:formatNumber value="${profileData.avgKDA}" pattern="0.00"/></span>
                                <span class="stat-label">평균 KDA</span>
                            </div>
                            <div class="stat-item">
                                <div class="favorite-hero">
                                    <img src="${profileData.favoriteHeroImage}" alt="${profileData.favoriteHero}" class="hero-icon">
                                    <span class="stat-value">${profileData.favoriteHero}</span>
                                </div>
                                <span class="stat-label">주 캐릭터</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="profile-tabs">
                <nav class="tab-nav">
                    <button class="tab-button ${currentTab == 'matches' ? 'active' : ''}" 
                            onclick="switchTab('matches')">
                        매치 기록
                    </button>
                    <button class="tab-button ${currentTab == 'stats' ? 'active' : ''}" 
                            onclick="switchTab('stats')">
                        통계
                    </button>
                    <button class="tab-button ${currentTab == 'heroes' ? 'active' : ''}" 
                            onclick="switchTab('heroes')">
                        캐릭터
                    </button>
                </nav>
                
                <div class="tab-content">
                    <div id="matches-tab" class="tab-pane ${currentTab == 'matches' ? 'active' : ''}">
                        <div class="matches-container">
                            <div class="matches-header">
                                <h3>최근 매치</h3>
                                
                                <!-- 패치 및 날짜 필터 -->
                                <div class="patch-filters">
                                    <div class="filter-group">
                                        <label for="patchSelector">패치:</label>
                                        <select id="patchSelector">
                                            <option value="all">전체</option>
                                            <option value="current" selected>현재 패치</option>
                                            <option value="previous">이전 패치</option>
                                        </select>
                                    </div>
                                    
                                    <div class="filter-group">
                                        <label for="dateRangeStart">기간:</label>
                                        <input type="date" id="dateRangeStart" 
                                               value="2025-05-08" max="2025-08-19">
                                        <span class="date-separator">~</span>
                                        <input type="date" id="dateRangeEnd" 
                                               value="2025-08-19" max="2025-08-19">
                                    </div>
                                </div>
                                
                                <!-- 기존 필터들 -->
                                <div class="matches-filters">
                                    <select id="heroFilter" onchange="filterMatches()">
                                        <option value="">모든 캐릭터</option>
                                    </select>
                                    <select id="resultFilter" onchange="filterMatches()">
                                        <option value="">승부 구분</option>
                                        <option value="win">승리</option>
                                        <option value="loss">패배</option>
                                    </select>
                                </div>
                            </div>
                            
                            <div class="matches-list" id="matchesList">
                                <c:forEach var="match" items="${profileData.recentMatches}">
                                    <div class="match-card ${match.result == 'WIN' ? 'win' : 'loss'}" data-match-id="${match.matchId}">
                                        <div class="match-result">
                                            <span class="result-text">${match.result == 'WIN' ? '승리' : '패배'}</span>
                                            <span class="match-id">#${match.matchId}</span>
                                        </div>
                                        
                                        <div class="match-hero">
                                            <img src="${match.heroImage}" 
                                                 alt="${match.hero}" class="hero-icon"
                                                 onerror="this.src='/resources/images/heroes/default.jpg'">
                                            <span class="hero-name">${match.hero}</span>
                                        </div>
                                        
                                        <div class="match-kda">
                                            <span class="kda-value">${match.kills}/${match.deaths}/${match.assists}</span>
                                            <span class="kda-ratio">
                                                <fmt:formatNumber value="${(match.kills + match.assists) / (match.deaths > 0 ? match.deaths : 1)}" 
                                                                pattern="0.00"/> KDA
                                            </span>
                                        </div>
                                        
                                        <div class="match-networth">
                                            <span class="networth-value">
                                                <fmt:formatNumber value="${match.netWorth}" pattern="#,###"/>
                                            </span>
                                            <span class="networth-label">Net Worth</span>
                                        </div>
                                        
                                        <div class="match-duration">
                                            <span class="duration-value">${match.duration}</span>
                                        </div>
                                        
                                        <div class="match-time">
                                            <span class="time-value" data-timestamp="${match.startTime}">
                                                <!-- JavaScript로 동적 업데이트 -->
                                            </span>
                                        </div>
                                        
                                        <div class="match-items">
                                            <span class="items-label">Final Items</span>
                                            <div class="items-grid">
                                                <c:forEach var="item" items="${match.finalItems}" varStatus="status">
                                                    <c:choose>
                                                        <c:when test="${fn:startsWith(item.image, 'http')}">
                                                            <img src="${item.image}" 
                                                                 alt="${item.name}" 
                                                                 class="item-icon"
                                                                 title="${item.name}"
                                                                 crossorigin="anonymous">
                                                        </c:when>
                                                        <c:otherwise>
                                                            <img src="${pageContext.request.contextPath}${item.image}" 
                                                                 alt="${item.name}" 
                                                                 class="item-icon"
                                                                 title="${item.name}">
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:forEach>
                                                <c:if test="${empty match.finalItems}">
                                                    <span class="no-items">No items data</span>
                                                </c:if>
                                            </div>
                                        </div>
                                    </div>
                                </c:forEach>
                            </div>
                            
                            <div class="load-more">
                                <button class="btn btn-secondary" onclick="loadMoreMatches()">
                                    더 보기
                                </button>
                            </div>
                        </div>
                    </div>
                    
                    <div id="stats-tab" class="tab-pane ${currentTab == 'stats' ? 'active' : ''}">
                        <div class="stats-container">
                            <div class="stats-grid">
                                <div class="stat-card">
                                    <h4>킬/데스/어시스트</h4>
                                    <div class="stat-row">
                                        <span class="stat-label">총 킬:</span>
                                        <span class="stat-value">${profileData.totalKills}</span>
                                    </div>
                                    <div class="stat-row">
                                        <span class="stat-label">총 데스:</span>
                                        <span class="stat-value">${profileData.totalDeaths}</span>
                                    </div>
                                    <div class="stat-row">
                                        <span class="stat-label">총 어시스트:</span>
                                        <span class="stat-value">${profileData.totalAssists}</span>
                                    </div>
                                </div>
                                
                                <div class="stat-card">
                                    <h4>성과</h4>
                                    <div class="stat-row">
                                        <span class="stat-label">승률:</span>
                                        <span class="stat-value highlight">
                                            <fmt:formatNumber value="${profileData.winRate}" pattern="0.0"/>%
                                        </span>
                                    </div>
                                    <div class="stat-row">
                                        <span class="stat-label">평균 KDA:</span>
                                        <span class="stat-value highlight">
                                            <fmt:formatNumber value="${profileData.avgKDA}" pattern="0.00"/>
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div id="heroes-tab" class="tab-pane ${currentTab == 'heroes' ? 'active' : ''}">
                        <div class="heroes-container">
                            <h3>캐릭터 통계</h3>
                            <p class="coming-soon">캐릭터별 상세 통계는 곧 제공될 예정입니다.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
    
    <jsp:include page="layout/footer.jsp" />
    
    <script src="/resources/js/main.js"></script>
    <script src="/resources/js/profile.js"></script>
</body>
</html>