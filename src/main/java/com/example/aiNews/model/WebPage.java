package com.example.aiNews.model;

import com.example.aiNews.util.HTMLFetcher;
import java.util.Arrays;
import java.util.List;

/**
 * ## 🌐 網頁模型 (WebPage Model)
 *
 * 儲存單一網頁的資訊，包括 URL、內容、關鍵字計數和分數。
 * 負責從 URL 抓取內容，並根據預設和使用者提供的關鍵字計算網頁分數。
 */
public class WebPage {

    // =========================================================================
    // 屬性 (Instance Variables)
    // =========================================================================

    /** 網頁的 URL */
    public String url;
    /** 網頁的純文字內容 (已移除 HTML 標籤) */
    public String content;
    /** AI 相關關鍵字 (Strong + Weak) 的總計數 (僅供顯示用) */
    public int aiKeywordCount;
    /** 使用者指定關鍵字的計數 (僅供顯示用) */
    public int userKeywordCount;
    /** 根據各種關鍵字權重計算的最終分數 */
    public double score;

    // =========================================================================
    // 靜態常數 (Static Final Constants) - 關鍵字定義與權重
    // =========================================================================

    /** 權重較高的 AI 相關關鍵字 */
    private static final List<String> STRONG_AI_KEYWORDS = Arrays.asList(
            "machine learning", "deep learning", "neural network",
            "large language model", "generative ai", "computer vision",
            "natural language processing", "algorithm"
    );

    /** 權重較低的 AI 相關關鍵字 */
    private static final List<String> WEAK_AI_KEYWORDS = Arrays.asList(
            "ai", "gpt", "bot"
    );

    /** 內容類型或品質關鍵字 */
    private static final List<String> CONTENT_KEYWORDS = Arrays.asList(
            "news", "report", "study", "research", "analysis",
            "introduction", "guide", "review", "impact", "application",
            "reveals", "discovered", "hidden", "unusual detail", "masterpiece"
    );

    // =========================================================================
    // 建構子 (Constructor)
    // =========================================================================

    /**
     * 建構一個 WebPage 物件，執行內容抓取和分數計算。
     *
     * @param url 待處理網頁的 URL
     * @param userKeyword 使用者輸入的關鍵字 (用於加權)
     */
    public WebPage(String url, String userKeyword) {
        this.url = url;
        // 抓取網頁內容
        this.content = HTMLFetcher.fetch(url);

        String text = (content == null) ? "" : content.toLowerCase();
        String safeUserKeyword = (userKeyword == null) ? "" : userKeyword.toLowerCase();

        // 1. 執行分數計算
        calculateScore(text, safeUserKeyword);

        // 2. 為了 SearchResult 簡單統計
        // (注意: 這裡的 aiKeywordCount 統計包含未受上限限制的 rawWeakAiScore)
        double rawWeakAiScore = countKeywords(text, WEAK_AI_KEYWORDS) * 1.0;
        double strongAiScore = countKeywords(text, STRONG_AI_KEYWORDS) * 5.0;

        this.aiKeywordCount = (int)(strongAiScore / 5.0 + rawWeakAiScore);
        this.userKeywordCount = countKeyword(text, safeUserKeyword);
    }

    /**
     * 內部方法：根據網頁內容和關鍵字計算分數。
     *
     * @param text 網頁的純文字內容 (已轉為小寫)
     * @param safeUserKeyword 使用者關鍵字 (已轉為小寫)
     */
    private void calculateScore(String text, String safeUserKeyword) {
        // --- 計算各項分數 ---

        // Strong AI 關鍵字: 每出現一次 * 5.0 分
        double strongAiScore = countKeywords(text, STRONG_AI_KEYWORDS) * 5.0;

        // Weak AI 關鍵字: 每出現一次 * 1.0 分
        double rawWeakAiScore = countKeywords(text, WEAK_AI_KEYWORDS) * 1.0;
        // 設定上限，避免過度灌水 (e.g., 網頁內容全是 "ai ai ai...")
        double weakAiScore = Math.min(rawWeakAiScore, 30.0);

        // 內容/品質關鍵字: 每出現一次 * 12.0 分
        double contentScore  = countKeywords(text, CONTENT_KEYWORDS) * 12.0;

        // 使用者關鍵字: 每出現一次 * 30.0 分 (給予最高權重)
        double userKeyScore  = countKeyword(text, safeUserKeyword) * 30.0;

        // 最終分數 = 各項分數加總
        this.score = strongAiScore + weakAiScore + contentScore + userKeyScore;
    }

    // =========================================================================
    // 公用方法 (Public Methods)
    // =========================================================================

    /**
     * ★ 新增功能：取得關鍵字詳細統計字串 (供終端機列印用)
     *
     * @param userKeyword 使用者輸入的關鍵字
     * @return 關鍵字及其計數的字串，例如 "machine learning:3 news:1 ai:10"
     */
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

        // 統計 Weak AI (只印 ai，作為代表)
        int aiCount = countKeyword(text, "ai");
        if (aiCount > 0) sb.append("ai:").append(aiCount).append(" ");

        return sb.toString().trim();
    }

    // =========================================================================
    // 核心私有方法 (Private Utility Methods)
    // =========================================================================

    /**
     * 計算網頁內容中，指定關鍵字列表出現的總次數。
     *
     * @param text 網頁內容 (已轉為小寫)
     * @param keywords 關鍵字列表
     * @return 關鍵字出現的總次數
     */
    private int countKeywords(String text, List<String> keywords) {
        if (text.isEmpty()) return 0;
        int total = 0;
        for (String k : keywords) {
            total += countKeyword(text, k);
        }
        return total;
    }

    /**
     * 計算網頁內容中，單一關鍵字出現的次數。
     *
     * @param text 網頁內容 (已轉為小寫)
     * @param keyword 要計數的單一關鍵字 (已轉為小寫)
     * @return 關鍵字出現的次數
     */
    private int countKeyword(String text, String keyword) {
        if (text.isEmpty() || keyword.isEmpty()) return 0;
        int count = 0;
        int index = text.indexOf(keyword);
        // 使用 indexOf 循環尋找，效率較高
        while (index != -1) {
            count++;
            // 從上一個關鍵字出現的位置之後繼續尋找
            index = text.indexOf(keyword, index + keyword.length());
        }
        return count;
    }
}
