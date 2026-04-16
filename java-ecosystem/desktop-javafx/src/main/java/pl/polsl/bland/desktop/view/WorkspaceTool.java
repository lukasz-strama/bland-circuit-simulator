package pl.polsl.bland.desktop.view;

import pl.polsl.bland.desktop.service.WorkspaceService;

public enum WorkspaceTool {

    SELECT("Zaznacz", "S"),
    WIRE("Przewód", "W"),
    DELETE("Usuń", "X");

    private final String label;
    private final String shortLabel;

    WorkspaceTool(String label, String shortLabel) {
        this.label = label;
        this.shortLabel = shortLabel;
    }

    public String label() { return label; }
    public String shortLabel() { return shortLabel; }

    
    public static WorkspaceTool forElementType(WorkspaceService.ElementType type) {
        return SELECT;
    }
}
