#include "parser/netlist_parser.hpp"

namespace parser {

core::Circuit parse_netlist(const std::string &raw) {
    core::Circuit circuit;
    circuit.name       = "parsed_circuit";
    circuit.node_count = 0;

    if (raw.empty()) {
        return circuit;
    }

    return circuit;
}

} // namespace parser
