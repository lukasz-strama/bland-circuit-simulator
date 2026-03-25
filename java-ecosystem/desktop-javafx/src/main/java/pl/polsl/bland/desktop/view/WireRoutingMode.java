package pl.polsl.bland.desktop.view;

public enum WireRoutingMode {
    STRAIGHT("Odcinek prosty");

    private final String label;

    WireRoutingMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
