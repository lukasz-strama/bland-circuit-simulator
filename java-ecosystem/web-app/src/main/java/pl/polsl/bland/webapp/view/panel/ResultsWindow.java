package pl.polsl.bland.webapp.view.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import pl.polsl.bland.webapp.service.WorkspaceMockService;

public final class ResultsWindow extends Div {
    private final Span caption = new Span();
    private final Span analysisBadge = new Span();
    private final Span plotTitle = new Span();
    private final Div summaryTable = new Div();
    private final Div plotHint = new Div();
    private final Pre netlistBox = new Pre();
    private final Div logList = new Div();

    public ResultsWindow() {
        addClassNames("floating-window", "is-results-window");
        add(buildTitleBar(), buildBody());
    }

    public void update(WorkspaceMockService.ElementDetails details) {
        caption.setText("Aktywny element: " + details.id() + " / " + details.typeLabel());
        analysisBadge.setText(details.analysisLabel());
        plotTitle.setText("Aktywny ślad: " + details.traceName());
        plotHint.setText(details.simulationNote());
        netlistBox.setText(details.netlist());
        logList.removeAll();
        details.logs().forEach(line -> logList.add(new Div(new Text(line))));
        summaryTable.getElement().setProperty("innerHTML", renderTable(details.rows()));
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
        titleBar.add(textGroup, analysisBadge);
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
        tabBar.add(
                tabButton("Wyniki symulacji", true),
                tabButton("Przebiegi", false),
                tabButton("Netlista", false),
                tabButton("Logi", false));
        return tabBar;
    }

    private Component buildPanels() {
        Div panels = new Div();
        panels.addClassName("tab-panels");
        panels.add(
                panel(buildSummaryPanel(), true),
                panel(buildPlotPanel(), false),
                panel(buildNetlistPanel(), false),
                panel(buildLogsPanel(), false));
        return panels;
    }

    private Component buildSummaryPanel() {
        summaryTable.addClassName("table-wrap");
        return summaryTable;
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

    private Div tabButton(String text, boolean active) {
        Div button = new Div();
        button.addClassName("tab-button");
        if (active) {
            button.addClassName("is-active");
        }
        button.setText(text);
        return button;
    }

    private Div panel(Component content, boolean active) {
        Div panel = new Div();
        panel.addClassName("tab-panel");
        if (active) {
            panel.addClassName("is-active");
        }
        panel.add(content);
        return panel;
    }

    private String renderTable(java.util.List<WorkspaceMockService.ResultRow> rows) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <table aria-label="Tabela wyników">
                  <thead>
                    <tr>
                      <th>Czas</th>
                      <th>Napięcie</th>
                      <th>Prąd</th>
                      <th>Uwagi</th>
                    </tr>
                  </thead>
                  <tbody>
                """);

        for (WorkspaceMockService.ResultRow row : rows) {
            html.append("<tr>")
                    .append("<td>").append(row.time()).append("</td>")
                    .append("<td>").append(row.voltage()).append("</td>")
                    .append("<td>").append(row.current()).append("</td>")
                    .append("<td>").append(row.note()).append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody></table>");
        return html.toString();
    }
}
