#include "parser/netlist_parser.hpp"

#include <algorithm>
#include <cctype>
#include <sstream>
#include <unordered_map>

#include "core/errors.hpp"
#include "utils/logger.hpp"

namespace parser {
namespace {

std::string to_lower(std::string s) {
    std::transform(s.begin(), s.end(), s.begin(),
                   [](unsigned char c) { return std::tolower(c); });
    return s;
}

std::unordered_map<std::string, std::string>
parse_kv(const std::vector<std::string> &tokens, std::size_t start) {
    std::unordered_map<std::string, std::string> kv;
    for (std::size_t i = start; i < tokens.size(); ++i) {
        auto eq = tokens[i].find('=');
        if (eq == std::string::npos)
            continue;
        kv[to_lower(tokens[i].substr(0, eq))] = tokens[i].substr(eq + 1);
    }
    return kv;
}

std::size_t
get_or_create_node(const std::string                            &name,
                   std::unordered_map<std::string, std::size_t> &node_map,
                   std::vector<std::string>                     &node_names) {
    if (name == "0")
        return core::GROUND_NODE;

    auto it = node_map.find(name);
    if (it != node_map.end())
        return it->second;

    std::size_t idx = node_names.size();
    node_map[name]  = idx;
    node_names.push_back(name);
    return idx;
}

double require_val(const std::unordered_map<std::string, std::string> &kv,
                   const std::string &component_name) {
    auto it = kv.find("val");
    if (it == kv.end())
        throw core::ParseError("Missing parameter 'val' in element " +
                               component_name);
    return std::stod(it->second);
}

} // namespace

core::Circuit parse_netlist(const std::string &raw) {
    core::Circuit circuit;
    circuit.name = "parsed_circuit";

    LOG_INFO("Parsing netlist (" + std::to_string(raw.size()) + " bytes)");

    if (raw.empty()) {
        LOG_WARN("Empty netlist received");
        return circuit;
    }

    std::unordered_map<std::string, std::size_t> node_map;
    std::istringstream                           stream(raw);
    std::string                                  line;

    while (std::getline(stream, line)) {
        if (line.empty())
            continue;

        // Trim leading whitespace
        auto first_non_space = line.find_first_not_of(" \t");
        if (first_non_space == std::string::npos)
            continue;
        if (first_non_space != 0)
            line = line.substr(first_non_space);

        if (line[0] == '*')
            continue;
        if (line[0] == '.')
            continue;

        std::istringstream       ls(line);
        std::vector<std::string> tokens;
        std::string              tok;
        while (ls >> tok)
            tokens.push_back(tok);

        if (tokens.size() < 4)
            throw core::ParseError(
                "Malformed line (need at least TYPE NAME NODE_A NODE_B): " +
                line);

        std::string type = to_lower(tokens[0]);
        std::string name = tokens[1];
        std::size_t node_a =
            get_or_create_node(tokens[2], node_map, circuit.node_names);
        std::size_t node_b =
            get_or_create_node(tokens[3], node_map, circuit.node_names);
        auto kv = parse_kv(tokens, 4);

        if (type == "res") {
            circuit.components.emplace_back(
                core::Resistor{name, node_a, node_b, require_val(kv, name)});
        } else if (type == "cap") {
            circuit.components.emplace_back(
                core::Capacitor{name, node_a, node_b, require_val(kv, name)});
        } else if (type == "ind") {
            circuit.components.emplace_back(
                core::Inductor{name, node_a, node_b, require_val(kv, name)});
        } else if (type == "vsrc") {
            auto   src_type = kv.count("type") ? to_lower(kv["type"]) : "dc";
            double freq     = kv.count("freq") ? std::stod(kv["freq"]) : 0.0;
            circuit.components.emplace_back(core::VoltageSource{
                name, node_a, node_b, src_type, require_val(kv, name), freq});
        } else if (type == "isrc") {
            auto   src_type = kv.count("type") ? to_lower(kv["type"]) : "dc";
            double freq     = kv.count("freq") ? std::stod(kv["freq"]) : 0.0;
            circuit.components.emplace_back(core::CurrentSource{
                name, node_a, node_b, src_type, require_val(kv, name), freq});
        } else {
            throw core::ParseError("Unknown component type: " + tokens[0]);
        }

        LOG_DEBUG("Parsed component: " + name);
    }

    circuit.node_count = circuit.node_names.size();
    LOG_INFO("Parsed " + std::to_string(circuit.components.size()) +
             " components, " + std::to_string(circuit.node_count) + " nodes");
    return circuit;
}

} // namespace parser
