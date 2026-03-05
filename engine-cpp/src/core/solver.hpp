#pragma once

#include <string>
#include <vector>

#include "core/circuit.hpp"

namespace core {

struct SimulationResult {
    std::string                      analysis_type;
    std::vector<double>              time_points;
    std::vector<std::vector<double>> node_voltages;
    std::vector<std::vector<double>> branch_currents;
};

[[nodiscard]] SimulationResult solve_dc(const Circuit &circuit);

[[nodiscard]] SimulationResult solve_transient(const Circuit &circuit,
                                               double t_start, double t_end,
                                               double t_step);

} // namespace core
