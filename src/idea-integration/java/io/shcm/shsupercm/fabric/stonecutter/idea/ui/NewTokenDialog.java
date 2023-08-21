package io.shcm.shsupercm.fabric.stonecutter.idea.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.LanguageTextField;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterSetup;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

public class NewTokenDialog extends DialogWrapper {
    private final Project project;
    private final Editor editor;
    private final StonecutterSetup stonecutter;

    public final Collection<RangeHighlighter> highlighters;

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
        this.highlighters = new ArrayList<>();

        setTitle("Create New Stonecutter Token");
        init();
        initValidation();
    }

    private void createUIComponents() {
        this.tIdentifier = new LanguageTextField(Language.findLanguageByID("TEXT"), project, "", true);
        this.tReader = new LanguageTextField(Language.findLanguageByID("RegExp"), project, "", true);
        this.tWriter = new LanguageTextField(Language.findLanguageByID("TEXT"), project, "", true);

        tIdentifier.setText(stonecutter.tokenCache().missingTokens.isEmpty() ? "" : stonecutter.tokenCache().missingTokens.iterator().next());
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!tIdentifier.getText().trim().matches("^[-_a-z0-9]+$"))
            return new ValidationInfo("Token identifiers should only contain a-z, 0-9, dashes and underscores.", tIdentifier).withOKEnabled();
        if (stonecutter.tokenCache().commonTokens.contains(tIdentifier.getText().trim()))
            return new ValidationInfo("Token already exists in this version.", tIdentifier);

        for (RangeHighlighter highlighter : highlighters)
            editor.getMarkupModel().removeHighlighter(highlighter);
        highlighters.clear();

        try {
            Pattern readerPattern = Pattern.compile(tReader.getText());

            final var matches = new Object() {
                public int total = 0, errored = 0;
            };

            readerPattern.matcher(editor.getDocument().getText()).results().forEach(result -> {
                final boolean reconstructs; {
                    String original = result.group(),
                           replaced = readerPattern.matcher(original).replaceFirst(tWriter.getText());
                    reconstructs = original.equals(replaced);
                }

                matches.total++;
                if (!reconstructs)
                    matches.errored++;

                highlighters.add(editor.getMarkupModel().addRangeHighlighter(
                        reconstructs ? EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES : EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES,
                        result.start(), result.end(),
                        HighlighterLayer.SELECTION + 2, HighlighterTargetArea.EXACT_RANGE));
            });

            if (matches.errored > 0)
                return new ValidationInfo(matches.errored + "/" + matches.total + " matches in the current file would not be written correctly", tWriter).withOKEnabled();
        } catch (Exception ignored) { }

        return null;
    }

    @Override
    protected void dispose() {
        super.dispose();
        try {
            for (RangeHighlighter highlighter : highlighters)
                editor.getMarkupModel().removeHighlighter(highlighter);
            highlighters.clear();
        } catch (Exception ignored) { }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return root;
    }

    public void execute() {
        try {
            Files.writeString(new File(stonecutter.gradleProject().getChildProjects().get(stonecutter.currentActive()).getProjectDir(), "tokens.gradle").toPath(), new StringBuilder()
                    .append('\n')
                    .append("token ('").append(tIdentifier.getText()).append("') {\n")
                    .append("    read ~/").append(tReader.getText().replace("/", "\\/")).append("/\n")
                    .append("    write '").append(tWriter.getText().replace("'", "\\'")).append("'\n")
                    .append("    defaultEnabled ").append(cEnableByDefault.isSelected()).append('\n')
                    .append("}\n")
                    , StandardCharsets.ISO_8859_1, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

