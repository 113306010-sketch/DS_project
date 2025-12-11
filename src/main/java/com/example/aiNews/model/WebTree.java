package com.example.aiNews.model;

public class WebTree {
    public WebNode root;

    public WebTree(WebPage rootPage) {
        this.root = new WebNode(rootPage);
    }

    /**
     * 使用深度優先搜尋 (DFS) 計算整棵樹的總分數
     * 這是一個遞迴演算法
     * 
     * 時間複雜度: O(n) - n 為節點數量
     * 空間複雜度: O(h) - h 為樹的高度（遞迴堆疊深度）
     */
    public int computeScore() {
        return dfs(root);
    }

    /**
     * DFS 遞迴函數
     * 計算當前節點及其所有子節點的總分數
     */
    private int dfs(WebNode node) {
        if (node == null) return 0;
        
        // 當前節點的分數
        int sum = node.page.score;
        
        // 加上所有子節點的分數（遞迴）
        for (WebNode child : node.children) {
            sum += dfs(child);
        }
        
        // 儲存總分數
        node.totalScore = sum;
        return sum;
    }
}