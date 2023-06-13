package io.shcm.shsupercm.fabric.stonecutter.idea;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElementVisitor;
import io.shcm.shsupercm.fabric.stonecutter.idea.util.GradleHelper;
import org.jetbrains.annotations.NotNull;

public class UnclosedInspection extends AbstractBaseJavaLocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (GradleHelper.isStonecutter(holder.getFile()))
            return super.buildVisitor(holder, isOnTheFly);

        return new JavaElementVisitor() {
            @Override
            public void visitComment(@NotNull PsiComment comment) {
                if (comment.getText().startsWith("/*?") && !comment.getText().endsWith("?*/")) {
                    holder.registerProblem(comment, "Unclosed Stonecutter expression", ProblemHighlightType.ERROR);
                }
            }
        };
    }
}
