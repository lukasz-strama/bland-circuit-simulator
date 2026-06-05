package pl.polsl.bland.desktop.service;

import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.NetlistParser;
import pl.polsl.bland.models.SimulationRequest;

/**
 * Serwis odpowiedzialny za wykonanie symulacji.
 *
 * Łączy trzy etapy:
 * 1. Generowanie netlisty (NetlistParser)
 * 2. Wysyłanie jej do silnika symulacyjnego (ApiClient)
 * 3. Parsowanie wyniku CSV (SimulationCsvService)
 *
 * Klasa nie zawiera logiki symulacji.
 */
public class SimulationService {

    private final ApiClient api = new ApiClient();
    private final SimulationCsvService csv = new SimulationCsvService();
    private final NetlistParser netlistParser = new NetlistParser();

    /**
     * Wykonuje pełną symulację schematu.
     *
     * @param schematic schemat obwodu
     * @param request parametry symulacji (DC / TRANSIENT)
     * @return wynik symulacji w postaci obiektu ParsedSimulation
     * @throws Exception gdy generowanie netlisty lub komunikacja z silnikiem zakończy się błędem
     */
    public SimulationCsvService.ParsedSimulation simulate(
            CircuitSchematic schematic,
            SimulationRequest request
    ) throws Exception {

        String netlist = netlistParser.parse(schematic, request);

        String csvText = api.runNetlist(netlist);

        return csv.parse(csvText);
    }
}
