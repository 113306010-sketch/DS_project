package com.example.aiNews.service;

import com.example.aiNews.util.Translator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleQuery {

    // ✅ 地端/備援：保留你原本 application.properties 的設定
    @Value("${google.cse.apiKey}")
    private String apiKey;

    @Value("${google.cse.cx}")
    private String cx;

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ 輪換用（每次 search 會輪到下一組）
    private static final AtomicInteger ROTATE_INDEX = new AtomicInteger(0);

    public static class SearchItem {
        public String url;
        public String title;
        public String snippet;

        public SearchItem(String url, String title, String snippet) {
            this.url = url;
            this.title = title;
            this.snippet = snippet;
        }

        @Override
        public String toString() {
            return "SearchItem{url='" + url + "', title='" + title + "'}";
        }
    }

    // 讀取環境變數：GOOGLE_API_KEY_1..N / GOOGLE_CX_1..N
    private List<String> loadEnvList(String prefix) {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= 30; i++) { // 你要更多就加大
            String v = System.getenv(prefix + i);
            if (v == null || v.isBlank()) break;
            list.add(v.trim());
        }
        return list;
    }

    private String pickApiKey(List<String> envKeys, int idx) {
        if (!envKeys.isEmpty()) {
            return envKeys.get(Math.floorMod(idx, envKeys.size()));
        }
        // 沒有設 env，就用你原本 properties
        return apiKey;
    }

    private String pickCx(List<String> envCxs, int idx) {
        if (!envCxs.isEmpty()) {
            return envCxs.get(Math.floorMod(idx, envCxs.size()));
        }
        return cx;
    }

    public List<SearchItem> search(String userKeyword) {
        try {
            // ✅ 每次搜尋取一個輪換 index
            int idx = ROTATE_INDEX.getAndIncrement();

            // ✅ 每次呼叫時讀取 env（Render 改 env 後重啟就會生效）
            List<String> envKeys = loadEnvList("GOOGLE_API_KEY_");
            List<String> envCxs  = loadEnvList("GOOGLE_CX_");

            String keyToUse = pickApiKey(envKeys, idx);
            String cxToUse  = pickCx(envCxs, idx);

            StringBuilder urlBuilder = new StringBuilder("https://www.googleapis.com/customsearch/v1");
            urlBuilder.append("?key=").append(keyToUse);
            urlBuilder.append("&cx=").append(cxToUse);
            urlBuilder.append("&num=10");

            String q;

            if (containsChinese(userKeyword)) {
                System.out.println("✅ Detected Chinese input. Applying Hybrid Search.");
                String translatedKeyword = Translator.translate("zh-TW", "en", userKeyword);
                String expandedKeyword = translatedKeyword + " AI technology news";
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                urlBuilder.append("&q=").append(q);
                urlBuilder.append("&gl=tw");
            } else {
                System.out.println("✅ Detected English/Global input.");
                String expandedKeyword = userKeyword + " AI technology news";
                q = URLEncoder.encode(expandedKeyword, StandardCharsets.UTF_8);
                urlBuilder.append("&q=").append(q);
            }

            String url = urlBuilder.toString();

            System.out.println("\n=== Google Search Request ===");
            // ✅ mask：避免 log 露出真正 key（如果 keyToUse 來自 properties，也一起 mask）
            System.out.println("Query URL (masked): " + url.replace(keyToUse, "***"));
            if (!envKeys.isEmpty() || !envCxs.isEmpty()) {
                System.out.println("Using env rotation index=" + idx +
                        " (envKeys=" + envKeys.size() + ", envCxs=" + envCxs.size() + ")");
            } else {
                System.out.println("Using properties fallback (no env GOOGLE_API_KEY_*/GOOGLE_CX_*)");
            }

            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = resp.getBody();
            List<SearchItem> items = new ArrayList<>();

            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> resultItems = (List<Map<String, Object>>) body.get("items");

                for (Map<String, Object> item : resultItems) {
                    String link = (String) item.get("link");
                    String title = (String) item.get("title");
                    String snippet = (String) item.get("snippet");

                    if (link != null && link.matches(".*\\.(pdf|xml|csv|xls|xlsx|doc|docx|ppt|pptx|zip|rar|gz|mht)$")) {
                        continue;
                    }

                    items.add(new SearchItem(link, title, snippet));
                }
            }

            return items;

        } catch (Exception e) {
            System.err.println("GoogleQuery Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private boolean containsChinese(String text) {
        if (text == null) return false;
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(text);
        return m.find();
    }
}
