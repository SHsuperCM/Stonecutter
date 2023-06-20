package io.shcm.shsupercm.fabric.stonecutter;

import groovy.lang.MissingPropertyException;
import io.shcm.shsupercm.fabric.stonecutter.cutter.StoneRegexTokenizer;
import io.shcm.shsupercm.fabric.stonecutter.cutter.StonecutterTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.util.Objects;

public class StonecutterBuildGradle {
    private final Project project;
    private final StonecutterProjectSetups.Setup setup;
    private final Version version;

    public StonecutterBuildGradle(Project project) {
        this.project = project;
        this.setup = project.getGradle().getExtensions().getByType(StonecutterProjectSetups.class).get(Objects.requireNonNull(project.getParent()));
        this.version = new Version(this, project.getName());

        project.getTasks().register("setupChiseledStonecutterBuild", StonecutterTask.class, task -> {
            task.getFromVersion().set(project.getParent().project(setup.current()).getExtensions().getByType(StonecutterBuildGradle.class).current());
            task.getToVersion().set(version);
            task.getInputDir().set(project.getParent().file("./src"));
            task.getOutputDir().set(new File(project.getBuildDir(), "chiseledSrc"));
        });

        project.afterEvaluate(this::afterEvaluate);
    }

    private void afterEvaluate(Project project) {
        try {
            if (setup.anyChiseled(project.getGradle().getStartParameter().getTaskNames())) {
                for (SourceSet sourceSet : (SourceSetContainer) Objects.requireNonNull(project.property("sourceSets"))) {
                    sourceSet.getJava().srcDir(new File(project.getBuildDir(), "chiseledSrc/" + sourceSet.getName() + "/java"));
                    sourceSet.getResources().srcDir(new File(project.getBuildDir(), "chiseledSrc/" + sourceSet.getName() + "/resources"));
                }
                return;
            }

            if (this.version.isActiveVersion()) {
                for (SourceSet sourceSet : (SourceSetContainer) Objects.requireNonNull(project.property("sourceSets"))) {
                    sourceSet.getJava().srcDir("../../src/" + sourceSet.getName() + "/java");
                    sourceSet.getResources().srcDir("../../src/" + sourceSet.getName() + "/resources");
                }
            }
        } catch (MissingPropertyException ignored) { }
    }

    public Version current() {
        return this.version;
    }

    public Iterable<String> versions() {
        return setup.versions();
    }

    public static class Version {
        private final StonecutterBuildGradle plugin;
        private final String version;
        private StoneRegexTokenizer tokenizer = null;

        public Version(StonecutterBuildGradle plugin, String version) {
            this.plugin = plugin;
            this.version = version;
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

        public StoneRegexTokenizer tokenizer() {
            if (this.tokenizer == null) {
                this.tokenizer = new StoneRegexTokenizer();
                final File tokensFile = project().file(plugin.setup.tokensFile());
                if (tokensFile.exists())
                    project().apply(it -> {
                        it.from(tokensFile);
                        it.to(this.tokenizer);
                    });
            }

            return this.tokenizer;
        }
    }
}
