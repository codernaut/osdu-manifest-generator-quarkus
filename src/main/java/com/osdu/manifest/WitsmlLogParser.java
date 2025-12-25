package com.osdu.manifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WitsmlLogParser {

    public WitsmlLogMetadata parse(Path xmlFile) {
        try (InputStream in = Files.newInputStream(xmlFile)) {
            final Document document = buildDocumentBuilder().parse(in);
            document.getDocumentElement().normalize();
            final String schemaVersion = resolveSchemaVersion(document);
            final Element logElement = resolveLogElement(document, xmlFile);
            return toMetadata(schemaVersion, logElement);
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            throw new ManifestGenerationException("Failed to parse WITSML file " + xmlFile + ": " + ex.getMessage(), ex);
        }
    }

    private DocumentBuilder buildDocumentBuilder() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder();
    }

    private Element resolveLogElement(Document document, Path source) {
        NodeList logNodes = document.getElementsByTagNameNS("http://www.witsml.org/schemas/1series", "log");
        if (logNodes.getLength() == 0) {
            logNodes = document.getElementsByTagName("log");
        }
        if (logNodes.getLength() == 0) {
            throw new ManifestGenerationException("No <log> element found in " + source);
        }
        final Node node = logNodes.item(0);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new ManifestGenerationException("First <log> node is not an element in " + source);
        }
        return (Element) node;
    }

    private String resolveSchemaVersion(Document document) {
        final Element root = document.getDocumentElement();
        final String version = root.getAttribute("version");
        return version == null || version.isBlank() ? null : version;
    }

    private WitsmlLogMetadata toMetadata(String schemaVersion, Element log) {
        return new WitsmlLogMetadata(
                log.getAttribute("uid"),
                log.getAttribute("uidWell"),
                log.getAttribute("uidWellbore"),
                getChildText(log, "name"),
                getChildText(log, "nameWell"),
                getChildText(log, "nameWellbore"),
                getChildText(log, "serviceCompany"),
                getChildText(log, "runNumber"),
                getChildText(log, "indexType"),
                getChildText(log, "startIndex"),
                getChildText(log, "endIndex"),
                schemaVersion,
                parseInstant(getChildText(log, "dateCreation")),
                parseInstant(getChildText(log, "dateTimeLastChange")));
    }

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagNameNS("http://www.witsml.org/schemas/1series", tagName);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(tagName);
        }
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        final String textContent = node.getTextContent();
        return textContent == null || textContent.isBlank() ? null : textContent.trim();
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
