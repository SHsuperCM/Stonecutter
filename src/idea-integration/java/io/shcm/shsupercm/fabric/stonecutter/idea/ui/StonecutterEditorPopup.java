package io.shcm.shsupercm.fabric.stonecutter.idea.ui;

import com.google.common.collect.Lists;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.StackingPopupDispatcher;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterService;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterSetup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StonecutterEditorPopup {
    private static final ActiveIcon ICON = new ActiveIcon(StonecutterService.ICON);

    private final Project project;
    private final Editor editor;
    private final VirtualFile file;
    private final StonecutterSetup stonecutter;

    private TextRange mainSyntaxRange = null;

    public JPanel root;
    public JButton bVersions;
    public JButton bTokens;

    public static ComponentPopupBuilder builder(Project project, Editor editor, VirtualFile file) {
        return builder(new StonecutterEditorPopup(project, editor, file));
    }

    private static ComponentPopupBuilder builder(StonecutterEditorPopup popup) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popup.root, null)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnWindowDeactivation(true)
                .setRequestFocus(true)
                .setTitle("Stonecutter")
                .setTitleIcon(ICON);
    }

    private StonecutterEditorPopup(Project project, Editor editor, VirtualFile file) {
        this.project = project;
        this.editor = editor;
        this.file = file;
        this.stonecutter = project.getService(StonecutterService.class).fromVersionedFile(file);


        bVersions.addActionListener(this::clickVersions);
        bTokens.addActionListener(this::clickTokens);

        bVersions.setText(stonecutter.currentActive());

        for (FoldRegion foldRegion : editor.getFoldingModel().getAllFoldRegions()) {
            if (foldRegion.getStartOffset() == editor.getCaretModel().getOffset()) {
                TextRange commentRange = TextRange.create(foldRegion.getStartOffset() - 1, foldRegion.getEndOffset());
                String text = foldRegion.getDocument().getText(commentRange);
                if (text.startsWith("/*?") && text.endsWith("?*/")) {
                    this.mainSyntaxRange = commentRange;
                    break;
                }
            }
        }

        if (this.mainSyntaxRange != null) {
            root.add(new EditSyntax().tabRoot, BorderLayout.CENTER);
        } else if (editor.getSelectionModel().hasSelection()) {
            root.add(new NewConstraint().tabRoot, BorderLayout.CENTER);
        }
    }

    private void clickVersions(ActionEvent e) {
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(Lists.newArrayList(stonecutter.versions()))
                .setTitle("Switch Stonecutter Active Version")
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                .setItemChosenCallback(version -> project.getService(StonecutterService.class).switchActive(version))
                .createPopup().showInBestPositionFor(editor);
    }

    private void clickTokens(ActionEvent e) {
        editor.getSelectionModel().removeSelection();
        root.remove(((BorderLayout) root.getLayout()).getLayoutComponent(BorderLayout.CENTER));
        root.add(new Tokens().tabRoot, BorderLayout.CENTER);
    }

    public class NewConstraint {
        public JPanel tabRoot;
        public JButton bNewConstraint;
        public JButton bNewElse;

        public NewConstraint() {
            bNewConstraint.addActionListener(this::clickNewConstraint);

            int start = editor.getSelectionModel().getSelectionStart();
            bNewElse.setEnabled(start > 4 && editor.getDocument().getText(TextRange.create(start - 4, start)).equals("}?*/"));
            if (bNewElse.isEnabled())
                bNewElse.addActionListener(this::clickNewElse);
        }

        private void clickNewConstraint(ActionEvent e) {
            StackingPopupDispatcher.getInstance().closeActivePopup();
            String selectionText = editor.getSelectionModel().getSelectedText();
            if (selectionText == null)
                return;
            WriteCommandAction.runWriteCommandAction(project, null, null, () -> {
                int selectionStart = editor.getSelectionModel().getSelectionStart(),
                        selectionEnd = editor.getSelectionModel().getSelectionEnd(),
                        startLine = editor.getDocument().getLineNumber(selectionStart), startLineStartOffset = editor.getDocument().getLineStartOffset(startLine), startLineEndOffset = editor.getDocument().getLineEndOffset(startLine),
                        endLine = editor.getDocument().getLineNumber(selectionEnd), endLineStartOffset = editor.getDocument().getLineStartOffset(endLine), endLineEndOffset = editor.getDocument().getLineEndOffset(endLine);
                editor.getSelectionModel().removeSelection();
                if (editor.getDocument().getText(TextRange.create(selectionEnd, endLineEndOffset)).isBlank()) {
                    String newLine = "\n" + CodeStyleManager.getInstance(project).getLineIndent(editor.getDocument(), endLineEndOffset);
                    editor.getDocument().insertString(selectionEnd, newLine);
                    selectionEnd += newLine.length();
                }
                editor.getDocument().insertString(selectionEnd, "/*?}?*/");
                if (editor.getDocument().getText(TextRange.create(startLineStartOffset, selectionStart)).isBlank()) {
                    String newLine = CodeStyleManager.getInstance(project).getLineIndent(editor.getDocument(), startLineStartOffset) + "\n";
                    editor.getDocument().insertString(startLineStartOffset, newLine);
                    selectionStart = startLineStartOffset + newLine.length() - 1;
                }
                editor.getDocument().insertString(selectionStart, "/*?" + stonecutter.currentActive() + " {?*/");
                editor.getCaretModel().moveToOffset(selectionStart + 1);
            });
        }

        private void clickNewElse(ActionEvent e) {
            StackingPopupDispatcher.getInstance().closeActivePopup();
            String selectionText = editor.getSelectionModel().getSelectedText();
            if (selectionText == null)
                return;
            WriteCommandAction.runWriteCommandAction(project, null, null, () -> {
                int selectionStart = editor.getSelectionModel().getSelectionStart(),
                        selectionEnd = editor.getSelectionModel().getSelectionEnd(),
                        endLine = editor.getDocument().getLineNumber(selectionEnd), endLineStartOffset = editor.getDocument().getLineStartOffset(endLine), endLineEndOffset = editor.getDocument().getLineEndOffset(endLine);
                editor.getSelectionModel().removeSelection();
                if (editor.getDocument().getText(TextRange.create(selectionEnd, endLineEndOffset)).isBlank()) {
                    String newLine = "\n" + CodeStyleManager.getInstance(project).getLineIndent(editor.getDocument(), endLineEndOffset);
                    editor.getDocument().insertString(selectionEnd, newLine);
                    selectionEnd += newLine.length();
                }
                editor.getDocument().insertString(selectionEnd, "/*?}?*/");
                editor.getDocument().insertString(selectionStart - 3, " else {");
                editor.getCaretModel().moveToOffset(selectionStart - 6);
            });
        }
    }

    public class EditSyntax {
        public JPanel tabRoot;
        public JTextField tSyntax;

        public EditSyntax() {
            editor.getSelectionModel().removeSelection();

            String syntax = editor.getDocument().getText(mainSyntaxRange);
            syntax = syntax.substring(3, syntax.length() - 3).trim();
            tSyntax.setText(syntax);
            tSyntax.addActionListener(e -> {
                WriteCommandAction.runWriteCommandAction(project, null, null, () -> {
                    String newSyntax = tSyntax.getText();
                    newSyntax = newSyntax.isBlank() ? "" : "/*?" + newSyntax + "?*/";
                    editor.getDocument().replaceString(mainSyntaxRange.getStartOffset(), mainSyntaxRange.getEndOffset(), newSyntax);
                });
                StackingPopupDispatcher.getInstance().closeActivePopup();
            });
        }
    }

    public class Tokens {
        public JPanel tabRoot;

        public Tokens() {
            bTokens.setEnabled(false);
        }
    }
}
