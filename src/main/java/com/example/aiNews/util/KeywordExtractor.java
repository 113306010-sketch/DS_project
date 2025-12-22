package com.example.aiNews.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用 Google Autocomplete API 取得搜尋建議
 * 取代原本的 KeywordExtractor
 */
public class KeywordExtractor {

    /**
     * 取得 Google 搜尋建議
     * 
     * @param keyword 使用者輸入的關鍵字
     * @return 搜尋建議列表
     */
    public static List<String> getSuggestions(String keyword) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // 組合查詢：關鍵字 + AI
            String query = keyword + " AI";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            // Google Autocomplete API（非官方但免費）
            String urlStr = "https://suggestqueries.google.com/complete/search"
                    + "?client=firefox"
                    + "&q=" + encodedQuery
                    + "&hl=zh-TW";  // 繁體中文
            
            System.out.println("🔍 Google Suggest URL: " + urlStr);
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("⚠️ Google Suggest API 回應碼: " + responseCode);
                return suggestions;
            }
            
            // 讀取回應
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // 解析 JSON 回應
            // 格式: ["query", ["suggestion1", "suggestion2", ...]]
            String json = response.toString();
            suggestions = parseJsonArray(json);
            
            System.out.println("✅ Google Suggest 回傳 " + suggestions.size() + " 個建議");
            
        } catch (Exception e) {
            System.err.println("❌ Google Suggest 錯誤: " + e.getMessage());
        }
        
        return suggestions;
    }
    
    /**
     * 解析 Google Autocomplete 的 JSON 回應
     * 格式: ["原始查詢", ["建議1", "建議2", "建議3", ...]]
     */
    private static List<String> parseJsonArray(String json) {
        List<String> results = new ArrayList<>();
        
        try {
            // 找到第二個陣列的位置（建議列表）
            int firstBracket = json.indexOf('[');
            int secondBracket = json.indexOf('[', firstBracket + 1);
            int closeBracket = json.indexOf(']', secondBracket);
            
            if (secondBracket == -1 || closeBracket == -1) {
                return results;
            }
            
            // 取出建議陣列的內容
            String arrayContent = json.substring(secondBracket + 1, closeBracket);
            
            // 分割字串，取出每個建議
            // 格式: "建議1","建議2","建議3"
            String[] items = arrayContent.split("\",\"");
            
            for (String item : items) {
                // 移除多餘的引號
                String cleaned = item.replace("\"", "").trim();
                if (!cleaned.isEmpty()) {
                    results.add(cleaned);
                }
            }
            
        } catch (Exception e) {
            System.err.println("JSON 解析錯誤: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * 測試用 main 方法
     */
    public static void main(String[] args) {
        System.out.println("=== 測試 Google Suggest ===");
        
        // 測試中文
        List<String> results1 = getSuggestions("政大");
        System.out.println("政大 AI 建議: " + results1);
        
        System.out.println();
        
        // 測試另一個關鍵字
        List<String> results2 = getSuggestions("灌籃高手");
        System.out.println("灌籃高手 AI 建議: " + results2);
    }
}