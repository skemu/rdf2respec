package com.skemu.rdf.rdf2respec.respecmapping;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class IssueMapping {

    private String issuePredicate;

    private String issueIdPredicate;

    private String issueHtmlTag = "aside";

}
