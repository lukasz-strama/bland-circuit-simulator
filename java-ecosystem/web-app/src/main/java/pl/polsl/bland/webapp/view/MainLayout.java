package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

@Route("")
@PageTitle("Bland Circuit Simulator")
public class MainLayout extends Div {

    public MainLayout() {
        addClassName("main-view");
        setSizeFull();
        add(buildAppShell());
    }

    private Div buildAppShell() {
        Div appShell = new Div();
        appShell.addClassName("app-shell");
        appShell.add(
                buildMenuBar(),
                buildToolbar(),
                buildComponentBar(),
                buildEditorShell(),
                buildStatusBar());
        return appShell;
    }

    private Div buildMenuBar() {
        Div menuBar = new Div();
        menuBar.addClassName("menubar");
        menuBar.add(
                buildMenuButton("Plik"),
                buildMenuButton("Edycja"),
                buildMenuButton("Widok"),
                buildMenuButton("Wstaw"),
                buildMenuButton("Narzędzia"),
                buildMenuButton("Symulacja"),
                buildMenuButton("Pomoc"));

        Div meta = new Div();
        meta.addClassName("menubar-meta");
        meta.add(new Span("Projekt: filtr_rlc_lab.asc"));
        meta.add(new Span("Arkusz 1 / 1"));
        menuBar.add(meta);
        return menuBar;
    }

    private Div buildMenuButton(String label) {
        Div group = new Div();
        group.addClassName("menu-group");

        Span button = new Span(label);
        button.addClassName("menu-button");
        group.add(button);
        return group;
    }

    private Div buildToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("toolbar");

        toolbar.add(buildToolbarGroup(
                createAction("Nowy", "tool-button"),
                createAction("Zapisz", "tool-button"),
                createAction("Eksport", "tool-button"),
                createAction("Symuluj", "tool-button", "is-primary"),
                createAction("Pokaż wyniki", "tool-button")));

        toolbar.add(separator());

