package io.shcm.shsupercm.fabric.stonecutter;

import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class StonecutterSettingsGradle {
    private final Settings settings;
    private final StonecutterProjectSetups stonecutterProjects;

    public StonecutterSettingsGradle(Settings settings) {
        this.settings = settings;
        this.stonecutterProjects = settings.getGradle().getExtensions().create("stonecutterProjects", StonecutterProjectSetups.class);
    }

    public void versioned(ProjectDescriptor project, String... versions) {
        if (versions.length == 0)
            throw new IllegalArgumentException("Must have at least one version");
        if (!stonecutterProjects.registerVersioned(project.getPath(), versions))
            throw new IllegalArgumentException("Project already registered as a stonecutter project");

        try {
            project.setBuildFileName("stonecutter.gradle");
            File stonecutterGradle = new File(project.getProjectDir(), "stonecutter.gradle");
            if (!stonecutterGradle.exists())
                Files.write(stonecutterGradle.toPath(),
                        """
                        plugins.apply 'io.shcm.shsupercm.fabric.stonecutter'
                        stonecutterSetup.active '%s'
                        //-------- !DO NOT EDIT ABOVE THIS LINE! --------\\\\
                        """.formatted(versions[0]).getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String version : versions) {
            String path = project.getPath() + ":" + version;
            settings.include(path);
            ProjectDescriptor versionedProject = settings.project(path);
            versionedProject.setProjectDir(new File(project.getProjectDir(), "/versions/" + version));
            versionedProject.setBuildFileName("../../build.gradle");
            versionedProject.setName(version);
        }
    }
}
