package io.shcm.shsupercm.fabric.stonecutter.idea;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class StonecutterService {
    public static final Icon ICON = IconLoader.getIcon("/Stonecutter.svg", StonecutterService.class);

    private final Project project;

    private final Map<Module, StonecutterSetup> byController = new HashMap<>(), byVersioned = new HashMap<>();

    public StonecutterService(Project project) {
        this.project = project;
        loadFromProject();
    }

    public void switchActive(String version) {
        Notifications.Bus.notify(new Notification(NotificationGroup.createIdWithTitle("stonecutter", "Stonecutter"), "Stonecutter", "Switching active stonecutter version to " + version, NotificationType.INFORMATION));
    }

    public StonecutterSetup fromControllerModule(Module module) {
        return byController.get(module);
    }

    public StonecutterSetup fromVersionedModule(Module module) {
        return byVersioned.get(module);
    }

    public StonecutterSetup fromControllerFile(VirtualFile file) {
        return fromControllerModule(ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(file));
    }

    public StonecutterSetup fromVersionedFile(VirtualFile file) {
        return fromVersionedModule(ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(file));
    }

    public void loadFromProject() {
        ExternalProjectDataCache gradleCache = ExternalProjectDataCache.getInstance(project);
        ModuleManager moduleManager = ModuleManager.getInstance(project);

        byController.clear();
        byVersioned.clear();

        Map<File, Module> moduleByRoot = new HashMap<>();

        for (Module module : moduleManager.getModules())
            for (VirtualFile root : module.getComponent(ModuleRootManager.class).getContentRoots())
                moduleByRoot.put(new File(root.getPath()), module);

        for (Module module : moduleManager.getModules())
            for (VirtualFile root : module.getComponent(ModuleRootManager.class).getContentRoots()) {
                ExternalProject externalProject = gradleCache.getRootExternalProject(root.getPath());
                if (externalProject == null)
                    continue;

                exploreGradleProject(externalProject, moduleByRoot::get);
            }

        for (StonecutterSetup setup : byController.values())
            for (String version : setup.versions())
                byVersioned.put(moduleByRoot.get((setup.gradleProject().getChildProjects().get(version)).getProjectDir()), setup);

        for (Map.Entry<Module, StonecutterSetup> entry : new HashMap<>(byVersioned).entrySet())
            for (String sourceSetName : gradleCache.findExternalProject(entry.getValue().gradleProject(), entry.getKey()).keySet()) {
                Module sourceSetModule = moduleManager.findModuleByName(entry.getKey().getName() + "." + sourceSetName);
                if (sourceSetModule != null)
                    byVersioned.put(sourceSetModule, entry.getValue());
            }
    }

    private void exploreGradleProject(ExternalProject project, Function<File, Module> moduleGetter) {
        try {
            File stonecutterGradleFile = project.getBuildFile();
            if (!Objects.requireNonNull(stonecutterGradleFile).getName().equals("stonecutter.gradle"))
                throw new Exception();

            try (BufferedReader reader = new BufferedReader(new FileReader(stonecutterGradleFile, StandardCharsets.ISO_8859_1))) {
                if (!reader.readLine().equals("plugins.apply 'io.shcm.shsupercm.fabric.stonecutter'"))
                    throw new Exception();

                String currentActive = reader.readLine();
                currentActive = currentActive.substring(currentActive.indexOf('\'') + 1, currentActive.lastIndexOf('\''));

                byController.putIfAbsent(moduleGetter.apply(project.getProjectDir()), new StonecutterSetup(project, currentActive));
            }
        } catch (Exception ignored) { }

        for (ExternalProject child : project.getChildProjects().values())
            exploreGradleProject(child, moduleGetter);
    }

    public static class ReloadListener extends AbstractProjectResolverExtension {
        @Override
        public void resolveFinished(@NotNull DataNode<ProjectData> projectDataNode) {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                StonecutterService service = project.getServiceIfCreated(StonecutterService.class);
                if (service != null)
                    service.loadFromProject();
            }
        }
    }
}
