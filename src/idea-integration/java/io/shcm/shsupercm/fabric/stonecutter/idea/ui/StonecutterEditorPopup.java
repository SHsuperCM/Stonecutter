package io.shcm.shsupercm.fabric.stonecutter.idea.ui;

import com.google.common.collect.Lists;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.StackingPopupDispatcher;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.CollectionComboBoxModel;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterService;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterSetup;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
                .createComponentPopupBuilder(popup.root, popup.root)
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

        SwingUtilities.invokeLater(() -> bVersions.requestFocusInWindow());

        for (Component component : new Component[] { bVersions, bTokens }) {
            component.setFocusTraversalKeysEnabled(false);
            component.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        if (bVersions.isFocusOwner())
                            bTokens.requestFocusInWindow();
                        else if (bTokens.isFocusOwner())
                            bVersions.requestFocusInWindow();
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        if (bVersions.isFocusOwner() || bTokens.isFocusOwner()) {
                            Component centerComponent = ((BorderLayout) root.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                            if (centerComponent != null)
                                (centerComponent instanceof JPanel && ((JPanel) centerComponent).getComponents().length > 0 ? ((JPanel) centerComponent).getComponent(0) : centerComponent).requestFocusInWindow();

                            e.consume();
                        }
                    }
                }
            });
        }

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
        StackingPopupDispatcher.getInstance().closeActivePopup();

        StonecutterEditorPopup popup = new StonecutterEditorPopup(project, editor, file);
        popup.root.add(popup.new Tokens().tabRoot, BorderLayout.CENTER);
        builder(popup).setMinSize(new Dimension(300, 200)).setResizable(true).createPopup().showInBestPositionFor(editor);
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

            SwingUtilities.invokeLater(() -> bNewConstraint.requestFocusInWindow());

            for (Component component : tabRoot.getComponents()) {
                component.setFocusTraversalKeysEnabled(false);
                component.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                            if (bNewConstraint.isFocusOwner() && bNewElse.isEnabled())
                                bNewElse.requestFocusInWindow();
                            else if (bNewElse.isFocusOwner())
                                bNewConstraint.requestFocusInWindow();
                            e.consume();
                        } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                            if (bNewConstraint.isFocusOwner() || bNewElse.isFocusOwner()) {
                                bVersions.requestFocusInWindow();
                                e.consume();
                            }
                        }
                    }
                });
            }
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

            SwingUtilities.invokeLater(() -> tSyntax.requestFocusInWindow());

            tSyntax.setFocusTraversalKeysEnabled(false);
            tSyntax.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        if (tSyntax.hasFocus())
                            bVersions.requestFocusInWindow();

                        e.consume();
                    }
                }
            });
        }
    }

    public class Tokens extends AbstractTableModel {
        public JPanel tabRoot;
        public JBTable tTokens;
        public JComboBox<String> cVersion;
        public JButton bNewToken;
        public JButton bCreateFlag;

        public List<StonecutterSetup.TokenMapper.Token> loadedTokens;

        public Tokens() {
            bTokens.setEnabled(false);

            tTokens.setModel(this);
            tTokens.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            tTokens.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int row = tTokens.rowAtPoint(e.getPoint());
                    if (e.getClickCount() == 2 && row != -1) {
                        try {
                            //noinspection SuspiciousMethodCalls
                            File tokensFile = new File(stonecutter.gradleProject().getChildProjects().get(cVersion.getSelectedItem()).getProjectDir(), "tokens.gradle");
                            if (!tokensFile.exists())
                                throw new Exception();

                            FileEditorManager.getInstance(project).openFile(Objects.requireNonNull(VirtualFileManager.getInstance().findFileByNioPath(tokensFile.toPath())), true);
                            Editor newEditor = Objects.requireNonNull(FileEditorManager.getInstance(project).getSelectedTextEditor());
                            newEditor.getCaretModel().moveToOffset(newEditor.getDocument().getText().indexOf((String) tTokens.getModel().getValueAt(row, 0)));
                        } catch (Exception ignored) { }
                    }
                }
            });

            CollectionComboBoxModel<String> versionModel = new CollectionComboBoxModel<>();
            for (String version : stonecutter.versions())
                versionModel.add(version);
            versionModel.setSelectedItem(stonecutter.currentActive());
            cVersion.setModel(versionModel);
            cVersion.addActionListener(this::versionChanged);

            tTokens.getSelectionModel().addListSelectionListener(e -> bCreateFlag.setEnabled(tTokens.getSelectedRow() != -1));

            bCreateFlag.addActionListener(this::clickCreateFlag);
            bNewToken.addActionListener(this::clickNewToken);

            refreshTable();
        }

        private void clickCreateFlag(ActionEvent actionEvent) {
            int tokenRow = tTokens.getSelectedRow();
            if (tokenRow == -1)
                return;

            String token = (String) tTokens.getModel().getValueAt(tokenRow, 0);

            JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(List.of("/*?$token enable " + token + "?*/", "/*?$token disable " + token + "?*/"))
                    .setTitle("Create Flag for Token: " + token)
                    .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                    .setItemChosenCallback(flag -> {
                        WriteCommandAction.runWriteCommandAction(project, null, null, () -> {
                            editor.getDocument().replaceString(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd(), flag);
                        });
                    })
                    .createPopup().showInBestPositionFor(editor);
        }

        private void clickNewToken(ActionEvent actionEvent) {

        }

        private void refreshTable() {
            //noinspection SuspiciousMethodCalls
            loadedTokens = new ArrayList<>(stonecutter.tokenCache().tokensByVersion.get(cVersion.getSelectedItem()).values());
            tTokens.revalidate();
            tTokens.repaint();
        }

        private void versionChanged(ActionEvent actionEvent) {
            refreshTable();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public String getColumnName(int column) {
            return new String[] {
                    "Identifier", "Reader", "Writer"
            }[column];
        }

        @Override
        public int getRowCount() {
            return loadedTokens.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StonecutterSetup.TokenMapper.Token token = loadedTokens.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return token.id;
                case 1:
                    return token.read.pattern();
                case 2:
                    return token.write;
            }
            return "";
        }
    }
}
