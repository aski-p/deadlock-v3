// Profile page specific functionality

let currentMatchPage = 0;
let loadingMatches = false;
let hasMoreMatches = true;
let currentPatchFilter = null;
let currentDateRange = null;

// Time formatting utility functions
function formatTimeAgo(timestamp) {
    const now = new Date();
    const matchTime = new Date(timestamp);
    const diff = now - matchTime;
    
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    const months = Math.floor(days / 30);
    const years = Math.floor(days / 365);
    
    if (seconds < 60) {
        return '방금 전';
    } else if (minutes < 60) {
        return `${minutes}분 전`;
    } else if (hours < 24) {
        return `${hours}시간 전`;
    } else if (days < 30) {
        return `${days}일 전`;
    } else if (months < 12) {
        return `${months}개월 전`;
    } else {
        return `${years}년 전`;
    }
}

document.addEventListener('DOMContentLoaded', function() {
    initializeProfilePage();
});

function initializeProfilePage() {
    // Initialize tab switching
    initializeTabs();
    
    // Initialize match filtering
    initializeMatchFilters();
    
    // Initialize patch filtering
    initializePatchFilters();
    
    // Initialize infinite scroll for matches
    initializeInfiniteScroll();
    
    // Update all timestamps on page load
    updateAllTimestamps();
    
    // Load initial data
    loadInitialData();
}

// Tab functionality
function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    
    tabButtons.forEach(button => {
        button.addEventListener('click', function() {
            const tabName = this.textContent.trim();
            switchTab(getTabKey(tabName));
        });
    });
}

function getTabKey(tabName) {
    const tabMap = {
        '매치 기록': 'matches',
        '통계': 'stats',
        '캐릭터': 'heroes'
    };
    return tabMap[tabName] || 'matches';
}

function switchTab(tabKey) {
    // Update URL without page refresh
    const url = new URL(window.location);
    url.searchParams.set('tab', tabKey);
    window.history.pushState({}, '', url);
    
    // Update active tab button
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    
    const activeButton = Array.from(document.querySelectorAll('.tab-button')).find(btn => {
        return getTabKey(btn.textContent.trim()) === tabKey;
    });
    
    if (activeButton) {
        activeButton.classList.add('active');
    }
    
    // Update active tab pane
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.remove('active');
    });
    
    const activePane = document.getElementById(tabKey + '-tab');
    if (activePane) {
        activePane.classList.add('active');
    }
    
    // Load tab-specific data
    loadTabData(tabKey);
}

// Match filtering
function initializeMatchFilters() {
    const heroFilter = document.getElementById('heroFilter');
    const resultFilter = document.getElementById('resultFilter');
    
    if (heroFilter) {
        heroFilter.addEventListener('change', filterMatches);
    }
    
    if (resultFilter) {
        resultFilter.addEventListener('change', filterMatches);
    }
    
    // Populate hero filter options
    populateHeroFilter();
}

function populateHeroFilter() {
    const heroFilter = document.getElementById('heroFilter');
    if (!heroFilter) return;
    
    // Get unique heroes from match data
    const matchCards = document.querySelectorAll('.match-card');
    const heroes = new Set();
    
    matchCards.forEach(card => {
        const heroName = card.querySelector('.hero-name');
        if (heroName) {
            heroes.add(heroName.textContent.trim());
        }
    });
    
    // Add options to filter
    heroes.forEach(hero => {
        const option = document.createElement('option');
        option.value = hero;
        option.textContent = hero;
        heroFilter.appendChild(option);
    });
}

function filterMatches() {
    const heroFilter = document.getElementById('heroFilter');
    const resultFilter = document.getElementById('resultFilter');
    
    const selectedHero = heroFilter ? heroFilter.value : '';
    const selectedResult = resultFilter ? resultFilter.value : '';
    
    const matchCards = document.querySelectorAll('.match-card');
    
    matchCards.forEach(card => {
        let showCard = true;
        
        // Filter by hero
        if (selectedHero) {
            const heroName = card.querySelector('.hero-name');
            if (!heroName || heroName.textContent.trim() !== selectedHero) {
                showCard = false;
            }
        }
        
        // Filter by result
        if (selectedResult) {
            const hasWinClass = card.classList.contains('win');
            const hasLossClass = card.classList.contains('loss');
            
            if (selectedResult === 'win' && !hasWinClass) {
                showCard = false;
            } else if (selectedResult === 'loss' && !hasLossClass) {
                showCard = false;
            }
        }
        
        // Show/hide card
        card.style.display = showCard ? 'grid' : 'none';
    });
}

