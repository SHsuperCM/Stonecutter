package io.shcm.shsupercm.fabric.stonecutter.idea.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterSetup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NewTokenDialog extends DialogWrapper {
    private final Project project;
    private final Editor editor;
    private final StonecutterSetup stonecutter;

    public JPanel root;
    public JCheckBox cEnableByDefault;
    public LanguageTextField tIdentifier;
    public LanguageTextField tReader;
    public LanguageTextField tWriter;

    public NewTokenDialog(Project project, Editor editor, StonecutterSetup stonecutter) {
        super(project);
        this.project = project;
        this.editor = editor;
        this.stonecutter = stonecutter;

        setTitle("Create New Stonecutter Token");
        init();
    }

    private void createUIComponents() {
        this.tIdentifier = new LanguageTextField(Language.findLanguageByID("TEXT"), project, "", true);
        this.tReader = new LanguageTextField(Language.findLanguageByID("RegExp"), project, "", true);
        this.tWriter = new LanguageTextField(Language.findLanguageByID("TEXT"), project, "", true);

        tIdentifier.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.@NotNull DocumentEvent event) {
                boolean valid = tIdentifier.getText().trim().matches("^[-_a-z0-9]+$") && !stonecutter.tokenCache().commonTokens.contains(tIdentifier.getText().trim());
                setOKActionEnabled(valid);
                tIdentifier.setForeground(valid ? JBColor.black : JBColor.red);
            }
        });
        tIdentifier.setText(stonecutter.tokenCache().missingTokens.isEmpty() ? "" : stonecutter.tokenCache().missingTokens.iterator().next());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return root;
    }

    public void execute() {

    }
}

