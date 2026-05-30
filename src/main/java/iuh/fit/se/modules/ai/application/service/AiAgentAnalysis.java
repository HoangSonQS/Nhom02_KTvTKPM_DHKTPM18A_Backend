package iuh.fit.se.modules.ai.application.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import iuh.fit.se.modules.ai.domain.AiAgentIntent;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAgentAnalysis(
        AiAgentIntent intent,
        AiAgentEntities entities,
        double confidence,
        AiAgentSource source,
        @JsonAlias({"needConfirmation", "requiresConfirmation"})
        boolean needConfirmation,
        String reason,
        List<String> missingFields
) {
    public static AiAgentAnalysis unknown(AiAgentSource source, String reason) {
        return new AiAgentAnalysis(AiAgentIntent.UNKNOWN, new AiAgentEntities(), 0.0, source, false, reason, List.of());
    }

    public AiAgentAnalysis withDefaults(AiAgentSource defaultSource) {
        AiAgentIntent normalizedIntent = intent == null ? AiAgentIntent.UNKNOWN : intent.normalized();
        AiAgentEntities safeEntities = entities == null ? new AiAgentEntities() : entities;
        return new AiAgentAnalysis(
                normalizedIntent,
                safeEntities,
                confidence,
                source == null ? defaultSource : source,
                needConfirmation || normalizedIntent.isImportantWrite(),
                reason,
                missingFields == null ? List.of() : missingFields
        );
    }
}
