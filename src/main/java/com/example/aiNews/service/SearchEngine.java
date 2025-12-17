package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.model.WebTree;
import com.example.aiNews.service.GoogleQuery.SearchItem;
import com.example.aiNews.util.Translator; // ★ 匯入翻譯工具
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

        // ★ 準備計分用的關鍵字字串 (中文 + 英文)
        String scoringKeyword = userKeyword;
        if (containsChinese(userKeyword)) {
            String translated = Translator.translate("zh-TW", "en", userKeyword);
            // 組合： "狗狗 dog"
            scoringKeyword = userKeyword + " " + translated;
        }

        for (SearchItem item : items) {
            String url = item.url;
            String title = (item.title != null) ? item.title : item.url;

            if (isBlockedSite(url)) {
                System.out.println("🚫 Blocked junk site: " + url);
                continue;
            }

            // ★ 關鍵修改：使用 scoringKeyword (雙語) 並傳入 title, snippet
            WebPage rootPage = new WebPage(url, title, item.snippet, scoringKeyword);
            WebTree tree = new WebTree(rootPage, scoringKeyword);

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

            System.out.println("\n=== Tree Structure for: " + title + " ===");
            tree.eularPrintTree();
            System.out.println("========================================\n");

            // 過濾低分 (門檻 10 分，避免錯殺標題相關但內文抓不到的)
           if (rootPage.userKeywordCount == 0 && treeScore < 10) {
        // 只有在「沒提到使用者關鍵字」且「分數很低」時才過濾
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
    
    // 補上判斷中文的方法
    private boolean containsChinese(String text) {
        if (text == null) return false;
        return java.util.regex.Pattern.compile("[\u4e00-\u9fa5]").matcher(text).find();
    }
}