package pl.polsl.bland.webapp.view.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import pl.polsl.bland.webapp.service.WorkspaceMockService;

import java.util.EnumMap;
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
    private final Pre netlistBox = new Pre();
    private final Div logList = new Div();
    private final Map<ResultTab, Div> tabButtons = new EnumMap<>(ResultTab.class);
    private final Map<ResultTab, Div> tabPanels = new EnumMap<>(ResultTab.class);
    private Runnable closeHandler = () -> {};
    private ResultTab activeTab = ResultTab.SUMMARY;

    public ResultsWindow() {
        addClassNames("floating-window", "is-results-window");
        configureResultsGrid();
        closeButton.addClickListener(event -> closeHandler.run());
        add(buildTitleBar(), buildBody());
        setActiveTab(ResultTab.SUMMARY);
    }

    public void update(WorkspaceMockService.ElementDetails details, String analysisLabel, boolean simulationReady) {
        caption.setText("Aktywny element: " + details.id() + " / " + details.typeLabel());
        analysisBadge.setText(analysisLabel);
        plotTitle.setText("Aktywny ślad: " + details.traceName());
        plotHint.setText(simulationReady
                ? details.simulationNote()
                : "Uruchom symulację, aby odblokować przebiegi i statystyki dla " + details.id() + ".");
        netlistBox.setText(details.netlist());
        resultsGrid.setItems(details.rows());
        logList.removeAll();
        details.logs().forEach(line -> logList.add(new Div(new Text(line))));
        summaryPlaceholder.setText("Brak wyników dla " + details.id() + ". Kliknij „Symuluj”, aby wypełnić zakładkę danymi.");
        summaryPlaceholder.setVisible(!simulationReady);
        resultsGrid.setVisible(simulationReady);
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
        plotBox.add(plotTitle, plotHint);
        return plotBox;
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

    private static void setClass(Component component, String className, boolean enabled) {
        if (enabled) {
            component.addClassName(className);
        } else {
            component.removeClassName(className);
        }
    }
}
