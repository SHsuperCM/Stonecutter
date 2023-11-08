package io.shcm.shsupercm.fabric.stonecutter;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
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

    private StonecutterProjectBuilder shared = StonecutterProjectBuilder.DEFAULT;

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

    public void shared(Action<StonecutterProjectBuilder> stonecutterProjectBuilder) {
        this.shared = new StonecutterProjectBuilder(shared, stonecutterProjectBuilder);
    }

    public void create(ProjectDescriptor... projects) {
        for (ProjectDescriptor project : projects)
            create(project, builder -> {
                if (builder.versions.length == 0)
                    throw new GradleException("[Stonecutter] To create a stonecutter project without a configuration element, make use of shared default values.");
            });
    }

    public void create(ProjectDescriptor project, Action<StonecutterProjectBuilder> stonecutterProjectBuilder) {
        StonecutterProjectBuilder builder = new StonecutterProjectBuilder(shared, stonecutterProjectBuilder);

        if (builder.versions.length == 0)
            throw new GradleException("[Stonecutter] Stonecutter projects must have at the very least one version specified.");

        if (builder.vcsVersion == null)
            builder.vcsVersion = builder.versions[0];

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
                        """.formatted(builder.vcsVersion).getBytes(), StandardOpenOption.CREATE);
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
            try { new File(versionDir, "tokens.gradle").createNewFile(); } catch (IOException ignored) { }

            versionedProject.setProjectDir(versionDir);
            versionedProject.setName(version);
            if (!Boolean.getBoolean("stonecutter.disableCentralBuildScript"))
                versionedProject.setBuildFileName("../../build.gradle");
        }
    }

    public static class StonecutterProjectBuilder {
        public static final StonecutterProjectBuilder DEFAULT = new StonecutterProjectBuilder(); StonecutterProjectBuilder() { }

        protected String[] versions = new String[0];
        protected String vcsVersion = null;

        protected StonecutterProjectBuilder(StonecutterProjectBuilder defaultValues, Action<StonecutterProjectBuilder> builder) {
            this.versions = defaultValues.versions;
            this.vcsVersion = defaultValues.vcsVersion;

            builder.execute(this);
        }

        public void versions(String... versions) {
            if (versions.length == 0)
                throw new IllegalArgumentException("Invalid list of versions");

            this.versions = versions;
        }

        public void vcsVersion(String version) {
            this.vcsVersion = version;
        }
    }
}
