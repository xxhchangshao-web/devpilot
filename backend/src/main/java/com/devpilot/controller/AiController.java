package com.devpilot.controller;

import com.devpilot.ai.AiService;
import com.devpilot.ai.AiSuggestionRequest;
import com.devpilot.ai.AiSuggestionResponse;
import com.devpilot.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/suggest")
    public ApiResponse<AiSuggestionResponse> suggest(@Valid @RequestBody AiSuggestionRequest request) {
        AiSuggestionResponse result = aiService.suggest(request);
        return ApiResponse.success(result);
    }
}
