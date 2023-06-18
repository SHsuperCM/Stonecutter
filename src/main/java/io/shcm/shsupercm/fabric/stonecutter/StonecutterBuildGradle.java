package io.shcm.shsupercm.fabric.stonecutter;

import io.shcm.shsupercm.fabric.stonecutter.cutter.StonecutterTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.Objects;

public class StonecutterBuildGradle {
    private final Project project;
    private final StonecutterProjectSetups.Setup setup;
    private final VersionData data;

    public StonecutterBuildGradle(Project project) {
        this.project = project;
        this.setup = project.getGradle().getExtensions().getByType(StonecutterProjectSetups.class).get(Objects.requireNonNull(project.getParent()));
        this.data = project.getExtensions().create("stonecutterVersion", VersionData.class, this);

        project.getTasks().register("setupChiseledStonecutterBuild", StonecutterTask.class, task -> {
            task.getFromVersion().set(project.getParent().project(setup.current()).getExtensions().getByType(VersionData.class));
            task.getToVersion().set(data);
            task.getInputDir().set(project.getParent().file("./src"));
            task.getOutputDir().set(new File(project.getBuildDir(), "chiseledSrc"));
        });

        project.afterEvaluate(this::afterEvaluate);
    }

    private void afterEvaluate(Project project) {
        if (setup.isChiseled(project.getGradle().getStartParameter().getTaskNames())) {
            for (SourceSet sourceSet : (SourceSetContainer) Objects.requireNonNull(project.property("sourceSets"))) {
                sourceSet.getJava().srcDir(new File(project.getBuildDir(), "chiseledSrc/" + sourceSet.getName() + "/java"));
                sourceSet.getResources().srcDir(new File(project.getBuildDir(), "chiseledSrc/" + sourceSet.getName() + "/resources"));
            }
            return;
        }

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

        public Project project() {
            return this.plugin.project;
        }
    }
}
