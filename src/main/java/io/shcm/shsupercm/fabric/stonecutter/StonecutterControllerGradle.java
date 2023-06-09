package io.shcm.shsupercm.fabric.stonecutter;

import org.gradle.api.Project;

import java.util.Set;

public class StonecutterControllerGradle {
    private final StonecutterSetup settings;

    public StonecutterControllerGradle(Project project) {
        settings = project.getExtensions().create("stonecutter", StonecutterSetup.class, (Runnable) () -> {
            StonecutterSetup settings = project.getExtensions().getByType(StonecutterSetup.class);
            for (String version : settings.versions) {
                Project versionProject = project.project(project.getPath() + ":" + version);
                versionProject.getPluginManager().apply(StonecutterPluginSplitter.class);;
                versionProject.getExtensions().getByType(StonecutterBuildGradle.VersionData.class).apply(settings);
                project.getTasks().create("Set active version to " + version, task -> {
                    task.setGroup("stonecutter versions");
                    task.dependsOn(versionProject.getPath() + ":stonecutterSetActiveVersion");
                });
            }
        });
    }

    public static class StonecutterSetup {
        private final Runnable onVersionsSet;
        private String[] versions;
        private String current;

        public StonecutterSetup(Runnable onVersionsSet) {
            this.onVersionsSet = onVersionsSet;
        }

        public void versions(String... versions) {
            this.versions = versions;
            onVersionsSet.run();
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
