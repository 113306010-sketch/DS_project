package com.example.aiNews.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class GoogleQuery { 
    private final String apiKey = System.getProperty("google.cse.apiKey", "AIzaSyCpVORgpEpRzZuWTKhvqXQjwCcA_uocilg"); 
    private final String cx = System.getProperty("google.cse.cx", "97ebc9910e337486f"); 
    private final RestTemplate restTemplate = new RestTemplate(); 

    public List<String> search(String userKeyword) { 
        try { 
            String expandedKeyword = userKeyword + "news"; 
            String q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8); 
            String url = "https://www.googleapis.com/customsearch/v1" + "?key=" + apiKey + "&cx=" + cx + "&num=10" + "&q=" + q ; 
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class); 
            Map body = resp.getBody(); 
            List<String> urls = new ArrayList<>(); 
            if (body != null && body.get("items") != null) { 
                java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) body.get("items"); 
                for (Map<String, Object> item : items) { 
                    String link = (String) item.get("link"); 
                    urls.add(link); 
                } 
            } 
            System.out.println("Query URL: " + url); 
            System.out.println("Got URLs: " + urls.size()); 
            return urls; 
        } catch (Exception e) { 
            System.out.println("GoogleQuery error: " + e.getMessage()); 
            return java.util.List.of(); 
        } 
    }
}