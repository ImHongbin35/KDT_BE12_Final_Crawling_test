package com.kdt.crawler;

import com.kdt.crawler.model.ProductInfo;
import com.kdt.crawler.service.GoogleTrendsService;
import com.kdt.crawler.service.SsadaguCrawlerService;
import com.kdt.crawler.util.FileUtils;
import com.microsoft.playwright.*;
import java.util.Collections;
import java.util.List;

public class SsadaguCrawler {

    public static void main(String[] args) {
        System.out.println("🚀 === 싸다구몰 자동 상품 정보 크롤러 시작 ===");
        
        try (Playwright playwright = Playwright.create()) {
            // 브라우저 설정
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(false) // 브라우저 화면 보이도록 설정
                    .setSlowMo(2000)    // 액션 간 2초 대기 (안정성 향상)
                    .setArgs(Collections.singletonList("--lang=ko-KR"));

            Browser browser = playwright.chromium().launch(launchOptions);
            
            // 새로운 컨텍스트와 페이지 생성
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080));
            
            Page page = context.newPage();
            
            // 서비스 클래스 인스턴스 생성
            GoogleTrendsService trendsService = new GoogleTrendsService();
            SsadaguCrawlerService crawlerService = new SsadaguCrawlerService();
            
            try {
                // 1단계: 구글 트렌드에서 키워드 가져오기
                System.out.println("\n📊 1단계: 구글 트렌드에서 인기 키워드 수집 중...");
                FileUtils.saveLog("구글 트렌드 크롤링 시작");
                
                List<String> trendingKeywords = trendsService.getTrendingKeywords(page, 10);
                
                if (trendingKeywords.isEmpty()) {
                    System.out.println("⚠️ 트렌드 키워드를 가져오지 못했습니다. 기본 키워드를 사용합니다.");
                    FileUtils.saveLog("트렌드 키워드 수집 실패 - 기본 키워드 사용");
                } else {
                    System.out.println("✅ " + trendingKeywords.size() + "개의 트렌드 키워드를 수집했습니다!");
                    FileUtils.saveLog("트렌드 키워드 " + trendingKeywords.size() + "개 수집 완료");
                }
                
                // 2단계: 자동으로 첫 번째 키워드 선택
                System.out.println("\n🎯 2단계: 자동으로 키워드 선택");
                String selectedKeyword = trendsService.selectFirstKeyword(trendingKeywords);
                FileUtils.saveLog("선택된 키워드: " + selectedKeyword);
                
                // 3단계: 싸다구몰에서 상품 정보 크롤링
                System.out.println("\n🛒 3단계: 싸다구몰에서 '" + selectedKeyword + "' 상품 정보 수집 중...");
                FileUtils.saveLog("싸다구몰 크롤링 시작 - 키워드: " + selectedKeyword);
                
                ProductInfo productInfo = crawlerService.crawlProductInfo(page, selectedKeyword);
                
                // 4단계: 결과 출력 및 검증
                System.out.println("\n📋 4단계: 수집된 정보 검증 및 출력");
                validateAndDisplayResults(productInfo);
                
                // 5단계: 파일로 저장
                System.out.println("\n💾 5단계: 결과 파일 저장 중...");
                FileUtils.saveProductInfo(productInfo);
                FileUtils.saveLog("크롤링 완료 - " + productInfo.getProductName());
                
                System.out.println("\n🎉 === 크롤링 작업이 완료되었습니다! ===");
                System.out.println("📁 결과 파일이 data/ 폴더에 저장되었습니다.");
                System.out.println("🔍 키워드: " + selectedKeyword);
                System.out.println("📦 상품명: " + productInfo.getProductName());
                System.out.println("💰 가격: " + productInfo.getPrice());
                
            } catch (Exception e) {
                System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
                FileUtils.saveLog("크롤링 오류: " + e.getMessage());
                e.printStackTrace();
                
                // 오류 발생 시에도 기본 정보 저장
                ProductInfo errorInfo = new ProductInfo();
                errorInfo.setKeyword("오류 발생");
                errorInfo.setProductName("크롤링 실패");
                errorInfo.setPrice("정보 없음");
                errorInfo.setCrawledAt(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                FileUtils.saveProductInfo(errorInfo);
                
            } finally {
                // 브라우저 정리
                System.out.println("\n🔧 브라우저 정리 중...");
                page.waitForTimeout(3000); // 결과 확인을 위한 잠시 대기
                browser.close();
                System.out.println("✅ 브라우저가 정리되었습니다.");
            }
            
        } catch (PlaywrightException e) {
            System.err.println("❌ Playwright 초기화 오류: " + e.getMessage());
            System.err.println("💡 해결 방법:");
            System.err.println("   1. PlaywrightInstaller.java를 먼저 실행하세요.");
            System.err.println("   2. 인터넷 연결을 확인하세요.");
            System.err.println("   3. 바이러스 백신 소프트웨어를 일시 중지하세요.");
            e.printStackTrace();
        }
    }
    
    private static void validateAndDisplayResults(ProductInfo productInfo) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 수집된 상품 정보");
        System.out.println("=".repeat(60));
        
        // 키워드
        System.out.println("🔍 검색 키워드: " + productInfo.getKeyword());
        
        // 상품명
        String productName = productInfo.getProductName();
        if (productName != null && !productName.equals("정보 수집 실패") && !productName.equals("정보 없음")) {
            System.out.println("✅ 상품명: " + productName);
        } else {
            System.out.println("❌ 상품명: " + (productName != null ? productName : "정보 없음"));
        }
        
        // 가격
        String price = productInfo.getPrice();
        if (price != null && !price.equals("가격 정보 없음") && !price.equals("정보 없음")) {
            System.out.println("✅ 가격: " + price);
        } else {
            System.out.println("❌ 가격: " + (price != null ? price : "정보 없음"));
        }
        
        // 평점
        String rating = productInfo.getRating();
        if (rating != null && !rating.equals("평점 정보 없음") && !rating.equals("정보 없음")) {
            System.out.println("✅ 평점: " + rating);
        } else {
            System.out.println("❌ 평점: " + (rating != null ? rating : "정보 없음"));
        }
        
        // 재구매율 (설명에서 추출)
        String description = productInfo.getDescription();
        if (description != null && description.contains("재구매율")) {
            System.out.println("✅ 재구매율: " + description);
        }
        
        // 판매개수 (리뷰수에서 추출)
        String reviewCount = productInfo.getReviewCount();
        if (reviewCount != null && !reviewCount.equals("판매 정보 없음") && !reviewCount.equals("정보 없음")) {
            System.out.println("✅ 판매개수: " + reviewCount);
        }
        
        // URL
        String url = productInfo.getProductUrl();
        if (url != null && !url.equals("URL 정보 없음") && !url.equals("정보 없음")) {
            System.out.println("✅ 상품 URL: " + url);
        }
        
        // 이미지 URL
        String imageUrl = productInfo.getImageUrl();
        if (imageUrl != null && !imageUrl.equals("이미지 정보 없음") && !imageUrl.equals("정보 없음")) {
            System.out.println("✅ 이미지 URL: " + imageUrl);
        }
        
        // 수집 시간
        System.out.println("⏰ 수집 시간: " + productInfo.getCrawledAt());
        
        System.out.println("=".repeat(60));
        
        // 성공/실패 판단
        boolean isSuccess = (productName != null && !productName.equals("정보 수집 실패") && !productName.equals("정보 없음")) &&
                           (price != null && !price.equals("가격 정보 없음") && !price.equals("정보 없음"));
        
        if (isSuccess) {
            System.out.println("🎉 크롤링 성공! 주요 정보가 정상적으로 수집되었습니다.");
        } else {
            System.out.println("⚠️ 크롤링 부분 성공! 일부 정보만 수집되었습니다.");
            System.out.println("💡 다음 사항을 확인해보세요:");
            System.out.println("   - 싸다구몰 사이트의 구조가 변경되었을 수 있습니다.");
            System.out.println("   - 검색 결과에 상품이 없을 수 있습니다.");
            System.out.println("   - 네트워크 연결 상태를 확인하세요.");
        }
    }
}
