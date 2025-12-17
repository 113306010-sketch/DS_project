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

    // 1. 強力 AI 關鍵字 (加入中文) - 權重 5.0
    private static final List<String> STRONG_AI_KEYWORDS = Arrays.asList(
            // English
            "machine learning", "deep learning", "neural network", 
            "large language model", "generative ai", "computer vision", 
            "natural language processing", "algorithm", "reinforcement learning",
            "artificial intelligence", "big model",
            // Chinese
            "機器學習", "深度學習", "神經網絡", "神經網路", 
            "大型語言模型", "生成式", "電腦幻覺", "自然語言處理", "演算法", "強化學習",
            "人工智慧", "大模型"
    );

    // 2. 弱 AI 關鍵字 (加入中文) - 權重 1.0 (有上限)
    private static final List<String> WEAK_AI_KEYWORDS = Arrays.asList(
            "ai", "gpt", "bot", "chatgpt", "openai", "gemini", "copilot",
            "模型", "智能", "智慧", "機器人"
    );

    // 3. 內容/品質關鍵字 (加入中文) - 權重 12.0
    private static final List<String> CONTENT_KEYWORDS = Arrays.asList(
            // English
            "news", "report", "study", "research", "analysis", 
            "introduction", "guide", "review", "impact", "application",
            "reveals", "discovered", "hidden", "unusual detail", "masterpiece", "overview",
            // Chinese
            "新聞", "報導", "研究", "分析", "介紹", "指南", "評論", "影響", 
            "應用", "名作", "趨勢", "技術", "觀點", "專題", "懶人包"
    );

    public WebPage(String url, String userKeyword) {
        this(url, userKeyword, "");
    }

    public WebPage(String url, String userKeyword, String snippet) {
        this.url = url;
        this.content = HTMLFetcher.fetch(url);
        
        // Snippet Fallback
        if (this.content == null || this.content.trim().isEmpty()) {
            this.content = snippet;
        }
        
        String text = (content == null) ? "" : content.toLowerCase();
        String safeUserKeyword = (userKeyword == null) ? "" : userKeyword.toLowerCase();

        // --- 分數計算 ---
        double strongAiScore = countKeywords(text, STRONG_AI_KEYWORDS) * 5.0; 
        
        double rawWeakAiScore = countKeywords(text, WEAK_AI_KEYWORDS) * 1.0;
        double weakAiScore = Math.min(rawWeakAiScore, 30.0); // 上限 30 分

        double contentScore  = countKeywords(text, CONTENT_KEYWORDS) * 12.0; 
        
        double userKeyScore  = countKeyword(text, safeUserKeyword) * 30.0;

        this.score = strongAiScore + weakAiScore + contentScore + userKeyScore;
        
        // 統計數據 (合併 Strong 和 Weak 供顯示)
        this.aiKeywordCount = (int)(strongAiScore / 5.0 + rawWeakAiScore); 
        this.userKeywordCount = (int)(userKeyScore / 30.0);
    }

    public String getKeywordStats(String userKeyword) {
        StringBuilder sb = new StringBuilder();
        String text = (content == null) ? "" : content.toLowerCase();
        
        if (userKeyword != null && !userKeyword.isEmpty()) {
            int count = countKeyword(text, userKeyword.toLowerCase());
            if (count > 0) sb.append(userKeyword).append(":").append(count).append(" ");
        }

        // 只印出有找到的 Strong Keywords
        for (String k : STRONG_AI_KEYWORDS) {
            int count = countKeyword(text, k);
            if (count > 0) sb.append(k).append(":").append(count).append(" ");
        }
        
        // 為了版面整潔，Weak AI 只顯示總數概念，不一一列出
        int aiCount = countKeyword(text, "ai") + countKeyword(text, "人工智慧");
        if (aiCount > 0) sb.append("ai/人工智慧:").append(aiCount).append(" ");

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