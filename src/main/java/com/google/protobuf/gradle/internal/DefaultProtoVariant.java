package com.google.protobuf.gradle.internal;

import com.google.protobuf.gradle.tasks.GenerateProtoTaskSpec;
import com.google.protobuf.gradle.tasks.ProtoSourceSet;
import com.google.protobuf.gradle.tasks.ProtoVariant;
import org.gradle.api.model.ObjectFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class DefaultProtoVariant implements ProtoVariant {
    private final String name;
    private final ProtoSourceSet sources;
    private final GenerateProtoTaskSpec generateProtoTaskSpec;

    private Set<String> sourceSets = new HashSet<String>();
    private boolean isTest = false;
    private String buildType = null;
    private Set<String> flavors = new HashSet<String>();

    public DefaultProtoVariant(String name, ObjectFactory objects) {
        this.name = name;
        this.sources = new DefaultProtoSourceSet(name, objects);
        this.generateProtoTaskSpec = new DefaultGenerateProtoTaskSpec(objects);
        this.generateProtoTaskSpec.getOutputSubDir().convention(name);
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public GenerateProtoTaskSpec getGenerateProtoTaskSpec() {
        return this.generateProtoTaskSpec;
    }

    @Override
    public ProtoSourceSet getSources() {
        return this.sources;
    }

    @Override
    public Set<String> getSourceSets() {
        return this.sourceSets;
    }

    @Override
    public void setSourceSets(Set<String> name) {
        this.sourceSets = name;
    }

    @Override
    public boolean getIsTest() {
        return this.isTest;
    }

    @Override
    public void setIsTest(boolean isTest) {
        this.isTest = isTest;
    }

    @Override
    public String getBuildType() {
        return this.buildType;
    }

    @Override
    public void setBuiltType(String name) {
        this.buildType = name;
    }

    @Override
    public Set<String> getFlavors() {
        return this.flavors;
    }

    @Override
    public void setFlavors(Set<String> flavors) {
        this.flavors = flavors;
    }
}
;
