package com.example.aiNews.controller;

import com.example.aiNews.model.SearchResult;
import com.example.aiNews.service.GoogleQuery;
import com.example.aiNews.service.GoogleQuery.SearchItem;
import com.example.aiNews.service.SearchEngine;
import com.example.aiNews.util.KeywordExtractor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class SearchController {

    private final GoogleQuery googleQuery;
    private final SearchEngine searchEngine;

    private final Map<String, List<SearchResult>> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<SearchResult>> eldest) {
            return size() > 100; 
        }
    };

    public SearchController(GoogleQuery googleQuery, SearchEngine searchEngine) {
        this.googleQuery = googleQuery;
        this.searchEngine = searchEngine;
    }

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam String keyword) {

        if (cache.containsKey(keyword)) {
            return cache.get(keyword);
        }

        List<SearchItem> items = googleQuery.search(keyword);
        List<SearchResult> results = searchEngine.rankPages(items, keyword);

        if (!results.isEmpty()) {
            cache.put(keyword, results);
        }

        return results;
    }
    
    /**
     * Stage 4: Semantics Analysis
     * 從搜尋結果提取相關關鍵字
     */
    @GetMapping("/related-keywords")
    public Map<String, Object> getRelatedKeywords(@RequestParam String keyword) {
        System.out.println("\n🔍 === Stage 4: 提取相關關鍵字 ===");
        
        // 1. 先搜尋
        List<SearchItem> items = googleQuery.search(keyword);
        
        // 2. 收集前 3 個結果的內容
        List<String> contents = new ArrayList<>();
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            SearchItem item = items.get(i);
            String content = item.snippet != null ? item.snippet : "";
            if (!content.isEmpty()) {
                contents.add(content);
            }
        }
        
        // 3. 提取相關關鍵字
        List<String> relatedKeywords = KeywordExtractor.extractFromMultiplePages(contents, 10);
        
        System.out.println("原始關鍵字: " + keyword);
        System.out.println("相關關鍵字: " + relatedKeywords);
        System.out.println("====================\n");
        
        // 4. 回傳結果
        Map<String, Object> response = new HashMap<>();
        response.put("original_keyword", keyword);
        response.put("related_keywords", relatedKeywords);
        response.put("source_count", contents.size());
        
        return response;
    }
    
    /**
     * Stage 6: 與 LLM 比較
     * 產生比較用的 prompt
     */
    @GetMapping("/compare-llm")
    public Map<String, Object> compareWithLLM(@RequestParam String keyword) {
        System.out.println("\n🤖 === Stage 6: 準備與 LLM 比較 ===");
        
        // 1. 執行搜尋
        List<SearchItem> items = googleQuery.search(keyword);
        List<SearchResult> results = searchEngine.rankPages(items, keyword);
        
        // 2. 建立 Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("# 搜尋任務\n\n");
        prompt.append("請針對關鍵字「").append(keyword).append("」搜尋與 AI 技術相關的新聞或文章。\n\n");
        prompt.append("## 我的搜尋引擎找到的結果\n\n");
        
        // 列出前 5 個結果
        int count = Math.min(5, results.size());
        for (int i = 0; i < count; i++) {
            SearchResult r = results.get(i);
            prompt.append("### ").append(i + 1).append(". ").append(r.title).append("\n");
            prompt.append("- **URL**: ").append(r.url).append("\n");
            prompt.append("- **綜合分數**: ").append(r.score).append("\n");
            prompt.append("- **AI 關鍵字出現次數**: ").append(r.aiKeywordCount).append("\n");
            prompt.append("- **使用者關鍵字出現次數**: ").append(r.userKeywordCount).append("\n\n");
        }
        
        prompt.append("---\n\n");
        prompt.append("## 請你協助評估\n\n");
        prompt.append("1. **評估品質**: 這些結果的相關性和品質如何？\n");
        prompt.append("2. **提供建議**: 你會推薦哪些關於「").append(keyword).append(" + AI」的新聞或文章？\n");
        prompt.append("3. **比較分析**: 你推薦的內容與我的搜尋結果有何不同？\n\n");
        prompt.append("請提供 3-5 篇你認為高品質的相關文章，並說明推薦理由。");
        
        // 3. 準備回傳資料
        Map<String, Object> response = new HashMap<>();
        response.put("keyword", keyword);
        response.put("result_count", results.size());
        response.put("top_results", results.subList(0, count));
        response.put("prompt_for_chatgpt", prompt.toString());
        response.put("instructions", "複製上方 prompt 到 ChatGPT/Claude/Gemini 進行比較");
        
        System.out.println("✅ Prompt 已產生");
        System.out.println("關鍵字: " + keyword);
        System.out.println("結果數: " + results.size());
        System.out.println("Prompt 長度: " + prompt.length() + " 字元");
        System.out.println("====================\n");
        
        return response;
    }
}