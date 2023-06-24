package io.shcm.shsupercm.fabric.stonecutter.idea.ext;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.util.TextRange;
import io.shcm.shsupercm.fabric.stonecutter.idea.StonecutterService;
import io.shcm.shsupercm.fabric.stonecutter.idea.actions.OpenStonecutterEditorAction;
import org.jetbrains.annotations.NotNull;

public class EditorMouseClick implements EditorMouseListener {
    @Override
    public void mouseClicked(@NotNull EditorMouseEvent event) {
        if (event.getEditor().getProject() == null)
            return;
        if (event.getEditor().getProject().getService(StonecutterService.class) == null)
            return;

        FoldRegion clickedFolding = event.getCollapsedFoldRegion();
        if (clickedFolding != null) {
            String text = clickedFolding.getEditor().getDocument().getText(TextRange.create(clickedFolding.getStartOffset() - 1, clickedFolding.getEndOffset()));
            if (text.startsWith("/*?") && text.endsWith("?*/")) {
                event.consume();
                ActionManager.getInstance().tryToExecute(ActionManager.getInstance().getAction(OpenStonecutterEditorAction.class.getName()), event.getMouseEvent(), event.getEditor().getComponent(), null, true);
            } else if (text.startsWith("/*")) {
                for (FoldRegion foldRegion : event.getEditor().getFoldingModel().getAllFoldRegions())
                    if (foldRegion.getEndOffset() + 1 == clickedFolding.getStartOffset()) {
                        clickedFolding = foldRegion;
                        text = clickedFolding.getEditor().getDocument().getText(TextRange.create(clickedFolding.getStartOffset(), clickedFolding.getEndOffset()));
                        if (text.startsWith("/*?") && text.endsWith("?*/")) {
                            event.consume();
                            ActionManager.getInstance().tryToExecute(ActionManager.getInstance().getAction(OpenStonecutterEditorAction.class.getName()), event.getMouseEvent(), event.getEditor().getComponent(), null, true);
                            return;
                        }
                    }
            }
        }
    }
}
