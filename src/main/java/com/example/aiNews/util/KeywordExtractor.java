package com.example.aiNews.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stage 4: Semantics Analysis
 * 從網頁內容提取相關關鍵字
 */
public class KeywordExtractor {
    
    // 停用詞（常見但無意義的詞）
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        // English
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
        "be", "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "can", "may", "might", "must", "shall", "should", "this", "that", "these",
        "those", "what", "which", "who", "when", "where", "why", "how", "all",
        "each", "every", "both", "few", "more", "most", "other", "some", "such",
        "than", "too", "very", "can", "just", "its", "our", "their", "about",
        // Chinese
        "的", "是", "在", "有", "和", "與", "或", "但", "了", "為", "以", "將",
        "及", "等", "等等", "如", "如果", "因為", "所以", "但是", "然而", "而且",
        "並且", "或者", "還是", "不過", "可是", "只是", "就是", "都是", "也是",
        "這個", "那個", "什麼", "怎麼", "為什麼", "哪裡", "如何", "可以", "能夠",
        "已經", "正在", "將要", "可能", "應該", "必須", "需要", "想要", "希望"
    ));
    
    /**
     * 從單一文本提取關鍵字
     * 
     * @param content 文本內容
     * @param topN 返回前 N 個關鍵字
     * @return 關鍵字列表（依頻率排序）
     */
    public static List<String> extractKeywords(String content, int topN) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, Integer> wordCount = new HashMap<>();
        
        // 轉小寫
        String text = content.toLowerCase();
        
        // 分詞：支援中英文
        String[] words = text.split("[\\s\\p{Punct}]+");
        
        for (String word : words) {
            // 移除非字母數字和中文字元
            word = word.replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "");
            
            // 過濾條件：
            // 1. 長度 > 2
            // 2. 不是停用詞
            // 3. 不是純數字
            if (word.length() > 2 
                && !STOP_WORDS.contains(word) 
                && !word.matches("\\d+")) {
                
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }
        
        // 排序並返回前 N 個
        return wordCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * 從多個文本提取綜合關鍵字
     * 
     * @param contents 文本列表
     * @param topN 返回前 N 個關鍵字
     * @return 關鍵字列表（依綜合頻率排序）
     */
    public static List<String> extractFromMultiplePages(List<String> contents, int topN) {
        if (contents == null || contents.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, Integer> combinedCount = new HashMap<>();
        
        // 從每個文本提取關鍵字，並累計計數
        for (String content : contents) {
            List<String> keywords = extractKeywords(content, 30);
            for (String keyword : keywords) {
                combinedCount.put(keyword, combinedCount.getOrDefault(keyword, 0) + 1);
            }
        }
        
        // 排序並返回前 N 個
        return combinedCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}