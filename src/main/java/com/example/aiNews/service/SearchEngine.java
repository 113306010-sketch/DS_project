package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.model.WebTree;
import com.example.aiNews.service.GoogleQuery.SearchItem;
import org.springframework.stereotype.Service; // ★ 記得匯入這個

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service // ★★★ 關鍵修正：加上這一行，Spring 才能找到它 ★★★
public class SearchEngine {

    // 權威新聞網站 (加分用)
    private static final List<String> NEWS_DOMAINS = Arrays.asList(
            "theverge.com", "wired.com", "reuters.com", "bloomberg.com",
            "techcrunch.com", "cnet.com", "engadget.com", "digitaltrends.com", 
            "bbc.com", "cnn.com", "sciencealert.com",
            "ithome.com.tw", "bnext.com.tw", "technews.tw", "udn.com", "cw.com.tw" 
    );

    // 垃圾網站黑名單 (直接封鎖)
    private static final List<String> BLOCKED_DOMAINS = Arrays.asList(
            "linkedin.com", "facebook.com", "instagram.com", "twitter.com", 
            "pinterest.com", "reddit.com", "threads.net", "threads.com", 
            "google.com", "google.com.tw", "maps.google.com", "calendar.google.com", 
            "youtube.com", "play.google.com", "apps.apple.com", "podcasts.apple.com", 
            "momoshop.com.tw", "pchome.com.tw", "shopee.tw", "books.com.tw", 
            "104.com.tw", "1111.com.tw", 
            "dictionary.cambridge.org", "moedict.tw"
    );

    public List<SearchResult> rankPages(List<SearchItem> items, String userKeyword) {
        List<SearchResult> results = new ArrayList<>();

        for (SearchItem item : items) {
            String url = item.url;
            String title = (item.title != null) ? item.title : item.url;

            // 1. 檢查黑名單
            if (isBlockedSite(url)) {
                System.out.println("🚫 Blocked junk site: " + url);
                continue;
            }

            WebPage rootPage = new WebPage(url, userKeyword, item.snippet);
            WebTree tree = new WebTree(rootPage, userKeyword);

            try {
                if (rootPage.content != null && rootPage.content.equals(item.snippet)) {
                    // Snippet 模式不爬子網頁
                } else {
                    tree.buildTree(2);
                }
            } catch (Exception e) {
                System.out.println("Tree error: " + e.getMessage());
            }

            double treeScore = tree.computeTotalScore();

            if (isNewsSite(url)) {
                treeScore += 200; 
                tree.root.nodeScore += 200;
            }
            rootPage.score = treeScore;

            // 印出樹狀結構 (除錯用)
            System.out.println("\n=== Tree Structure for: " + title + " ===");
            tree.eularPrintTree();
            System.out.println("========================================\n");

            // 過濾低分 (門檻設低一點，避免錯殺)
            if (rootPage.userKeywordCount == 0 || rootPage.aiKeywordCount == 0 || treeScore < 5) {
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

    private boolean isBlockedSite(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        for (String domain : BLOCKED_DOMAINS) {
            if (lowerUrl.contains(domain)) return true;
        }
        return false;
    }
}