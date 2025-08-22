package com.kdt.crawler.model;

public class ProductInfo {
    private String keyword;
    private String productName;
    private String price;
    private String originalPrice;
    private String discountRate;
    private String description;
    private String seller;
    private String rating;
    private String reviewCount;
    private String deliveryInfo;
    private String productUrl;
    private String imageUrl;
    private String crawledAt;

    public ProductInfo() {}

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(String originalPrice) { this.originalPrice = originalPrice; }

    public String getDiscountRate() { return discountRate; }
    public void setDiscountRate(String discountRate) { this.discountRate = discountRate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeller() { return seller; }
    public void setSeller(String seller) { this.seller = seller; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getReviewCount() { return reviewCount; }
    public void setReviewCount(String reviewCount) { this.reviewCount = reviewCount; }

    public String getDeliveryInfo() { return deliveryInfo; }
    public void setDeliveryInfo(String deliveryInfo) { this.deliveryInfo = deliveryInfo; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCrawledAt() { return crawledAt; }
    public void setCrawledAt(String crawledAt) { this.crawledAt = crawledAt; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 상품 정보 ===\n");
        sb.append("검색 키워드: ").append(keyword).append("\n");
        sb.append("상품명: ").append(productName).append("\n");
        sb.append("현재가격: ").append(price).append("\n");
        sb.append("원래가격: ").append(originalPrice).append("\n");
        sb.append("할인율: ").append(discountRate).append("\n");
        sb.append("상품설명: ").append(description).append("\n");
        sb.append("판매자: ").append(seller).append("\n");
        sb.append("평점: ").append(rating).append("\n");
        sb.append("리뷰수: ").append(reviewCount).append("\n");
        sb.append("배송정보: ").append(deliveryInfo).append("\n");
        sb.append("상품URL: ").append(productUrl).append("\n");
        sb.append("이미지URL: ").append(imageUrl).append("\n");
        sb.append("수집일시: ").append(crawledAt).append("\n");
        sb.append("==================\n");
        return sb.toString();
    }
}
