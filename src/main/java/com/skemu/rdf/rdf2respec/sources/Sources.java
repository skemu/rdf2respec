package com.skemu.rdf.rdf2respec.sources;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sources {

    private List<Source> sources = new ArrayList<>();
}
