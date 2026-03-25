package pl.polsl.bland.webapp.view.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import pl.polsl.bland.webapp.service.WorkspaceMockService;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ResultsWindow extends Div {
    public enum ResultTab {
        SUMMARY("Wyniki symulacji"),
        PLOT("Przebiegi"),
        NETLIST("Netlista"),
        LOGS("Logi");

        private final String label;

        ResultTab(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final Span caption = new Span();
    private final Span analysisBadge = new Span();
    private final Span closeButton = new Span("Zamknij");
    private final Span plotTitle = new Span();
    private final Grid<WorkspaceMockService.ResultRow> resultsGrid = new Grid<>(WorkspaceMockService.ResultRow.class, false);
    private final Div summaryPlaceholder = new Div();
    private final Div plotHint = new Div();
    private final Div plotMeta = new Div();
    private final Select<String> traceSelect = new Select<>();
    private final Div plotViewport = new Div();
    private final Div plotEmptyState = new Div();
    private final Svg waveformSvg = new Svg();
    private final Pre netlistBox = new Pre();
    private final Div logList = new Div();
    private final Map<ResultTab, Div> tabButtons = new EnumMap<>(ResultTab.class);
    private final Map<ResultTab, Div> tabPanels = new EnumMap<>(ResultTab.class);
    private final LinkedHashMap<String, PlotTrace> availableTraces = new LinkedHashMap<>();
    private Runnable closeHandler = () -> {};
    private ResultTab activeTab = ResultTab.SUMMARY;

    public ResultsWindow() {
        addClassNames("floating-window", "is-results-window");
        configureResultsGrid();
        configureTraceSelect();
        closeButton.addClickListener(event -> closeHandler.run());
        add(buildTitleBar(), buildBody());
        setActiveTab(ResultTab.SUMMARY);
    }

    public void clear(String analysisLabel, boolean simulationReady, String message) {
        caption.setText("Brak aktywnego elementu");
        analysisBadge.setText(analysisLabel);
        plotTitle.setText("Aktywny ślad: brak");
        plotHint.setText(message);
        clearAvailableTraces();
        clearPlot(message);
        netlistBox.setText("* Zaznacz element, aby zobaczyć mockowaną netlistę.");
        resultsGrid.setItems(List.of());
        logList.removeAll();
        logList.add(new Div(new Text(message)));
        summaryPlaceholder.setText(simulationReady
                ? message
                : "Brak wyników. Zaznacz element i uruchom symulację, aby wypełnić zakładki danymi.");
        summaryPlaceholder.setVisible(true);
        resultsGrid.setVisible(false);
    }

    public void update(WorkspaceMockService.ElementDetails details, String analysisLabel, boolean simulationReady) {
        caption.setText("Aktywny element: " + details.id() + " / " + details.typeLabel());
        analysisBadge.setText(analysisLabel);
        plotTitle.setText("Aktywny ślad: " + details.traceName());
        plotHint.setText(simulationReady
                ? details.simulationNote()
                : "Uruchom symulację, aby odblokować przebiegi i statystyki dla " + details.id() + ".");
        clearAvailableTraces();
        clearPlot(simulationReady
                ? "Ten widok dla mockowanych wynikow nie rysuje jeszcze osobnego przebiegu."
                : "Uruchom symulacje, aby narysowac przebieg.");
        netlistBox.setText(details.netlist());
        resultsGrid.setItems(details.rows());
        logList.removeAll();
        details.logs().forEach(line -> logList.add(new Div(new Text(line))));
        summaryPlaceholder.setText("Brak wyników dla " + details.id() + ". Kliknij „Symuluj”, aby wypełnić zakładkę danymi.");
        summaryPlaceholder.setVisible(!simulationReady);
        resultsGrid.setVisible(simulationReady);
    }

    public void showWire(WorkspaceMockService.WireDetails details, String analysisLabel) {
        caption.setText("Aktywny przewód: " + details.id());
        analysisBadge.setText(analysisLabel);
        plotTitle.setText("Połączenie: " + details.startPin() + " -> " + details.endPin());
        plotHint.setText("Przewody nie generują osobnego śladu. Użyj widoku netlisty albo logów, aby prześledzić połączenie.");
        clearAvailableTraces();
        clearPlot("Przewody nie maja osobnego przebiegu.");
        netlistBox.setText(details.netlist());
        resultsGrid.setItems(List.of());
        logList.removeAll();
        details.logs().forEach(line -> logList.add(new Div(new Text(line))));
        summaryPlaceholder.setText("Przewód nie ma własnych wyników liczbowych. Możesz go przepiąć do innego pinu z poziomu toolbaru.");
        summaryPlaceholder.setVisible(true);
        resultsGrid.setVisible(false);
    }

    public void showSimulation(
            String captionText,
            String analysisLabel,
            List<WorkspaceMockService.ResultRow> rows,
            List<PlotTrace> plotTraces,
            String preferredTraceKey,
            String netlist,
            List<String> logs) {
        caption.setText(captionText);
        analysisBadge.setText(analysisLabel);
        setAvailableTraces(plotTraces, preferredTraceKey);
        netlistBox.setText(netlist);
        resultsGrid.setItems(rows);
        logList.removeAll();
        logs.forEach(line -> logList.add(new Div(new Text(line))));
        summaryPlaceholder.setText(rows.isEmpty()
                ? resolveSummaryPlaceholder(plotTraces)
                : "Dane pochodzą z ostatniej symulacji uruchomionej z poziomu web-app.");
        summaryPlaceholder.setVisible(rows.isEmpty());
        resultsGrid.setVisible(!rows.isEmpty());
    }

    public void setCloseHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler == null ? () -> {} : closeHandler;
    }

    public void setActiveTab(ResultTab tab) {
        activeTab = tab;
        tabButtons.forEach((key, button) -> setClass(button, "is-active", key == activeTab));
        tabPanels.forEach((key, panel) -> setClass(panel, "is-active", key == activeTab));
    }

    private Component buildTitleBar() {
        Div titleBar = new Div();
        titleBar.addClassName("window-titlebar");

        Div textGroup = new Div();
        Span title = new Span("Wyniki symulacji");
        title.addClassName("window-title");
        caption.addClassName("window-caption");
        textGroup.add(title, caption);

        analysisBadge.addClassNames("badge", "badge-inline");
        closeButton.addClassNames("tool-button", "window-action");

        Div actions = new Div();
        actions.addClassName("window-actions");
        actions.add(analysisBadge, closeButton);

        titleBar.add(textGroup, actions);
        return titleBar;
    }

    private Component buildBody() {
        Div body = new Div();
        body.addClassName("results-window-body");
        body.add(buildTabBar(), buildPanels());
        return body;
    }

    private Component buildTabBar() {
        Div tabBar = new Div();
        tabBar.addClassName("tab-bar");
        for (ResultTab tab : ResultTab.values()) {
            tabBar.add(tabButton(tab));
        }
        return tabBar;
    }

    private Component buildPanels() {
        Div panels = new Div();
        panels.addClassName("tab-panels");
        panels.add(
                panel(ResultTab.SUMMARY, buildSummaryPanel()),
                panel(ResultTab.PLOT, buildPlotPanel()),
                panel(ResultTab.NETLIST, buildNetlistPanel()),
                panel(ResultTab.LOGS, buildLogsPanel()));
        return panels;
    }

    private Component buildSummaryPanel() {
        summaryPlaceholder.addClassNames("placeholder", "results-placeholder");

        Div wrap = new Div(summaryPlaceholder, resultsGrid);
        wrap.addClassName("table-wrap");
        return wrap;
    }

    private Component buildPlotPanel() {
        Div plotBox = new Div();
        plotBox.addClassName("plot-box");
        plotTitle.addClassName("plot-title");
        plotHint.addClassName("hint-box");
        plotMeta.addClassName("waveform-meta");
        traceSelect.addClassName("trace-select");
        plotViewport.addClassName("plot-viewport");
        plotEmptyState.addClassNames("hint-box", "plot-empty-state");
        waveformSvg.addClassName("waveform-svg");
        plotViewport.add(waveformSvg);
        plotBox.add(buildPlotToolbar(), plotHint, plotMeta, plotViewport, plotEmptyState);
        return plotBox;
    }

    private Component buildPlotToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("plot-toolbar");

        Span traceLabel = new Span("Ślad");
        traceLabel.addClassName("toolbar-label");

        Div traceGroup = new Div();
        traceGroup.addClassName("plot-trace-group");
        traceGroup.add(traceLabel, traceSelect);

        toolbar.add(plotTitle, traceGroup);
        return toolbar;
    }

    private Component buildNetlistPanel() {
        netlistBox.addClassName("netlist-box");
        return netlistBox;
    }

    private Component buildLogsPanel() {
        logList.addClassName("log-list");
        return logList;
    }

    private Div tabButton(ResultTab tab) {
        Div button = new Div();
        button.addClassName("tab-button");
        button.setText(tab.label());
        button.addClickListener(event -> setActiveTab(tab));
        tabButtons.put(tab, button);
        return button;
    }

    private Div panel(ResultTab tab, Component content) {
        Div panel = new Div();
        panel.addClassName("tab-panel");
        panel.add(content);
        tabPanels.put(tab, panel);
        return panel;
    }

    private void configureResultsGrid() {
        resultsGrid.addClassName("results-grid");
        resultsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        resultsGrid.setSelectionMode(Grid.SelectionMode.NONE);
        resultsGrid.setAllRowsVisible(true);
        resultsGrid.addColumn(WorkspaceMockService.ResultRow::time).setHeader("Czas").setAutoWidth(true).setFlexGrow(0);
        resultsGrid.addColumn(WorkspaceMockService.ResultRow::voltage).setHeader("Napięcie").setAutoWidth(true).setFlexGrow(0);
        resultsGrid.addColumn(WorkspaceMockService.ResultRow::current).setHeader("Prąd").setAutoWidth(true).setFlexGrow(0);
        resultsGrid.addColumn(WorkspaceMockService.ResultRow::note).setHeader("Uwagi").setFlexGrow(1);
    }

    private void configureTraceSelect() {
        traceSelect.setEnabled(false);
        traceSelect.setItemLabelGenerator(key -> {
            PlotTrace trace = availableTraces.get(key);
            return trace == null ? key : trace.label();
        });
        traceSelect.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                renderSelectedTrace(event.getValue());
            }
        });
    }

    private void setAvailableTraces(List<PlotTrace> plotTraces, String preferredTraceKey) {
        availableTraces.clear();
        if (plotTraces != null) {
            plotTraces.forEach(trace -> availableTraces.put(trace.key(), trace));
        }

        if (availableTraces.isEmpty()) {
            clearAvailableTraces();
            plotTitle.setText("Aktywny ślad: brak");
            plotHint.setText("Brak danych do narysowania przebiegu.");
            clearPlot("Brak danych do narysowania przebiegu.");
            return;
        }

        List<String> keys = List.copyOf(availableTraces.keySet());
        traceSelect.setItems(keys);
        traceSelect.setEnabled(keys.size() > 1);

        String selectedKey = preferredTraceKey != null && availableTraces.containsKey(preferredTraceKey)
                ? preferredTraceKey
                : keys.get(0);
        traceSelect.setValue(selectedKey);
        renderSelectedTrace(selectedKey);
    }

    private void clearAvailableTraces() {
        availableTraces.clear();
        traceSelect.clear();
        traceSelect.setItems(List.of());
        traceSelect.setEnabled(false);
    }

    private void renderSelectedTrace(String traceKey) {
        PlotTrace trace = availableTraces.get(traceKey);
        if (trace == null) {
            plotTitle.setText("Aktywny ślad: brak");
            plotHint.setText("Wybrany ślad nie jest dostępny w bieżących wynikach.");
            clearPlot("Wybrany ślad nie jest dostępny.");
            return;
        }

        plotTitle.setText(trace.title());
        plotHint.setText(trace.hint());
        renderPlot(trace.xValues(), trace.yValues(), trace.unit());
    }

    private String resolveSummaryPlaceholder(List<PlotTrace> plotTraces) {
        if (plotTraces == null || plotTraces.isEmpty()) {
            return "Brak wyników dla bieżącej symulacji.";
        }
        return "Brak tabelarycznych wyników dla tego widoku, ale dostępne są ślady w zakładce „Przebiegi”.";
    }

    private void clearPlot(String message) {
        plotMeta.setText("");
        waveformSvg.setSvg("");
        plotEmptyState.setText(message);
        plotEmptyState.setVisible(true);
    }

    private void renderPlot(double[] xValues, double[] yValues, String plotUnit) {
        if (xValues == null || yValues == null || xValues.length < 2 || xValues.length != yValues.length) {
            clearPlot("Brak danych do narysowania przebiegu.");
            return;
        }

        plotMeta.setText("Zakres czasu: "
                + formatNumber(xValues[0]) + " s -> " + formatNumber(xValues[xValues.length - 1]) + " s"
                + " | zakres sygnalu: " + formatNumber(min(yValues)) + " " + plotUnit
                + " -> " + formatNumber(max(yValues)) + " " + plotUnit);

        waveformSvg.setSvg(buildSvgMarkup(xValues, yValues));
        plotEmptyState.setVisible(false);
    }

    private String buildSvgMarkup(double[] xValues, double[] yValues) {
        double width = 760;
        double height = 240;
        double left = 48;
        double right = 18;
        double top = 14;
        double bottom = 28;
        double plotWidth = width - left - right;
        double plotHeight = height - top - bottom;
        double xMin = xValues[0];
        double xMax = xValues[xValues.length - 1];
        double yMin = min(yValues);
        double yMax = max(yValues);

        if (Math.abs(yMax - yMin) < 1.0e-9) {
            double padding = Math.max(1.0, Math.abs(yMax) * 0.1);
            yMin -= padding;
            yMax += padding;
        }

        StringBuilder svg = new StringBuilder()
                .append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append((int) width)
                .append(' ')
                .append((int) height)
                .append("\" preserveAspectRatio=\"none\">");

        svg.append(lineMarkup(left, top, left, top + plotHeight, "waveform-axis"));
        svg.append(lineMarkup(left, top + plotHeight, left + plotWidth, top + plotHeight, "waveform-axis"));

        for (int index = 0; index <= 4; index++) {
            double y = top + (plotHeight * index / 4.0);
            svg.append(lineMarkup(left, y, left + plotWidth, y, "waveform-grid"));
        }
        for (int index = 0; index <= 5; index++) {
            double x = left + (plotWidth * index / 5.0);
            svg.append(lineMarkup(x, top, x, top + plotHeight, "waveform-grid"));
        }

        if (yMin < 0 && yMax > 0) {
            double zeroY = top + plotHeight - ((0 - yMin) / (yMax - yMin) * plotHeight);
            svg.append(lineMarkup(left, zeroY, left + plotWidth, zeroY, "waveform-zero"));
        }

        StringBuilder points = new StringBuilder();
        for (int index = 0; index < xValues.length; index++) {
            double scaledX = left + normalize(xValues[index], xMin, xMax) * plotWidth;
            double scaledY = top + plotHeight - normalize(yValues[index], yMin, yMax) * plotHeight;
            if (index > 0) {
                points.append(' ');
            }
            points.append(formatNumber(scaledX)).append(',').append(formatNumber(scaledY));
        }

        svg.append("<polyline points=\"")
                .append(points)
                .append("\" fill=\"none\" stroke=\"#2f5f9b\" stroke-width=\"2.4\"")
                .append(" stroke-linecap=\"round\" stroke-linejoin=\"round\" />")
                .append("</svg>");
        return svg.toString();
    }

    private String lineMarkup(double x1, double y1, double x2, double y2, String className) {
        String style = switch (className) {
            case "waveform-axis" -> "stroke=\"#97a5b6\" stroke-width=\"1.2\"";
            case "waveform-zero" -> "stroke=\"#aabed8\" stroke-width=\"1.2\" stroke-dasharray=\"6 4\"";
            default -> "stroke=\"#d7dee8\" stroke-width=\"1\"";
        };
        return "<line "
                + style
                + " x1=\"" + formatNumber(x1)
                + "\" y1=\"" + formatNumber(y1)
                + "\" x2=\"" + formatNumber(x2)
                + "\" y2=\"" + formatNumber(y2)
                + "\" />";
    }

    private double min(double[] values) {
        double min = Double.POSITIVE_INFINITY;
        for (double value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private double max(double[] values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private double normalize(double value, double min, double max) {
        if (Math.abs(max - min) < 1.0e-9) {
            return 0.5;
        }
        return (value - min) / (max - min);
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static void setClass(Component component, String className, boolean enabled) {
        if (enabled) {
            component.addClassName(className);
        } else {
            component.removeClassName(className);
        }
    }

    public record PlotTrace(
            String key,
            String label,
            String title,
            String hint,
            double[] xValues,
            double[] yValues,
            String unit) {
    }
}
