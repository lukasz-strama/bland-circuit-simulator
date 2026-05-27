package pl.polsl.bland.desktop.view;

import pl.polsl.bland.desktop.service.WorkspaceService;

/**
 * Komponenty dostępne w pasku szybkiego wstawiania elementów.
 *
 * Każdy komponent posiada:
 * <ul>
 *     <li>glyph – skrót literowy (np. R, C, L),</li>
 *     <li>label – pełną nazwę wyświetlaną w UI,</li>
 *     <li>type – typ elementu w WorkspaceService.</li>
 * </ul>
 */
public enum QuickComponent {

    /** Rezystor (R). */
    RESISTOR("R", "Rezystor", WorkspaceService.ElementType.RESISTOR),
    /** Kondensator (C). */
    CAPACITOR("C", "Kondensator", WorkspaceService.ElementType.CAPACITOR),
    /** Cewka (L). */
    INDUCTOR("L", "Cewka", WorkspaceService.ElementType.INDUCTOR),
    /** Źródło napięcia (V). */
    VOLTAGE("V", "Źródło napięcia", WorkspaceService.ElementType.VOLTAGE),
    /** Źródło prądu (I). */
    CURRENT("I", "Źródło prądu", WorkspaceService.ElementType.CURRENT),
    /** Masa (G). */
    GROUND("G", "Masa", WorkspaceService.ElementType.GROUND);

    private final String glyph;
    private final String label;
    private final WorkspaceService.ElementType type;

    QuickComponent(String glyph, String label, WorkspaceService.ElementType type) {
        this.glyph = glyph;
        this.label = label;
        this.type = type;
    }

    /** @return skrót literowy komponentu (np. R, C, L) */
    public String glyph() { return glyph; }
    /** @return pełna nazwa komponentu wyświetlana w UI */
    public String label() { return label; }
    /** @return typ elementu używany przez WorkspaceService */
    public WorkspaceService.ElementType type() { return type; }

    /**
     * Sprawdza, czy komponent pasuje do filtra wyszukiwania.
     *
     * @param filter tekst wpisany przez użytkownika
     * @return true jeśli nazwa lub symbol zawiera filtr
     */
    public boolean matches(String filter) {
        return label.toLowerCase().contains(filter)
                || glyph.toLowerCase().contains(filter);
    }
}
