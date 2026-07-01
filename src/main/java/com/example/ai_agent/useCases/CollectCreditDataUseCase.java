package com.example.ai_agent.useCases;

import com.example.ai_agent.models.CreditAnalysisForm;
import com.example.ai_agent.services.CreditAnalysisSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CollectCreditDataUseCase {

    private static final String SYSTEM_PROMPT = """
            Você é um assistente de análise de crédito. Sua missão é coletar as seguintes informações do usuário, uma de cada vez, de forma amigável e conversacional:

            1. Valor total solicitado (em reais)
            2. Prazo em meses
            3. Renda mensal (em reais)

            Regras:
            - Colete apenas uma informação por vez.
            - Após receber uma informação, confirme o valor recebido e solicite o próximo campo em falta.
            - Quando todas as informações estiverem coletadas, apresente um resumo e informe que a solicitação está completa.
            - Aceite valores escritos de diversas formas (ex: "5 mil", "R$ 5.000", "5000").
            - Se o usuário informar um valor inválido, peça para repetir de forma clara.
            - Seja sempre educado e objetivo.
            """;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private CreditAnalysisSessionService sessionService;

    public Flux<String> execute(
            final String sessionId,
            final String command
    ) {
        this.sessionService.initSession(sessionId);

        return this.chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(command)
                .advisors(spec -> spec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .doOnComplete(() -> extractAndSaveFormData(sessionId, command));
    }

    private void extractAndSaveFormData(final String sessionId, final String userMessage) {
        CreditAnalysisForm form = sessionService.getForm(sessionId);
        String lower = userMessage.toLowerCase();

        BigDecimal parsed = parseAmount(userMessage);
        if (parsed == null) return;

        if (form.totalAmount() == null && containsAmountHint(lower)) {
            form = form.withTotalAmount(parsed);
        } else if (form.termMonths() == null && containsTermHint(lower)) {
            Integer months = parseMonths(userMessage);
            if (months != null) form = form.withTermMonths(months);
        } else if (form.monthlyIncome() == null && containsIncomeHint(lower)) {
            form = form.withMonthlyIncome(parsed);
        } else if (form.totalAmount() == null) {
            form = form.withTotalAmount(parsed);
        } else if (form.termMonths() == null) {
            Integer months = parseMonths(userMessage);
            if (months != null) form = form.withTermMonths(months);
        } else if (form.monthlyIncome() == null) {
            form = form.withMonthlyIncome(parsed);
        }

        sessionService.saveForm(sessionId, form);
    }

    private boolean containsAmountHint(String text) {
        return text.contains("valor") || text.contains("total") || text.contains("solicito") || text.contains("quero");
    }

    private boolean containsTermHint(String text) {
        return text.contains("meses") || text.contains("prazo") || text.contains("mes") || text.contains("parcela");
    }

    private boolean containsIncomeHint(String text) {
        return text.contains("renda") || text.contains("ganho") || text.contains("salário") || text.contains("salario") || text.contains("recebo");
    }

    private BigDecimal parseAmount(String text) {
        String cleaned = text.replaceAll("[Rr]\\$", "").trim();
        boolean isThousands = cleaned.toLowerCase().contains("mil") || cleaned.toLowerCase().contains("k");
        cleaned = cleaned.replaceAll("(?i)(mil|k)", "").trim();

        Matcher matcher = Pattern.compile("[\\d.,]+").matcher(cleaned);
        if (!matcher.find()) return null;

        try {
            String number = matcher.group().replace(".", "").replace(",", ".");
            BigDecimal value = new BigDecimal(number);
            return isThousands ? value.multiply(BigDecimal.valueOf(1000)) : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseMonths(String text) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*(?:meses?|mês|mes)").matcher(text.toLowerCase());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        Matcher plain = Pattern.compile("\\b(\\d{1,3})\\b").matcher(text);
        if (plain.find()) {
            try {
                return Integer.parseInt(plain.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
