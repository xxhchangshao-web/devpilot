package com.devpilot.ai;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;

    public AiService(@Value("${deepseek.api-key:}") String apiKey,
                     @Value("${deepseek.api-url:https://api.deepseek.com/v1/chat/completions}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.restTemplate = new RestTemplate();
    }

    public AiSuggestionResponse suggest(AiSuggestionRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            return AiSuggestionResponse.builder()
                    .investigation("AI 服务未配置（缺少 DEEPSEEK_API_KEY）")
                    .possibleCauses(List.of("请在 .env 中配置 DEEPSEEK_API_KEY"))
                    .build();
        }

        String prompt = buildPrompt(request);
        String aiResponse = callDeepSeek(prompt);
        return parseResponse(aiResponse);
    }

    private String buildPrompt(AiSuggestionRequest request) {
        String category = request.getCategory() != null ? request.getCategory() : "未知";
        return """
                你是资深 Java 后端工程师，擅长线上问题排查。
                用户描述了一个生产问题，请从排查角度给出建议。

                问题分类：%s
                问题描述：
                %s

                请严格输出 JSON（不要 markdown 代码块包裹）：
                {
                  "investigation": "排查步骤（Markdown 格式，用数字列表，每步含建议命令和说明）",
                  "possibleCauses": ["根因1", "根因2", "根因3"]
                }

                要求：
                - investigation 至少 3 步排查步骤
                - possibleCauses 至少 3 个，按可能性从高到低排列
                - 命令优先给出 jstack/jmap/jstat/arthas/kubectl 等真实可执行命令
                """.formatted(category, request.getDescription());
    }

    private String callDeepSeek(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一个严谨的技术排查助手，只输出 JSON，不输出其他内容。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.3,
                "max_tokens", 1500
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

        Map<String, Object> choice = (Map<String, Object>) ((List<?>) response.getBody().get("choices")).get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        return (String) message.get("content");
    }

    private AiSuggestionResponse parseResponse(String content) {
        try {
            String json = content.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            // 简单 JSON 解析（避免引入 jackson 类型）
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);

            String investigation = (String) map.getOrDefault("investigation", "");
            List<String> causes = (List<String>) map.getOrDefault("possibleCauses", List.of());

            return AiSuggestionResponse.builder()
                    .investigation(investigation)
                    .possibleCauses(causes)
                    .build();
        } catch (Exception e) {
            return AiSuggestionResponse.builder()
                    .investigation(content)
                    .possibleCauses(List.of())
                    .build();
        }
    }

    @Getter
    public static class DeepSeekMessage {
        private String role;
        private String content;
    }
}
