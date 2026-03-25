#include "api/handlers.hpp"

#include <cctype>
#include <iomanip>
#include <sstream>

#include "core/solver.hpp"
#include "parser/netlist_parser.hpp"

namespace api {

static std::string result_to_csv(const core::SimulationResult &result) {
    std::ostringstream csv;
    csv << std::fixed;

    csv << "time";
    for (const auto &name : result.node_names) {
        csv << ",V(" << name << ")";
    }
    for (const auto &name : result.current_names) {
        csv << ",I(" << name << ")";
    }
    csv << "\n";

    for (std::size_t t = 0; t < result.time_points.size(); ++t) {
        csv << std::setprecision(6) << result.time_points[t];

        if (t < result.node_voltages.size()) {
            for (const auto &v : result.node_voltages[t]) {
                csv << "," << std::setprecision(6) << v;
            }
        }
        if (t < result.component_currents.size()) {
            for (const auto &i : result.component_currents[t]) {
                csv << "," << std::setprecision(6) << i;
            }
        }
        csv << "\n";
    }

    return csv.str();
}

struct SimParams {
    std::string type  = "dc";
    double      tstop = 0.01;
    double      tstep = 0.0001;
};

struct ExtractResult {
    SimParams params;
    bool      found = false;
};

static ExtractResult extract_sim_params(const std::string &netlist) {
    ExtractResult      result;
    std::istringstream stream(netlist);
    std::string        line;

    while (std::getline(stream, line)) {
        if (line.empty())
            continue;
        std::string lower = line;
        for (auto &c : lower)
            c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));

        if (lower.rfind(".simulate", 0) != 0)
            continue;

        result.found = true;

        std::istringstream tokens(line);
        std::string        token;
        while (tokens >> token) {
            auto eq = token.find('=');
            if (eq == std::string::npos)
                continue;
            std::string key = token.substr(0, eq);
            std::string val = token.substr(eq + 1);
            for (auto &c : key)
                c = static_cast<char>(
                    std::tolower(static_cast<unsigned char>(c)));

            if (key == "type") {
                for (auto &c : val)
                    c = static_cast<char>(
                        std::tolower(static_cast<unsigned char>(c)));
                result.params.type = val;
            }
            if (key == "tstop")
                result.params.tstop = std::stod(val);
            if (key == "tstep")
                result.params.tstep = std::stod(val);
        }
    }

    return result;
}

crow::response handle_simulate(const crow::request &req) {
    try {
        const std::string &netlist = req.body;

        if (netlist.empty()) {
            return crow::response(400, "Empty netlist");
        }

        auto [sim_params, sim_found] = extract_sim_params(netlist);

        if (!sim_found) {
            return crow::response(400,
                                  "Missing .SIMULATE directive in netlist");
        }

        auto circuit = parser::parse_netlist(netlist);

        core::SimulationResult result;

        if (sim_params.type == "trans" || sim_params.type == "transient") {
            result = core::solve_transient(circuit, 0.0, sim_params.tstop,
                                           sim_params.tstep);
        } else {
            result = core::solve_dc(circuit);
        }

        std::string csv = result_to_csv(result);

        crow::response resp(200, csv);
        resp.set_header("Content-Type", "text/csv; charset=utf-8");
        return resp;
    } catch (const std::exception &e) {
        return crow::response(400, e.what());
    }
}

} // namespace api
