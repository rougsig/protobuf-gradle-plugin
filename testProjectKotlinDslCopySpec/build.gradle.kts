import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

buildscript {
  repositories {
    gradlePluginPortal()
  }
}

apply(plugin="java")
apply(plugin="java-library")
apply(plugin="com.google.protobuf")

repositories {
  mavenCentral()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation("io.grpc:grpc-protobuf:${ext.GRPC_PROTOBUF}")
  implementation("io.grpc:grpc-stub:$ext.GRPC_STUB")
  implementation("com.google.protobuf:protobuf-java:${ext.PROTOBUF_JAVA}")
  testImplementation("org.junit.jupiter:junit-jupiter-api:${ext.JUNIT_JUPITER_API}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${ext.JUNIT_JUPITER_ENGINE}")
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:${ext.PROTOC}"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:${ext.PROTOC_GEN_GRPC_JAVA}"
    }
  }
}

tasks {
  test {
    useJUnitPlatform {
    }
  }
}
