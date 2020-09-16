package com.skemu.rdf.rdf2respec;

import com.skemu.rdf.rdf2respec.respec.Paragraph;
import com.skemu.rdf.rdf2respec.respecmapping.HierarchicalRelation;
import com.skemu.rdf.rdf2respec.respecmapping.ReSpecMapping;
import com.skemu.rdf.rdf2respec.sources.Source;
import com.skemu.rdf.rdf2respec.sources.Sources;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class Rdf2ReSpecApplication implements CommandLineRunner {

    public static final String SOURCES_YML = "config/sources.yml";

    private final Sources sources;

    public Rdf2ReSpecApplication() {
        this.sources = getSources();
    }

    public static void main(String... args) {
        SpringApplication.run(Rdf2ReSpecApplication.class, args).close();
    }

    @Override
    public void run(String... args) {
        for (Source source: sources.getSources()) {
//            System.out.println(Rdf2ReSpecProcessor.of(source).serialize());
            Rdf2ReSpecProcessor.of(source).serializeToFileSystem();
        }

    }

    private Sources getSources() {
        Path sourcesPath = Paths.get(SOURCES_YML)
                .toAbsolutePath();
        Sources loadedSources = YamlConfigReader.parseYamlConfig(sourcesPath, Sources.class)
                .orElseThrow(() -> new Rdf2ReSpecException("Could not parse source (config/sources.yaml) configuration"));
        return loadedSources;
    }
}
