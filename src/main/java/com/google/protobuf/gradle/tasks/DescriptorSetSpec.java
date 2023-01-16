package com.google.protobuf.gradle.tasks;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

public interface DescriptorSetSpec {
    /**
     * If true, will set the protoc flag
     * --descriptor_set_out="${outputBaseDir}/descriptor_set.desc"
     *
     * Default: false
     */
    @Input
    Property<Boolean> getEnabled();

    /**
     * If set, specifies an alternative location than the default for storing the descriptor
     * set.
     *
     * Default: null
     */
    @Optional
    @OutputFile
    Property<String> getOutputFilePath();

    /**
     * If true, source information (comments, locations) will be included in the descriptor set.
     *
     * Default: false
     */
    @Input
    Property<Boolean> getIncludeSourceInfo();

    /**
     * If true, imports are included in the descriptor set, such that it is self-containing.
     *
     * Default: false
     */
    @Input
    Property<Boolean> getIncludeImports();
}
