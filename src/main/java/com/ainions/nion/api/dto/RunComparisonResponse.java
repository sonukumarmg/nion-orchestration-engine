package com.ainions.nion.api.dto;

import java.util.List;

public record RunComparisonResponse(
        String leftRunId,
        String rightRunId,
        List<String> differences
) {
}
