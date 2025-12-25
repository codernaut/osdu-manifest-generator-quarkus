package com.osdu.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class WorkProductComponentManifestBuilderTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WorkProductComponentManifestBuilder builder = new WorkProductComponentManifestBuilder();

    WorkProductComponentManifestBuilderTest() {
        builder.mapper = mapper;
    }

    @Test
    void shouldBuildManifestForSingleLog() {
        final ParsedLog parsed = new ParsedLog(Path.of("samples/log.xml"),
                new WitsmlLogMetadata(
                        "log-1",
                        "well-1",
                        "wellbore-1",
                        "Gamma Ray",
                        "Well Alpha",
                        "Bore A",
                        "ServiceCo",
                        "Run-07",
                        "time",
                        "2021-01-01T00:00:00Z",
                        "2021-01-02T00:00:00Z",
                        "1.4.1",
                        Instant.parse("2020-12-31T23:59:59Z"),
                        Instant.parse("2021-01-03T10:15:30Z")));

        final var manifest = builder.build(List.of(parsed),
                List.of("owner@osdu"),
                List.of("viewer@osdu"),
                List.of("legal-tag"),
                List.of("US"),
                "com.mycompany");

        assertThat(manifest.get("data")).isNotNull();
        assertThat(manifest.get("data").get(0).get("resourceID").asText())
                .isEqualTo("com.mycompany:work-product-component:log-1");
    }
}
