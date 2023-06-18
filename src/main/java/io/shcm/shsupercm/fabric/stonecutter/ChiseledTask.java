package io.shcm.shsupercm.fabric.stonecutter;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;

public abstract class ChiseledTask extends DefaultTask {
    public final StonecutterProjectSetups.Setup setup = getProject().getGradle().getExtensions().getByType(StonecutterProjectSetups.class).get(getProject());
    @Input public abstract ListProperty<String> getVersions(); {
        getVersions().convention(() -> setup.versions().iterator());
    }

    private final Task setupChiselTask;

    public ChiseledTask() {
        this.setupChiselTask = getProject().getTasks().getByName("chiseledStonecutter");
        this.dependsOn(setupChiselTask);
    }

    public void ofTask(String taskName) {
        for (String version : getVersions().get()) {
            Task task = getProject().project(version).getTasks().getByName(taskName);
            this.finalizedBy(task);
            task.mustRunAfter(setupChiselTask);
        }
    }
}