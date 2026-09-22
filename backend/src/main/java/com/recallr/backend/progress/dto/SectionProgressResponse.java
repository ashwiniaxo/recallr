package com.recallr.backend.progress.dto;

import java.util.List;

public record SectionProgressResponse(
        Long sectionId,
        List<ConceptProgressResponse> concepts
) {
}