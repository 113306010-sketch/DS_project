package com.example.aiNews.service;

import com.example.aiNews.util.Translator; // ★ 匯入翻譯工具
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

            // 保留原本的排除清單 (很好用，繼續留著)
            String excludeTerms = " -site:play.google.com"
                                + " -site:apps.apple.com"
                                + " -site:shopee.tw"
                                + " -site:momo.com.tw"
                                + " -site:pchome.com.tw"
                                + " -site:104.com.tw"
                                + " -site:1111.com.tw"
                                + " -site:wikipedia.org"
                                + " -site:moedict.tw";

            String q;
            
            if (containsChinese(userKeyword)) {
                // ★ 1. 翻譯：例如 "生成式" -> "Generative"
                String translatedKeyword = Translator.translate("zh-TW", "en", userKeyword);
                System.out.println("🔠 Hybrid Search: [" + userKeyword + "] + [" + translatedKeyword + "]");
                
                // ★ 2. 組合查詢：(中文 OR 英文) + AI + 排除名單
                // 這樣 Google 會同時找中文和英文的高相關網頁
                String expandedKeyword = "(" + userKeyword + " OR " + translatedKeyword + ") AI 新聞" + excludeTerms;
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                
                urlBuilder.append("&q=").append(q);
                urlBuilder.append("&gl=tw");           // 台灣優先
                urlBuilder.append("&dateRestrict=y1"); // 最近一年 (確保時效性)
                
            } else {
                System.out.println("✅ Detected English/Global input.");
                String expandedKeyword = userKeyword + " AI technology news" + excludeTerms;
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                
                urlBuilder.append("&q=").append(q);
                urlBuilder.append("&dateRestrict=y1");
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

                    // 過濾非網頁檔案
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