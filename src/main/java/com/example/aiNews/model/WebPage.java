package com.example.aiNews.model;

import com.example.aiNews.util.HTMLFetcher;
import java.util.Arrays;
import java.util.List;

public class WebPage {

    public String url;
    public String title; // ★ 新增
    public String content;
    public int aiKeywordCount;
    public int userKeywordCount;
    public double score;

    // 1. 強力 AI 關鍵字 (加入生活化與熱門詞彙)
    private static final List<String> STRONG_AI_KEYWORDS = Arrays.asList(
            // English
            "artificial intelligence", "generative ai", "chatgpt", "openai", 
            "large language model", "machine learning", "deep learning", "nvidia",
            // Chinese
            "人工智慧", "生成式", "語言模型", "機器學習", "深度學習",
            "聊天機器人", "AI技術", "AI工具", "AI應用", "AI繪圖", "AI晶片",
            "輝達", "黃仁勳", "OpenAI", "ChatGPT", "Gemini", "Copilot", "大模型"
    );

    // 2. 弱 AI 關鍵字
    private static final List<String> WEAK_AI_KEYWORDS = Arrays.asList(
            "ai", "gpt", "bot", "model", 
            "智慧", "智能", "機器人", "自動化", "演算法", "模型", "數位", "科技"
    );

    // 3. 內容/品質關鍵字 (加入媒體用語)
    private static final List<String> CONTENT_KEYWORDS = Arrays.asList(
            // English
            "news", "report", "trend", "guide", "review", "impact", "launch",
            "reveals", "future", "application", "top", "best",
            // Chinese
            "新聞", "報導", "趨勢", "懶人包", "教學", "必看", "首發", 
            "最新", "應用", "影響", "生活", "職場", "教育", "產業", 
            "話題", "熱議", "分析", "觀點", "專題", "資深記者", "採訪"
    );

    // 為了相容性保留舊建構子 (可以不刪，避免報錯)
    public WebPage(String url, String userKeyword) {
        this(url, "", "", userKeyword);
    }

    // ★ 新建構子：接收 title 和 snippet
    public WebPage(String url, String title, String snippet, String userKeyword) {
        this.url = url;
        this.title = title;
        this.content = HTMLFetcher.fetch(url);
        
        if (this.content == null) {
            this.content = ""; // 避免 null
        }
        
        // ★ 關鍵：計分內容 = 標題 + 摘要 + 內文
        String allText = (title + " " + snippet + " " + content).toLowerCase();
        String safeUserKeyword = (userKeyword == null) ? "" : userKeyword.toLowerCase();

        // 1. 計算分數
        double strongAiScore = countKeywords(allText, STRONG_AI_KEYWORDS) * 5.0; 
        double rawWeakAiScore = countKeywords(allText, WEAK_AI_KEYWORDS) * 1.0;
        double weakAiScore = Math.min(rawWeakAiScore, 30.0); 
        double contentScore  = countKeywords(allText, CONTENT_KEYWORDS) * 12.0; 
        
        // ★ 支援多關鍵字計分 (例如 "狗狗 dog")
        double userKeyScore = 0;
        int totalUserCount = 0;
        String[] keywords = safeUserKeyword.split("\\s+"); // 用空白切割
        
        for (String kw : keywords) {
            if (kw.length() > 0) {
                int count = countKeyword(allText, kw);
                userKeyScore += count * 30.0;
                totalUserCount += count;
            }
        }

        this.score = strongAiScore + weakAiScore + contentScore + userKeyScore;
        
        this.aiKeywordCount = (int)(strongAiScore / 5.0 + rawWeakAiScore); 
        this.userKeywordCount = totalUserCount;
    }

    public String getKeywordStats(String userKeyword) {
        StringBuilder sb = new StringBuilder();
        // 統計也包含標題，避免數據不一致
        String text = (title + " " + content).toLowerCase(); 
        
        if (userKeyword != null && !userKeyword.isEmpty()) {
            String[] keywords = userKeyword.toLowerCase().split("\\s+");
            for (String kw : keywords) {
                int count = countKeyword(text, kw);
                if (count > 0) sb.append(kw).append(":").append(count).append(" ");
            }
        }

        // 簡化顯示，只列出 Strong Keywords
        for (String k : STRONG_AI_KEYWORDS) {
            int count = countKeyword(text, k);
            if (count > 0) sb.append(k).append(":").append(count).append(" ");
        }
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