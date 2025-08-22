@echo off
chcp 65001 > nul
echo 🚀 싸다구몰 자동 크롤링 프로젝트
echo ========================================
echo.

echo 📁 프로젝트 디렉토리로 이동 중...
cd /d "D:\D_AI_projects\KDT_BE12_Final_crowling_test"

echo.
echo 🎯 실행할 작업을 선택하세요:
echo 1. 브라우저 설치 (PlaywrightInstaller)
echo 2. 테스트 크롤링 (SsadaguTestCrawler)  
echo 3. 완전 자동화 크롤링 (SsadaguCrawler)
echo 4. 결과 폴더 열기
echo 5. 종료
echo.

set /p choice="선택 (1-5): "

if "%choice%"=="1" (
    echo.
    echo 🔧 브라우저 설치 중...
    mvn compile exec:java -Dexec.mainClass="com.kdt.crawler.PlaywrightInstaller"
) else if "%choice%"=="2" (
    echo.
    echo 🧪 테스트 크롤링 실행 중...
    mvn compile exec:java -Dexec.mainClass="com.kdt.crawler.SsadaguTestCrawler"
) else if "%choice%"=="3" (
    echo.
    echo 🚀 완전 자동화 크롤링 실행 중...
    mvn compile exec:java -Dexec.mainClass="com.kdt.crawler.SsadaguCrawler"
) else if "%choice%"=="4" (
    echo.
    echo 📁 결과 폴더 열기...
    if exist "data" (
        explorer "data"
    ) else (
        echo ❌ data 폴더가 존재하지 않습니다.
        mkdir data
        echo ✅ data 폴더를 생성했습니다.
        explorer "data"
    )
) else if "%choice%"=="5" (
    echo.
    echo 👋 프로그램을 종료합니다.
    exit /b 0
) else (
    echo.
    echo ❌ 잘못된 선택입니다. 다시 시도해주세요.
)

echo.
echo 작업이 완료되었습니다.
pause
