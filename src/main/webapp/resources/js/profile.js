// Profile page specific functionality

let currentMatchPage = 0;
let loadingMatches = false;
let hasMoreMatches = true;

document.addEventListener('DOMContentLoaded', function() {
    initializeProfilePage();
});

function initializeProfilePage() {
    // Initialize tab switching
    initializeTabs();
    
    // Initialize match filtering
    initializeMatchFilters();
    
    // Initialize infinite scroll for matches
    initializeInfiniteScroll();
    
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
    
    card.innerHTML = `
        <div class="match-result">
            <span class="result-text">${match.result === 'win' ? '승리' : '패배'}</span>
        </div>
        
        <div class="match-hero">
            <img src="/resources/images/heroes/${match.hero}.jpg" 
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
        
        <div class="match-date">
            <span class="date-value">${AppUtils.formatDate(match.date)}</span>
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

// Export functions for global use
window.ProfilePage = {
    switchTab,
    filterMatches,
    loadMoreMatches
};