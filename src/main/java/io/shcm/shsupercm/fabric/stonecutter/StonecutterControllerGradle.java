package io.shcm.shsupercm.fabric.stonecutter;

import io.shcm.shsupercm.fabric.stonecutter.cutter.StonecutterTask;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class StonecutterControllerGradle {
    private final StonecutterSetup settings;

    public StonecutterControllerGradle(Project project) {
        settings = project.getExtensions().create("stonecutter", StonecutterSetup.class, (Consumer<StonecutterSetup>) (settings) -> {
            for (String version : settings.versions) {
                Project versionProject = project.project(version);
                versionProject.getPluginManager().apply(StonecutterPluginSplitter.class);;
                versionProject.getExtensions().getByType(StonecutterBuildGradle.VersionData.class).apply(settings);
            }
        });

        project.afterEvaluate(this::afterEvaluate);
    }

    private void afterEvaluate(Project project) {
        StonecutterBuildGradle.VersionData currentVersionData = project.project(settings.current).getExtensions().getByType(StonecutterBuildGradle.VersionData.class);
        for (String version : settings.versions) {
            Project versionProject = project.project(version);
            StonecutterBuildGradle.VersionData versionData = versionProject.getExtensions().getByType(StonecutterBuildGradle.VersionData.class);
            StonecutterTask task = project.getTasks().create("Set active version to " + versionData.version(), StonecutterTask.class);
            task.setGroup("stonecutter");

            task.getFromVersion().set(currentVersionData);
            task.getToVersion().set(versionData);
            task.getInputDir().set(project.file("./src"));
            task.getOutputDir().set(task.getInputDir().get());

            task.doLast(taskRun -> {
                try {
                    File stonecutterGradle = taskRun.getProject().getBuildFile();
                    List<String> stonecutterGradleLines = new ArrayList<>(Files.readAllLines(stonecutterGradle.toPath(), StandardCharsets.ISO_8859_1));

                    stonecutterGradleLines.replaceAll(line -> {
                        if (line.startsWith("stonecutter.current"))
                            return "stonecutter.current('" + versionData.version() + "')";

                        return line;
                    });

                    stonecutterGradle.delete();
                    Files.write(stonecutterGradle.toPath(), stonecutterGradleLines, StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static class StonecutterSetup {
        private final Consumer<StonecutterSetup> onVersionsSet;
        private String[] versions;
        private String current;

        public StonecutterSetup(Consumer<StonecutterSetup> onVersionsSet) {
            this.onVersionsSet = onVersionsSet;
        }

        public void versions(String... versions) {
            this.versions = versions;
            onVersionsSet.accept(this);
        }

        public void current(String current) {
            if (!availableVersions().contains(current))
                throw new IllegalArgumentException("Version '" + current + "' is not in supported versions");
            this.current = current;
        }

        public Set<String> availableVersions() {
            return Set.of(this.versions);
        }

        public String activeVersionString() {
            return this.current;
        }

        public StonecutterBuildGradle.VersionData activeVersion(Project controllerProject) {
            return controllerProject.project(controllerProject.getPath() + ":" + activeVersionString()).getExtensions().getByType(StonecutterBuildGradle.VersionData.class);
        }
    }
}
