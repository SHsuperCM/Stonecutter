package io.shcm.shsupercm.fabric.stonecutter.idea.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.*;

public class StonecutterEditorPopup extends JPanel {
    private final Project project;
    private final Editor editor;
    private final VirtualFile file;

    private StonecutterEditorPopup(Project project, Editor editor, VirtualFile file) {
        super(new BorderLayout());
        this.project = project;
        this.editor = editor;
        this.file = file;

    }

    public static ComponentPopupBuilder builder(Project project, Editor editor, VirtualFile file) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new StonecutterEditorPopup(project, editor, file), null)
                .setTitle("Stonecutter");
    }
}
