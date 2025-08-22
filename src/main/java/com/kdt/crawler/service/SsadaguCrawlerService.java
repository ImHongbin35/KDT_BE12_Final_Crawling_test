package com.kdt.crawler.service;

import com.kdt.crawler.model.ProductInfo;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SsadaguCrawlerService {

    public ProductInfo crawlProductInfo(Page page, String keyword) {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setKeyword(keyword);
        productInfo.setCrawledAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 1. 싸다구몰 메인 페이지 이동
            System.out.println("🌐 싸다구몰에 접속 중...");
            page.navigate("https://ssadagu.kr");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(3000);
            System.out.println("✅ 싸다구몰 접속 완료: " + page.url());

            // 2. 검색창 찾기 및 키워드 검색
            System.out.println("🔍 키워드 '" + keyword + "'로 검색 중...");
            
            // 검색창 찾기
            Locator searchBox = findSearchBox(page);
            
            if (searchBox == null) {
                throw new RuntimeException("검색창을 찾을 수 없습니다.");
            }

            // 검색어 입력 및 검색 (여러 방법 시도)
            try {
                searchBox.clear();
                page.waitForTimeout(500);
                searchBox.fill(keyword);
                page.waitForTimeout(1000);
                
                // Enter 키로 검색 시도
                searchBox.press("Enter");
                page.waitForTimeout(2000);
                
                // 검색이 안 된 경우 검색 버튼 클릭 시도
                String currentUrl = page.url();
                if (!currentUrl.contains("search") && !currentUrl.contains(keyword)) {
                    System.out.println("🔄 Enter 키 검색 실패, 검색 버튼 클릭 시도...");
                    
                    // 검색 버튼 찾기
                    String[] searchButtonSelectors = {
                        "button[type='submit']", "input[type='submit']",
                        ".search-button", "#search-button", ".btn-search",
                        "button:contains(검색)", "input[value*='검색']"
                    };
                    
                    for (String buttonSelector : searchButtonSelectors) {
                        try {
                            if (page.locator(buttonSelector).count() > 0) {
                                page.locator(buttonSelector).first().click();
                                page.waitForTimeout(2000);
                                break;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ 일반적인 검색 방법 실패, JavaScript로 시도...");
                
                // JavaScript로 강제 검색
                page.evaluate("() => {" +
                    "const searchInput = document.querySelector('input[name=\"ss_tx\"], input[type=\"text\"]');" +
                    "if (searchInput) {" +
                    "  searchInput.value = '" + keyword + "';" +
                    "  const form = searchInput.closest('form');" +
                    "  if (form) {" +
                    "    form.submit();" +
                    "  } else {" +
                    "    const event = new KeyboardEvent('keydown', { key: 'Enter' });" +
                    "    searchInput.dispatchEvent(event);" +
                    "  }" +
                    "}" +
                "}");
            }
            
            // 검색 결과 페이지 로딩 대기
            System.out.println("⏳ 검색 결과 로딩 중...");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(5000);
            
            System.out.println("✅ 검색 완료: " + page.url());

            // 3. 첫 번째 상품 찾기 및 실제 클릭
            System.out.println("🎯 첫 번째 상품을 찾아서 클릭 중...");
            
            boolean productClicked = clickFirstProduct(page);
            
            if (!productClicked) {
                System.out.println("❌ 첫 번째 상품을 클릭하지 못했습니다. 검색 결과에서 정보를 추출합니다.");
                extractFromSearchResults(page, productInfo);
                return productInfo;
            }
            
            // 4. 상품 상세 페이지에서 정보 추출
            System.out.println("📦 상품 상세 페이지에서 정보 추출 중...");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(3000);
            
            System.out.println("✅ 상품 상세 페이지 접속 완료: " + page.url());
            productInfo.setProductUrl(page.url());
            
            // 상세 페이지에서 정보 추출
            extractFromDetailPage(page, productInfo);
            
        } catch (Exception e) {
            System.err.println("❌ 크롤링 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            
            // 오류 발생 시 기본 정보라도 저장
            if (productInfo.getProductName() == null || productInfo.getProductName().isEmpty()) {
                productInfo.setProductName("정보 수집 실패");
                productInfo.setDescription("크롤링 오류: " + e.getMessage());
            }
        }

        return productInfo;
    }
    
    private Locator findSearchBox(Page page) {
        String[] searchSelectors = {
            "input[name='ss_tx']",           // 메인 검색창
            "input[type='text'][name*='search']",
            "input[type='text'][name*='ss']",
            "input[placeholder*='검색']",
            "input[placeholder*='상품']",
            "input[placeholder*='찾기']",
            "#ss_tx",
            ".search-input",
            "#search-input",
            "input[type='text']",
            "input[type='search']"
        };
        
        for (String selector : searchSelectors) {
            try {
                Locator locator = page.locator(selector);
                if (locator.count() > 0) {
                    // 검색창이 보이고 활성화되어 있는지 확인
                    Locator firstInput = locator.first();
                    if (firstInput.isVisible() && firstInput.isEnabled()) {
                        System.out.println("🎯 검색창 발견: " + selector);
                        return firstInput;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        // JavaScript로 검색창 찾기 시도
        try {
            System.out.println("🔄 JavaScript로 검색창 찾기 시도...");
            Object result = page.evaluate("() => {" +
                "const inputs = document.querySelectorAll('input[type=\"text\"], input[type=\"search\"], input:not([type])');" +
                "for (let input of inputs) {" +
                "  const placeholder = input.placeholder ? input.placeholder.toLowerCase() : '';" +
                "  const name = input.name ? input.name.toLowerCase() : '';" +
                "  const id = input.id ? input.id.toLowerCase() : '';" +
                "  if (placeholder.includes('검색') || placeholder.includes('찾기') || " +
                "      name.includes('search') || name.includes('ss') || " +
                "      id.includes('search') || id.includes('ss')) {" +
                "    input.focus();" +
                "    return true;" +
                "  }" +
                "}" +
                "if (inputs.length > 0) {" +
                "  inputs[0].focus();" +
                "  return true;" +
                "}" +
                "return false;" +
            "}");
            
            if (Boolean.TRUE.equals(result)) {
                System.out.println("✅ JavaScript로 검색창 포커스 성공");
                return page.locator("input:focus").first();
            }
        } catch (Exception e) {
            System.out.println("JavaScript 검색창 찾기 실패: " + e.getMessage());
        }
        
        System.err.println("❌ 검색창을 찾을 수 없습니다.");
        return null;
    }
    
    private boolean clickFirstProduct(Page page) {
        try {
            System.out.println("🔍 상품 링크를 찾는 중...");
            
            // 페이지가 완전히 로드될 때까지 잠시 더 기다림
            page.waitForTimeout(2000);
            
            // 실제 상품 링크만 선택 (search_view.php 제외)
            String[] productLinkSelectors = {
                "a[href*='view.php']:not([href*='search_view.php'])",  // view.php 포함하지만 search_view.php는 제외
                "a[href*='view.php'][href*='platform'][href*='num_iid']"  // 플랫폼과 상품ID가 있는 실제 상품 링크
            };
            
            for (String selector : productLinkSelectors) {
                try {
                    Locator products = page.locator(selector);
                    int count = products.count();
                    System.out.println("📋 선택자 '" + selector + "'로 " + count + "개 요소 발견");
                    
                    if (count > 0) {
                        // 첫 번째부터 세 번째까지 시도 (첫 번째가 광고일 수 있음)
                        for (int i = 0; i < Math.min(count, 3); i++) {
                            try {
                                Locator product = products.nth(i);
                                
                                // 요소가 보이고 클릭 가능한지 확인
                                if (product.isVisible() && product.isEnabled()) {
                                    System.out.println("✅ " + (i+1) + "번째 상품 클릭 시도...");
                                    
                                    // 스크롤해서 요소가 보이도록 함
                                    product.scrollIntoViewIfNeeded();
                                    page.waitForTimeout(1000);
                                    
                                    // 현재 URL 저장
                                    String beforeUrl = page.url();
                                    
                                    // 클릭 시도
                                    product.click();
                                    page.waitForTimeout(3000);
                                    
                                    // URL이 변경되었는지 확인
                                    String afterUrl = page.url();
                                    if (!afterUrl.equals(beforeUrl)) {
                                        System.out.println("🎉 상품 클릭 성공! 페이지 이동됨: " + afterUrl);
                                        return true;
                                    } else {
                                        System.out.println("⚠️ 클릭했지만 페이지가 변경되지 않음. 다음 상품 시도...");
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("❌ " + (i+1) + "번째 상품 클릭 실패: " + e.getMessage());
                                continue;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("❌ 선택자 '" + selector + "' 시도 중 오류: " + e.getMessage());
                    continue;
                }
            }
            
            // JavaScript로 실제 상품 링크만 클릭 시도
            System.out.println("🔄 JavaScript로 실제 상품 링크 클릭 시도...");
            Object result = page.evaluate("() => {" +
                // 실제 상품 링크만 찾기 (search_view.php 제외)
                "const productLinks = document.querySelectorAll('a[href*=\"view.php\"]:not([href*=\"search_view.php\"])');" +
                "console.log('실제 상품 링크 개수:', productLinks.length);" +
                
                "for (let i = 0; i < Math.min(productLinks.length, 5); i++) {" +
                "  const link = productLinks[i];" +
                "  const href = link.href;" +
                "  console.log('링크 ' + (i+1) + ':', href);" +
                
                "  // platform과 num_iid 파라미터가 있는 실제 상품 링크인지 확인" +
                "  if (href.includes('platform=') && href.includes('num_iid=') && link.offsetParent !== null) {" +
                "    try {" +
                "      link.scrollIntoView();" +
                "      link.click();" +
                "      console.log('실제 상품 링크 클릭 성공:', href);" +
                "      return {success: true, url: href, index: i};" +
                "    } catch(e) {" +
                "      console.log('링크 클릭 오류:', e.message);" +
                "    }" +
                "  } else {" +
                "    console.log('상품 링크가 아님:', href);" +
                "  }" +
                "}" +
                
                // 백업: 상품 컨테이너에서 실제 상품 링크 찾기
                "console.log('백업 방법: 상품 컨테이너에서 링크 찾기');" +
                "const containers = document.querySelectorAll('div[class*=\"product\"], div[class*=\"item\"]');" +
                "for (let i = 0; i < Math.min(containers.length, 3); i++) {" +
                "  const container = containers[i];" +
                "  const link = container.querySelector('a[href*=\"view.php\"]:not([href*=\"search_view.php\"])');" +
                "  if (link && link.href.includes('platform=') && link.href.includes('num_iid=') && link.offsetParent !== null) {" +
                "    try {" +
                "      link.scrollIntoView();" +
                "      link.click();" +
                "      console.log('컨테이너에서 상품 링크 클릭 성공:', link.href);" +
                "      return {success: true, url: link.href, index: i};" +
                "    } catch(e) {" +
                "      console.log('컨테이너 링크 클릭 오류:', e.message);" +
                "    }" +
                "  }" +
                "}" +
                
                "return {success: false};" +
            "}");
            
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                Boolean success = (Boolean) resultMap.get("success");
                String clickedUrl = (String) resultMap.get("url");
                
                if (Boolean.TRUE.equals(success)) {
                    page.waitForTimeout(3000);
                    String currentUrl = page.url();
                    System.out.println("🎉 JavaScript 클릭 성공!");
                    System.out.println("클릭한 링크: " + clickedUrl);
                    System.out.println("현재 URL: " + currentUrl);
                    return true;
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 상품 클릭 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("❌ 모든 방법으로 상품 클릭을 시도했지만 실패했습니다.");
        return false;
    }
    
    private void extractFromSearchResults(Page page, ProductInfo productInfo) {
        System.out.println("🔄 검색 결과 페이지에서 정보 추출 중...");
        
        try {
            String htmlContent = page.content();
            Document doc = Jsoup.parse(htmlContent);
            
            // 첫 번째 상품 정보 추출
            Element firstProduct = findFirstProductInResults(doc);
            
            if (firstProduct != null) {
                System.out.println("✅ 검색 결과에서 첫 번째 상품 발견!");
                
                // 상품명 추출
                String productName = extractProductName(firstProduct);
                productInfo.setProductName(productName);
                System.out.println("📝 상품명: " + productName);
                
                // 가격 추출
                String price = extractProductPrice(firstProduct);
                productInfo.setPrice(price);
                System.out.println("💰 가격: " + price);
                
                // 평점 추출
                String rating = extractProductRating(firstProduct);
                productInfo.setRating(rating);
                System.out.println("⭐ 평점: " + rating);
                
                // 재구매율 추출
                String repurchaseRate = extractRepurchaseRate(firstProduct);
                System.out.println("🔄 재구매율: " + repurchaseRate);
                
                // 판매개수 추출
                String salesCount = extractSalesCount(firstProduct);
                productInfo.setReviewCount(salesCount);
                System.out.println("📊 판매개수: " + salesCount);
                
                // 설명에 재구매율 정보 포함
                productInfo.setDescription("재구매율: " + repurchaseRate + ", 판매개수: " + salesCount);
                
                // 상품 URL 추출
                String productUrl = extractProductUrl(firstProduct);
                if (!productUrl.equals("URL 정보 없음")) {
                    productInfo.setProductUrl(productUrl);
                    System.out.println("🔗 상품 URL: " + productUrl);
                } else {
                    productInfo.setProductUrl(page.url());
                }
                
                // 이미지 URL 추출
                String imageUrl = extractImageUrl(firstProduct);
                productInfo.setImageUrl(imageUrl);
                System.out.println("🖼️ 이미지 URL: " + imageUrl);
                
                // 기본 정보 설정
                productInfo.setSeller("싸다구몰");
                productInfo.setDeliveryInfo("배송 정보 확인 필요");
            } else {
                System.out.println("❌ 검색 결과에서 상품을 찾을 수 없습니다.");
                setDefaultProductInfo(productInfo, page.url());
            }
            
        } catch (Exception e) {
            System.err.println("❌ 검색 결과 추출 중 오류: " + e.getMessage());
            setDefaultProductInfo(productInfo, page.url());
        }
    }
    
    private void extractFromDetailPage(Page page, ProductInfo productInfo) {
        System.out.println("🔍 상품 상세 페이지에서 정보 추출 중...");
        
        try {
            String htmlContent = page.content();
            Document doc = Jsoup.parse(htmlContent);
            
            // 상세 페이지에서 정보 추출
            extractDetailedProductInfo(doc, productInfo);
            
        } catch (Exception e) {
            System.err.println("❌ 상세 페이지 정보 추출 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Element findFirstProductInResults(Document doc) {
        String[] productSelectors = {
            "div.product_item",
            "div.product_info", 
            "div:has(.product_title)",
            "div:has(.product_price)",
            "div:has(a[href*='view.php'])",
            ".product_item",
            ".product_info"
        };
        
        for (String selector : productSelectors) {
            try {
                Elements elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    System.out.println("🎯 상품 선택자 발견: " + selector + " (개수: " + elements.size() + ")");
                    return elements.first();
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        return null;
    }
    
    private String extractProductName(Element productElement) {
        String[] nameSelectors = {
            ".product_title", "div.product_title", ".title", "[class*='title']", 
            "a[href*='view.php']", ".product-name", ".product_name",
            ".goods-name", ".item-title", ".product-title",
            "h1", "h2", "h3", "h4",
            ".name", "[class*='name']", 
            "a[title]", // 링크의 title 속성
            "img[alt]"   // 이미지의 alt 속성
        };
        
        for (String selector : nameSelectors) {
            try {
                Element element = productElement.selectFirst(selector);
                if (element != null) {
                    String text = element.text().trim();
                    
                    // 텍스트가 없으면 title이나 alt 속성 확인
                    if (text.isEmpty()) {
                        text = element.attr("title");
                        if (text.isEmpty()) {
                            text = element.attr("alt");
                        }
                    }
                    
                    if (!text.isEmpty() && text.length() > 3 && text.length() < 200) {
                        // 의미없는 텍스트 필터링
                        if (!text.equals("더보기") && !text.equals("상세보기") && 
                            !text.equals("이미지") && !text.matches("^[0-9]+$")) {
                            return text;
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        return "상품명 정보 없음";
    }
    
    private String extractProductPrice(Element productElement) {
        String[] priceSelectors = {
            ".product_price", "div.product_price", ".price", "[class*='price']", 
            "[class*='cost']", ".amount", ".money", ".won", ".sale-price",
            ".current-price", ".final-price", ".price-current",
            "span:contains(원)", "div:contains(원)", 
            "strong:contains(원)", "em:contains(원)"
        };
        
        for (String selector : priceSelectors) {
            try {
                Element element = productElement.selectFirst(selector);
                if (element != null && !element.text().trim().isEmpty()) {
                    String priceText = element.text().trim();
                    
                    // 숫자와 원이 포함된 텍스트만 가격으로 인정
                    if ((priceText.matches(".*[0-9,]+.*") && priceText.contains("원")) ||
                        priceText.matches(".*[0-9,]+\\s*원.*") ||
                        priceText.matches(".*\\d+.*원.*")) {
                        
                        // 가격에서 불필요한 텍스트 제거
                        priceText = priceText.replaceAll("판매가격|가격|Price|price", "").trim();
                        
                        if (priceText.length() > 0 && priceText.length() < 50) {
                            return priceText;
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        // 정규식으로 가격 패턴 직접 찾기
        try {
            String fullText = productElement.text();
            java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("([0-9,]+\\s*원)");
            java.util.regex.Matcher matcher = pricePattern.matcher(fullText);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // 무시
        }
        
        return "가격 정보 없음";
    }
    
    private String extractProductRating(Element productElement) {
        try {
            Element starContainer = productElement.selectFirst(".start");
            if (starContainer != null) {
                Elements allStars = starContainer.select("img[src*='icon_star.svg']");
                Elements fullStars = starContainer.select("img[src*='icon_star.svg']:not([src*='none'])");
                
                if (allStars.size() > 0) {
                    return fullStars.size() + "/" + allStars.size() + "점";
                }
            }
            
            String[] ratingSelectors = {
                ".rating", ".star", ".score", ".grade", "[class*='rating']"
            };
            
            for (String selector : ratingSelectors) {
                Element element = productElement.selectFirst(selector);
                if (element != null && !element.text().trim().isEmpty()) {
                    return element.text().trim();
                }
            }
            
        } catch (Exception e) {
            System.err.println("평점 추출 오류: " + e.getMessage());
        }
        
        return "평점 정보 없음";
    }
    
    private String extractRepurchaseRate(Element productElement) {
        try {
            Element repurchaseElement = productElement.selectFirst(".product_repurchaseRate");
            if (repurchaseElement != null) {
                String text = repurchaseElement.text().trim();
                if (text.contains("%")) {
                    String[] parts = text.split("\\s+");
                    for (String part : parts) {
                        if (part.contains("%")) {
                            return part;
                        }
                    }
                }
                return text;
            }
        } catch (Exception e) {
            System.err.println("재구매율 추출 오류: " + e.getMessage());
        }
        
        return "재구매율 정보 없음";
    }
    
    private String extractSalesCount(Element productElement) {
        try {
            Element salesElement = productElement.selectFirst(".product_sales");
            if (salesElement != null) {
                String text = salesElement.text().trim();
                if (text.contains("개")) {
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*개");
                    java.util.regex.Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        return matcher.group(1) + "개";
                    }
                }
                return text;
            }
        } catch (Exception e) {
            System.err.println("판매개수 추출 오류: " + e.getMessage());
        }
        
        return "판매 정보 없음";
    }
    
    private String extractProductUrl(Element productElement) {
        try {
            Element linkElement = productElement.selectFirst("a[href*='view.php']");
            if (linkElement != null) {
                String href = linkElement.attr("href");
                if (href.startsWith("http")) {
                    return href;
                } else if (href.startsWith("/")) {
                    return "https://ssadagu.kr" + href;
                } else {
                    return "https://ssadagu.kr/" + href;
                }
            }
        } catch (Exception e) {
            System.err.println("상품 URL 추출 오류: " + e.getMessage());
        }
        
        return "URL 정보 없음";
    }
    
    private String extractImageUrl(Element productElement) {
        try {
            String[] imageSelectors = {
                "img[src*='alicdn.com']", ".product_image img", "img[alt]:not([src*='icon'])", 
                "img[class*='hover']", "img"
            };
            
            for (String selector : imageSelectors) {
                Element imgElement = productElement.selectFirst(selector);
                if (imgElement != null) {
                    String src = imgElement.attr("src");
                    if (src != null && !src.isEmpty() && !src.contains("icon")) {
                        if (src.startsWith("http")) {
                            return src;
                        } else if (src.startsWith("/")) {
                            return "https://ssadagu.kr" + src;
                        } else {
                            return "https://ssadagu.kr/" + src;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("이미지 URL 추출 오류: " + e.getMessage());
        }
        
        return "이미지 정보 없음";
    }
    
    private void extractDetailedProductInfo(Document doc, ProductInfo productInfo) {
        System.out.println("📋 상세 페이지에서 정보 추출 중...");
        
        // 상품명 (상세 페이지에서 더 정확한 정보)
        String[] nameSelectors = {
            "h1", "h2", ".product-title", ".goods-name", ".title", 
            ".product-name", ".item-title", ".detail-title", ".product_title",
            ".goods-title", ".item-name", "[class*='title']", "[class*='name']"
        };
        
        String productName = extractTextBySelectors(doc, nameSelectors);
        if (!productName.equals("정보 없음") && productName.length() > 3) {
            productInfo.setProductName(productName);
            System.out.println("📝 상세 페이지 상품명: " + productName);
        }
        
        // 가격 정보 (더 포괄적인 선택자)
        String[] priceSelectors = {
            ".price", ".current-price", ".sale-price", ".price-current", 
            ".final-price", ".cost", ".won", ".product_price", ".amount",
            ".price-now", ".selling-price", "[class*='price']", "[class*='cost']",
            "span:contains(원)", "strong:contains(원)", "em:contains(원)"
        };
        
        String price = extractPriceBySelectors(doc, priceSelectors);
        if (!price.equals("정보 없음")) {
            productInfo.setPrice(price);
            System.out.println("💰 상세 페이지 가격: " + price);
        }
        
        // 상품 상세 스펙 정보 추출 (.pro-info-boxs 구조)
        String detailedSpecs = extractProductSpecs(doc);
        if (!detailedSpecs.isEmpty()) {
            // 기존 설명과 결합하거나 대체
            String existingDesc = productInfo.getDescription();
            if (existingDesc != null && !existingDesc.isEmpty()) {
                productInfo.setDescription(existingDesc + "\n\n=== 상품 상세 스펙 ===\n" + detailedSpecs);
            } else {
                productInfo.setDescription("=== 상품 상세 스펙 ===\n" + detailedSpecs);
            }
            System.out.println("📊 상품 스펙 정보를 추출했습니다.");
        }
        
        // 원가 정보
        String[] originalPriceSelectors = {
            ".original-price", ".regular-price", ".price-original",
            ".before-price", ".market-price", ".old-price", ".price-before"
        };
        
        String originalPrice = extractPriceBySelectors(doc, originalPriceSelectors);
        if (!originalPrice.equals("정보 없음")) {
            productInfo.setOriginalPrice(originalPrice);
            System.out.println("💵 원가: " + originalPrice);
        }
        
        // 할인율 정보
        String[] discountSelectors = {
            ".discount-rate", ".sale-rate", ".discount-percent",
            ".rate", ".discount", ".sale", "[class*='discount']"
        };
        
        String discountRate = extractTextBySelectors(doc, discountSelectors);
        if (!discountRate.equals("정보 없음")) {
            productInfo.setDiscountRate(discountRate);
            System.out.println("🏷️ 할인율: " + discountRate);
        }
        
        // 판매자 정보 (스펙에서도 추출)
        String seller = extractSellerFromSpecs(doc);
        if (!seller.equals("정보 없음")) {
            productInfo.setSeller(seller);
            System.out.println("🏪 판매자: " + seller);
        }
        
        // 배송 정보 (스펙에서도 추출)
        String deliveryInfo = extractDeliveryFromSpecs(doc);
        if (!deliveryInfo.equals("정보 없음")) {
            productInfo.setDeliveryInfo(deliveryInfo);
            System.out.println("🚚 배송 정보: " + deliveryInfo);
        }
        
        // 평점 정보 (상세 페이지에서)
        String[] ratingSelectors = {
            ".rating", ".star", ".score", ".grade", "[class*='rating']",
            "[class*='star']", ".review-rating"
        };
        
        String rating = extractTextBySelectors(doc, ratingSelectors);
        if (!rating.equals("정보 없음")) {
            productInfo.setRating(rating);
            System.out.println("⭐ 평점: " + rating);
        }
        
        // 리뷰 수 정보
        String[] reviewSelectors = {
            ".review-count", ".review-num", "[class*='review']", 
            ".comment-count", "[class*='comment']"
        };
        
        String reviewCount = extractTextBySelectors(doc, reviewSelectors);
        if (!reviewCount.equals("정보 없음")) {
            productInfo.setReviewCount(reviewCount);
            System.out.println("💬 리뷰 수: " + reviewCount);
        }
        
        // 이미지 URL (상세 페이지에서 더 고해상도 이미지)
        String[] imageSelectors = {
            ".product-image img", ".main-image img", ".goods-image img", 
            "img.product", "img[src*='alicdn.com']", ".detail-image img",
            "img[alt*='상품']", "img[alt*='제품']", "img"
        };
        
        String imageUrl = extractImageUrlBySelectors(doc, imageSelectors);
        if (!imageUrl.equals("이미지 없음")) {
            productInfo.setImageUrl(imageUrl);
            System.out.println("🖼️ 상세 페이지 이미지: " + imageUrl);
        }
    }
    
    private String extractTextBySelectors(Document doc, String[] selectors) {
        for (String selector : selectors) {
            try {
                Element element = doc.selectFirst(selector);
                if (element != null && !element.text().trim().isEmpty()) {
                    return element.text().trim();
                }
            } catch (Exception e) {
                continue;
            }
        }
        return "정보 없음";
    }
    
    private String extractPriceBySelectors(Document doc, String[] selectors) {
        for (String selector : selectors) {
            try {
                Element element = doc.selectFirst(selector);
                if (element != null && !element.text().trim().isEmpty()) {
                    String priceText = element.text().trim();
                    
                    // 숫자와 원이 포함된 텍스트만 가격으로 인정
                    if ((priceText.matches(".*[0-9,]+.*") && priceText.contains("원")) ||
                        priceText.matches(".*[0-9,]+\\s*원.*") ||
                        priceText.matches(".*\\d+.*원.*")) {
                        
                        // 가격에서 불필요한 텍스트 제거
                        priceText = priceText.replaceAll("판매가격|가격|Price|price", "").trim();
                        
                        if (priceText.length() > 0 && priceText.length() < 50) {
                            return priceText;
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        // 정규식으로 가격 패턴 직접 찾기
        try {
            String fullText = doc.text();
            java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("([0-9,]+\\s*원)");
            java.util.regex.Matcher matcher = pricePattern.matcher(fullText);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // 무시
        }
        
        return "정보 없음";
    }
    
    private String extractImageUrlBySelectors(Document doc, String[] selectors) {
        for (String selector : selectors) {
            try {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    String src = element.attr("src");
                    if (src != null && !src.isEmpty()) {
                        if (src.startsWith("http")) {
                            return src;
                        } else if (src.startsWith("/")) {
                            return "https://ssadagu.kr" + src;
                        } else {
                            return "https://ssadagu.kr/" + src;
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return "이미지 없음";
    }
    
    private String extractProductSpecs(Document doc) {
        StringBuilder specs = new StringBuilder();
        
        try {
            // .pro-info-boxs 구조 추출
            Element proInfoBoxs = doc.selectFirst(".pro-info-boxs");
            if (proInfoBoxs != null) {
                Elements proInfoItems = proInfoBoxs.select(".pro-info-item");
                
                System.out.println("📊 상품 스펙 " + proInfoItems.size() + "개 항목 발견");
                
                for (Element item : proInfoItems) {
                    Element titleElement = item.selectFirst(".pro-info-title");
                    Element infoElement = item.selectFirst(".pro-info-info");
                    
                    if (titleElement != null && infoElement != null) {
                        String title = titleElement.text().trim();
                        String info = infoElement.text().trim();
                        
                        if (!title.isEmpty() && !info.isEmpty() && info.length() < 100) {
                            specs.append(title).append(": ").append(info).append("\n");
                        }
                    }
                }
            }
            
            // 다른 상품 스펙 구조도 시도
            if (specs.length() == 0) {
                String[] specSelectors = {
                    ".product-attributes .attribute-item",
                    ".spec-list .spec-item", 
                    ".product-specs .spec",
                    ".details-list .detail-item",
                    "[class*='spec'] [class*='item']",
                    "[class*='attr'] [class*='item']"
                };
                
                for (String selector : specSelectors) {
                    Elements specItems = doc.select(selector);
                    if (specItems.size() > 0) {
                        System.out.println("📊 대체 스펙 구조에서 " + specItems.size() + "개 항목 발견");
                        
                        for (Element item : specItems) {
                            String text = item.text().trim();
                            if (text.contains(":") && text.length() < 100) {
                                specs.append(text).append("\n");
                            }
                        }
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("스펙 추출 중 오류: " + e.getMessage());
        }
        
        return specs.toString();
    }
    
    private String extractSellerFromSpecs(Document doc) {
        try {
            Element proInfoBoxs = doc.selectFirst(".pro-info-boxs");
            if (proInfoBoxs != null) {
                Elements proInfoItems = proInfoBoxs.select(".pro-info-item");
                
                for (Element item : proInfoItems) {
                    Element titleElement = item.selectFirst(".pro-info-title");
                    Element infoElement = item.selectFirst(".pro-info-info");
                    
                    if (titleElement != null && infoElement != null) {
                        String title = titleElement.text().trim().toLowerCase();
                        String info = infoElement.text().trim();
                        
                        if ((title.contains("상표") || title.contains("브랜드") || title.contains("제조사") || 
                             title.contains("판매자") || title.contains("brand") || title.contains("manufacturer")) 
                             && !info.isEmpty() && info.length() < 50) {
                            return info;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 무시
        }
        
        // 기본 선택자로 재시도
        String[] sellerSelectors = {
            ".seller", ".brand", ".manufacturer", ".vendor", ".shop-name", 
            ".store", ".seller-name", "[class*='seller']", "[class*='brand']"
        };
        
        return extractTextBySelectors(doc, sellerSelectors);
    }
    
    private String extractDeliveryFromSpecs(Document doc) {
        try {
            Element proInfoBoxs = doc.selectFirst(".pro-info-boxs");
            if (proInfoBoxs != null) {
                Elements proInfoItems = proInfoBoxs.select(".pro-info-item");
                
                for (Element item : proInfoItems) {
                    Element titleElement = item.selectFirst(".pro-info-title");
                    Element infoElement = item.selectFirst(".pro-info-info");
                    
                    if (titleElement != null && infoElement != null) {
                        String title = titleElement.text().trim().toLowerCase();
                        String info = infoElement.text().trim();
                        
                        if ((title.contains("배송") || title.contains("물류") || title.contains("운송") || 
                             title.contains("delivery") || title.contains("shipping") || 
                             title.contains("가장 빠른 배송") || title.contains("배송물류회사")) 
                             && !info.isEmpty() && info.length() < 100) {
                            return info;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 무시
        }
        
        // 기본 선택자로 재시도
        String[] deliverySelectors = {
            ".delivery-info", ".shipping-info", ".delivery",
            ".shipping", ".delivery-text", ".ship", "[class*='delivery']",
            "[class*='shipping']"
        };
        
        return extractTextBySelectors(doc, deliverySelectors);
    }
    
    private void setDefaultProductInfo(ProductInfo productInfo, String currentUrl) {
        if (productInfo.getProductName() == null || productInfo.getProductName().isEmpty()) {
            productInfo.setProductName("검색 결과: " + productInfo.getKeyword());
        }
        
        if (productInfo.getPrice() == null || productInfo.getPrice().isEmpty()) {
            productInfo.setPrice("가격 확인 필요");
        }
        
        productInfo.setProductUrl(currentUrl);
        productInfo.setDescription("검색 결과 페이지에서 추출된 정보");
        productInfo.setSeller("싸다구몰");
        productInfo.setRating("평점 정보 없음");
        productInfo.setReviewCount("판매 정보 없음");
        productInfo.setImageUrl("이미지 정보 없음");
        productInfo.setDeliveryInfo("배송 정보 확인 필요");
    }
}