        Select<String> analysis = new Select<>();
        analysis.setItems("Analiza przejściowa", "Punkt pracy DC");
        analysis.setValue("Analiza przejściowa");
        analysis.addClassName("toolbar-select");
        toolbar.add(buildToolbarGroup(
                createLabel("Analiza"),
                analysis));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(
                createLabel("Narzędzie"),
                createReadout("Zaznacz")));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(
                createAction("-", "mini-button"),
                createReadout("92%"),
                createAction("+", "mini-button"),
                createAction("Dopasuj", "mini-button"),
                createAction("100%", "mini-button")));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(buildStatusBadge()));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(createAction("?", "icon-button")));
        return toolbar;
    }

    private Div buildComponentBar() {
        Div componentBar = new Div();
        componentBar.addClassName("componentbar");

        componentBar.add(buildComponentGroup(
                createAction("Komponent...", "tool-button"),
                createLabel("Biblioteka")));

        componentBar.add(buildComponentGroup(
                createQuickComponent("R", "Rezystor", true),
                createQuickComponent("C", "Kondensator", false),
                createQuickComponent("L", "Cewka", false),
                createQuickComponent("V", "Źródło", false),
                createQuickComponent("0", "Masa", false),
                createQuickComponent("D", "Dioda", false),
                createQuickComponent("OP", "Wzmacniacz", false)));

        TextField search = new TextField();
        search.setPlaceholder("Szukaj w bibliotece: rezystor, dioda, opamp...");
        search.addClassName("component-search");
        search.setClearButtonVisible(true);

        Div fillGroup = buildComponentGroup(search, createAction("Otwórz bibliotekę", "mini-button"));
        fillGroup.addClassName("is-fill");
        componentBar.add(fillGroup);

        componentBar.add(buildComponentGroup(
                createLabel("Aktywny symbol"),
                createWideReadout("R / Rezystor")));
        return componentBar;
    }

    private Div buildEditorShell() {
        Div editorShell = new Div();
        editorShell.addClassName("editor-shell");
        editorShell.add(buildToolRail(), buildWorkspacePanel());
        return editorShell;
    }

    private Div buildToolRail() {
        Div toolRail = new Div();
        toolRail.addClassName("tool-rail");
        toolRail.add(
                createAction("Z", "rail-button", "is-active"),
                createAction("W", "rail-button"),
                createAction("S", "rail-button"),
                createAction("X", "rail-button"),
                createAction("D", "rail-button"));
        return toolRail;
    }

    private Div buildWorkspacePanel() {
        Div workspacePanel = new Div();
        workspacePanel.addClassName("workspace-panel");
        workspacePanel.getElement().setAttribute("data-tool", "select");

        Div workspaceViewport = new Div();
        workspaceViewport.addClassName("workspace-viewport");

        Div sheetStage = new Div();
        sheetStage.addClassName("sheet-stage");
        sheetStage.getElement().setProperty("innerHTML", SchematicPreviewFactory.render());

        workspaceViewport.add(sheetStage);
        workspacePanel.add(workspaceViewport, buildPropertiesWindow(), buildResultsWindow());
        return workspacePanel;
    }

    private Div buildPropertiesWindow() {
        Div propertiesWindow = new Div();
        propertiesWindow.addClassNames("floating-window");
        propertiesWindow.getElement().setProperty("innerHTML", """
                <div class="window-titlebar">
                  <div>
                    <div class="window-title">Właściwości elementu</div>
                    <div class="window-caption">Aktywny element: R1</div>
                  </div>
                  <div class="window-close">x</div>
                </div>
                <div class="window-body">
                  <div class="panel-block">
                    <div class="section-title">Parametry</div>
                    <dl class="property-grid">
                      <dt>ID elementu</dt><dd>R1</dd>
                      <dt>Typ</dt><dd>Rezystor</dd>
                      <dt>Wartość</dt><dd>120 Ohm</dd>
                      <dt>Węzeł A</dt><dd>IN</dd>
                      <dt>Węzeł B</dt><dd>N001</dd>
                      <dt>Orientacja</dt><dd>Poziomo</dd>
                      <dt>Opis</dt><dd>Rezystor wejściowy ograniczający prąd.</dd>
                    </dl>
                  </div>
                  <div class="panel-block">
                    <div class="section-title">Dane po symulacji</div>
                    <div class="hint-box">Dane nie są jeszcze dostępne. Uruchom symulację, a następnie kliknij element, aby odczytać jego ślad i statystyki.</div>
                  </div>
                </div>
                """);
        return propertiesWindow;
    }

    private Div buildResultsWindow() {
        Div resultsWindow = new Div();
        resultsWindow.addClassNames("floating-window", "is-results-window");
        resultsWindow.getElement().setProperty("innerHTML", """
                <div class="window-titlebar">
                  <div>
                    <div class="window-title">Wyniki symulacji</div>
                    <div class="window-caption">Aktywny element: R1 / Rezystor</div>
                  </div>
                  <div class="badge badge-inline">Analiza przejściowa</div>
                </div>
                <div class="results-window-body">
                  <div class="tab-bar">
                    <div class="tab-button is-active">Wyniki symulacji</div>
                    <div class="tab-button">Przebiegi</div>
                    <div class="tab-button">Netlista</div>
                    <div class="tab-button">Logi</div>
                  </div>
                  <div class="tab-panels">
                    <div class="tab-panel is-active">
                      <div class="placeholder">Brak wygenerowanych wyników. Otwórz ustawienia symulacji i uruchom profil, aby wypełnić tabele, przebiegi i statystyki.</div>
                    </div>
                    <div class="tab-panel">
                      <div class="plot-box">
                        <div class="plot-title">Aktywny ślad: I(R1)</div>
                        <div class="hint-box">Podgląd przebiegu pojawi się po integracji z backendem i silnikiem.</div>
                      </div>
                    </div>
                    <div class="tab-panel">
                      <div class="netlist-box">* netlista pojawi się tutaj po spięciu z backendem</div>
                    </div>
                    <div class="tab-panel">
                      <div class="log-list">
                        <div>System gotowy do pracy.</div>
                        <div>Silnik C++ oczekuje na integrację z backendem.</div>
                        <div>Warstwa web przechodzi z mockupu do implementacji Vaadin.</div>
                      </div>
                    </div>
                  </div>
                </div>
                """);
        return resultsWindow;
    }

    private Div buildStatusBar() {
        Div statusBar = new Div();
        statusBar.addClassName("statusbar");
        statusBar.add(
                createStatusSegment("Arkusz", "1"),
                createStatusSegment("Narzędzie", "Zaznacz"),
                createStatusSegment("Analiza", "Analiza przejściowa"),
                createStatusSegment("Symulacja", "Brak uruchomienia"),
                createStretchStatusSegment("Komunikat", "Makieta UI jest gotowa. Następny krok: mockowana interakcja."),
                createStatusSegment("Zoom", "92%"));
        return statusBar;
    }

    private Div buildToolbarGroup(com.vaadin.flow.component.Component... children) {
        Div group = new Div();
        group.addClassName("toolbar-group");
        group.add(children);
        return group;
    }

    private Div buildComponentGroup(com.vaadin.flow.component.Component... children) {
        Div group = new Div();
        group.addClassName("componentbar-group");
        group.add(children);
        return group;
    }

    private Div separator() {
        Div separator = new Div();
        separator.addClassName("toolbar-separator");
        return separator;
    }

    private Span createAction(String text, String... classNames) {
        Span action = new Span(text);
        action.addClassNames(classNames);
        return action;
    }

    private Span createLabel(String text) {
        Span label = new Span(text);
        label.addClassName("toolbar-label");
        return label;
    }

    private Span createReadout(String text) {
        Span readout = new Span(text);
        readout.addClassName("toolbar-readout");
        return readout;
    }

    private Span createWideReadout(String text) {
        Span readout = createReadout(text);
        readout.addClassName("is-wide");
        return readout;
    }

    private Div buildStatusBadge() {
        Div badge = new Div();
        badge.addClassName("badge");
        Span dot = new Span();
        dot.addClassNames("status-dot", "is-idle");
        badge.add(dot, new Text("Brak wyników"));
        return badge;
    }

    private Div createQuickComponent(String glyph, String label, boolean active) {
        Div component = new Div();
        component.addClassName("quick-component");
        if (active) {
            component.addClassName("is-active");
        }

        Span symbol = new Span(glyph);
        symbol.addClassName("quick-component-glyph");
        component.add(symbol, new Span(label));
        return component;
    }

    private Div createStatusSegment(String label, String value) {
        Div segment = new Div();
        segment.addClassName("status-segment");
        segment.add(new Span(label), new Span(value));
        return segment;
    }

    private Div createStretchStatusSegment(String label, String value) {
        Div segment = createStatusSegment(label, value);
        segment.addClassName("is-stretch");
        return segment;
    }
}
