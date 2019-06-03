package com.mappertests.processors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class SpecialMappings {
    private Map<String, String> fieldMappings;
    private Set<String> ignoredTargets;

    SpecialMappings() {
        fieldMappings = new HashMap<>();
        ignoredTargets = new HashSet<>();
    }

    private SpecialMappings(Map<String, String> fieldMappings, Set<String> ignoredTargets) {
        this.fieldMappings = fieldMappings;
        this.ignoredTargets = ignoredTargets;
    }

    public SpecialMappings addFieldMapping(final String source, final String target) {
        fieldMappings.put(source, target);
        return this;
    }

    public SpecialMappings ignoreTargetField(final String target) {
        ignoredTargets.add(target);
        return this;
    }
}
