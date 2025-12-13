package com.example.aiNews.model;

import com.example.aiNews.util.HTMLFetcher;
import java.util.Arrays;
import java.util.List;

public class WebPage {

    public String url;
    public String content;
    public int aiKeywordCount;
    public int userKeywordCount;
    public double score;

    // 定義權重與關鍵字
    private static final List<String> STRONG_AI_KEYWORDS = Arrays.asList(
            "machine learning", "deep learning", "neural network", 
            "large language model", "generative ai", "computer vision", 
            "natural language processing", "algorithm"
    );

    private static final List<String> WEAK_AI_KEYWORDS = Arrays.asList(
            "ai", "gpt", "bot"
    );

    private static final List<String> CONTENT_KEYWORDS = Arrays.asList(
            "news", "report", "study", "research", "analysis", 
            "introduction", "guide", "review", "impact", "application",
            "reveals", "discovered", "hidden", "unusual detail", "masterpiece"
    );

    public WebPage(String url, String userKeyword) {
        this.url = url;
        this.content = HTMLFetcher.fetch(url);
        
        String text = (content == null) ? "" : content.toLowerCase();
        String safeUserKeyword = (userKeyword == null) ? "" : userKeyword.toLowerCase();

        // --- 1. 計算分數 ---
        double strongAiScore = countKeywords(text, STRONG_AI_KEYWORDS) * 5.0; 
        
        double rawWeakAiScore = countKeywords(text, WEAK_AI_KEYWORDS) * 1.0;
        double weakAiScore = Math.min(rawWeakAiScore, 30.0); // 設定上限

        double contentScore  = countKeywords(text, CONTENT_KEYWORDS) * 12.0; 
        double userKeyScore  = countKeyword(text, safeUserKeyword) * 30.0;

        this.score = strongAiScore + weakAiScore + contentScore + userKeyScore;
        
        // --- 2. 為了 SearchResult 簡單統計 ---
        this.aiKeywordCount = (int)(strongAiScore / 5.0 + rawWeakAiScore); 
        this.userKeywordCount = (int)(userKeyScore / 30.0);
    }

    // ★ 新增功能：取得關鍵字詳細統計 (供終端機列印用)
    public String getKeywordStats(String userKeyword) {
        StringBuilder sb = new StringBuilder();
        String text = (content == null) ? "" : content.toLowerCase();
        
        // 統計 User Keyword
        if (userKeyword != null && !userKeyword.isEmpty()) {
            int count = countKeyword(text, userKeyword.toLowerCase());
            if (count > 0) sb.append(userKeyword).append(":").append(count).append(" ");
        }

        // 統計 Strong Keywords
        for (String k : STRONG_AI_KEYWORDS) {
            int count = countKeyword(text, k);
            if (count > 0) sb.append(k).append(":").append(count).append(" ");
        }
        
        // 統計 Content Keywords (選幾個重要的印就好，不然太長)
        for (String k : CONTENT_KEYWORDS) {
            int count = countKeyword(text, k);
            if (count > 0) sb.append(k).append(":").append(count).append(" ");
        }

        // 統計 Weak AI (只印 ai)
        int aiCount = countKeyword(text, "ai");
        if (aiCount > 0) sb.append("ai:").append(aiCount).append(" ");

        return sb.toString().trim();
    }

    private int countKeywords(String text, List<String> keywords) {
        if (text.isEmpty()) return 0;
        int total = 0;
        for (String k : keywords) {
            total += countKeyword(text, k);
        }
        return total;
    }

    private int countKeyword(String text, String keyword) {
        if (text.isEmpty() || keyword.isEmpty()) return 0;
        int count = 0;
        int index = text.indexOf(keyword);
        while (index != -1) {
            count++;
            index = text.indexOf(keyword, index + keyword.length());
        }
        return count;
    }
}