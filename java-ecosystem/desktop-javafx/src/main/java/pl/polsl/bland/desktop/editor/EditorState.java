package pl.polsl.bland.desktop.editor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.Wire;

public class EditorState {
    private List<DrawableElement> elements = new ArrayList<>();
    private List<DrawableWire> wires = new ArrayList<>();
    private DrawableElement selectedElement;
    private EditorMode mode = EditorMode.SELECT;

// EdytorMode to enum definiujący tryby edytora, np. SELECT itp.
public void setMode(EditorMode mode) {
    this.mode = mode;
}

public EditorMode getMode() {
    return mode;
}

// Metody do zarządzania elementami i przewodami
//Elementy
public void addElement(CircuitElement element) {
    elements.add(new DrawableElement(element));
}

public List<DrawableElement> getElements() {
    return elements;
}

// Przewody
public void addWire(Wire wire) {
    wires.add(new DrawableWire(wire));
}

public List<DrawableWire> getWires() {
    return wires;
}

// Zaznaczanie 
public DrawableElement getSelectedElement() {
    return selectedElement;
}

public void clearSelection() {
    selectedElement = null;
}

public void selectElementAtPosition(int x, int y){
    // Sprawdzanie, czy kliknięto na element DO ZMIANY
    for (DrawableElement element : elements) {
        if (Math.abs(element.getX() - x) < 20 && Math.abs(element.getY() - y) < 20) {
            selectedElement = element;
        return;
        }
    }
    selectedElement = null; // Jeśli nie znaleziono elementu, odznacz
}

// Przesuwanie zaznaczonego elementu o określone przesunięcie (deltaX, deltaY)
public void moveSelectedElement(int deltaX, int deltaY){    
    if(selectedElement != null) {
        selectedElement.move(deltaX, deltaY);
    }
}

// Rysowanie przewodu - dodawanie punktów do przewodu podczas rysowania
public void startWire(int x, int y) {
    Wire w = new Wire("wire_"+ 
    UUID.randomUUID(), // Unikalny identyfikator przewodu
    null, // DO USTALENIA
    new ArrayList<>(List.of(new int[]{x, y}))

 // Początkowy punkt przewodu
);
wires.add(new DrawableWire(w));
}

public void endWire(int x, int y) {
    if (!wires.isEmpty()) {
        DrawableWire currentWire = wires.get(wires.size() - 1);
        currentWire.setEndPoint(x, y);
    }
}

// 
  public CircuitSchematic toSchematic() {
        List<CircuitElement> elementModels = elements.stream()
                .map(DrawableElement::getElement)
                .toList();

        List<Wire> wireModels = wires.stream()
                .map(DrawableWire::getWire)
                .toList();

        return new CircuitSchematic(
                null,
                "schematic",
                null,
                elementModels,
                wireModels,
                Instant.now()
        );
    }

    // public void loadFromSchematic(CircuitSchematic schematic) {
    //     elements = schematic.getElement().stream()
    //             .map(DrawableElement::new)
    //             .toList();

    //     wires = schematic.getWire().stream()
    //             .map(DrawableWire::new)
    //             .toList();
    // }
}

