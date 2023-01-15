package com.google.protobuf.gradle.tasks;

import org.gradle.api.Named;

import java.util.Set;

public interface ProtoVariant extends Named {
    ProtoSourceSet getSources();

    Set<String> getSourceSets();
    void setSourceSets(Set<String> sourceSets);

    boolean getIsTest();
    void setIsTest(boolean isTest);

    String getBuildType();
    void setBuiltType(String name);

    Set<String> getFlavors();
    void setFlavors(Set<String> flavors);
}
