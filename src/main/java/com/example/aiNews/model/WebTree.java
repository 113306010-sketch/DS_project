package com.example.aiNews.model;

import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebTree {
    public WebNode root;
    private HashSet<String> visitedUrls;
    private String userKeyword;

    public WebTree(WebPage rootPage, String userKeyword) {
        this.root = new WebNode(rootPage);
        this.userKeyword = userKeyword;
        this.visitedUrls = new HashSet<>();
        this.visitedUrls.add(rootPage.url);
    }

    public void buildTree(int depth) {
        buildTreeRecursive(root, depth, 1);
    }

    private void buildTreeRecursive(WebNode parentNode, int maxDepth, int currentDepth) {
        if (currentDepth >= maxDepth) return;

        int linksFound = 0;
        final int MAX_LINKS_PER_PAGE = 3; // 限制每個頁面只抓 3 個子連結 (加速)

        String content = parentNode.webPage.content;
        if (content == null || content.isEmpty()) return;

        Pattern p = Pattern.compile("href=\"(http[^\"]*)\"");
        Matcher m = p.matcher(content);

        while (m.find()) {
            if (linksFound >= MAX_LINKS_PER_PAGE) break;
            String childUrl = m.group(1);

            // 過濾非網頁資源
            if (childUrl.matches(".*\\.(css|js|png|jpg|jpeg|gif|pdf)$")) continue;
            if (visitedUrls.contains(childUrl)) continue;

            WebPage childPage = new WebPage(childUrl, userKeyword);
            visitedUrls.add(childUrl);
            WebNode childNode = new WebNode(childPage);
            parentNode.addChild(childNode);
            linksFound++;

            buildTreeRecursive(childNode, maxDepth, currentDepth + 1);
        }
    }

    public double computeTotalScore() {
        return root.computeNodeScore();
    }
    
    // ★ 新增：列印功能
    public void eularPrintTree() {
        eularPrintTree(root);
    }

    private void eularPrintTree(WebNode node) {
        if (node == null) return;
        
        // 計算縮排
        int depth = node.getDepth();
        String indent = "";
        for (int i = 0; i < depth - 1; i++) indent += "  ";
        
        // 取得關鍵字統計
        String stats = node.webPage.getKeywordStats(userKeyword);
        
        // 列印格式：(URL, 總分 [關鍵字詳情])
        System.out.print(indent + "(" + node.webPage.url + "," + String.format("%.1f", node.nodeScore));
        
        if (!stats.isEmpty()) {
            System.out.print(" [" + stats + "]");
        }
        
        if (node.children.isEmpty()) {
            System.out.println(")");
        } else {
            System.out.println(); // 換行印子節點
            for (WebNode child : node.children) {
                eularPrintTree(child);
            }
            System.out.println(indent + ")");
        }
    }
}