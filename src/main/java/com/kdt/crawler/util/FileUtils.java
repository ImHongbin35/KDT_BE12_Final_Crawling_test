package com.kdt.crawler.util;

import com.kdt.crawler.model.ProductInfo;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileUtils {
    
    private static final String DATA_DIR = "data";
    private static final String FILE_PREFIX = "product_";
    private static final String FILE_EXTENSION = ".txt";
    
    public static void saveProductInfo(ProductInfo productInfo) {
        try {
            // data 디렉토리 생성
            File dataDir = new File(DATA_DIR);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            // 파일명 생성 (키워드와 시간 기반)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = FILE_PREFIX + productInfo.getKeyword() + "_" + timestamp + FILE_EXTENSION;
            String filePath = DATA_DIR + File.separator + fileName;
            
            // 파일 저장
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
                writer.write(productInfo.toString());
                writer.newLine();
                writer.write("=== 상세 정보 (JSON 형태) ===\n");
                writer.write(toJsonString(productInfo));
            }
            
            System.out.println("파일이 저장되었습니다: " + filePath);
            
        } catch (IOException e) {
            System.err.println("파일 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String toJsonString(ProductInfo productInfo) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"keyword\": \"").append(escapeJson(productInfo.getKeyword())).append("\",\n");
        json.append("  \"productName\": \"").append(escapeJson(productInfo.getProductName())).append("\",\n");
        json.append("  \"price\": \"").append(escapeJson(productInfo.getPrice())).append("\",\n");
        json.append("  \"originalPrice\": \"").append(escapeJson(productInfo.getOriginalPrice())).append("\",\n");
        json.append("  \"discountRate\": \"").append(escapeJson(productInfo.getDiscountRate())).append("\",\n");
        json.append("  \"description\": \"").append(escapeJson(productInfo.getDescription())).append("\",\n");
        json.append("  \"seller\": \"").append(escapeJson(productInfo.getSeller())).append("\",\n");
        json.append("  \"rating\": \"").append(escapeJson(productInfo.getRating())).append("\",\n");
        json.append("  \"reviewCount\": \"").append(escapeJson(productInfo.getReviewCount())).append("\",\n");
        json.append("  \"deliveryInfo\": \"").append(escapeJson(productInfo.getDeliveryInfo())).append("\",\n");
        json.append("  \"productUrl\": \"").append(escapeJson(productInfo.getProductUrl())).append("\",\n");
        json.append("  \"imageUrl\": \"").append(escapeJson(productInfo.getImageUrl())).append("\",\n");
        json.append("  \"crawledAt\": \"").append(escapeJson(productInfo.getCrawledAt())).append("\"\n");
        json.append("}");
        return json.toString();
    }
    
    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\"", "\\\"")
                  .replace("\\", "\\\\")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    public static void saveLog(String message) {
        try {
            File dataDir = new File(DATA_DIR);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            String logFile = DATA_DIR + File.separator + "crawler_log.txt";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write("[" + timestamp + "] " + message);
                writer.newLine();
            }
            
        } catch (IOException e) {
            System.err.println("로그 저장 중 오류 발생: " + e.getMessage());
        }
    }
}
