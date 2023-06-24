package io.shcm.shsupercm.fabric.stonecutter.idea.ext;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiComment;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterService;

import java.util.*;

public class CutterFolding implements FoldingBuilder {
    public static FoldingGroup GROUP_SYNTAX = FoldingGroup.newGroup("stonecutterSyntax");

    private List<FoldingDescriptor> descriptors = null;

    @Override
    public FoldingDescriptor[] buildFoldRegions(ASTNode node, Document document) {
        if (node.getPsi().getProject().getService(StonecutterService.class).fromVersionedFile(node.getPsi().getContainingFile().getVirtualFile()) == null)
            return FoldingDescriptor.EMPTY;

        descriptors = new ArrayList<>();

        buildFolds(node);

        return descriptors.isEmpty() ? FoldingDescriptor.EMPTY : descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void buildFolds(ASTNode node) {
        if (node.getElementType() == JavaTokenType.C_STYLE_COMMENT)
            buildFold(node.getPsi(PsiComment.class));

        for (ASTNode child : node.getChildren(null))
            buildFolds(child);
    }

    private void buildFold(PsiComment comment) {
        if (comment.getText().startsWith("/*?") && comment.getText().endsWith("?*/")) {
            descriptors.add(new FoldingDescriptor(comment.getNode(), new TextRange(comment.getTextOffset() + 1, comment.getTextOffset() + comment.getTextLength()), null, Set.of(comment), true, comment.getText().substring(3, comment.getTextLength() - 3).trim(), true));
        } else if (!descriptors.isEmpty() && comment.getText().startsWith("/*") && comment.getText().endsWith("?*/") &&
                    descriptors.get(descriptors.size() - 1).getRange().getEndOffset() == comment.getTextRange().getStartOffset()) {
            int nextExpression = comment.getText().lastIndexOf("/*?");
            descriptors.add(new FoldingDescriptor(comment.getNode(), new TextRange(comment.getTextOffset() + 1, comment.getTextOffset() + nextExpression), null, Set.of(comment), true, comment.getText().substring(2, nextExpression).trim(), true));
            descriptors.add(new FoldingDescriptor(comment.getNode(), new TextRange(comment.getTextOffset() + nextExpression + 1, comment.getTextOffset() + comment.getTextLength()), null, Set.of(comment), true, comment.getText().substring(nextExpression + 3, comment.getTextLength() - 3).trim(), true));
        }
    }

    @Override
    public String getPlaceholderText(ASTNode node) {
        return "";
    }

    @Override
    public boolean isCollapsedByDefault(ASTNode node) {
        return true;
    }
}
