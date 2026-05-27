package pl.polsl.bland.desktop.view;

import pl.polsl.bland.desktop.service.WorkspaceService;

/**
 * Narzędzia dostępne w edytorze schematów.
 *
 * Każde narzędzie zmienia sposób interpretacji kliknięć na płótnie:
 * <ul>
 *     <li>SELECT – zaznaczanie i przeciąganie elementów,</li>
 *     <li>WIRE – rysowanie przewodów między pinami,</li>
 *     <li>DELETE – usuwanie elementów i przewodów.</li>
 * </ul>
 */
public enum WorkspaceTool {

    /** Narzędzie do zaznaczania i przeciągania elementów. */
    SELECT("Zaznacz", "S"),
    /** Narzędzie do rysowania przewodów między pinami. */
    WIRE("Przewód", "W"),
    /** Narzędzie do usuwania elementów i przewodów. */
    DELETE("Usuń", "X");

    private final String label;
    private final String shortLabel;

    WorkspaceTool(String label, String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    /** @return pełna nazwa narzędzia wyświetlana w UI */
    public String label() { return label; }
    /** @return skrót literowy używany w pasku narzędzi */
    public String shortLabel() { return shortLabel; }

    /**
     * Zwraca domyślne narzędzie dla danego typu elementu.
     * Obecnie zawsze SELECT, ale metoda pozostawiona na przyszłą rozbudowę.
     */
    public static WorkspaceTool forElementType(WorkspaceService.ElementType type) {
        return SELECT;
    }
}