// Infinite scroll for matches
function initializeInfiniteScroll() {
    const matchesList = document.getElementById('matchesList');
    if (!matchesList) return;
    
    // Add scroll event listener to the matches container
    let timeoutId;
    matchesList.addEventListener('scroll', function() {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            const { scrollTop, scrollHeight, clientHeight } = matchesList;
            
            if (scrollTop + clientHeight >= scrollHeight - 100 && !loadingMatches && hasMoreMatches) {
                loadMoreMatches();
            }
        }, 100);
    });
}

// Data loading functions
function loadInitialData() {
    const currentTab = getCurrentTab();
    loadTabData(currentTab);
}

function getCurrentTab() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('tab') || 'matches';
}

async function loadTabData(tabKey) {
    switch (tabKey) {
        case 'matches':
            // Matches are already loaded server-side, but we can refresh if needed
            break;
        case 'stats':
            await loadStatsData();
            break;
        case 'heroes':
            await loadHeroesData();
            break;
    }
}

async function loadStatsData() {
    try {
        AppUtils.showLoading('stats-tab');
        
        const stats = await AppUtils.apiCall('/profile/api/stats');
        
        // Update stats display
        updateStatsDisplay(stats);
        
    } catch (error) {
        console.error('Failed to load stats:', error);
        AppUtils.showError('stats-tab', '통계 데이터를 불러올 수 없습니다.');
    } finally {
        AppUtils.hideLoading('stats-tab');
    }
}

async function loadHeroesData() {
    // Heroes data loading will be implemented when available
    console.log('Loading heroes data...');
}

async function loadMoreMatches() {
    if (loadingMatches || !hasMoreMatches) return;
    
    loadingMatches = true;
    const loadMoreBtn = document.querySelector('.load-more .btn');
    
    try {
        if (loadMoreBtn) {
            loadMoreBtn.textContent = '로딩 중...';
            loadMoreBtn.disabled = true;
        }
        
        currentMatchPage++;
        const matches = await AppUtils.apiCall(`/profile/api/matches?page=${currentMatchPage}&size=10`);
        
        if (matches.matches && matches.matches.length > 0) {
            appendMatchesToList(matches.matches);
            
            // Check if we have more matches
            if (matches.matches.length < 10) {
                hasMoreMatches = false;
                if (loadMoreBtn) {
                    loadMoreBtn.style.display = 'none';
                }
            }
        } else {
            hasMoreMatches = false;
            if (loadMoreBtn) {
                loadMoreBtn.style.display = 'none';
            }
        }
        
    } catch (error) {
        console.error('Failed to load more matches:', error);
        currentMatchPage--; // Revert page increment on error
        
        if (loadMoreBtn) {
            loadMoreBtn.textContent = '다시 시도';
        }
    } finally {
        loadingMatches = false;
        
        if (loadMoreBtn && hasMoreMatches) {
            loadMoreBtn.textContent = '더 보기';
            loadMoreBtn.disabled = false;
        }
    }
}

function appendMatchesToList(matches) {
    const matchesList = document.getElementById('matchesList');
    if (!matchesList) return;
    
    matches.forEach(match => {
        const matchCard = createMatchCard(match);
        matchesList.appendChild(matchCard);
    });
    
    // Update hero filter options
    populateHeroFilter();
}

