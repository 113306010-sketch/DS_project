package com.example.aiNews.service;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.model.WebPage;
import com.example.aiNews.model.WebNode;
import com.example.aiNews.model.WebTree;
import com.example.aiNews.service.GoogleQuery.SearchItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchEngine {

    private static final List<String> NEWS_DOMAINS = Arrays.asList(
            "theverge.com",
            "wired.com",
            "reuters.com",
            "bloomberg.com",
            "techcrunch.com",
            "cnet.com",
            "engadget.com",
            "digitaltrends.com",
            "bbc.com"
    );

    /**
     * 對搜尋結果進行排序和過濾
     * 使用樹狀結構組織搜尋結果的階層關係
     * 
     * 時間複雜度: O(n log n) - 主要來自排序
     * 空間複雜度: O(n) - 儲存所有網頁節點
     */
    public List<SearchResult> rankPages(List<SearchItem> items, String userKeyword) {
        long startTime = System.currentTimeMillis();
        
        // 建立搜尋樹的根節點
        WebPage rootPage = new WebPage("https://search.root", userKeyword);
        rootPage.score = 0;
        WebTree searchTree = new WebTree(rootPage);
        
        System.out.println("\n🌲 === 建立搜尋樹 ===");
        System.out.println("根節點: 搜尋關鍵字 '" + userKeyword + "'");
        
        // 將每個搜尋結果作為子節點加入樹
        List<WebPageWithTitle> pages = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            SearchItem item = items.get(i);
            WebPage page = new WebPage(item.url, userKeyword);
            
            // 計算分數
            int score = page.aiKeywordCount * 5 + page.userKeywordCount * 30;
            if (isNewsSite(item.url)) {
                score += 30;
            }
            page.score = score;
            
            // 將網頁加入樹狀結構
            WebNode childNode = new WebNode(page);
            searchTree.root.children.add(childNode);
            
            pages.add(new WebPageWithTitle(page, item.title));
            
            if (item.title.length() > 50) {
                System.out.println("  ├─ 子節點 " + (i + 1) + ": " + item.title.substring(0, 50) + "...");
            } else {
                System.out.println("  ├─ 子節點 " + (i + 1) + ": " + item.title);
            }
        }

        // 使用 DFS 計算樹的總分數
        int totalScore = searchTree.computeScore();
        
        // 計算樹的統計資訊
        int treeDepth = getTreeDepth(searchTree.root);
        int leafCount = searchTree.root.children.size();
        
        System.out.println("\n📊 === 搜尋樹統計 ===");
        System.out.println("樹的總分數: " + totalScore + " (使用 DFS 遞迴計算)");
        System.out.println("樹的深度: " + treeDepth);
        System.out.println("子節點數量: " + leafCount);
        if (leafCount > 0) {
            System.out.println("平均每個節點分數: " + (totalScore / leafCount));
        }

        // 排序
        System.out.println("\n🔢 === 排序演算法 ===");
        System.out.println("使用 TimSort 排序 " + pages.size() + " 個結果");
        System.out.println("時間複雜度: O(n log n)");
        
        pages.sort((a, b) -> Integer.compare(b.page.score, a.page.score));

        // 過濾和建立結果
        List<SearchResult> results = new ArrayList<>();
        int filteredCount = 0;
        
        for (WebPageWithTitle p : pages) {
            if (p.page.userKeywordCount < 3) {
                filteredCount++;
                continue;
            }
            results.add(new SearchResult(
                p.page.url, 
                p.title, 
                p.page.aiKeywordCount, 
                p.page.userKeywordCount, 
                p.page.score
            ));
        }
        
        // 效能分析
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\n⏱️  === 效能分析 ===");
        System.out.println("總執行時間: " + duration + " ms");
        System.out.println("處理網頁數: " + items.size());
        System.out.println("過濾掉的結果: " + filteredCount);
        System.out.println("最終結果數: " + results.size());
        if (items.size() > 0) {
            System.out.println("平均每頁處理時間: " + (duration / items.size()) + " ms");
        }
        System.out.println("================\n");
        
        return results;
    }

    /**
     * 計算樹的深度（高度）
     * 使用遞迴演算法（DFS）
     * 
     * 時間複雜度: O(n) - 遍歷所有節點
     * 空間複雜度: O(h) - h 為樹的高度（遞迴堆疊）
     */
    private int getTreeDepth(WebNode node) {
        if (node == null || node.children.isEmpty()) {
            return 1;
        }
        
        int maxDepth = 0;
        for (WebNode child : node.children) {
            int childDepth = getTreeDepth(child);
            maxDepth = Math.max(maxDepth, childDepth);
        }
        
        return maxDepth + 1;
    }

    /**
     * 檢查 URL 是否來自知名新聞網站
     * 時間複雜度: O(k) - k 為新聞網站數量
     */
    private boolean isNewsSite(String url) {
        for (String domain : NEWS_DOMAINS) {
            if (url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 內部類別：儲存網頁和標題的配對
     */
    private static class WebPageWithTitle {
        WebPage page;
        String title;
        
        WebPageWithTitle(WebPage page, String title) {
            this.page = page;
            this.title = title;
        }
    }
}