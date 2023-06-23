package io.shcm.shsupercm.fabric.stonecutter;

import org.gradle.api.Action;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class StonecutterSettingsGradle {
    private final Settings settings;
    private final StonecutterProjectSetups stonecutterProjects;

    public StonecutterSettingsGradle(Settings settings) {
        this.settings = settings;
        this.stonecutterProjects = settings.getGradle().getExtensions().create("stonecutterProjects", StonecutterProjectSetups.class);

        try {
            File stonecutter = new File(this.settings.getRootDir(), ".gradle/stonecutter");
            stonecutter.mkdirs();
            File thisJar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            stonecutter = new File(stonecutter, thisJar.getName());
            if (!stonecutter.exists())
                Files.copy(thisJar.toPath(), stonecutter.toPath());
        } catch (Exception ignored) { }
    }

    public void create(ProjectDescriptor project, Action<StonecutterProjectBuilder> stonecutterProjectBuilder) {
        StonecutterProjectBuilder builder = new StonecutterProjectBuilder(stonecutterProjectBuilder);

        if (!stonecutterProjects.registerVersioned(project.getPath(), builder))
            throw new IllegalArgumentException("Project already registered as a stonecutter project");

        try {
            project.setBuildFileName("stonecutter.gradle");
            File stonecutterGradle = new File(project.getProjectDir(), "stonecutter.gradle");
            if (!stonecutterGradle.exists())
                Files.write(stonecutterGradle.toPath(),
                        """
                        plugins.apply 'io.shcm.shsupercm.fabric.stonecutter'
                        stonecutter.active '%s'
                        //-------- !DO NOT EDIT ABOVE THIS LINE! --------\\\\
                        """.formatted(builder.versions[0]).getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String version : builder.versions) {
            String path = project.getPath();
            if (!path.endsWith(":"))
                path = path + ":";
            path = path + version;

            settings.include(path);
            ProjectDescriptor versionedProject = settings.project(path);
            File versionDir = new File(project.getProjectDir(), "/versions/" + version);
            versionDir.mkdirs();
            versionedProject.setProjectDir(versionDir);
            versionedProject.setBuildFileName("../../build.gradle");
            versionedProject.setName(version);
        }
    }

    public static class StonecutterProjectBuilder {
        protected String[] versions;
        protected String tokensFile = "./tokens.gradle";

        private StonecutterProjectBuilder(Action<StonecutterProjectBuilder> builder) {
            builder.execute(this);
        }

        public void versions(String... versions) {
            if (versions.length == 0 || versions.length != Set.of(versions).size())
                throw new IllegalArgumentException("Invalid list of versions");

            this.versions = versions;
        }

        public void tokensFile(String file) {
            this.tokensFile = file;
        }
    }
}