function createMatchCard(match) {
    const card = document.createElement('div');
    card.className = `match-card ${match.result === 'win' ? 'win' : 'loss'}`;
    
    const kda = (match.kills + match.assists) / (match.deaths > 0 ? match.deaths : 1);
    
    // 특별 이미지 처리가 필요한 히어로들
    const specialHeroes = {
        'lash': 'lash.webp',
        'holliday': 'holliday.webp', 
        'mirage': 'mirage.jpg',
        'vyper': 'vyper.jpg'
    };
    
    const heroKey = match.hero.toLowerCase();
    const heroImagePath = specialHeroes[heroKey] || (match.hero.toLowerCase() + '.jpg');
    
    card.innerHTML = `
        <div class="match-result">
            <span class="result-text">${match.result === 'win' ? '승리' : '패배'}</span>
        </div>
        
        <div class="match-hero">
            <img src="/resources/images/heroes/${heroImagePath}" 
                 alt="${match.hero}" class="hero-icon"
                 onerror="this.src='/resources/images/heroes/default.jpg'">
            <span class="hero-name">${match.hero}</span>
        </div>
        
        <div class="match-kda">
            <span class="kda-value">${match.kills}/${match.deaths}/${match.assists}</span>
            <span class="kda-ratio">${kda.toFixed(2)} KDA</span>
        </div>
        
        <div class="match-duration">
            <span class="duration-value">${match.duration}</span>
        </div>
        
        <div class="match-time">
            <span class="time-value">${formatTimeAgo(match.startTime || match.date)}</span>
        </div>
        
        <div class="match-items">
            <span class="items-label">Final Items</span>
            <div class="items-grid">
                ${match.finalItems && match.finalItems.length > 0 ? 
                    match.finalItems.map(item => 
                        `<img src="${item.image}" alt="${item.name}" class="item-icon" title="${item.name}">`
                    ).join('') : 
                    '<span class="no-items">No items data</span>'
                }
            </div>
        </div>
    `;
    
    return card;
}

function updateStatsDisplay(stats) {
    // Update individual stat elements if they exist
    const updateStat = (selector, value) => {
        const element = document.querySelector(selector);
        if (element) {
            element.textContent = value;
        }
    };
    
    updateStat('.total-kills-value', AppUtils.formatNumber(stats.totalKills));
    updateStat('.total-deaths-value', AppUtils.formatNumber(stats.totalDeaths));
    updateStat('.total-assists-value', AppUtils.formatNumber(stats.totalAssists));
    updateStat('.win-rate-value', stats.winRate.toFixed(1) + '%');
    updateStat('.avg-kda-value', stats.avgKDA.toFixed(2));
    updateStat('.favorite-hero-value', stats.favoriteHero);
}

// Patch filtering functionality
function initializePatchFilters() {
    // URL에서 패치 정보 읽기
    const urlParams = new URLSearchParams(window.location.search);
    currentPatchFilter = urlParams.get('pd-picker-tab');
    currentDateRange = urlParams.get('date_range');
    
    // 패치 선택 드롭다운 초기화
    const patchSelector = document.getElementById('patchSelector');
    if (patchSelector) {
        patchSelector.addEventListener('change', function() {
            const selectedPatch = this.value;
            loadDataByPatch(selectedPatch);
        });
        
        // 현재 선택된 패치로 설정
        if (currentPatchFilter) {
            patchSelector.value = currentPatchFilter;
        }
    }
    
    // 날짜 범위 선택기 초기화
    const dateRangeStart = document.getElementById('dateRangeStart');
    const dateRangeEnd = document.getElementById('dateRangeEnd');
    
    if (dateRangeStart && dateRangeEnd) {
        dateRangeStart.addEventListener('change', updateDateRange);
        dateRangeEnd.addEventListener('change', updateDateRange);
        
        // URL의 날짜 범위로 초기화
        if (currentDateRange && currentDateRange.includes('_')) {
            const [start, end] = currentDateRange.split('_');
            dateRangeStart.value = start.substring(0, 10); // ISO date에서 날짜 부분만
            dateRangeEnd.value = end.substring(0, 10);
        }
    }
}

