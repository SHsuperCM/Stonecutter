package io.shcm.shsupercm.fabric.stonecutter;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Objects;

public class StonecutterBuildGradle {
    private final Project project;
    private final StonecutterProjectSetups.Setup setup;
    private final VersionData data;

    public StonecutterBuildGradle(Project project) {
        this.project = project;
        this.setup = project.getGradle().getExtensions().getByType(StonecutterProjectSetups.class).get(Objects.requireNonNull(project.getParent()));
        this.data = project.getExtensions().create("stonecutterVersion", VersionData.class, this);

        project.afterEvaluate(this::afterEvaluate);
    }

    private void afterEvaluate(Project project) {
        if (this.data.isActiveVersion()) {
            for (SourceSet sourceSet : (SourceSetContainer) Objects.requireNonNull(project.property("sourceSets"))) {
                sourceSet.getJava().srcDir("../../src/" + sourceSet.getName() + "/java");
                sourceSet.getResources().srcDir("../../src/" + sourceSet.getName() + "/resources");
            }
        }
    }

    public static class VersionData {
        private final StonecutterBuildGradle plugin;
        private final String version;

        public VersionData(StonecutterBuildGradle plugin) {
            this.plugin = plugin;
            this.version = plugin.project.getName();
        }

        public String version() {
            return this.version;
        }

        public boolean isActiveVersion() {
            return this.version.equals(plugin.setup.current());
        }
    }
}
