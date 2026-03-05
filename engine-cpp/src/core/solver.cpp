#include "core/solver.hpp"

#include <Eigen/Dense>

namespace core {

SimulationResult solve_dc(const Circuit &circuit) {
    const auto n = circuit.node_count;

    Eigen::MatrixXd G   = Eigen::MatrixXd::Zero(static_cast<Eigen::Index>(n),
                                                static_cast<Eigen::Index>(n));
    Eigen::VectorXd rhs = Eigen::VectorXd::Zero(static_cast<Eigen::Index>(n));

    SimulationResult result;
    result.analysis_type = "dc";
    result.time_points   = {0.0};

    if (n > 0) {
        Eigen::VectorXd     solution = G.colPivHouseholderQr().solve(rhs);
        std::vector<double> voltages(solution.data(),
                                     solution.data() + solution.size());
        result.node_voltages.push_back(std::move(voltages));
    }

    return result;
}

SimulationResult solve_transient(const Circuit &circuit, double t_start,
                                 double t_end, double t_step) {
    SimulationResult result;
    result.analysis_type = "transient";

    for (double t = t_start; t <= t_end; t += t_step) {
        result.time_points.push_back(t);

        std::vector<double> voltages(circuit.node_count, 0.0);
        result.node_voltages.push_back(std::move(voltages));

        std::vector<double> currents(circuit.components.size(), 0.0);
        result.branch_currents.push_back(std::move(currents));
    }

    return result;
}

} // namespace core
