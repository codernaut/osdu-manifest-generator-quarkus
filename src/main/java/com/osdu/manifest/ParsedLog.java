package com.osdu.manifest;

import java.nio.file.Path;

public record ParsedLog(Path source, WitsmlLogMetadata metadata) {
}
