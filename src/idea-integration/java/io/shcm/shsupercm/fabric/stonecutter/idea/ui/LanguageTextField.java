package io.shcm.shsupercm.fabric.stonecutter.idea.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// JavaC2 is a little stupid and cant see LanguageTextField so I'm giving it my amazing implementation of it.
public class LanguageTextField extends com.intellij.ui.LanguageTextField {
    public LanguageTextField() {
    }

    public LanguageTextField(Language language, @Nullable Project project, @NotNull String value) {
        super(language, project, value);
    }

    public LanguageTextField(Language language, @Nullable Project project, @NotNull String value, boolean oneLineMode) {
        super(language, project, value, oneLineMode);
    }

    public LanguageTextField(@Nullable Language language, @Nullable Project project, @NotNull String value, @NotNull DocumentCreator documentCreator) {
        super(language, project, value, documentCreator);
    }

    public LanguageTextField(@Nullable Language language, @Nullable Project project, @NotNull String value, @NotNull DocumentCreator documentCreator, boolean oneLineMode) {
        super(language, project, value, documentCreator, oneLineMode);
    }
}
