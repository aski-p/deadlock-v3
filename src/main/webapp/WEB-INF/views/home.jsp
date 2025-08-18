<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <link rel="stylesheet" href="/resources/css/main.css">
    <link rel="stylesheet" href="/resources/css/home.css">
</head>
<body>
    <c:set var="currentPage" value="home" scope="request"/>
    <jsp:include page="layout/header.jsp" />
    
    <main class="main-content">
        <section class="hero-section">
            <div class="container">
                <div class="hero-content">
                    <h1 class="hero-title">Deadlock Stats Tracker</h1>
                    <p class="hero-subtitle">${message}</p>
                    
                    <c:if test="${not empty error}">
                        <div class="alert alert-error">
                            ${error}
                        </div>
                    </c:if>
                    
                    <div class="hero-actions">
                        <c:choose>
                            <c:when test="${isLoggedIn}">
                                <a href="/profile" class="btn btn-primary btn-large">
                                    내 프로필 보기
                                </a>
                            </c:when>
                            <c:otherwise>
                                <a href="/auth/login" class="btn btn-primary btn-large">
                                    <img src="/resources/images/steam-icon-white.png" alt="Steam" class="btn-icon">
                                    Steam으로 시작하기
                                </a>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                
                <div class="hero-image">
                    <img src="/resources/images/deadlock-hero.jpg" alt="Deadlock Game" class="hero-img">
                </div>
            </div>
        </section>
        
        <section class="features-section">
            <div class="container">
                <h2 class="section-title">주요 기능</h2>
                
                <div class="features-grid">
                    <div class="feature-card">
                        <div class="feature-icon">📊</div>
                        <h3>상세 통계</h3>
                        <p>K/D/A, 승률, 게임별 상세 통계를 확인하세요</p>
                    </div>
                    
                    <div class="feature-card">
                        <div class="feature-icon">🎮</div>
                        <h3>매치 히스토리</h3>
                        <p>최근 게임 기록과 성과를 한눈에 보세요</p>
                    </div>
                    
                    <div class="feature-card">
                        <div class="feature-icon">🏆</div>
                        <h3>랭킹 추적</h3>
                        <p>랭크 변동사항을 실시간으로 추적합니다</p>
                    </div>
                    
                    <div class="feature-card">
                        <div class="feature-icon">👥</div>
                        <h3>친구와 비교</h3>
                        <p>친구들과 통계를 비교하고 경쟁하세요</p>
                    </div>
                </div>
            </div>
        </section>
    </main>
    
    <jsp:include page="layout/footer.jsp" />
    
    <script src="/resources/js/main.js"></script>
</body>
</html>