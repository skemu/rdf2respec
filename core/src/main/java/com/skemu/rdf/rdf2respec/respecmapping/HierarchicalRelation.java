package com.skemu.rdf.rdf2respec.respecmapping;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
public class HierarchicalRelation {
    @NonNull
    private Direction direction;

    private List<String> predicates = new ArrayList<>();
}
