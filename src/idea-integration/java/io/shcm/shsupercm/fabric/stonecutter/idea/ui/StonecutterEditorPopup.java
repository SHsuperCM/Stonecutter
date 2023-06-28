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
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;

public class StonecutterEditorPopup extends JPanel {
    private static final ActiveIcon ICON = new ActiveIcon(StonecutterService.ICON);

    private final Project project;
    private final Editor editor;
    private final VirtualFile file;
    private final StonecutterSetup stonecutter;

    private TextRange mainSyntaxRange = null;

    public static ComponentPopupBuilder builder(Project project, Editor editor, VirtualFile file) {
        StonecutterEditorPopup popup = new StonecutterEditorPopup(project, editor, file);
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popup, popup.firstFocus)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnWindowDeactivation(true)
                .setRequestFocus(true)
                .setTitle("Stonecutter")
                .setTitleIcon(ICON);
    }

    private StonecutterEditorPopup(Project project, Editor editor, VirtualFile file) {
        super(new GridBagLayout());
        this.project = project;
        this.editor = editor;
        this.file = file;
        this.stonecutter = project.getService(StonecutterService.class).fromVersionedFile(file);

        initComponents();

        bVersion.setText(stonecutter.currentActive());

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
            editor.getSelectionModel().removeSelection();
            add(pEditSyntax, BorderLayout.CENTER);
            String syntax = editor.getDocument().getText(this.mainSyntaxRange);
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

            tSyntax.requestFocusInWindow(FocusEvent.Cause.ACTIVATION);
            firstFocus = tSyntax;
        } else if (editor.getSelectionModel().hasSelection()) {
            add(pNewConstraint, BorderLayout.CENTER);
            bNewConstraint.requestFocusInWindow(FocusEvent.Cause.ACTIVATION);
            firstFocus = bNewConstraint;
        } else {
            firstFocus = bVersion;
        }
    }

    private void clickVersion(ActionEvent e) {
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(Lists.newArrayList(stonecutter.versions()))
                .setTitle("Switch Stonecutter Active Version")
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                .setItemChosenCallback(version -> project.getService(StonecutterService.class).switchActive(version))
                .createPopup().showInBestPositionFor(editor);
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

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        pTopButtons = new JPanel();
        bVersion = new JButton();
        pEditSyntax = new JPanel();
        var label1 = new JLabel();
        tSyntax = new JTextField();
        pNewConstraint = new JPanel();
        bNewConstraint = new JButton();

        //======== this ========
        setLayout(new BorderLayout(5, 5));

        //======== pTopButtons ========
        {
            pTopButtons.setBorder(new EtchedBorder());
            pTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT));

            //---- bVersion ----
            bVersion.setText("version");
            bVersion.setPreferredSize(new Dimension(80, 25));
            bVersion.addActionListener(e -> clickVersion(e));
            pTopButtons.add(bVersion);
        }
        add(pTopButtons, BorderLayout.NORTH);

        //======== pEditSyntax ========
        {
            pEditSyntax.setBorder(new EtchedBorder());
            pEditSyntax.setPreferredSize(new Dimension(300, 55));
            pEditSyntax.setLayout(new BorderLayout(5, 5));

            //---- label1 ----
            label1.setText("Version constraint:");
            pEditSyntax.add(label1, BorderLayout.NORTH);

            //---- tSyntax ----
            tSyntax.setPreferredSize(new Dimension(80, 16));
            tSyntax.setText("syntax");
            pEditSyntax.add(tSyntax, BorderLayout.CENTER);
        }

        //======== pNewConstraint ========
        {
            pNewConstraint.setBorder(new EtchedBorder());
            pNewConstraint.setPreferredSize(new Dimension(300, 45));
            pNewConstraint.setLayout(new FlowLayout(FlowLayout.LEFT));

            //---- bNewConstraint ----
            bNewConstraint.setText("New Constraint");
            bNewConstraint.addActionListener(e -> clickNewConstraint(e));
            pNewConstraint.add(bNewConstraint);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel pTopButtons;
    private JButton bVersion;
    private JPanel pEditSyntax;
    private JTextField tSyntax;
    private JPanel pNewConstraint;
    private JButton bNewConstraint;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    private final JComponent firstFocus;
}