async function loadDataByPatch(patchName) {
    try {
        AppUtils.showLoading('matches-tab');
        
        // 패치별 기본 날짜 범위 설정
        const patchDates = getPatchDateRange(patchName);
        
        const response = await AppUtils.apiCall('/profile/api/patch-data', {
            patchTab: patchName,
            tab: 'matches',
            dateRange: patchDates.start + '_' + patchDates.end
        });
        
        // URL 업데이트
        updateURL({
            'pd-picker-tab': patchName,
            'date_range': patchDates.start + '_' + patchDates.end
        });
        
        // 매치 리스트 업데이트
        updateMatchesList(response.data.matches);
        
        // 통계 업데이트
        updateStatsDisplay(response.data.stats);
        
    } catch (error) {
        console.error('Failed to load patch data:', error);
        AppUtils.showError('matches-tab', '패치 데이터를 불러올 수 없습니다.');
    } finally {
        AppUtils.hideLoading('matches-tab');
    }
}

async function updateDateRange() {
    const dateRangeStart = document.getElementById('dateRangeStart');
    const dateRangeEnd = document.getElementById('dateRangeEnd');
    
    if (!dateRangeStart.value || !dateRangeEnd.value) return;
    
    try {
        AppUtils.showLoading('matches-tab');
        
        const startDate = dateRangeStart.value + 'T00:00:00.000Z';
        const endDate = dateRangeEnd.value + 'T23:59:59.999Z';
        
        const response = await AppUtils.apiCall('/profile/api/matches/daterange', {
            startDate: startDate,
            endDate: endDate
        });
        
        // URL 업데이트
        updateURL({
            'date_range': startDate + '_' + endDate
        });
        
        // 매치 리스트 업데이트
        updateMatchesList(response.matches);
        
    } catch (error) {
        console.error('Failed to load date range data:', error);
        AppUtils.showError('matches-tab', '날짜별 데이터를 불러올 수 없습니다.');
    } finally {
        AppUtils.hideLoading('matches-tab');
    }
}

function getPatchDateRange(patchName) {
    // 패치별 대략적인 날짜 범위 (실제 패치 노트 기반으로 업데이트 필요)
    const patchDates = {
        'current': {
            start: '2025-07-01T00:00:00.000Z',
            end: '2025-08-19T23:59:59.999Z'
        },
        'previous': {
            start: '2025-05-08T19:43:20.000Z',
            end: '2025-06-30T23:59:59.999Z'
        },
        'all': {
            start: '2025-01-01T00:00:00.000Z',
            end: '2025-08-19T23:59:59.999Z'
        }
    };
    
    return patchDates[patchName] || patchDates['all'];
}

function updateURL(params) {
    const url = new URL(window.location);
    
    Object.keys(params).forEach(key => {
        if (params[key]) {
            url.searchParams.set(key, params[key]);
        } else {
            url.searchParams.delete(key);
        }
    });
    
    window.history.pushState({}, '', url);
}

function updateMatchesList(matches) {
    const matchesList = document.getElementById('matchesList');
    if (!matchesList) return;
    
    // 기존 매치들 제거
    matchesList.innerHTML = '';
    
    // 새 매치들 추가
    if (matches && matches.length > 0) {
        matches.forEach(match => {
            const matchCard = createMatchCard(match);
            matchesList.appendChild(matchCard);
        });
        
        // 필터 옵션 업데이트
        populateHeroFilter();
    } else {
        matchesList.innerHTML = '<div class="no-matches">선택한 기간에 매치 기록이 없습니다.</div>';
    }
    
    // 페이지 정보 리셋
    currentMatchPage = 0;
    hasMoreMatches = matches && matches.length >= 10;
}

// Update all timestamps on the page
function updateAllTimestamps() {
    const timeElements = document.querySelectorAll('.time-value[data-timestamp]');
    timeElements.forEach(element => {
        const timestamp = element.getAttribute('data-timestamp');
        if (timestamp) {
            element.textContent = formatTimeAgo(parseInt(timestamp));
        }
    });
}

// Export functions for global use
window.ProfilePage = {
    switchTab,
    filterMatches,
    loadMoreMatches,
    loadDataByPatch,
    updateDateRange,
    formatTimeAgo,
    updateAllTimestamps
};

// Expose formatTimeAgo globally for inline usage
window.formatTimeAgo = formatTimeAgo;