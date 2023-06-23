package io.shcm.shsupercm.fabric.stonecutter.idea.util;

import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.Objects;

public final class GradleHelper { private GradleHelper() {}
    public static boolean isStonecutter(PsiFile file) {
        return isStonecutter(file.getVirtualFile(), file.getProject());
    }

    public static boolean isStonecutter(VirtualFile file, Project project) {
        return getStonecutterGradle(getGradle(file, project), project) != null;
    }

    public static ExternalProject getStonecutterGradle(ExternalProject gradleProject, Project ideaProject) {
        try {
            Objects.requireNonNull(gradleProject);
            if (new File(gradleProject.getProjectDir(), "versions").exists() && gradleProject.getBuildFile() != null && gradleProject.getBuildFile().getName().equals("stonecutter.gradle"))
                return gradleProject;

            File versions = gradleProject.getProjectDir().getParentFile();
            if (versions.getName().equals("versions"))
                return getStonecutterGradle(parent(gradleProject, ideaProject), ideaProject);
        } catch (Exception ignored) { }

        return null;
    }

    public static ExternalProject getGradle(PsiFile file) {
        return getGradle(file.getVirtualFile(), file.getProject());
    }

    public static ExternalProject getGradle(VirtualFile file, Project project) {
        return getGradle(ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(file));
    }

    public static ExternalProject getGradle(Module module) {
        try {
            for (ExternalProjectInfo externalProjectInfo : ProjectDataManager.getInstance().getExternalProjectsData(module.getProject(), GradleConstants.SYSTEM_ID)) {
                ExternalProject rootProject = getGradle(module.getProject(), externalProjectInfo.getExternalProjectPath());

                for (ExternalSourceSet sourceSet : ExternalProjectDataCache.getInstance(module.getProject()).findExternalProject(Objects.requireNonNull(rootProject), module).values()) {
                    ExternalProject project = findWithSourceSet(rootProject, sourceSet);
                    if (project != null)
                        return project;
                }
            }
        } catch (Exception ignored) { }

        return null;
    }

    public static ExternalProject getGradle(Project project, String path) {
        return ExternalProjectDataCache.getInstance(project).getRootExternalProject(path);
    }

    public static ExternalProject findWithSourceSet(ExternalProject root, ExternalSourceSet sourceSet) {
        if (root.getSourceSets().containsValue(sourceSet))
            return root;

        for (ExternalProject subProject : root.getChildProjects().values()) {
            ExternalProject sourceSetProject = findWithSourceSet(subProject, sourceSet);
            if (sourceSetProject != null)
                return sourceSetProject;
        }

        return null;
    }

    public static ExternalProject parent(ExternalProject gradleProject, Project ideaProject) {
        for (ExternalProjectInfo projectInfo : ProjectDataManager.getInstance().getExternalProjectsData(ideaProject, GradleConstants.SYSTEM_ID)) {
            ExternalProject root = ExternalProjectDataCache.getInstance(ideaProject).getRootExternalProject(projectInfo.getExternalProjectPath());
            if (root == null)
                continue;
            ExternalProject parent = parent(root, gradleProject);
            if (parent != null)
                return parent;
        }

        return null;
    }

    public static ExternalProject parent(ExternalProject root, ExternalProject child) {
        if (root.getChildProjects().containsValue(child))
            return root;

        for (ExternalProject subRoot : root.getChildProjects().values()) {
            ExternalProject parent = parent(subRoot, child);
            if (parent != null)
                return parent;
        }

        return null;
    }
}
