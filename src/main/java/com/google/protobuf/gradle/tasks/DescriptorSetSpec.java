package com.google.protobuf.gradle.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

public interface DescriptorSetSpec {
    @Input
    Property<Boolean> getEnabled();

    @Optional
    @OutputFile
    RegularFileProperty getOutputFile();

    @Input
    Property<Boolean> getIncludeSourceInfo();

    @Input
    Property<Boolean> getIncludeImports();
}
