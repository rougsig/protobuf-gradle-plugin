package com.google.protobuf.gradle.tasks;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import java.util.List;

public interface PluginSpec extends Named {
    @Input
    String getName();

    @Internal
    PluginSpec option(final String option);

    @Input
    List<String> getOptions();

    @Input
    Property<String> getOutputSubDir();
}
