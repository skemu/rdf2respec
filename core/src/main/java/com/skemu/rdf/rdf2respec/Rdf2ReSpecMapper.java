package com.skemu.rdf.rdf2respec;

import com.skemu.rdf.rdf2respec.respecmapping.ReSpecMapping;
import com.skemu.rdf.util.ModelUtil;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.steppschuh.markdowngenerator.Markdown;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Rdf2ReSpecMapper {

    private static SimpleValueFactory VF = SimpleValueFactory.getInstance();

    private ReSpecMapping reSpecMapping;

    private IRI sectionTitlePredicate;

    private Map<IRI, String> attributeMappings;

    private IRI issuePredicate;

    private IRI issueIdPredicate;

    public static Rdf2ReSpecMapper from(ReSpecMapping reSpecMapping) {
        IRI sectionTitlePredicate = resolveIriWithPrefixes(reSpecMapping.getPrefixes(), reSpecMapping.getSectionTitlePredicate());
        Map<IRI, String> attributeMappings = prepareAttributeMappings(reSpecMapping);
        IRI issuePredicate = resolveIriWithPrefixes(reSpecMapping.getPrefixes(), reSpecMapping.getIssueMapping().getIssuePredicate());
        IRI issueIdPredicate = resolveIriWithPrefixes(reSpecMapping.getPrefixes(), reSpecMapping.getIssueMapping().getIssueIdPredicate());
        return new Rdf2ReSpecMapper(reSpecMapping, sectionTitlePredicate, attributeMappings, issuePredicate, issueIdPredicate);
    }

    private static IRI resolveIriWithPrefixes(Map<String, String> prefixes, String toResolve) {
        return ModelUtil.resolveIri(toResolve, prefixes)
                .orElseThrow(() -> new Rdf2ReSpecException(String.format("Could not resolve a valid section predicate IRI for %s",
                        toResolve)));
    }

    private static Map<IRI, String> prepareAttributeMappings(ReSpecMapping reSpecMapping) {
        Map<String, String> attributeMapping = reSpecMapping.getAttributeMapping();

        if (attributeMapping == null || attributeMapping.isEmpty()) {
            return new HashMap<>();
        }

        return reSpecMapping.getAttributeMapping()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> ModelUtil.resolveIri(
                        entry.getValue(), reSpecMapping.getPrefixes())
                            .orElseThrow(() -> new Rdf2ReSpecException(
                                    String.format("Could not resolve a valid section predicate IRI for %s", entry.getValue()))),
                        Map.Entry::getKey,
                        (entry, other) -> entry,
                        LinkedHashMap::new));
    }

    public String mapAsSection(Model rdf, Resource resource, int sectionLevel) {
        StringBuilder out = new StringBuilder();

        processSectionHeading(out, rdf, resource, sectionLevel);
        processAttributes(out, rdf, resource);
        processIssues(out, rdf, resource);

        return out.toString();
    }

    private void processSectionHeading(StringBuilder out, Model rdf, Resource resource, int sectionLevel) {
        String sectionName = rdf.filter(resource, sectionTitlePredicate, null)
                .objects()
                .stream()
                .map(Value::stringValue)
                .findFirst()
                .orElseGet(() -> resource instanceof IRI ? ((IRI) resource).getLocalName() : resource.toString());

        Heading heading = Markdown.heading(sectionName, sectionLevel);
        heading.setUnderlineStyle(false);
        out.append(heading);
    }

    private void processAttributes(StringBuilder out, Model rdf, Resource resource) {
        if (attributeMappings.isEmpty()) {
            rdf.filter(resource, null, null).predicates()
                    .forEach(predicate -> processAttribute(out, rdf, resource,
                            new AbstractMap.SimpleEntry<>(predicate, predicate.getLocalName())));
        } else {
            attributeMappings.entrySet()
                    .forEach(entry -> processAttribute(out, rdf, resource, entry));
        }
    }

    private void processAttribute(StringBuilder out, Model rdf, Resource resource, Map.Entry<IRI, String> attributeMapping) {
        String value = rdf.filter(resource, attributeMapping.getKey(), null)
                .objects()
                .stream()
                .map(val -> processValueToLinkOrString(rdf, val))
                .collect(Collectors.joining(String.format(";%n")));


        out.append(String.format("%n"))
                .append(Markdown.bold(attributeMapping.getValue()))
                .append(": ")
                .append(Markdown.text(value))
                .append(String.format("%n"));
    }

    private String processValueToLinkOrString(Model rdf, Value value) {
        if (value instanceof Resource) {
            Resource target = (Resource) value;

            Optional<String> sectionTitle = rdf.filter(target, sectionTitlePredicate, null)
                    .objects()
                    .stream()
                    .map(Value::stringValue)
                    .findFirst();

            if (sectionTitle.isPresent()) {
                String title = sectionTitle.get();
                return String.format("[%s](#%s)", title, getSectionAnchor(title));
            }
        }

        return value.stringValue();
    }

    private void processIssues(StringBuilder out, Model rdf, Resource resource) {
        Set<Value> issues = rdf.filter(resource, issuePredicate, null)
                .objects();

        rdf.stream()
                .filter(statement -> issues.contains(statement.getSubject()) && issueIdPredicate.equals(statement.getPredicate()))
                .map(Statement::getObject)
                .map(Value::stringValue)
                .forEach(issueId -> processIssue(out, issueId));
    }

    private void processIssue(StringBuilder out, String issueId) {
        String tag = reSpecMapping.getIssueMapping().getIssueHtmlTag();
        out.append(String.format("%n<%s class=\"issue\" data-number=\"%s\"></%s>%n", tag, issueId, tag));
    }

    private static String getSectionAnchor(String sectionName) {
        // TODO check/improve this naive(?) implementation
        return sectionName.trim()
                .toLowerCase()
                .replaceAll("[(,'\"!@#$%^&*;:)]", "")
                .replaceAll("\\s", "-");
    }

}
