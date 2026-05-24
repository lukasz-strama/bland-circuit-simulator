package pl.polsl.bland.desktop.service;

import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.NetlistParser;
import pl.polsl.bland.models.SimulationRequest;

public class SimulationService {

    private final ApiClient api = new ApiClient();
    private final SimulationCsvService csv = new SimulationCsvService();
    private final NetlistParser netlistParser = new NetlistParser();

    public SimulationCsvService.ParsedSimulation simulate(
            CircuitSchematic schematic,
            SimulationRequest request
    ) throws Exception {

        String netlist = netlistParser.parse(schematic, request);

        String csvText = api.runNetlist(netlist);

        return csv.parse(csvText);
    }
}
