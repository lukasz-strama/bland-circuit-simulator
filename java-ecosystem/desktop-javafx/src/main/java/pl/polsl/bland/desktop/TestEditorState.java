package pl.polsl.bland.desktop;

import pl.polsl.bland.desktop.editor.EditorState;
import pl.polsl.bland.desktop.editor.DrawableElement;
import pl.polsl.bland.models.CircuitElement;

public class TestEditorState {
    public static void main(String[] args) {

        EditorState state = new EditorState();

        CircuitElement e1 = new CircuitElement(
        "R1",
        CircuitElement.ElementType.R,
        "IN",
        "OUT",
        1000.0,
        null,
        null,
        10,
        10,
        0
);



        state.addElement(e1);

        state.selectElementAtPosition(10, 10);
        state.moveSelectedElement(10, 20);

        DrawableElement selected = state.getSelectedElement();
        System.out.println("X = " + selected.getX());
        System.out.println("Y = " + selected.getY());
    }
}
