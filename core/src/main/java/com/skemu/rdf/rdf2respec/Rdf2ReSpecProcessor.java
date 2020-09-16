package com.skemu.rdf.rdf2respec;

import com.skemu.rdf.rdf2respec.respec.Paragraph;
import com.skemu.rdf.rdf2respec.respecmapping.Direction;
import com.skemu.rdf.rdf2respec.respecmapping.HierarchicalRelation;
import com.skemu.rdf.rdf2respec.respecmapping.ReSpecMapping;
import com.skemu.rdf.rdf2respec.sources.Source;
import com.skemu.rdf.rdf2respec.sources.SourceException;
import com.skemu.rdf.util.ModelUtil;
import com.skemu.rdf.util.RdfIoUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;

public class Rdf2ReSpecProcessor {

    private static final ValueFactory VF = SimpleValueFactory.getInstance();

    private Source source;

    private Rdf2ReSpecMapper mapper;

    private Rdf2ReSpecProcessor(Source source, Rdf2ReSpecMapper mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    public static Rdf2ReSpecProcessor of(Source source) {
        return new Rdf2ReSpecProcessor(source, Rdf2ReSpecMapper.from(source.getReSpecMapping()));
    }

    public String serialize() {
        // read everything from location
        List<Path> input = getPaths(source.getInputLocation(), source.getIgnorePathsWith());
        Model rdf = input.stream()
                .flatMap(path -> RdfIoUtil.read(path.toFile(), RDFFormat.TURTLE).stream())
                .collect(Collectors.toCollection(LinkedHashModel::new));

        return rdf2ReSpecMarkdown(rdf);
    }

    public void serializeToFileSystem() {
        List<Path> input = getPaths(source.getInputLocation(), source.getIgnorePathsWith());
        for (Path path : input) {
            Model rdf = RdfIoUtil.read(path.toFile(), RDFFormat.TURTLE);
            String fileNameWithoutExt = FilenameUtils.removeExtension(path.getFileName().toString());
            Path outputPath = Paths.get(source.getOutputLocation()).resolve(Paths.get(String.format("%s.md", fileNameWithoutExt)));

            rdf2ReSpecMarkdownFile(rdf, outputPath);
        }
    }

    // TODO: make file extension filter configurable in source
    private List<Path> getPaths(String location, List<String> ignores) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(location))) {
            return pathStream.filter(Files::isRegularFile)
                    .filter(filePath -> ignores.stream().noneMatch(ignore -> filePath.toString().contains(ignore)))
                    .filter(filePath -> filePath.toString().endsWith(".ttl"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new SourceException(String.format("Cannot resolve location %s", location));
        }
    }

    private void rdf2ReSpecMarkdownFile(Model rdf, Path path) {
        try {
            Files.writeString(path, rdf2ReSpecMarkdown(rdf));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String rdf2ReSpecMarkdown(Model rdf) {
        ReSpecMapping mapping = source.getReSpecMapping();
        // get all paragraphs

        if (mapping.getType() != Paragraph.Type.hierarchical) {
            throw new IllegalStateException("Only hierarchical mapping supported.");
        }

        HierarchicalRelation rel = mapping.getHierarchicalRelation();

        List<IRI> hierarchyPredicates = rel.getPredicates()
                .stream()
                .map(predicate -> ModelUtil.resolveIri(predicate, mapping.getPrefixes())
                        .orElseThrow(() -> new Rdf2ReSpecException(String.format(
                                "Could not resolve hierarchical relation predicate IRIs %s", mapping.getPrefixes()))))
                .collect(Collectors.toList());

        List<Node> nodes = new ArrayList<>();
        if (rel.getDirection() == Direction.upward) {
            nodes = buildTreeWithUpwardRelations(rdf, hierarchyPredicates);
        } else {
            throw new RuntimeException("TODO: support downward hierarchical relations");
        }
        return generateReSpecMarkdown(rdf, nodes, 1).stream().collect(Collectors.joining(String.format("%n")));
    }

    private static List<Node> buildTreeWithUpwardRelations(Model model, List<IRI> hierarchyPredicates) {
        Map<Resource, Node> lookup = new LinkedHashMap<>();

        Set<Resource> subjects = model.stream()
                .filter(statement -> hierarchyPredicates.contains(statement.getPredicate()))
                .map(Statement::getSubject)
                .collect(Collectors.toSet());

        for (Resource subject : subjects) {

            // complete possible earlier references
            Node thiz;
            if (lookup.containsKey(subject)) {
                thiz = lookup.get(subject);
            } else {
                thiz = new Node(subject);
                lookup.put(subject, thiz);
            }

            // link into parents
            List<Resource> parents = model.stream()
                    .filter(statement -> statement.getSubject().equals(subject) && hierarchyPredicates.contains(statement.getPredicate()))
                    .map(Statement::getObject)
                    .filter(Resource.class::isInstance)
                    .map(Resource.class::cast)
                    .collect(Collectors.toList());

            for(Resource parent: parents) {
                Node parentNode;
                if (!lookup.containsKey(parent)) {
                    parentNode = new Node(parent);
                    lookup.put(parent, parentNode);
                } else {
                    parentNode = lookup.get(parent);
                }
                parentNode.addChild(thiz);
                thiz.addParent(parentNode);
            }
        }

        return lookup.values().stream()
                .collect(Collectors.toList());
    }

    private List<String> serializeAsTree(List<Node> nodes) {
        return  getRoots(nodes).stream()
                .map(node -> serializeNodeAsTree(node, "\n"))
                .collect(Collectors.toList());
    }

    private String serializeNodeAsTree(Node node, String delimiter) {
        String newDelimiter = calculateNewDelimiter(delimiter);
        String children = node.getChildren().stream()
                .map(child -> serializeNodeAsTree(child, newDelimiter))
                .collect(Collectors.joining(""));
        return String.format("%s%s%s", node.getSource(), newDelimiter, children);
    }

    private String calculateNewDelimiter(String delimiter) {
        int spaces = delimiter.substring(delimiter.lastIndexOf("\n")).length();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(delimiter);
        stringBuilder.append("\n");
        for (int i = 0; i < spaces + 2; i++){
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    private List<Node> getRoots(List<Node> nodes) {
        return nodes.stream()
                .filter(node -> node.getParents().isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> generateReSpecMarkdown(Model rdf, List<Node> nodes, int startDepth) {
        return getRoots(nodes).stream()
                .map(node -> generateReSpecMarkdown(rdf, node, startDepth))
                .collect(Collectors.toList());
    }

    private String generateReSpecMarkdown(Model rdf, Node node, int depth) {
        String children = node.getChildren().stream()
                .map(child -> generateReSpecMarkdown(rdf, child, depth + 1 ))
                .collect(Collectors.joining(String.format("%n")));
        return String.format("%s%n%s", mapper.mapAsSection(rdf, node.getSource(), depth), children);
    }

}
