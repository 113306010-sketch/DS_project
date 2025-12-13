package com.example.aiNews.service;

import com.example.aiNews.model.Keyword;
import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.model.WebTree;
import com.example.aiNews.service.GoogleQuery.SearchItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchEngine {

    // 定義 AI 相關關鍵字 (用於統計次數與加分)
    private static final List<String> AI_KEYWORDS_LIST = Arrays.asList(
            "ai", "artificial intelligence", "machine learning", "deep learning",
            "neural network", "llm", "gpt", "generative ai"
    );

    // 定義新聞網域 (用於額外加分)
    private static final List<String> NEWS_DOMAINS = Arrays.asList(
            "theverge.com", "wired.com", "reuters.com", "bloomberg.com",
            "techcrunch.com", "cnet.com", "engadget.com", "digitaltrends.com", "bbc.com"
    );

    public List<SearchResult> rankPages(List<SearchItem> items, String userKeyword) {
        List<WebPage> pages = new ArrayList<>();
        List<SearchResult> results = new ArrayList<>();

        // 1. 準備關鍵字列表與權重 (AI 關鍵字 + 使用者搜尋字)
        // 這些將傳遞給 WebTree 用於計算節點分數
        ArrayList<Keyword> keywords = new ArrayList<>();
        
        // AI 關鍵字權重設為 10
        for (String k : AI_KEYWORDS_LIST) {
            keywords.add(new Keyword(k, 10.0));
        }
        // 使用者關鍵字權重設為 30 (讓它比 AI 關鍵字更重要)
        keywords.add(new Keyword(userKeyword, 30.0));

        // 2. 處理每一個搜尋結果
        for (SearchItem item : items) {
            // [修正 1] 使用 item.url (根據你的 Log，這裡應該是 url 而不是 link)
            String url = item.url; 
            // 如果 item.title 為 null，則暫時用關鍵字代替
            String title = (item.title != null) ? item.title : userKeyword;

            // 建立根網頁與 WebTree
            WebPage rootPage = new WebPage(url, title);
            WebTree tree = new WebTree(rootPage);

            try {
                // [修正 2] 啟動爬蟲 (使用你提供的 WebTree 方法)
                // 設定爬蟲深度為 2 (代表會往下抓一層子網頁)，避免執行時間過長
                tree.startCrawlFromNode(tree.root, 2, 1);

                // [修正 3] 計算整棵樹的分數 (Post-order DFS)
                tree.setPostOrderScore(keywords);
                
            } catch (IOException e) {
                System.out.println("Processing error for: " + url + " -> " + e.getMessage());
            }

            // 額外加分邏輯：如果是權威新聞網站，直接幫根節點加分
            if (isNewsSite(url)) {
                tree.root.nodeScore += 500; 
            }

            // 將計算好的總分 (tree.root.nodeScore) 存回 rootPage，以便稍後排序
            rootPage.score = tree.root.nodeScore;
            pages.add(rootPage);
        }

        // 3. 排序：分數高的排前面
        pages.sort((a, b) -> Double.compare(b.score, a.score));

        // 4. 轉換為 SearchResult 物件 (供前端顯示)
        for (WebPage p : pages) {
            // [修正 4] 手動計算關鍵字次數
            // 因為新版 WebPage 移除了計數欄位，我們必須呼叫 counter 來計算，才能填入 SearchResult
            int aiCount = 0;
            int userCount = 0;
            
            try {
                // 累加所有 AI 關鍵字的出現次數
                for (String k : AI_KEYWORDS_LIST) {
                    aiCount += p.counter.countKeyword(k);
                }
                // 計算使用者關鍵字出現次數
                userCount = p.counter.countKeyword(userKeyword);
            } catch (IOException e) {
                // 忽略讀取錯誤，維持 0
            }

            // 過濾掉分數太低或完全不相關的結果
            if (p.score <= 0) {
                continue; 
            }

            // 建立最終結果物件
            // 注意：這裡的 score 是整棵樹的加權總分，而 counts 僅是主頁面的次數
            results.add(new SearchResult(p.url, aiCount, userCount, (int) p.score));
        }

        return results;
    }

    private boolean isNewsSite(String url) {
        for (String domain : NEWS_DOMAINS) {
            if (url.contains(domain)) {
                return true;
            }
        }
        return false;
    }
}