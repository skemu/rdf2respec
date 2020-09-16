package com.skemu.rdf.rdf2respec.sources;

import com.skemu.rdf.rdf2respec.respecmapping.ReSpecMapping;
import java.util.List;
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
public class Source {

    @NonNull
    private String inputLocation;

    private List<String> ignorePathsWith;

    @NonNull
    private String outputLocation;

    @NonNull
    private ReSpecMapping reSpecMapping;
}
