package com.example.aiNews.model;

public class WebTree {
    public WebNode root;

    public WebTree(WebPage rootPage) {
        this.root = new WebNode(rootPage);
    }

    public int computeScore() {
        return dfs(root);
    }

    private int dfs(WebNode node) {
        if (node == null) return 0;
        int sum = node.page.score;
        for (WebNode child : node.children) {
            sum += dfs(child);
        }
        node.totalScore = sum;
        return sum;
    }
}
