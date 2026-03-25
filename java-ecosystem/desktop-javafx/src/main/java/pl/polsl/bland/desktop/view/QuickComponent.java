package pl.polsl.bland.desktop.view;

import pl.polsl.bland.desktop.service.WorkspaceService;

public enum QuickComponent {

    RESISTOR("R", "Rezystor", WorkspaceService.ElementType.RESISTOR),
    CAPACITOR("C", "Kondensator", WorkspaceService.ElementType.CAPACITOR),
    INDUCTOR("L", "Cewka", WorkspaceService.ElementType.INDUCTOR),
    VOLTAGE("V", "Źródło napięcia", WorkspaceService.ElementType.VOLTAGE),
    CURRENT("I", "Źródło prądu", WorkspaceService.ElementType.CURRENT),
    GROUND("G", "Masa", WorkspaceService.ElementType.GROUND);

    private final String glyph;
    private final String label;
    private final WorkspaceService.ElementType type;

    QuickComponent(String glyph, String label, WorkspaceService.ElementType type) {
        this.glyph = glyph;
        this.label = label;
        this.type = type;
    }

    public String glyph() { return glyph; }
    public String label() { return label; }
    public WorkspaceService.ElementType type() { return type; }

    public boolean matches(String filter) {
        return label.toLowerCase().contains(filter)
                || glyph.toLowerCase().contains(filter);
    }
}
