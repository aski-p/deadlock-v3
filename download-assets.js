#!/usr/bin/env node

const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');

// 이미지 저장 경로 설정
const HEROES_DIR = path.join(__dirname, 'src', 'main', 'webapp', 'resources', 'images', 'heroes');
const ITEMS_DIR = path.join(__dirname, 'src', 'main', 'webapp', 'resources', 'images', 'items');

// 디렉토리 생성
if (!fs.existsSync(HEROES_DIR)) {
    fs.mkdirSync(HEROES_DIR, { recursive: true });
}
if (!fs.existsSync(ITEMS_DIR)) {
    fs.mkdirSync(ITEMS_DIR, { recursive: true });
}

// Deadlock 캐릭터 목록
const HEROES = [
    'Abrams', 'Bebop', 'Dynamo', 'Grey_Talon', 'Haze', 'Infernus', 'Ivy', 'Kelvin',
    'Lady_Geist', 'Lash', 'McGinnis', 'Mo_&_Krill', 'Paradox', 'Pocket', 'Seven',
    'Shiv', 'Vindicta', 'Viscous', 'Warden', 'Wraith', 'Yamato'
];

// Deadlock 아이템 목록 (주요 아이템들)
const ITEMS = [
    // Weapon Items
    'Basic_Magazine', 'Close_Quarters', 'Headshot_Booster', 'High_Velocity_Mag',
    'Hollow_Point_Ward', 'Monster_Rounds', 'Rapid_Rounds', 'Restorative_Shot',
    
    // Vitality Items
    'Extra_Health', 'Extra_Regen', 'Extra_Stamina', 'Healing_Rite', 'Melee_Lifesteal',
    'Sprint_Boots', 'Bullet_Armor', 'Bullet_Lifesteal', 'Combat_Barrier',
    
    // Spirit Items
    'Extra_Charge', 'Extra_Spirit', 'Infuser', 'Mystic_Burst', 'Spirit_Strike',
    'Ammo_Scavenger', 'Arcane_Surge', 'Cold_Front', 'Decay', 'Ethereal_Shift',
    
    // Flex Items
    'Enduring_Speed', 'Improved_Burst', 'Improved_Reach', 'Mystic_Vulnerability',
    'Quicksilver_Reload', 'Restorative_Locket', 'Superior_Cooldown', 'Torment_Pulse'
];

// 이미지 다운로드 함수
async function downloadImage(url, filePath) {
    return new Promise((resolve, reject) => {
        const protocol = url.startsWith('https:') ? https : http;
        
        protocol.get(url, (response) => {
            if (response.statusCode === 200) {
                const fileStream = fs.createWriteStream(filePath);
                response.pipe(fileStream);
                
                fileStream.on('finish', () => {
                    fileStream.close();
                    console.log(`✅ Downloaded: ${path.basename(filePath)}`);
                    resolve();
                });
                
                fileStream.on('error', (err) => {
                    fs.unlink(filePath, () => {}); // 실패시 파일 삭제
                    reject(err);
                });
            } else if (response.statusCode >= 300 && response.statusCode < 400) {
                // 리다이렉트 처리
                downloadImage(response.headers.location, filePath).then(resolve).catch(reject);
            } else {
                reject(new Error(`HTTP ${response.statusCode}: ${url}`));
            }
        }).on('error', reject);
    });
}

// 캐릭터 이미지 다운로드
async function downloadHeroImages() {
    console.log('🎮 Downloading hero images...');
    
    for (const hero of HEROES) {
        const fileName = `${hero.toLowerCase().replace(/[^a-z0-9]/g, '_')}.jpg`;
        const filePath = path.join(HEROES_DIR, fileName);
        
        // 이미 파일이 존재하면 스킵
        if (fs.existsSync(filePath)) {
            console.log(`⏭️  Skipped (exists): ${fileName}`);
            continue;
        }
        
        try {
            // 여러 소스에서 이미지 다운로드 시도
            const sources = [
                `https://assets-bucket.deadlock-api.com/heroes/${hero.toLowerCase()}.jpg`,
                `https://deadlock-api.com/images/heroes/${hero.toLowerCase()}.jpg`,
                `https://raw.githubusercontent.com/deadlock-game/assets/main/heroes/${hero.toLowerCase()}.jpg`
            ];
            
            let downloaded = false;
            for (const url of sources) {
                try {
                    await downloadImage(url, filePath);
                    downloaded = true;
                    break;
                } catch (error) {
                    console.log(`❌ Failed source: ${url}`);
                }
            }
            
            if (!downloaded) {
                console.log(`⚠️  No source found for: ${hero}`);
                // 기본 이미지 생성
                createPlaceholderImage(filePath, hero);
            }
            
        } catch (error) {
            console.error(`❌ Error downloading ${hero}:`, error.message);
        }
        
        // 요청 간격 조절
        await new Promise(resolve => setTimeout(resolve, 500));
    }
}

