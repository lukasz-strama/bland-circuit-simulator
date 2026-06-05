package pl.polsl.bland.models;

import java.util.Locale;
import java.util.Map;
import java.util.List;

import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.models.SimulationRequest.AnalysisType;

/**
 * Parser generujący netlistę zgodną z ENGINE.CONTRACT.md.
 *
 * Odpowiada za:
 * - walidację schematu,
 * - mapowanie elementów domenowych na format netlisty,
 * - generowanie linii elementów (RES, CAP, IND, VSRC, ISRC),
 * - generowanie dyrektywy .SIMULATE,
 * - formatowanie wartości liczbowych zgodnie z wymaganiami silnika.
 *
 * Mapowanie typów:
 * R → RES  
 * L → IND  
 * C → CAP  
 * V → VSRC  
 * I → ISRC  
 */
public class NetlistParser {

 /**
     * Generuje pełną netlistę na podstawie schematu i parametrów symulacji.
     * Wykonuje:
     * <ul>
     *     <li>weryfikację poprawności schematu,</li>
     *     <li>budowę linii elementów,</li>
     *     <li>dodanie dyrektywy .SIMULATE.</li>
     * </ul>
     *
     * @param schematic schemat obwodu
     * @param request parametry symulacji (DC lub TRANSIENT)
     * @return tekst netlisty gotowy do wysłania do silnika
     * @throws NetlistParseException gdy schemat jest niepoprawny lub brakuje parametrów
     */
    public String parse(CircuitSchematic schematic, SimulationRequest request) throws NetlistParseException {

        validateSchematic(schematic);

        StringBuilder sb = new StringBuilder();

        sb.append("* Netlista wygenerowana automatycznie\n");
        sb.append("* Schemat: ").append(schematic.name()).append("\n");

        for (CircuitElement el : schematic.elements()) {
            String line = buildElementLine(el);
                if (line != null) sb.append(line).append("\n");
        }

        sb.append(buildSimulateDirective(request)).append("\n");

        return sb.toString();
    }

/**
     * Tworzy pojedynczą linię netlisty opisującą element.
     * Pomija element GND (silnik nie wymaga linii dla masy).
     *
     * @param element element obwodu
     * @return linia netlisty lub null dla GND
     * @throws NetlistParseException gdy węzły są niepoprawne
     */
    private String buildElementLine(CircuitElement element) throws NetlistParseException{

          if (element.type() == CircuitElement.ElementType.GND) {
            return null;
        }

        String node1 = sanitizeNode(element.node1(), element.id(), "node1");
        String node2 = sanitizeNode(element.node2(), element.id(), "node2");

        return switch (element.type()){
            case R -> passive("RES", element, node1, node2);
            case L -> passive("IND", element, node1, node2);
            case C -> passive("CAP", element, node1, node2);
            case V -> source("VSRC", element, node1, node2);
            case I -> source("ISRC", element, node1, node2);
            case GND -> null;    
        };
    }

 /**
     * Buduje linię netlisty dla elementów pasywnych (R, L, C).
     *
     * @param engineType nazwa typu w silniku (RES, IND, CAP)
     * @param element element obwodu
     * @param node1 pierwszy węzeł
     * @param node2 drugi węzeł
     * @return linia netlisty
     */
    private String passive(String engineType, CircuitElement element, String node1, String node2){

            return "%s %s %s %s val=%s".formatted(engineType, element.id(), node1, node2, fmt(element.value()));

    }

 /**
     * Buduje linię netlisty dla źródeł napięcia i prądu.
     * Obsługuje:
     * <ul>
     *     <li>typ źródła (dc/sine),</li>
     *     <li>częstotliwość dla sygnałów sinusoidalnych.</li>
     * </ul>
     *
     * @param engineType VSRC lub ISRC
     * @param el element źródła
     * @param node1 pierwszy węzeł
     * @param node2 drugi węzeł
     * @return linia netlisty
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
     * Generuje dyrektywę .SIMULATE na podstawie typu analizy.
     *
     * @param request parametry symulacji
     * @return linia .SIMULATE
     * @throws NetlistParseException gdy brakuje parametrów tstop/tstep
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
     * Waliduje minimalną poprawność schematu:
     * <ul>
     *     <li>schemat nie jest pusty,</li>
     *     <li>zawiera masę (element GND lub węzeł 0).</li>
     * </ul>
     *
     * @param schematic schemat obwodu
     * @throws NetlistParseException gdy schemat jest niepoprawny
     */
    private void validateSchematic(CircuitSchematic schematic) throws NetlistParseException {
        if(schematic == null) {
            throw new NetlistParseException("Brak schematu");
        }

        List<CircuitElement> elements = schematic.elements();

        if(elements == null || elements.isEmpty()){
            throw new NetlistParseException("Brak elementów na schemacie");
        }

        boolean hasGround = elements.stream().anyMatch(el ->
                el.type() == CircuitElement.ElementType.GND
                || "0".equals(el.node1())
                || "0".equals(el.node2()));
 

        if(!hasGround){
            throw new NetlistParseException("Brak masy na schemacie");
        }
    }

/**
     * Pobiera wymagany parametr analizy (np. tstop, tstep).
     *
     * @param params mapa parametrów
     * @param key nazwa parametru
     * @param type typ analizy
     * @return wartość parametru
     * @throws NetlistParseException gdy parametr nie istnieje
     */
    private double requireParam(Map<String, Double> params, String key, AnalysisType type) throws NetlistParseException{
        if(params == null || !params.containsKey(key)) {
            throw new NetlistParseException("Analiza wymaga %s parametry \"%s\".".formatted(type, key));
        }

        return params.get(key);
    }

  /**
     * Normalizuje nazwę węzła:
     * <ul>
     *     <li>usuwa spacje,</li>
     *     <li>sprawdza null/empty,</li>
     *     <li>zastępuje spacje podkreślnikami.</li>
     * </ul>
     *
     * @param node nazwa węzła
     * @param elementId identyfikator elementu
     * @param fieldName nazwa pola (node1/node2)
     * @return poprawna nazwa węzła
     * @throws NetlistParseException gdy węzeł jest pusty
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
     * <ul>
     *     <li>kropka dziesiętna (Locale.US),</li>
     *     <li>notacja naukowa dla bardzo małych/dużych wartości,</li>
     *     <li>usuwanie zbędnych zer.</li>
     * </ul>
     *
     * @param v wartość liczbowa
     * @return sformatowana wartość
     */
    private String fmt(double v) {
            if (v != 0.0 && (Math.abs(v) < 1e-3 || Math.abs(v) >= 1e6)) {
                return String.format(Locale.US, "%e", v);
            }
            String s = String.format(Locale.US, "%.10f", v).replaceAll("0+$", "");
            return s.endsWith(".") ? s + "0" : s;
        }


/**
     * Wyjątek zgłaszany przy błędach walidacji lub generowania netlisty.
     */
    public static class NetlistParseException extends Exception {
        public NetlistParseException (String message){
            super(message);
        }
    }
    
}
