package com.osdu.manifest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WorkProductComponentManifestBuilder {

    private static final String KIND = "osdu:wks:work-product-component:1.0.0";

    @Inject
    ObjectMapper mapper;

    public ObjectNode build(List<ParsedLog> logs,
            List<String> owners,
            List<String> viewers,
            List<String> legalTags,
            List<String> countries,
            String dataPartition) {
        if (logs == null || logs.isEmpty()) {
            throw new ManifestGenerationException("Cannot build manifest without parsed logs");
        }

        final ObjectNode manifest = mapper.createObjectNode();
        manifest.put("kind", KIND);
        manifest.put("acl", (String) null);
        manifest.set("legal", buildLegalSection(legalTags, countries));
        manifest.set("acl", buildAclSection(owners, viewers));

        final ArrayNode data = manifest.putArray("data");
        for (ParsedLog parsed : logs) {
            data.add(buildComponent(parsed, dataPartition));
        }
        return manifest;
    }

    private ObjectNode buildComponent(ParsedLog parsed, String dataPartition) {
        final ObjectNode component = mapper.createObjectNode();
        final WitsmlLogMetadata metadata = parsed.metadata();
        component.put("resourceID", metadata.resourceId(dataPartition));
        component.put("resourceType", "osdu:wks:dataset--Log:1.0.0");
        component.put("name", metadata.name());
        component.put("source", parsed.source().toString());
        component.put("schema", metadata.schemaVersion());

        final ObjectNode datasetProperties = component.putObject("datasetProperties");
        datasetProperties.put("wellUid", metadata.wellUid());
        datasetProperties.put("wellboreUid", metadata.wellboreUid());
        datasetProperties.put("wellName", metadata.wellName());
        datasetProperties.put("wellboreName", metadata.wellboreName());
        datasetProperties.put("serviceCompany", metadata.serviceCompany());
        datasetProperties.put("runNumber", metadata.runNumber());
        datasetProperties.put("indexType", metadata.indexType());
        datasetProperties.put("startIndex", metadata.startIndex());
        datasetProperties.put("endIndex", metadata.endIndex());
        datasetProperties.put("creationTime", toIso(metadata.creationTime()));
        datasetProperties.put("lastChangeTime", toIso(metadata.lastChangeTime()));
        return component;
    }

    private ObjectNode buildAclSection(List<String> owners, List<String> viewers) {
        final ObjectNode acl = mapper.createObjectNode();
        acl.set("owners", toArrayNode(owners));
        acl.set("viewers", toArrayNode(viewers));
        return acl;
    }

    private ObjectNode buildLegalSection(List<String> legalTags, List<String> countries) {
        final ObjectNode legal = mapper.createObjectNode();
        legal.set("legaltags", toArrayNode(legalTags));
        legal.set("otherRelevantDataCountries", toArrayNode(countries));
        return legal;
    }

    private ArrayNode toArrayNode(List<String> values) {
        final ArrayNode array = mapper.createArrayNode();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    array.add(value.trim());
                }
            }
        }
        return array;
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
