package pl.polsl.bland.desktop.view;

/**
 * Tryby rysowania przewodów w edytorze.
 *
 * <ul>
 *     <li>STRAIGHT – przewód rysowany jako pojedynczy odcinek prostej.</li>
 *     <!-- Można dodać ORTHOGONAL w przyszłości -->
 * </ul>
 */
public enum WireRoutingMode {
    /** Przewód rysowany jako prosta linia między pinami. */
    STRAIGHT("Odcinek prosty");

    private final String label;

    WireRoutingMode(String label) {
        this.label = label;
    }

    /** @return opis trybu wyświetlany w UI */
    public String label() {
        return label;
    }
}
