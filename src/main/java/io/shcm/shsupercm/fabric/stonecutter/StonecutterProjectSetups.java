package io.shcm.shsupercm.fabric.stonecutter;

import org.gradle.api.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StonecutterProjectSetups {
    private final Map<String, Setup> controllerSetups = new HashMap<>();

    protected boolean registerVersioned(String project, String[] versions) {
        return this.controllerSetups.putIfAbsent(project, new Setup(versions)) == null;
    }

    public Setup get(Project project) {
        return this.controllerSetups.get(project.getPath());
    }

    public static class Setup {
        private final String[] versions;
        private String current;

        private Setup(String[] versions) {
            this.versions = versions;
        }

        public List<String> versions() {
            return List.of(this.versions);
        }

        public String current() {
            return this.current;
        }

        public void setCurrent(String version) {
            if (!versions().contains(version))
                throw new IllegalArgumentException("Version not registered for project");
            this.current = version;
        }
    }
}
