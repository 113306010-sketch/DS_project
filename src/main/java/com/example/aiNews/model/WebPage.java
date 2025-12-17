package com.example.aiNews.model;

import com.example.aiNews.util.HTMLFetcher;
import java.util.Arrays;
import java.util.List;

public class WebPage {

    public String url;
    public String title;
    public String content;
    public int aiKeywordCount;
    public int userKeywordCount;
    public double score;

    // 關鍵字清單 (省略，保持原本的內容即可)...
    private static final List<String> STRONG_AI_KEYWORDS = Arrays.asList(
            "artificial intelligence", "generative ai", "chatgpt", "openai", 
            "large language model", "machine learning", "deep learning", "nvidia",
            "neural network", "algorithm",
            "人工智慧", "生成式", "語言模型", "機器學習", "深度學習",
            "聊天機器人", "AI技術", "AI工具", "AI應用", "AI繪圖", "AI晶片",
            "輝達", "黃仁勳", "OpenAI", "ChatGPT", "Gemini", "Copilot", "大模型"
    );

    private static final List<String> WEAK_AI_KEYWORDS = Arrays.asList(
            "ai", "gpt", "bot", "model", 
            "tech", "technology", "digital", "data", "system", "smart",
            "智慧", "智能", "機器人", "自動化", "演算法", "模型", "數位", "科技"
    );

    private static final List<String> CONTENT_KEYWORDS = Arrays.asList(
            "news", "report", "trend", "guide", "review", "impact", "launch",
            "reveals", "future", "application", "top", "best", "update", "analysis",
            "新聞", "報導", "趨勢", "懶人包", "教學", "必看", "首發", 
            "最新", "應用", "影響", "生活", "職場", "教育", "產業", 
            "話題", "熱議", "分析", "觀點", "專題", "資深記者", "採訪"
    );

    // ==========================================
    // ★ 解決錯誤的關鍵在這裡！請補上這段代碼
    // ==========================================
    
    // 1. 相容性建構子 (給 WebTree 使用)
    // 當我們只有 URL 和 關鍵字 時 (例如爬蟲爬到的子連結)，就呼叫這個
    public WebPage(String url, String userKeyword) {
        // 自動呼叫下面那個 4 個參數的建構子，並把 title 和 snippet 設為空字串
        this(url, "", "", userKeyword);
    }

    // 2. 完整建構子 (給 SearchEngine 使用)
    // 這是我們用來接收 Google 搜尋結果 (含標題、摘要) 的
    public WebPage(String url, String title, String snippet, String userKeyword) {
        this.url = url;
        this.title = title;
        this.content = HTMLFetcher.fetch(url);
        
        if (this.content == null) {
            this.content = ""; 
        }
        
        // 計分內容 = 標題 + 摘要 + 內文
        String allText = (title + " " + snippet + " " + content).toLowerCase();
        String safeUserKeyword = (userKeyword == null) ? "" : userKeyword.toLowerCase();

        // --- 分數計算邏輯 ---
        double strongAiScore = countKeywords(allText, STRONG_AI_KEYWORDS) * 5.0; 
        double rawWeakAiScore = countKeywords(allText, WEAK_AI_KEYWORDS) * 1.0;
        double weakAiScore = Math.min(rawWeakAiScore, 30.0); 
        double contentScore  = countKeywords(allText, CONTENT_KEYWORDS) * 12.0; 
        
        double userKeyScore = 0;
        int totalUserCount = 0;
        String[] keywords = safeUserKeyword.split("\\s+");
        
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

    // ... (KeywordStats 和 countKeyword 方法保持不變) ...
    public String getKeywordStats(String userKeyword) {
        StringBuilder sb = new StringBuilder();
        String text = (title + " " + content).toLowerCase(); 
        if (userKeyword != null && !userKeyword.isEmpty()) {
            String[] keywords = userKeyword.toLowerCase().split("\\s+");
            for (String kw : keywords) {
                int count = countKeyword(text, kw);
                if (count > 0) sb.append(kw).append(":").append(count).append(" ");
            }
        }
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