package pl.polsl.bland.desktop.service;

import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;

public class SimulationService {

    private final ApiClient api = new ApiClient();
    private final SimulationCsvService csv = new SimulationCsvService();

    public SimulationCsvService.ParsedSimulation simulate(
            CircuitSchematic schematic,
            SimulationRequest request
    ) throws Exception {

        String netlist = NetlistBuilder.build(schematic, request);

        String csvText = api.runNetlist(netlist);

        return csv.parse(csvText);
    }
}
