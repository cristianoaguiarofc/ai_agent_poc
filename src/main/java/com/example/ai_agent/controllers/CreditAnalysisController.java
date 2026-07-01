package com.example.ai_agent.controllers;

import com.example.ai_agent.models.CreditAnalysisForm;
import com.example.ai_agent.services.CreditAnalysisSessionService;
import com.example.ai_agent.useCases.CollectCreditDataUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class CreditAnalysisController {

    @Autowired
    private CollectCreditDataUseCase collectCreditDataUseCase;

    @Autowired
    private CreditAnalysisSessionService sessionService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> creditAnalyst(
            @RequestParam(value = "command") final String command,
            @RequestParam(value = "sessionId", required = false) final String sessionId
    ) {
        return this.collectCreditDataUseCase.execute(sessionId, command);
    }

    @GetMapping("/form/{sessionId}")
    public CreditAnalysisForm getForm(@PathVariable String sessionId) {
        return sessionService.getForm(sessionId);
    }
}
