package com.example.aiNews.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleQuery {
    
    @Value("${google.cse.apiKey}")
    private String apiKey;
    
    @Value("${google.cse.cx}")
    private String cx;
    
    private final RestTemplate restTemplate = new RestTemplate();

    public static class SearchItem {
        public String url;
        public String title;
        public String snippet;
        
        public SearchItem(String url, String title, String snippet) {
            this.url = url;
            this.title = title;
            this.snippet = snippet;
        }
        
        @Override
        public String toString() {
            return "SearchItem{url='" + url + "', title='" + title + "'}";
        }
    }

    public List<SearchItem> search(String userKeyword) {
        try {
            // 1. 基礎 URL
            StringBuilder urlBuilder = new StringBuilder("https://www.googleapis.com/customsearch/v1");
            urlBuilder.append("?key=").append(apiKey);
            urlBuilder.append("&cx=").append(cx);
            urlBuilder.append("&num=10");

            String q;
            
            // 2. 語言偵測與動態參數設定
            if (containsChinese(userKeyword)) {
                System.out.println("✅ Detected Chinese input. Applying Localization (TW).");
                
                // 中文模式：關鍵字擴充 + 地區限制
                String expandedKeyword = userKeyword + " AI 新聞";
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                
                // 加入 Google API 在地化參數
                urlBuilder.append("&q=").append(q);

                urlBuilder.append("&gl=tw");         // 限制地區：台灣
                
            } else {
                System.out.println("✅ Detected English/Global input.");
                
                // 英文模式：關鍵字擴充
                String expandedKeyword = userKeyword + " AI technology news";
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                
                urlBuilder.append("&q=").append(q);
                // 英文模式通常不特別限制地區，保持全球搜尋
            }

            String url = urlBuilder.toString();
            
            System.out.println("\n=== Google Search Request ===");
            System.out.println("Query URL (masked): " + url.replace(apiKey, "***"));
            
            // 3. 發送請求
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = resp.getBody();
            List<SearchItem> items = new ArrayList<>();
            
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> resultItems = (List<Map<String, Object>>) body.get("items");
                
                for (Map<String, Object> item : resultItems) {
                    String link = (String) item.get("link");
                    String title = (String) item.get("title");
                    String snippet = (String) item.get("snippet");

                    // 4. 過濾非網頁檔案
                    if (link.matches(".*\\.(pdf|xml|csv|xls|xlsx|doc|docx|ppt|pptx|zip|rar|gz|mht)$")) {
                        System.out.println("Ignored non-HTML file: " + link);
                        continue;
                    }
                    
                    items.add(new SearchItem(link, title, snippet));
                }
            }
            return items;
            
        } catch (Exception e) {
            System.err.println("GoogleQuery Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ★ 語言偵測：檢查是否包含中文字元
    private boolean containsChinese(String text) {
        if (text == null) return false;
        // Unicode 範圍 4E00-9FFF 是常用漢字
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(text);
        return m.find();
    }
}