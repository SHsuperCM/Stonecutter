package io.shcm.shsupercm.fabric.stonecutter.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterService;
import io.shcm.shsupercm.fabric.stonecutter.idea.ui.StonecutterEditorPopup;
import org.jetbrains.annotations.NotNull;

public class OpenStonecutterEditorAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getRequiredData(PlatformDataKeys.PROJECT);
        Editor editor = e.getRequiredData(PlatformDataKeys.EDITOR);
        VirtualFile file = e.getRequiredData(PlatformDataKeys.VIRTUAL_FILE);

        StonecutterEditorPopup.builder(project, editor, file)
                .createPopup()
                .showInBestPositionFor(editor);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);

        e.getPresentation().setEnabled(project != null && editor != null && file != null && project.getService(StonecutterService.class).fromVersionedFile(file) != null);
    }
}
