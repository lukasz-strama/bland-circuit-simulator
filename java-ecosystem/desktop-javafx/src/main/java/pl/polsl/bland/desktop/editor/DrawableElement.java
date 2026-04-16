package pl.polsl.bland.desktop.editor;

import pl.polsl.bland.models.CircuitElement;


public class DrawableElement {
    private CircuitElement element;

public DrawableElement(CircuitElement element) {
    this.element = element;
}   
public DrawableElement(
        String id,
        CircuitElement.ElementType type,
        String node1,
        String node2,
        double value,
        String sourceType,
        Double frequency,
        int x,
        int y,
        int rotation
) {
    this.element = new CircuitElement(id, type, node1, node2, value, sourceType, frequency, x, y, rotation);
}




public CircuitElement getElement() {
    return element;
}

// Gettery dla pól x, y, rotation oraz elementu
public int getX() {
    return element.x();
}
public int getY() {
    return element.y();
}
public int getRotation() {
    return element.rotation();
}

// Ustawianie pozycji elementu
public void setPosition(int x, int y) {
    this.element = new CircuitElement(
        element.id(),
        element.type(),
        element.node1(),
        element.node2(),
        element.value(),
        element.sourceType(),
        element.frequency(),
        x,
        y,
        element.rotation()
    );
}

// Przesuwanie elementu o określone przesunięcie (współrzędne deltaX i deltaY)
public void move(int deltaX, int deltaY) {
    setPosition(getX() + deltaX, getY() + deltaY);
}

// Obracanie elementu o określony kąt (w stopniach)
public void rotate(int deltaRotation) {

    int newRotation = (element.rotation() + deltaRotation) % 360;
        if (newRotation < 0) {
            newRotation += 360;
        }

    this.element = new CircuitElement(
        element.id(),
        element.type(),
        element.node1(),
        element.node2(),
        element.value(),
        element.sourceType(),
        element.frequency(),
        element.x(),
        element.y(),
        newRotation
    );
}

};


