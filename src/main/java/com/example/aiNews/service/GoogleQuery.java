package com.example.aiNews.service;

import com.example.aiNews.util.Translator; 
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
            StringBuilder urlBuilder = new StringBuilder("https://www.googleapis.com/customsearch/v1");
            urlBuilder.append("?key=").append(apiKey);
            urlBuilder.append("&cx=").append(cx);
            urlBuilder.append("&num=10");

            String q;
            
            if (containsChinese(userKeyword)) {
                System.out.println("✅ Detected Chinese input. Applying Hybrid Search.");
                // 1. 翻譯：中文 -> 英文
                String translatedKeyword = Translator.translate("zh-TW", "en", userKeyword);
                
                // 2. 簡化查詢：只用翻譯後的英文 + AI
                String expandedKeyword = translatedKeyword + " AI technology news";
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                
                urlBuilder.append("&q=").append(q);
                urlBuilder.append("&gl=tw");           // 台灣優先
                
                
            } else {
                System.out.println("✅ Detected English/Global input.");
                // 英文模式：關鍵字擴充
                String expandedKeyword = userKeyword + " AI technology news";
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                
                urlBuilder.append("&q=").append(q);
                // ★ 關鍵修正：移除 dateRestrict，與 GitHub 版本保持一致
                // 這樣可以搜到更多豐富的英文資料
            }

            String url = urlBuilder.toString();
            
            System.out.println("\n=== Google Search Request ===");
            System.out.println("Query URL (masked): " + url.replace(apiKey, "***"));
            
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = resp.getBody();
            List<SearchItem> items = new ArrayList<>();
            
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> resultItems = (List<Map<String, Object>>) body.get("items");
                
                for (Map<String, Object> item : resultItems) {
                    String link = (String) item.get("link");
                    String title = (String) item.get("title");
                    String snippet = (String) item.get("snippet");

                    if (link.matches(".*\\.(pdf|xml|csv|xls|xlsx|doc|docx|ppt|pptx|zip|rar|gz|mht)$")) {
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

    private boolean containsChinese(String text) {
        if (text == null) return false;
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(text);
        return m.find();
    }
}