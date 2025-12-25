package com.osdu.manifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ManifestWriter {

    @Inject
    ObjectMapper mapper;

    public void write(Path output, ObjectNode manifest, boolean overwrite) throws IOException {
        final Path target = output.toAbsolutePath();
        if (Files.exists(target) && !overwrite) {
            throw new ManifestGenerationException("Manifest file already exists: " + target);
        }
        final Path directory = target.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
        final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(target.toFile(), manifest);
    }
}
