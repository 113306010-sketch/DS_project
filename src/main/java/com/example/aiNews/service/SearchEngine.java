package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.model.WebTree;
import com.example.aiNews.service.GoogleQuery.SearchItem;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SearchEngine {

    // 1. 權威網站 (加分)
    private static final List<String> NEWS_DOMAINS = Arrays.asList(
            "theverge.com", "wired.com", "reuters.com", "bloomberg.com",
            "techcrunch.com", "cnet.com", "engadget.com", "digitaltrends.com", 
            "bbc.com", "cnn.com", "sciencealert.com"
    );

    // 2. ★ 新增：黑名單網站 (直接封鎖)
    // 這裡加入 linkedin.com 可以徹底解決你的問題
    private static final List<String> BLOCKED_DOMAINS = Arrays.asList(
            "linkedin.com", "facebook.com", "instagram.com", "twitter.com", 
            "pinterest.com", "reddit.com"
    );

    // 在 SearchEngine.java 中，替換原有的 rankPages 方法
public List<SearchResult> rankPages(List<SearchItem> items, String userKeyword) {
    // 【✨ 新增：使用執行緒池來管理並行任務，這裡設定 15 個執行緒來同時處理 15 個網頁 ✨】
    ExecutorService executor = Executors.newFixedThreadPool(15); 
    
    // 使用 CompletableFuture 來儲存非同步任務的未來結果
    List<CompletableFuture<SearchResult>> futures = new ArrayList<>();

    for (SearchItem item : items) {
        String url = item.url;
        String title = (item.title != null && !item.title.isEmpty()) ? item.title : url;

        // 檢查黑名單，如果是黑名單網站則跳過
        if (isBlockedSite(url)) {
            continue;
        }

        // 【✨ 關鍵：將 WebTree 建立和計分邏輯包裝成一個非同步任務 ✨】
        CompletableFuture<SearchResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 建立 WebPage (抓取內容)
                WebPage rootPage = new WebPage(url, userKeyword);
                
                // 2. 建立 WebTree (抓取子連結)
                WebTree tree = new WebTree(rootPage, userKeyword);
                tree.buildTree(2); // 限制深度 2

                // 3. 計算分數
                double treeScore = tree.computeTotalScore();
                
                // 4. 權威網站加分邏輯
                if (isNewsSite(url)) {
                    treeScore += 200; 
                    tree.root.nodeScore += 200; // 同步更新節點分數
                }
                rootPage.score = treeScore;
                
                // 5. 輸出樹狀結構 (保留用於報告)
                System.out.println("\n=== Tree Structure for: " + title + " ===");
                tree.eularPrintTree();
                System.out.println("========================================\n");

                // 6. 過濾低分結果
                if (tree.root.webPage.aiKeywordCount == 0) {
    // 檢查 WebPage.aiKeywordCount (Strong + Weak AI 關鍵字總和) 是否為 0
    System.out.println("Low AI relevance ignored (AI count is 0): " + url);
    return null; // 完全沒有 AI 關鍵字，直接丟棄
}

if (treeScore < 80) { // 設定一個比 50 更高的門檻，例如 80 分
    // 雖然有 AI 關鍵字，但分數仍偏低，可能只是順帶提到。
    System.out.println("Low overall score ignored (Score < 80): " + url);
    return null; 
}

                // 7. 建立最終結果物件
                return new SearchResult(
                    url, 
                    title, 
                    rootPage.aiKeywordCount, 
                    rootPage.userKeywordCount, 
                    (int) treeScore
                );
            } catch (Exception e) {
                // 處理單一網頁處理失敗，不會中斷整個搜尋
                System.err.println("Error processing URL: " + url + " -> " + e.getMessage());
                return null;
            }
        }, executor); // 指定在哪個執行緒池運行
        
        futures.add(future);
    }
    
    // 【✨ 關鍵：等待所有任務完成，並收集結果 ✨】
    List<SearchResult> results = futures.stream()
            // 處理結果，如果任務失敗或返回 null 則忽略
            .map(f -> f.join()) 
            .filter(r -> r != null) 
            .collect(Collectors.toList());
            
    // 關閉執行緒池
    executor.shutdown(); 
    
    // 排序和返回結果
    results.sort((a, b) -> Integer.compare(b.score, a.score));
    return results;
}

    private boolean isNewsSite(String url) {
        if (url == null) return false;
        for (String domain : NEWS_DOMAINS) {
            if (url.toLowerCase().contains(domain)) return true;
        }
        return false;
    }

    // ★ 黑名單檢查邏輯
    private boolean isBlockedSite(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        for (String domain : BLOCKED_DOMAINS) {
            if (lowerUrl.contains(domain)) return true;
        }
        return false;
    }
}