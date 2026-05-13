package pl.polsl.bland.desktop.view;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pl.polsl.bland.desktop.service.SimulationCsvService;

import java.util.*;

public class ResultsWindow {

    private final SimulationCsvService.ParsedSimulation sim;
    private final String netlist;

    public ResultsWindow(SimulationCsvService.ParsedSimulation sim, String netlist) {
        this.sim = sim;
        this.netlist = netlist;
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Wyniki symulacji");
        stage.initModality(Modality.NONE);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().addAll(
                buildRawTab(),
                buildTableTab(),
                buildPlotTab(),
                buildAveragesTab(),
                buildNetlistTab()
        );

        Scene scene = new Scene(tabPane, 900, 650);
        stage.setScene(scene);
        stage.show();
    }

    // --- Tab 1: surowe dane ---
    private Tab buildRawTab() {
        Tab tab = new Tab("Surowe dane");
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setStyle("-fx-font-family: monospace;");

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", sim.headers())).append("\n");
        double[] time = sim.timePoints();
        for (int i = 0; i < time.length; i++) {
            sb.append(time[i]);
            for (String h : sim.headers().subList(1, sim.headers().size())) {
                sb.append(",").append(sim.series().get(h)[i]);
            }
            sb.append("\n");
        }
        area.setText(sb.toString());
        tab.setContent(area);
        return tab;
    }

    // --- Tab 2: tabela ---
    private Tab buildTableTab() {
        Tab tab = new Tab("Tabela");

        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        for (String header : sim.headers()) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(header);
            col.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleStringProperty(
                            String.valueOf(data.getValue().get(header))));
            table.getColumns().add(col);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        double[] time = sim.timePoints();
        for (int i = 0; i < time.length; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(sim.headers().get(0), time[i]);
            for (String h : sim.headers().subList(1, sim.headers().size())) {
                row.put(h, sim.series().get(h)[i]);
            }
            rows.add(row);
        }
        table.setItems(FXCollections.observableArrayList(rows));
        tab.setContent(table);
        return tab;
    }

    // --- Tab 3: przebiegi SVG z selektorem ---
   private Tab buildPlotTab() {
    Tab tab = new Tab("Przebiegi");

    List<String> signals = sim.headers().subList(1, sim.headers().size());
    if (signals.isEmpty()) {
        tab.setContent(new Label("Brak danych do wyświetlenia."));
        return tab;
    }

    Canvas canvas = new Canvas(860, 300);
    Label metaLabel = new Label();
    metaLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");

    ComboBox<String> selector = new ComboBox<>();
    selector.getItems().addAll(signals);
    selector.setValue(signals.get(0));

    selector.valueProperty().addListener((obs, old, selected) -> {
        if (selected == null) return;
        String unit = resolveUnit(selected);
        metaLabel.setText(buildMeta(sim.timePoints(), sim.series().get(selected), unit));
        drawPlot(canvas, sim.timePoints(), sim.series().get(selected), selected, unit);
    });

    // inicjalne rysowanie
    String first = signals.get(0);
    String firstUnit = resolveUnit(first);
    metaLabel.setText(buildMeta(sim.timePoints(), sim.series().get(first), firstUnit));
    drawPlot(canvas, sim.timePoints(), sim.series().get(first), first, firstUnit);

    HBox toolbar = new HBox(8, new Label("Sygnał:"), selector);
    toolbar.setPadding(new Insets(8));
    toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

    VBox box = new VBox(4, toolbar, metaLabel, canvas);
    box.setPadding(new Insets(4));
    tab.setContent(box);
    return tab;
}

private String resolveUnit(String signalName) {
    String upper = signalName.toUpperCase();
    if (upper.startsWith("V(")) return "V";
    if (upper.startsWith("I(")) return "A";
    return "";
}

private String buildMeta(double[] x, double[] y, String unit) {
    double yMin = Arrays.stream(y).min().orElse(0);
    double yMax = Arrays.stream(y).max().orElse(0);
    return String.format("Zakres czasu: %.6g s - %.6g s  |  Zakres sygnału: %.6g %s - %.6g %s",
            x[0], x[x.length - 1], yMin, unit, yMax, unit);
}

