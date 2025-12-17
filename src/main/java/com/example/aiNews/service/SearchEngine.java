package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.model.WebTree;
import com.example.aiNews.service.GoogleQuery.SearchItem;
import com.example.aiNews.util.Translator; 
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SearchEngine {

    private static final List<String> NEWS_DOMAINS = Arrays.asList(
            "theverge.com", "wired.com", "reuters.com", "bloomberg.com",
            "techcrunch.com", "cnet.com", "engadget.com", "digitaltrends.com", 
            "bbc.com", "cnn.com", "sciencealert.com",
            "ithome.com.tw", "bnext.com.tw", "technews.tw", "udn.com", "cw.com.tw" 
    );

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

        String scoringKeyword = userKeyword;
        if (containsChinese(userKeyword)) {
            String translated = Translator.translate("zh-TW", "en", userKeyword);
            scoringKeyword = userKeyword + " " + translated;
        }

        for (SearchItem item : items) {
            String url = item.url;
            String title = (item.title != null) ? item.title : item.url;

            if (isBlockedSite(url)) {
                System.out.println("🚫 Blocked junk site: " + url);
                continue;
            }

            WebPage rootPage = new WebPage(url, title, item.snippet, scoringKeyword);
            WebTree tree = new WebTree(rootPage, scoringKeyword);

            try {
                // 只有內文夠長才去爬子網頁，節省時間
                if (rootPage.content != null && rootPage.content.length() > 200) {
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

            System.out.println("\n=== Tree Structure for: " + title + " ===");
            tree.eularPrintTree();
            System.out.println("========================================\n");

            // ★ 關鍵修改：大幅降低門檻
            // 原本是 < 10，現在改成 < 1。
            // 只要 Google 搜出來，且我們沒有判斷它是負分，就顯示給使用者。
            // 這樣可以避免英文新聞因為摘要太短而被誤殺。
            if (rootPage.userKeywordCount == 0 && treeScore < 1) {
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
    
    private boolean containsChinese(String text) {
        if (text == null) return false;
        return java.util.regex.Pattern.compile("[\u4e00-\u9fa5]").matcher(text).find();
    }
}