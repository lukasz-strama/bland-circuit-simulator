package pl.polsl.bland.desktop.view;

import pl.polsl.bland.desktop.service.NetlistBuilder;
import pl.polsl.bland.desktop.service.SimulationCsvService;
import pl.polsl.bland.desktop.service.SimulationService;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.desktop.service.NetlistBuilder;

import java.util.Map;

public class MainController {

    private final SimulationService simulationService = new SimulationService();
    private final MainView view;

    public MainController(MainView view) {
        this.view = view;
    }

    public void onSimulateClicked() {
    try {
        CircuitSchematic schematic = view.buildCurrentSchematic();

        SimulationRequest request = new SimulationRequest(
                null,
                SimulationRequest.AnalysisType.TRANSIENT,
                Map.of("tstop", 0.008, "tstep", 0.0001)
        );

        String netlist = NetlistBuilder.build(schematic, request);

        SimulationCsvService.ParsedSimulation result =
                simulationService.simulate(schematic, request);

        view.showResults(result, netlist);  // <-- dodany netlist

    } catch (Exception e) {
        e.printStackTrace();
        //view.showError("Błąd symulacji: " + e.getMessage());  // <-- view.showError
    }
}
}