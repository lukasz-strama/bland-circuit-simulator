package pl.polsl.bland.desktop.service;

import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;

public class NetlistBuilder {

    public static String build(CircuitSchematic schematic, SimulationRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("* Bland Circuit Simulator / desktop\n");
        sb.append("* name=").append(schematic.name()).append("\n");

        for (CircuitElement el : schematic.elements()) {
            if (el.id().startsWith("GND")) continue;
            sb.append(formatElement(el)).append("\n");
        }

        sb.append(formatDirective(request));

        return sb.toString();
    }

    private static String formatElement(CircuitElement el) {
        return switch (el.type()) {
            case R -> "RES " + el.id() + " " + el.node1() + " " + el.node2() + " val=" + el.value();
            case L -> "IND " + el.id() + " " + el.node1() + " " + el.node2() + " val=" + el.value();
            case C -> "CAP " + el.id() + " " + el.node1() + " " + el.node2() + " val=" + el.value();
            case V -> {
            boolean flipped = el.rotation() == 180 || el.rotation() == 90;
            String n1 = flipped ? el.node2() : el.node1();
            String n2 = flipped ? el.node1() : el.node2();
            yield "VSRC " + el.id() + " " + n1 + " " + n2 + " type=dc val=" + el.value();
        }
        case I -> {
            boolean flipped = el.rotation() == 180 || el.rotation() == 90;
            String n1 = flipped ? el.node2() : el.node1();
            String n2 = flipped ? el.node1() : el.node2();
            yield "ISRC " + el.id() + " " + n1 + " " + n2 + " type=dc val=" + el.value();
        }
        default -> throw new IllegalArgumentException("Unknown element type: " + el.type());
        };
    }

    private static String formatDirective(SimulationRequest request) {
        double tstop = request.parameters().getOrDefault("tstop", 0.008);
        double tstep = request.parameters().getOrDefault("tstep", 0.0001);
        return ".SIMULATE type=trans tstop=" + tstop + " tstep=" + tstep;
    }
}
