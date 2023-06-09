package io.shcm.shsupercm.fabric.stonecutter;

import io.shcm.shsupercm.fabric.stonecutter.cutter.Stonecutter;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Objects;

public class StonecutterBuildGradle {
    private final Project project;
    private final VersionData data;
    private StonecutterControllerGradle.StonecutterSetup stonecutterSetup;

    public StonecutterBuildGradle(Project project) {
        this.project = project;
        this.data = project.getExtensions().create("stonecutterVersion", VersionData.class, this);

        project.getTasks().create("stonecutterSetActiveVersion", taskCreate ->
            taskCreate.doFirst(task ->
                    new Stonecutter(Objects.requireNonNull(this.project.getParent()), stonecutterSetup.activeVersion(this.project.getParent()), data).run(task)));

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

    public void apply(StonecutterControllerGradle.StonecutterSetup stonecutterSetup) {
        this.stonecutterSetup = stonecutterSetup;
    }

    public static class VersionData {
        private final StonecutterBuildGradle plugin;
        private final String version;

        public VersionData(StonecutterBuildGradle plugin) {
            this.plugin = plugin;
            this.version = plugin.project.getName();
        }

        void apply(StonecutterControllerGradle.StonecutterSetup stonecutterSetup) {
            plugin.apply(stonecutterSetup);
        }

        public String version() {
            return this.version;
        }

        public boolean isActiveVersion() {
            return this.version.equals(plugin.stonecutterSetup.activeVersionString());
        }
    }
}
