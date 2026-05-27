package com.ainions.nion.orchestration.model;

import com.ainions.nion.domain.enums.MessageType;
import java.util.List;

public record L1Plan(
        String intent,
        MessageType messageType,
        List<String> gaps,
        List<PlannedTaskModel> tasks
) {
}
