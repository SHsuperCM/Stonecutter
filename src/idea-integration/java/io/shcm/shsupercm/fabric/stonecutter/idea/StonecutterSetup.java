package io.shcm.shsupercm.fabric.stonecutter.idea;

import org.jetbrains.plugins.gradle.model.ExternalProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StonecutterSetup {
    private final ExternalProject gradleProject;
    private final String currentActive;
    private final String[] versions;

    public StonecutterSetup(ExternalProject gradleProject, String currentActive) {
        this.gradleProject = gradleProject;
        this.currentActive = currentActive;

        List<String> versions = new ArrayList<>(); {
            for (Map.Entry<String, ? extends ExternalProject> entry : gradleProject.getChildProjects().entrySet()) {
                File versionedDir = entry.getValue().getProjectDir();
                if (versionedDir.getName().equals(entry.getKey()) && versionedDir.getParentFile().getName().equals("versions"))
                    versions.add(entry.getKey());
            }
        } this.versions = versions.toArray(String[]::new);
    }

    public ExternalProject gradleProject() {
        return this.gradleProject;
    }

    public String currentActive() {
        return this.currentActive;
    }

    public Iterable<String> versions() {
        return Arrays.asList(this.versions);
    }
}
