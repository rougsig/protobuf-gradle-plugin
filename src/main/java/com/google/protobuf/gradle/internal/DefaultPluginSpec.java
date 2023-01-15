package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.PluginSpec;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import java.util.ArrayList;
import java.util.List;

public class DefaultPluginSpec implements PluginSpec {
    private final String name;
    private final List<String> options;
    private final Property<String> outputSubDir;

    public DefaultPluginSpec(String name, ObjectFactory objects) {
        this.name = name;
        this.options = new ArrayList<>();
        this.outputSubDir = objects.property(String.class);
        this.outputSubDir.convention(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public PluginSpec option(String option) {
        options.add(option);
        return this;
    }

    @Override
    public List<String> getOptions() {
        return this.options;
    }

    @Override
    public Property<String> getOutputSubDir() {
        return this.outputSubDir;
    }
}