private void drawPlot(Canvas canvas, double[] xValues, double[] yValues, String signalName, String unit) {
    javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
    double width = canvas.getWidth();
    double height = canvas.getHeight();
    double left = 80, right = 16, top = 16, bottom = 32;
    double plotW = width - left - right;
    double plotH = height - top - bottom;

    gc.clearRect(0, 0, width, height);

    double xMin = xValues[0];
    double xMax = xValues[xValues.length - 1];
    double yMin = Arrays.stream(yValues).min().orElse(0);
    double yMax = Arrays.stream(yValues).max().orElse(0);

    if (Math.abs(yMax - yMin) < 1e-9) {
        double pad = Math.max(1.0, Math.abs(yMax) * 0.1);
        yMin -= pad;
        yMax += pad;
    }

    // tło
    gc.setFill(javafx.scene.paint.Color.rgb(248, 249, 251));
    gc.fillRect(left, top, plotW, plotH);
    gc.setStroke(javafx.scene.paint.Color.rgb(205, 211, 219));
    gc.strokeRect(left, top, plotW, plotH);

    // siatka pozioma + etykiety Y
    gc.setStroke(javafx.scene.paint.Color.rgb(215, 222, 232));
    gc.setLineWidth(1);
    gc.setLineDashes(4, 4);
    gc.setFont(javafx.scene.text.Font.font(10));
    gc.setFill(javafx.scene.paint.Color.rgb(100, 110, 120));
    for (int i = 0; i <= 4; i++) {
        double y = top + plotH * i / 4.0;
        double val = yMax - (yMax - yMin) * i / 4.0;
        gc.strokeLine(left, y, left + plotW, y);
        gc.fillText(String.format("%.3g", val), 26, y + 4);
    }

    // siatka pionowa + etykiety X
    for (int i = 0; i <= 5; i++) {
        double x = left + plotW * i / 5.0;
        double val = xMin + (xMax - xMin) * i / 5.0;
        gc.strokeLine(x, top, x, top + plotH);
        gc.fillText(String.format("%.3g", val), x - 12, top + plotH + 14);
    }
    gc.setLineDashes(0);

    // oś zero
    if (yMin < 0 && yMax > 0) {
        double zeroY = top + plotH - normalize(0, yMin, yMax) * plotH;
        gc.setStroke(javafx.scene.paint.Color.rgb(170, 190, 221));
        gc.setLineDashes(6, 4);
        gc.strokeLine(left, zeroY, left + plotW, zeroY);
        gc.setLineDashes(0);
    }

    // osie
    gc.setStroke(javafx.scene.paint.Color.rgb(120, 143, 166));
    gc.setLineWidth(1.5);
    gc.strokeLine(left, top, left, top + plotH);
    gc.strokeLine(left, top + plotH, left + plotW, top + plotH);

    // etykiety osi
    gc.setFill(javafx.scene.paint.Color.rgb(60, 70, 80));
    gc.setFont(javafx.scene.text.Font.font(11));
    gc.fillText("Czas [s]", left + plotW / 2 - 20, height - 2);
    gc.save();
    gc.translate(10, top + plotH / 2);
    gc.rotate(-90);
    gc.fillText(signalName + " [" + unit + "]", -30, 0);
    gc.restore();

    // przebieg
    gc.setStroke(javafx.scene.paint.Color.rgb(47, 95, 155));
    gc.setLineWidth(2);
    gc.beginPath();
    for (int i = 0; i < xValues.length; i++) {
        double px = left + normalize(xValues[i], xMin, xMax) * plotW;
        double py = top + plotH - normalize(yValues[i], yMin, yMax) * plotH;
        if (i == 0) gc.moveTo(px, py);
        else gc.lineTo(px, py);
    }
    gc.stroke();
}

private double normalize(double value, double min, double max) {
    if (Math.abs(max - min) < 1e-9) return 0.5;
    return (value - min) / (max - min);
}

    // --- Tab 4: wartości średnie ---
    private Tab buildAveragesTab() {
        Tab tab = new Tab("Wartości średnie");

        TableView<Map<String, Object>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Map<String, Object>, String> colName = new TableColumn<>("Sygnał");
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().get("name"))));
        TableColumn<Map<String, Object>, String> colMin = new TableColumn<>("Min");
        colMin.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().get("min"))));
        TableColumn<Map<String, Object>, String> colMax = new TableColumn<>("Max");
        colMax.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().get("max"))));
        TableColumn<Map<String, Object>, String> colAvg = new TableColumn<>("Średnia");
        colAvg.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().get("avg"))));
        TableColumn<Map<String, Object>, String> colRms = new TableColumn<>("RMS");
        colRms.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().get("rms"))));

        table.getColumns().addAll(colName, colMin, colMax, colAvg, colRms);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String h : sim.headers().subList(1, sim.headers().size())) {
            double[] values = sim.series().get(h);
            double min = Arrays.stream(values).min().orElse(0);
            double max = Arrays.stream(values).max().orElse(0);
            double avg = Arrays.stream(values).average().orElse(0);
            double rms = Math.sqrt(Arrays.stream(values).map(v -> v * v).average().orElse(0));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", h);
            row.put("min", String.format("%.6g", min));
            row.put("max", String.format("%.6g", max));
            row.put("avg", String.format("%.6g", avg));
            row.put("rms", String.format("%.6g", rms));
            rows.add(row);
        }
        table.setItems(FXCollections.observableArrayList(rows));
        tab.setContent(table);
        return tab;
    }

    // --- Tab 5: netlist ---
    private Tab buildNetlistTab() {
        Tab tab = new Tab("Netlist");
        TextArea area = new TextArea(netlist == null ? "(brak netlisty)" : netlist);
        area.setEditable(false);
        area.setStyle("-fx-font-family: monospace;");
        tab.setContent(area);
        return tab;
    }
}