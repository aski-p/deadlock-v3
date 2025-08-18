<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header class="header">
    <div class="container">
        <nav class="navbar">
            <div class="navbar-brand">
                <a href="/" class="brand-link">
                    <img src="/resources/images/deadlock-logo.png" alt="Deadlock" class="brand-logo">
                    <span class="brand-text">Deadlock Stats</span>
                </a>
            </div>
            
            <div class="navbar-menu">
                <div class="navbar-nav">
                    <a href="/" class="nav-link ${currentPage == 'home' ? 'active' : ''}">홈</a>
                    <a href="/about" class="nav-link ${currentPage == 'about' ? 'active' : ''}">소개</a>
                    <c:if test="${isLoggedIn}">
                        <a href="/profile" class="nav-link ${currentPage == 'profile' ? 'active' : ''}">프로필</a>
                    </c:if>
                </div>
                
                <div class="navbar-actions">
                    <c:choose>
                        <c:when test="${isLoggedIn}">
                            <div class="user-dropdown">
                                <button class="user-btn" onclick="toggleDropdown()">
                                    <img src="${user.avatar}" alt="${user.personaName}" class="user-avatar">
                                    <span class="user-name">${user.personaName}</span>
                                    <i class="dropdown-arrow">▼</i>
                                </button>
                                <div class="dropdown-menu" id="userDropdown">
                                    <a href="/profile" class="dropdown-item">
                                        <i class="icon">👤</i> 프로필
                                    </a>
                                    <div class="dropdown-divider"></div>
                                    <a href="/auth/logout" class="dropdown-item">
                                        <i class="icon">🚪</i> 로그아웃
                                    </a>
                                </div>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <a href="/auth/login" class="steam-login-btn">
                                <img src="/resources/images/steam-icon.png" alt="Steam">
                                Steam으로 로그인
                            </a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </nav>
    </div>
</header>

<script>
function toggleDropdown() {
    const dropdown = document.getElementById('userDropdown');
    dropdown.classList.toggle('show');
}

// 외부 클릭시 드롭다운 닫기
window.onclick = function(event) {
    if (!event.target.matches('.user-btn') && !event.target.closest('.user-btn')) {
        const dropdown = document.getElementById('userDropdown');
        if (dropdown && dropdown.classList.contains('show')) {
            dropdown.classList.remove('show');
        }
    }
}
</script>