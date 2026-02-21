package com.howners.gestion.dto.photo;

import java.util.List;
import java.util.UUID;

public record ReorderPhotosRequest(
        List<PhotoOrderItem> photos
) {
    public record PhotoOrderItem(
            UUID photoId,
            Integer displayOrder
    ) {
    }
}
