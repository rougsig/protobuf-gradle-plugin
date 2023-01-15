package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.DescriptorSetSpec;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class DefaultDescriptorSetSpec implements DescriptorSetSpec {
    private final Property<Boolean> enabled;
    private final RegularFileProperty outputFile;
    private final Property<Boolean> includeSourceInfo;
    private final Property<Boolean> includeImports;

    DefaultDescriptorSetSpec(ObjectFactory objects) {
        this.enabled = objects.property(Boolean.class);
        this.enabled.convention(false);

        this.outputFile = objects.fileProperty();

        this.includeSourceInfo = objects.property(Boolean.class);
        this.includeSourceInfo.convention(false);

        this.includeImports = objects.property(Boolean.class);
        this.includeImports.convention(false);
    }

    @Override
    public Property<Boolean> getEnabled() {
        return this.enabled;
    }

    @Override
    public RegularFileProperty getOutputFile() {
        return this.outputFile;
    }

    @Override
    public Property<Boolean> getIncludeSourceInfo() {
        return this.includeSourceInfo;
    }

    @Override
    public Property<Boolean> getIncludeImports() {
        return this.includeImports;
    }
}
