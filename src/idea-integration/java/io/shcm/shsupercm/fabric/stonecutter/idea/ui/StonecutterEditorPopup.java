package io.shcm.shsupercm.fabric.stonecutter.idea.ui;

import javax.swing.border.*;
import com.google.common.collect.Lists;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterService;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterSetup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StonecutterEditorPopup extends JPanel {
    private final Project project;
    private final Editor editor;
    private final VirtualFile file;
    private final StonecutterSetup stonecutter;

    private TextRange mainSyntaxRange = null;


    public static ComponentPopupBuilder builder(Project project, Editor editor, VirtualFile file) {
        return JBPopupFactory.getInstance()
                .createComponentPopupBuilder(new StonecutterEditorPopup(project, editor, file), null)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnWindowDeactivation(true)
                .setRequestFocus(true)
                .setTitle("Stonecutter");
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
        }
    }

    private void clickVersion(ActionEvent e) {
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(Lists.newArrayList(stonecutter.versions()))
                .setTitle("Switch Stonecutter Active Version")
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                .setItemChosenCallback(this::clickVersion)
                .createPopup().showInBestPositionFor(editor);
    }

    private void clickVersion(String version) {
        project.getService(StonecutterService.class).switchActive(version);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        pTopButtons = new JPanel();
        bVersion = new JButton();
        pEditSyntax = new JPanel();
        var label1 = new JLabel();
        tSyntax = new EditorTextField();

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
            pEditSyntax.setPreferredSize(new Dimension(300, 80));
            pEditSyntax.setLayout(new BorderLayout(5, 5));

            //---- label1 ----
            label1.setText("Version constraint:");
            pEditSyntax.add(label1, BorderLayout.NORTH);

            //---- tSyntax ----
            tSyntax.setPreferredSize(new Dimension(80, 25));
            pEditSyntax.add(tSyntax, BorderLayout.CENTER);
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel pTopButtons;
    private JButton bVersion;
    private JPanel pEditSyntax;
    private EditorTextField tSyntax;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
