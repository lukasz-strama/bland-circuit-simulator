package pl.polsl.bland.models;

import java.util.Locale;
import java.util.Map;
import java.util.List;

import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.models.SimulationRequest.AnalysisType;

/**
* Parser netlisty zgodnej z ENGINE.CONTRACT.md
* Mapowanie typów: R - RES, L - IND, C - CAP, V - VSRC, I - ISRC
*/

public class NetlistParser {

/**
 * Generuje pełną netlistę na podstawie schematu i parametrów symulacji.
 * Waliduje schemat, buduje linie elementów i dodaje dyrektywę .SIMULATE.
 */

    public String parse(CircuitSchematic schematic, SimulationRequest request) throws NetlistParseException {

        validateSchematic(schematic);

        StringBuilder sb = new StringBuilder();

        sb.append("* Netlista wygenerowana automatycznie\n");
        sb.append("* Schemat: ").append(schematic.name()).append("\n");

        for (CircuitElement el : schematic.elements()) {
            sb.append(buildElementLine(el)).append("\n");
        }

        sb.append(buildSimulateDirective(request)).append("\n");

        return sb.toString();
    }

    // Tworzy pojedynczą 

/**
 * Tworzy pojedynczą linię netlisty opisującą element (RES, CAP, IND, VSRC, ISRC).
 * Sprawdza poprawność węzłów i deleguje do metod passive() lub source().
 */

    private String buildElementLine(CircuitElement element) throws NetlistParseException{
        String node1 = sanitizeNode(element.node1(), element.id(), "node1");
        String node2 = sanitizeNode(element.node2(), element.id(), "node2");

        return switch (element.type()){
            case R -> passive("RES", element, node1, node2);
            case L -> passive("IND", element, node1, node2);
            case C -> passive("CAP", element, node1, node2);
            case V -> passive("VSRC", element, node1, node2);
            case I -> passive("ISRC", element, node1, node2);
        };
    }

 /**
 * Buduje linię netlisty dla elementów pasywnych (R, L, C)
 */

    private String passive(String engineType, CircuitElement element, String node1, String node2){

            return "%s %s %s %s val=%s".formatted(engineType, element.id(), node1, node2, fmt(element.value()));

    }

/**
 * Buduje linię netlisty dla źródeł napięcia/prądu (VSRC, ISRC)
 * Obsługuje typ źródła (dc/sine) oraz częstotliwość dla sygnałów sinusoidalnych
 */

     private String source(String engineType, CircuitElement el, String node1, String node2) {
        String srcType = (el.sourceType() != null)
                ? el.sourceType().toLowerCase(Locale.ROOT)
                : "dc";

        String line = "%s %s %s %s type=%s val=%s"
                .formatted(engineType, el.id(), node1, node2, srcType, fmt(el.value()));

        if ("sine".equals(srcType) && el.frequency() != null) {
            line += " freq=" + fmt(el.frequency());
        }

        return line;
    }

    /**
 * Generuje dyrektywę .SIMULATE na podstawie typu analizy (DC lub TRANSIENT)
 * Dla analizy transient wymaga parametrów tstop i tstep
 */


    private String buildSimulateDirective (SimulationRequest request) throws NetlistParseException{
        
        if(request == null || request.analysisType() == null){
            throw new NetlistParseException("Brak typu analizy w Simulation Request");
        }
        return switch(request.analysisType()){
            case DC -> ".SIMULATE type=dc";

            case TRANSIENT -> {
                Map<String, Double> p = request.parameters();
                double tstop = requireParam(p, "tstop", AnalysisType.TRANSIENT);
                double tstep = requireParam(p, "tstep", AnalysisType.TRANSIENT);
                yield ".SIMULATE type=trans tstop=%s tstep=%s".formatted(fmt(tstop), fmt(tstep));
            }
        };
    }
    
/**
 * Sprawdza minimalną poprawność schematu:
 * - schemat nie jest pusty,
 * - istnieje węzeł masy "0".
 */

    private void validateSchematic(CircuitSchematic schematic) throws NetlistParseException {
        if(schematic == null) {
            throw new NetlistParseException("Brak schematu");
        }

        List<CircuitElement> elements = schematic.elements();

        if(elements == null || elements.isEmpty()){
            throw new NetlistParseException("Brak elementów na schemacie");
        }

        boolean hasGround = elements.stream().anyMatch(el -> "0".equals(el.node1()) || "0".equals(el.node2()));

        if(!hasGround){
            throw new NetlistParseException("Brak masy na schemacie");
        }
    }

/**
 * Pobiera wymagany parametr analizy (np. tstop, tstep)
 * Rzuca wyjątek, jeśli parametr nie istnieje
 */

    private double requireParam(Map<String, Double> params, String key, AnalysisType type) throws NetlistParseException{
        if(params == null || !params.containsKey(key)) {
            throw new NetlistParseException("Analiza wymaga %s parametry \"%s\".".formatted(type, key));
        }

        return params.get(key);
    }

/**
 * Waliduje i normalizuje nazwę węzła (usuwa spacje, sprawdza null/empty)
 */

    private String sanitizeNode(String node, String elementId, String fieldName) throws NetlistParseException {
        if (node == null || node.isBlank()) {
            throw new NetlistParseException(
                    "Element \"%s\" ma pusty węzeł \"%s\".".formatted(elementId, fieldName));
        }
        return node.trim().replace(' ', '_');
    }

/**
 * Formatuje liczby double zgodnie z wymaganiami silnika:
 * - kropka dziesiętna (Locale.US),
 * - notacja naukowa dla bardzo małych/dużych wartości
 */


    private String fmt(double v) {
            if (v != 0.0 && (Math.abs(v) < 1e-3 || Math.abs(v) >= 1e6)) {
                return String.format(Locale.US, "%e", v);
            }
            String s = String.format(Locale.US, "%.10f", v).replaceAll("0+$", "");
            return s.endsWith(".") ? s + "0" : s;
        }


/**
 * Wyjątek zgłaszany przy błędach walidacji lub generowania netlisty
 */


    public static class NetlistParseException extends Exception {
        public NetlistParseException (String message){
            super(message);
        }
    }
    
}