// 아이템 이미지 다운로드
async function downloadItemImages() {
    console.log('🔧 Downloading item images...');
    
    for (const item of ITEMS) {
        const fileName = `${item.toLowerCase().replace(/[^a-z0-9]/g, '_')}.png`;
        const filePath = path.join(ITEMS_DIR, fileName);
        
        // 이미 파일이 존재하면 스킵
        if (fs.existsSync(filePath)) {
            console.log(`⏭️  Skipped (exists): ${fileName}`);
            continue;
        }
        
        try {
            // 여러 소스에서 이미지 다운로드 시도
            const sources = [
                `https://assets-bucket.deadlock-api.com/items/${item.toLowerCase()}.png`,
                `https://deadlock-api.com/images/items/${item.toLowerCase()}.png`,
                `https://raw.githubusercontent.com/deadlock-game/assets/main/items/${item.toLowerCase()}.png`
            ];
            
            let downloaded = false;
            for (const url of sources) {
                try {
                    await downloadImage(url, filePath);
                    downloaded = true;
                    break;
                } catch (error) {
                    console.log(`❌ Failed source: ${url}`);
                }
            }
            
            if (!downloaded) {
                console.log(`⚠️  No source found for: ${item}`);
                // 기본 이미지 생성
                createPlaceholderImage(filePath, item);
            }
            
        } catch (error) {
            console.error(`❌ Error downloading ${item}:`, error.message);
        }
        
        // 요청 간격 조절
        await new Promise(resolve => setTimeout(resolve, 500));
    }
}

// 플레이스홀더 이미지 생성 (SVG)
function createPlaceholderImage(filePath, name) {
    const isHero = filePath.includes('heroes');
    const extension = path.extname(filePath);
    const svgPath = filePath.replace(extension, '.svg');
    
    const svgContent = `
<svg width="64" height="64" xmlns="http://www.w3.org/2000/svg">
    <rect width="64" height="64" fill="${isHero ? '#4a5568' : '#2d3748'}"/>
    <text x="32" y="20" font-family="Arial" font-size="8" fill="white" text-anchor="middle">
        ${isHero ? '🎮' : '🔧'}
    </text>
    <text x="32" y="45" font-family="Arial" font-size="6" fill="white" text-anchor="middle">
        ${name.replace(/_/g, ' ')}
    </text>
</svg>`.trim();
    
    fs.writeFileSync(svgPath, svgContent);
    console.log(`📝 Created placeholder: ${path.basename(svgPath)}`);
}

// 기본 이미지들 생성
function createDefaultImages() {
    console.log('🖼️  Creating default images...');
    
    // 기본 캐릭터 이미지
    const defaultHeroPath = path.join(HEROES_DIR, 'default.svg');
    if (!fs.existsSync(defaultHeroPath)) {
        createPlaceholderImage(defaultHeroPath, 'Unknown Hero');
    }
    
    // 기본 아이템 이미지
    const defaultItemPath = path.join(ITEMS_DIR, 'default.svg');
    if (!fs.existsSync(defaultItemPath)) {
        createPlaceholderImage(defaultItemPath, 'Unknown Item');
    }
}

// 메인 실행 함수
async function main() {
    console.log('🚀 Starting Deadlock assets download...');
    console.log(`📁 Heroes directory: ${HEROES_DIR}`);
    console.log(`📁 Items directory: ${ITEMS_DIR}`);
    console.log('');
    
    try {
        await downloadHeroImages();
        console.log('');
        await downloadItemImages();
        console.log('');
        createDefaultImages();
        
        console.log('');
        console.log('✅ Asset download completed!');
        console.log(`📊 Heroes: ${fs.readdirSync(HEROES_DIR).length} files`);
        console.log(`📊 Items: ${fs.readdirSync(ITEMS_DIR).length} files`);
        
    } catch (error) {
        console.error('❌ Download failed:', error);
        process.exit(1);
    }
}

// 스크립트 직접 실행시
if (require.main === module) {
    main();
}

module.exports = { main, downloadHeroImages, downloadItemImages };