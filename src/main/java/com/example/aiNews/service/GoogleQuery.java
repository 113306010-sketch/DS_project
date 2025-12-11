package com.example.aiNews.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
        
        public SearchItem(String url, String title) {
            this.url = url;
            this.title = title;
        }
        
        @Override
        public String toString() {
            return "SearchItem{url='" + url + "', title='" + title + "'}";
        }
    }

    public List<SearchItem> search(String userKeyword) {
        try {
            String expandedKeyword = userKeyword + " AI";
            String q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
            String url = "https://www.googleapis.com/customsearch/v1"
                    + "?key=" + apiKey
                    + "&cx=" + cx
                    + "&num=10"
                    + "&q=" + q;
            
            System.out.println("\n=== Google Search Request ===");
            System.out.println("Query: " + expandedKeyword);
            System.out.println("URL: " + url.replace(apiKey, "***"));
            
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = resp.getBody();
            List<SearchItem> items = new ArrayList<>();
            
            if (body != null) {
                System.out.println("\n=== API Response ===");
                System.out.println("Response keys: " + body.keySet());
                
                if (body.containsKey("items")) {
                    List<Map<String, Object>> resultItems = (List<Map<String, Object>>) body.get("items");
                    System.out.println("Total items from API: " + resultItems.size());
                    
                    if (!resultItems.isEmpty()) {
                        System.out.println("\n=== First Item (Full Data) ===");
                        Map<String, Object> firstItem = resultItems.get(0);
                        for (Map.Entry<String, Object> entry : firstItem.entrySet()) {
                            System.out.println(entry.getKey() + " = " + entry.getValue());
                        }
                    }
                    
                    System.out.println("\n=== Processing All Items ===");
                    for (int i = 0; i < resultItems.size(); i++) {
                        Map<String, Object> item = resultItems.get(i);
                        
                        System.out.println("\n--- Item " + (i + 1) + " ---");
                        
                        String link = (String) item.get("link");
                        String title = (String) item.get("title");
                        String htmlTitle = (String) item.get("htmlTitle");
                        String snippet = (String) item.get("snippet");
                        
                        System.out.println("link: " + link);
                        System.out.println("title: " + title);
                        System.out.println("htmlTitle: " + htmlTitle);
                        System.out.println("snippet: " + (snippet != null ? snippet.substring(0, Math.min(50, snippet.length())) + "..." : null));
                        
                        String finalTitle = null;
                        
                        if (title != null && !title.trim().isEmpty()) {
                            finalTitle = title;
                            System.out.println("✓ Using 'title'");
                        } else if (htmlTitle != null && !htmlTitle.trim().isEmpty()) {
                            finalTitle = htmlTitle;
                            System.out.println("✓ Using 'htmlTitle'");
                        } else if (snippet != null && !snippet.trim().isEmpty()) {
                            finalTitle = snippet.substring(0, Math.min(100, snippet.length()));
                            System.out.println("✓ Using 'snippet' (truncated)");
                        } else {
                            finalTitle = link;
                            System.out.println("✗ No title found, using URL");
                        }
                        
                        if (finalTitle != null) {
                            finalTitle = finalTitle.replaceAll("<[^>]*>", "").trim();
                        }
                        
                        System.out.println("Final title: " + finalTitle);
                        
                        SearchItem searchItem = new SearchItem(link, finalTitle);
                        items.add(searchItem);
                        System.out.println("Added: " + searchItem);
                    }
                } else {
                    System.out.println("\n✗ WARNING: No 'items' in response!");
                    if (body.containsKey("error")) {
                        System.out.println("API Error: " + body.get("error"));
                    }
                }
            } else {
                System.out.println("\n✗ ERROR: Response body is null!");
            }
            
            System.out.println("\n=== Search Complete ===");
            System.out.println("Returning " + items.size() + " items");
            for (int i = 0; i < items.size(); i++) {
                System.out.println((i+1) + ". " + items.get(i));
            }
            
            return items;
            
        } catch (Exception e) {
            System.err.println("\n✗ GoogleQuery Exception: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
