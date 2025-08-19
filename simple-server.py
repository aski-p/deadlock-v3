#!/usr/bin/env python3
"""
Simple HTTP server for Deadlock Stats Tracker
정적 파일 서빙 및 기본 라우팅 제공
"""

import http.server
import socketserver
import os
import urllib.parse
import json
from datetime import datetime

PORT = 8080

class DeadlockHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory="/Users/aski/deadlock-v3/src/main/webapp", **kwargs)
    
    def do_GET(self):
        # URL 파싱
        parsed_path = urllib.parse.urlparse(self.path)
        path = parsed_path.path
        
        print(f"Request: {path}")
        
        # 라우팅 처리
        if path == '/' or path == '/home':
            self.serve_html_page('home.html')
        elif path == '/profile':
            self.serve_html_page('profile.html')
        elif path == '/auth/login':
            self.serve_steam_login()
        elif path.startswith('/resources/'):
            # 정적 리소스 처리
            super().do_GET()
        elif path.startswith('/api/'):
            self.serve_api(path)
        else:
            super().do_GET()
    
    def serve_html_page(self, filename):
        """HTML 페이지 서빙 (JSP 대신 기본 HTML로)"""
        try:
            # 기본 HTML 템플릿 생성
            html_content = self.generate_html_page(filename)
            
            self.send_response(200)
            self.send_header('Content-type', 'text/html; charset=utf-8')
            self.end_headers()
            self.wfile.write(html_content.encode('utf-8'))
        except Exception as e:
            self.send_error(500, f"Server Error: {str(e)}")
    
    def generate_html_page(self, page_type):
        """HTML 페이지 생성"""
        if page_type == 'home.html':
            return '''
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Deadlock Stats Tracker</title>
    <link rel="stylesheet" href="/resources/css/main.css">
    <link rel="stylesheet" href="/resources/css/home.css">
</head>
<body>
    <header class="header">
        <div class="container">
            <nav class="navbar">
                <div class="navbar-brand">
                    <a href="/" class="brand-link">
                        <span class="brand-text">Deadlock Stats</span>
                    </a>
                </div>
                <div class="navbar-menu">
                    <div class="navbar-nav">
                        <a href="/" class="nav-link active">홈</a>
                    </div>
                    <div class="navbar-actions">
                        <a href="/auth/login" class="steam-login-btn">
                            Steam으로 로그인
                        </a>
                    </div>
                </div>
            </nav>
        </div>
    </header>
    
    <main class="main-content">
        <section class="hero-section">
            <div class="container">
                <div class="hero-content">
                    <h1 class="hero-title">Deadlock Stats Tracker</h1>
                    <p class="hero-subtitle">Steam 계정으로 로그인하여 Deadlock 게임 통계를 확인하세요!</p>
                    <div class="hero-actions">
                        <a href="/auth/login" class="btn btn-primary btn-large">
                            Steam으로 시작하기
                        </a>
                    </div>
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
    
    <footer class="footer">
        <div class="container">
            <div class="footer-content">
                <div class="footer-section">
                    <h4>Deadlock Stats</h4>
                    <p>Deadlock 게임 통계 추적 서비스</p>
                </div>
            </div>
            <div class="footer-bottom">
                <div class="footer-copyright">
                    <p>&copy; 2024 Deadlock Stats. All rights reserved.</p>
                </div>
            </div>
        </div>
    </footer>
    
    <script src="/resources/js/main.js"></script>
</body>
</html>
            '''
        elif page_type == 'profile.html':
            return '''
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>프로필 - Deadlock Stats</title>
    <link rel="stylesheet" href="/resources/css/main.css">
    <link rel="stylesheet" href="/resources/css/profile.css">
</head>
<body>
    <header class="header">
        <div class="container">
            <nav class="navbar">
                <div class="navbar-brand">
                    <a href="/" class="brand-link">
                        <span class="brand-text">Deadlock Stats</span>
                    </a>
                </div>
                <div class="navbar-menu">
                    <div class="navbar-nav">
                        <a href="/" class="nav-link">홈</a>
                        <a href="/profile" class="nav-link active">프로필</a>
                    </div>
                </div>
            </nav>
        </div>
    </header>
    
    <main class="main-content">
        <div class="container">
            <div class="profile-header">
                <div class="profile-info">
                    <div class="profile-avatar">
                        <img src="https://avatars.steamstatic.com/b5bd56c1aa4644a474a2e4972be27ef9e82e517e_full.jpg" 
                             alt="Player" class="avatar-large">
                        <div class="rank-badge">
                            <span class="rank-name">Ascendant</span>
                        </div>
                    </div>
                    <div class="profile-details">
                        <h1 class="profile-name">Demo Player</h1>
                        <p class="steam-id">Steam ID: 54776284</p>
                        <div class="profile-stats-summary">
                            <div class="stat-item">
                                <span class="stat-value">156</span>
                                <span class="stat-label">총 게임</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value">67.3%</span>
                                <span class="stat-label">승률</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value">2.14</span>
                                <span class="stat-label">평균 KDA</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-value">Viscous</span>
                                <span class="stat-label">주 캐릭터</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="profile-tabs">
                <nav class="tab-nav">
                    <button class="tab-button active" onclick="ProfilePage.switchTab('matches')">
                        매치 기록
                    </button>
                    <button class="tab-button" onclick="ProfilePage.switchTab('stats')">
                        통계
                    </button>
                    <button class="tab-button" onclick="ProfilePage.switchTab('heroes')">
                        캐릭터
                    </button>
                </nav>
                
                <div class="tab-content">
                    <div id="matches-tab" class="tab-pane active">
                        <div class="matches-container">
                            <div class="matches-header">
                                <h3>최근 매치</h3>
                            </div>
                            <div class="matches-list">
                                <div class="match-card win" data-match-id="match_54776284">
                                    <div class="match-result">
                                        <span class="result-text">승리</span>
                                        <span class="match-id">#54776284</span>
                                    </div>
                                    <div class="match-hero">
                                        <img src="/resources/images/heroes/viscous.jpg" alt="Viscous" class="hero-icon">
                                        <span class="hero-name">Viscous</span>
                                    </div>
                                    <div class="match-kda">
                                        <span class="kda-value">12/3/8</span>
                                        <span class="kda-ratio">6.67 KDA</span>
                                    </div>
                                    <div class="match-networth">
                                        <span class="networth-value">23,450</span>
                                        <span class="networth-label">Net Worth</span>
                                    </div>
                                    <div class="match-duration">
                                        <span class="duration-value">28:45</span>
                                    </div>
                                    <div class="match-time">
                                        <span class="time-value">2시간 전</span>
                                    </div>
                                </div>
                                
                                <div class="match-card loss" data-match-id="match_54776285">
                                    <div class="match-result">
                                        <span class="result-text">패배</span>
                                        <span class="match-id">#54776285</span>
                                    </div>
                                    <div class="match-hero">
                                        <img src="/resources/images/heroes/bebop.jpg" alt="Bebop" class="hero-icon">
                                        <span class="hero-name">Bebop</span>
                                    </div>
                                    <div class="match-kda">
                                        <span class="kda-value">8/7/5</span>
                                        <span class="kda-ratio">1.86 KDA</span>
                                    </div>
                                    <div class="match-networth">
                                        <span class="networth-value">18,720</span>
                                        <span class="networth-label">Net Worth</span>
                                    </div>
                                    <div class="match-duration">
                                        <span class="duration-value">35:12</span>
                                    </div>
                                    <div class="match-time">
                                        <span class="time-value">5시간 전</span>
                                    </div>
                                </div>
                                
                                <div class="match-card win" data-match-id="match_54776286">
                                    <div class="match-result">
                                        <span class="result-text">승리</span>
                                        <span class="match-id">#54776286</span>
                                    </div>
                                    <div class="match-hero">
                                        <img src="/resources/images/heroes/mcginnis.jpg" alt="McGinnis" class="hero-icon">
                                        <span class="hero-name">McGinnis</span>
                                    </div>
                                    <div class="match-kda">
                                        <span class="kda-value">15/2/12</span>
                                        <span class="kda-ratio">13.50 KDA</span>
                                    </div>
                                    <div class="match-networth">
                                        <span class="networth-value">26,890</span>
                                        <span class="networth-label">Net Worth</span>
                                    </div>
                                    <div class="match-duration">
                                        <span class="duration-value">24:16</span>
                                    </div>
                                    <div class="match-time">
                                        <span class="time-value">8시간 전</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <div id="stats-tab" class="tab-pane">
                        <div class="stats-container">
                            <h3>전체 통계</h3>
                            <p>상세 통계 데이터가 로드됩니다...</p>
                        </div>
                    </div>
                    
                    <div id="heroes-tab" class="tab-pane">
                        <div class="heroes-container">
                            <h3>캐릭터 통계</h3>
                            <p class="coming-soon">캐릭터별 상세 통계는 곧 제공될 예정입니다.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
    
    <script src="/resources/js/main.js"></script>
    <script src="/resources/js/profile.js"></script>
</body>
</html>
            '''
        return "<h1>Page Not Found</h1>"
    
    def serve_steam_login(self):
        """Steam 로그인 시뮬레이션"""
        self.send_response(302)
        self.send_header('Location', '/profile')
        self.end_headers()
    
    def serve_api(self, path):
        """API 엔드포인트 시뮬레이션"""
        response_data = {"status": "success", "message": "API endpoint working"}
        
        if "matches" in path:
            response_data = {
                "matches": [
                    {
                        "matchId": "12345",
                        "result": "win",
                        "hero": "Viscous",
                        "kills": 12,
                        "deaths": 3,
                        "assists": 8,
                        "duration": "28:45",
                        "date": "2024-01-20T10:30:00Z"
                    }
                ],
                "totalMatches": 156
            }
        elif "stats" in path:
            response_data = {
                "totalKills": 1248,
                "totalDeaths": 584,
                "totalAssists": 892,
                "winRate": 67.3,
                "avgKDA": 2.14,
                "favoriteHero": "Viscous"
            }
        
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(response_data).encode('utf-8'))
    
    def log_message(self, format, *args):
        """로그 메시지 출력"""
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {format % args}")

if __name__ == "__main__":
    os.chdir("/Users/aski/deadlock-v3")
    
    with socketserver.TCPServer(("", PORT), DeadlockHandler) as httpd:
        print(f"""
🚀 Deadlock Stats Tracker Server Started!
=========================================
✅ Server running at: http://localhost:{PORT}
📁 Serving from: /home/aski/deadlock-v3/src/main/webapp
🎮 Features:
   - Steam 로그인 시뮬레이션
   - 프로필 페이지 미리보기
   - API 엔드포인트 테스트
   - 정적 리소스 서빙

Press Ctrl+C to stop the server
        """)
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n\n🛑 Server stopped by user")
            httpd.shutdown()