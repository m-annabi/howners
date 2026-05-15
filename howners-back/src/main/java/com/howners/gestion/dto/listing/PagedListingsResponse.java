package com.howners.gestion.dto.listing;

import java.util.List;

public record PagedListingsResponse(
        List<ListingResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
