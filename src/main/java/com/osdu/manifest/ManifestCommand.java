package com.osdu.manifest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Option;

@TopCommand
@Command(name = "witsml-manifest",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Generate an OSDU WorkProductComponent manifest from WITSML log XML files.")
@Dependent
public class ManifestCommand implements Runnable {

    private static final Logger LOG = Logger.getLogger(ManifestCommand.class);

    @Option(names = {"-i", "--input"},
            description = "Directory containing WITSML log XML files",
            defaultValue = "samples")
    Path inputDirectory;

    @Option(names = {"-o", "--output"},
            description = "Target path for the manifest JSON file",
            defaultValue = "manifest.json")
    Path output;

    @Option(names = "--overwrite",
            description = "Overwrite the manifest file when it already exists")
    boolean overwrite;

    @Option(names = "--owners",
            description = "Comma separated list of ACL owners",
            split = ",",
            defaultValue = "data.default.owner@osdu")
    List<String> owners;

    @Option(names = "--viewers",
            description = "Comma separated list of ACL viewers",
            split = ",",
            defaultValue = "data.default.viewer@osdu")
    List<String> viewers;

    @Option(names = "--legaltags",
            description = "Comma separated list of legal tags",
            split = ",",
            defaultValue = "osdu-default-legaltag")
    List<String> legalTags;

    @Option(names = "--countries",
            description = "Comma separated list of other relevant data countries",
            split = ",",
            defaultValue = "US")
    List<String> countries;

    @Option(names = "--data-partition",
            description = "Data partition or namespace used in generated resource identifiers",
            defaultValue = "osdu")
    String dataPartition;

    @Inject
    WitsmlLogParser logParser;

    @Inject
    WorkProductComponentManifestBuilder manifestBuilder;

    @Inject
    ManifestWriter manifestWriter;

    @Override
    public void run() {
        try {
            final Path sourceDirectory = resolveInputDirectory();
            final List<Path> xmlFiles = findXmlFiles(sourceDirectory);
            final List<ParsedLog> parsedLogs = parseLogs(xmlFiles);
            final ObjectNode manifest = manifestBuilder.build(parsedLogs, owners, viewers, legalTags, countries, dataPartition);
            manifestWriter.write(output, manifest, overwrite);
            System.out.println("Manifest written to " + output.toAbsolutePath());
        } catch (ManifestGenerationException | IOException ex) {
            LOG.error("Manifest generation failed", ex);
            throw new ExecutionException(new CommandLine(this), ex.getMessage(), ex);
        }
    }

    private Path resolveInputDirectory() throws IOException {
        final Path directory = inputDirectory.toAbsolutePath();
        if (!Files.exists(directory)) {
            throw new ManifestGenerationException("Input directory does not exist: " + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new ManifestGenerationException("Input path is not a directory: " + directory);
        }
        return directory;
    }

    private List<Path> findXmlFiles(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            final List<Path> xmlFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".xml"))
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
            if (xmlFiles.isEmpty()) {
                throw new ManifestGenerationException("No XML files found in " + directory);
            }
            return xmlFiles;
        }
    }

    private List<ParsedLog> parseLogs(List<Path> files) {
        final List<ParsedLog> parsed = new ArrayList<>(files.size());
        for (Path file : files) {
            parsed.add(new ParsedLog(file, logParser.parse(file)));
        }
        return parsed;
    }
}
