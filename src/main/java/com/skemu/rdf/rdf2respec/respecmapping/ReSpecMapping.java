package com.skemu.rdf.rdf2respec.respecmapping;

import com.skemu.rdf.rdf2respec.respec.Paragraph;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReSpecMapping {

    private Map<String, String> prefixes;

    private String paragraphEntity;

    @NonNull
    private Paragraph.Type type;

    private HierarchicalRelation hierarchicalRelation;

    private String sectionTitlePredicate;

    private Map<String, String> attributeMapping = new LinkedHashMap<>();

    private IssueMapping issueMapping;
}
