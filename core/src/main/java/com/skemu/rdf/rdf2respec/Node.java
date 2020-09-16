package com.skemu.rdf.rdf2respec;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.rdf4j.model.Resource;

@NoArgsConstructor
@Getter
@Setter
public class Node {

    private Set<Node> children = new LinkedHashSet<>();
    private Set<Node> parents = new LinkedHashSet<>();
    private Resource source;

    public Node(Resource source) {
        this.source = source;
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public void addParent(Node parent) {
        parents.add(parent);
    }
}
