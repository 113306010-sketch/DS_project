package com.example.aiNews.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebTree {
    public WebNode root;
    private HashSet<String> visitedUrls; // 防止重複爬取相同的 URL
    private String userKeyword; // 搜尋關鍵字，傳遞給子頁面計算分數用

    public WebTree(WebPage rootPage, String userKeyword) {
        this.root = new WebNode(rootPage);
        this.userKeyword = userKeyword;
        this.visitedUrls = new HashSet<>();
        this.visitedUrls.add(rootPage.url);
    }

    // 啟動爬蟲建立樹狀結構
    // depth: 爬蟲深度 (建議 2，太深會很慢)
    public void buildTree(int depth) {
        buildTreeRecursive(root, depth, 1);
    }

    private void buildTreeRecursive(WebNode parentNode, int maxDepth, int currentDepth) {
        if (currentDepth >= maxDepth) {
            return;
        }

        // ★ 限制每個頁面最多只抓前 4 個連結，不然速度會太慢
        int linksFound = 0;
        final int MAX_LINKS_PER_PAGE = 4;

        String content = parentNode.webPage.content;
        if (content == null || content.isEmpty()) return;

        // 使用 Regex 抓取 href 連結
        Pattern p = Pattern.compile("href=\"(http[^\"]*)\"");
        Matcher m = p.matcher(content);

        while (m.find()) {
            if (linksFound >= MAX_LINKS_PER_PAGE) break;

            String childUrl = m.group(1);

            // 過濾一些非網頁的連結
            if (childUrl.endsWith(".css") || childUrl.endsWith(".js") || childUrl.endsWith(".png") || childUrl.endsWith(".jpg")) {
                continue;
            }

            // 防止重複爬取
            if (visitedUrls.contains(childUrl)) {
                continue;
            }

            // 建立子網頁 (這裡會觸發 HTMLFetcher 抓取內容並算分)
            // 注意：這裡可能會花時間，因為要連線
            WebPage childPage = new WebPage(childUrl, userKeyword);
            
            // 如果子網頁沒內容或分數為0，可能就不加入樹中 (可選)
            // if (childPage.score == 0) continue;

            visitedUrls.add(childUrl);
            WebNode childNode = new WebNode(childPage);
            parentNode.addChild(childNode);
            linksFound++;

            // 遞迴爬取下一層
            buildTreeRecursive(childNode, maxDepth, currentDepth + 1);
        }
    }

    // 計算整棵樹的總分
    public double computeTotalScore() {
        return root.computeNodeScore();
    }
}
