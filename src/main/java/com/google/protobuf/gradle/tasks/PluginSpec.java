package com.google.protobuf.gradle.tasks;

import com.google.protobuf.gradle.GenerateProtoTask;
import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import java.util.List;

/**
 * The container of command-line options for a protoc plugin or a built-in output.
 */
public interface PluginSpec extends Named {
    /**
     * Returns the name of the plugin or builtin.
     */
    @Input
    String getName();

    /**
     * Adds a plugin option.
     */
    PluginSpec option(final String option);

    @Input
    List<String> getOptions();

    /**
     * Set the output directory for this plugin,
     * relative to {@link GenerateProtoTask#getOutputBaseDir()}.
     */
    @Input
    Property<String> getOutputSubDir();
}
