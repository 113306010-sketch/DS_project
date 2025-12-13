package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.model.WebTree;
import com.example.aiNews.service.GoogleQuery.SearchItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public List<SearchResult> rankPages(List<SearchItem> items, String userKeyword) {
        List<SearchResult> results = new ArrayList<>();

        for (SearchItem item : items) {
            String url = item.url;
            String title = (item.title != null) ? item.title : item.url;

            // ★ 檢查黑名單，若是 LinkedIn 直接跳過
            if (isBlockedSite(url)) {
                System.out.println("Blocked site ignored: " + url);
                continue;
            }

            // 建立樹與爬蟲
            WebPage rootPage = new WebPage(url, userKeyword);
            WebTree tree = new WebTree(rootPage, userKeyword);

            try {
                // 深度設為 1 (只爬當前頁)，若要爬子頁面設為 2 (會比較慢)
                tree.buildTree(1); 
            } catch (Exception e) {
                System.out.println("Tree error: " + e.getMessage());
            }

            // 計算總分
            double treeScore = tree.computeTotalScore();

            if (isNewsSite(url)) {
                treeScore += 200; 
                tree.root.nodeScore += 200; // 同步更新節點分數以利列印
            }
            rootPage.score = treeScore;

            // ★ 關鍵步驟：在終端機列印樹狀結構與關鍵字次數
            System.out.println("\n=== Tree Structure for: " + title + " ===");
            tree.eularPrintTree();
            System.out.println("========================================\n");

            // 過濾低分結果
            if (rootPage.userKeywordCount == 0 && treeScore < 50) {
                continue;
            }

            results.add(new SearchResult(
                url, 
                title, 
                rootPage.aiKeywordCount, 
                rootPage.userKeywordCount, 
                (int) treeScore
            ));
        }

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