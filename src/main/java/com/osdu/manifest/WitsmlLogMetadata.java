package com.osdu.manifest;

import java.time.Instant;

public record WitsmlLogMetadata(
        String uid,
        String wellUid,
        String wellboreUid,
        String name,
        String wellName,
        String wellboreName,
        String serviceCompany,
        String runNumber,
        String indexType,
        String startIndex,
        String endIndex,
        String schemaVersion,
        Instant creationTime,
        Instant lastChangeTime) {

    public String resourceId(String dataPartition) {
        final String safePartition = dataPartition == null || dataPartition.isBlank() ? "osdu" : dataPartition.trim();
        final String safeUid = uid == null || uid.isBlank() ? "unknown" : uid.trim();
        return "%s:work-product-component:%s".formatted(safePartition, safeUid);
    }
}
