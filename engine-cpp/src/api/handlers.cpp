#include "api/handlers.hpp"

#include <nlohmann/json.hpp>

#include "core/solver.hpp"
#include "parser/netlist_parser.hpp"

namespace api {

using json = nlohmann::json;

static json simulation_result_to_json(const core::SimulationResult &result) {
    json j;
    j["analysis_type"]   = result.analysis_type;
    j["time_points"]     = result.time_points;
    j["node_voltages"]   = result.node_voltages;
    j["branch_currents"] = result.branch_currents;
    return j;
}

crow::response handle_simulate(const crow::request &req) {
    try {
        auto body = json::parse(req.body);

        std::string netlist  = body.value("netlist", "");
        std::string analysis = body.value("analysis", "dc");

        auto circuit = parser::parse_netlist(netlist);

        core::SimulationResult result;

        if (analysis == "transient") {
            double t_start = body.value("t_start", 0.0);
            double t_end   = body.value("t_end", 1.0);
            double t_step  = body.value("t_step", 0.001);
            result = core::solve_transient(circuit, t_start, t_end, t_step);
        } else {
            result = core::solve_dc(circuit);
        }

        json response;
        response["status"] = "ok";
        response["result"] = simulation_result_to_json(result);

        return crow::response(200, response.dump());
    } catch (const std::exception &e) {
        json error;
        error["status"]  = "error";
        error["message"] = e.what();
        return crow::response(400, error.dump());
    }
}

} // namespace api
