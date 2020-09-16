package com.skemu.rdf.util;

import com.github.jsonldjava.shaded.com.google.common.collect.ImmutableList;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.swing.text.html.Option;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValidatingValueFactory;

public class ModelUtil {

    private static final ValueFactory VF = new ValidatingValueFactory();

    private ModelUtil() {}

    public static Set<Resource> getFirstResourcesInPropertyChain(Model model, String operator, IRI... predicates) {
        // TODO: validate operator

        List<IRI> predicateList = Arrays.asList(predicates);

        Set<Resource> resources = new HashSet<>();
        Set<Resource> subjectsWithPredicate = model.stream()
                .filter(statement -> predicateList.contains(statement.getPredicate()))
                .map(Statement::getSubject)
                .collect(Collectors.toSet());

        if (operator.equals("*")) {
            Set<Resource> subjects = model.subjects();
            subjects.removeAll(subjectsWithPredicate);

            resources.addAll(subjects);
        }

        subjectsWithPredicate.stream()
                .flatMap(subject -> getFirstResourcesInPropertyChain(model, subject, predicates).stream())
                .forEach(resources::add);

        return resources;
    }

    public static Set<Resource> getFirstResourcesInPropertyChain(Model model, Resource start, IRI... predicates) {
        // TODO: support operator?
        List<IRI> predicateList = Arrays.asList(predicates);

        Set<Resource> earlierInChain = model.stream()
                .filter(statement -> predicateList.contains(statement.getPredicate()) && start.equals(statement.getObject()))
                .map(Statement::getSubject)
                .collect(Collectors.toSet());

        if (earlierInChain.isEmpty()) {
            return Set.of(start);
        }

        return earlierInChain.stream()
                .flatMap(resource -> getFirstResourcesInPropertyChain(model, resource, predicates).stream())
                .collect(Collectors.toSet());
    }

    public static Set<Resource> getLastResourcesInPropertyChain(Model model, String operator, IRI... predicates) {
        // TODO: validate operator

        List<IRI> predicateList = Arrays.asList(predicates);

        Set<Resource> resources = new HashSet<>();
        Set<Resource> subjectsWithPredicate = model.stream()
                .filter(statement -> predicateList.contains(statement.getPredicate()))
                .map(Statement::getSubject)
                .collect(Collectors.toSet());

        if (operator.equals("*")) {
            Set<Resource> subjects = model.subjects();
            subjects.removeAll(subjectsWithPredicate);

            resources.addAll(subjects);
        }

        subjectsWithPredicate.stream()
                .flatMap(subject -> getLastResourcesInPropertyChain(model, subject, predicates).stream())
                .forEach(resources::add);

        return resources;
    }

    public static Set<Resource> getLastResourcesInPropertyChain(Model model, Resource start, IRI... predicates) {
        // TODO: support operator?
        List<IRI> predicateList = Arrays.asList(predicates);

        Set<Value> laterInChain =  model.stream()
                .filter(statement -> predicateList.contains(statement.getPredicate()) && start.equals(statement.getSubject()))
                .map(Statement::getObject)
                .collect(Collectors.toSet());

        if (laterInChain.isEmpty()) {
            return Set.of(start);
        }

        return laterInChain.stream()
                .filter(Resource.class::isInstance)
                .map(Resource.class::cast)
                .flatMap(resource -> getLastResourcesInPropertyChain(model, resource, predicates).stream())
                .collect(Collectors.toSet());
    }

    public static Optional<IRI> resolveIri(String iri, Map<String, String> namespaces) {
        if (iri == null) {
            return Optional.empty();
        }

        if (!iri.contains(":")) {
            throw new RuntimeException(String.format("Not a valid (prefixed) IRI", iri));
        }

        List<String> split = Arrays.asList(iri.split(":"));
        String prefix = split.get(0);

        if (split.size() == 2) {
            if (namespaces.containsKey(prefix)) {
                return Optional.of(VF.createIRI(String.format("%s%s", namespaces.get(prefix), split.get(1))));
            }
        }

        if (isValidIri(iri)) {
            return Optional.of(VF.createIRI(iri));
        }

        return Optional.empty();
    }

    public static boolean isValidIri(String str) {
        if (!str.contains(":")) {
            return false;
        }
        try {
            return new ParsedIRI(str).getScheme() != null;
        } catch (URISyntaxException uriException) {
            return false;
        }
    }

}
