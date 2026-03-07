#include "core/solver.hpp"

#include <stdexcept>
#include <string>

#include <Eigen/Dense>

#include "core/types.hpp"
#include "utils/logger.hpp"

namespace core {

static void populate_names(const Circuit &circuit, SimulationResult &result) {
    result.node_names = circuit.node_names;
    for (const auto &comp : circuit.components) {
        if (std::holds_alternative<VoltageSource>(comp))
            result.source_names.push_back(std::get<VoltageSource>(comp).name);
    }
}

static std::size_t count_voltage_sources(const Circuit &circuit) {
    std::size_t count = 0;
    for (const auto &comp : circuit.components) {
        if (std::holds_alternative<VoltageSource>(comp))
            ++count;
    }
    return count;
}

static std::size_t count_inductors(const Circuit &circuit) {
    std::size_t count = 0;
    for (const auto &comp : circuit.components) {
        if (std::holds_alternative<Inductor>(comp))
            ++count;
    }
    return count;
}

static void stamp(Eigen::MatrixXd &A, std::size_t row, std::size_t col,
                  double value) {
    if (row == GROUND_NODE || col == GROUND_NODE)
        return;
    A(static_cast<Eigen::Index>(row), static_cast<Eigen::Index>(col)) += value;
}

static void stamp_rhs(Eigen::VectorXd &b, std::size_t row, double value) {
    if (row == GROUND_NODE)
        return;
    b(static_cast<Eigen::Index>(row)) += value;
}

SimulationResult solve_dc(const Circuit &circuit) {
    const auto n        = circuit.node_count;
    const auto num_vsrc = count_voltage_sources(circuit);
    const auto num_ind  = count_inductors(circuit);
    const auto mna_size = n + num_vsrc + num_ind;

    LOG_INFO("DC analysis: " + std::to_string(n) + " nodes, " +
             std::to_string(num_vsrc) + " voltage sources, " +
             std::to_string(num_ind) + " inductors");

    SimulationResult result;
    result.analysis_type = "dc";
    result.time_points   = {0.0};
    populate_names(circuit, result);

    if (mna_size == 0) {
        LOG_WARN("Empty circuit");
        return result;
    }

    auto            sz = static_cast<Eigen::Index>(mna_size);
    Eigen::MatrixXd A  = Eigen::MatrixXd::Zero(sz, sz);
    Eigen::VectorXd b  = Eigen::VectorXd::Zero(sz);

    std::size_t vsrc_idx = 0;
    std::size_t ind_idx  = 0;

    for (const auto &comp : circuit.components) {
        std::visit(
            [&](const auto &elem) {
                using T = std::decay_t<decltype(elem)>;

                if constexpr (std::is_same_v<T, Resistor>) {
                    double g  = 1.0 / elem.value;
                    auto   na = elem.node_a;
                    auto   nb = elem.node_b;
                    stamp(A, na, na, g);
                    stamp(A, nb, nb, g);
                    stamp(A, na, nb, -g);
                    stamp(A, nb, na, -g);
                    LOG_DEBUG("Stamped RES " + elem.name +
                              " g=" + std::to_string(g));

                } else if constexpr (std::is_same_v<T, VoltageSource>) {
                    auto na  = elem.node_a;
                    auto nb  = elem.node_b;
                    auto row = n + vsrc_idx;
                    stamp(A, row, na, 1.0);
                    stamp(A, na, row, 1.0);
                    stamp(A, row, nb, -1.0);
                    stamp(A, nb, row, -1.0);
                    b(static_cast<Eigen::Index>(row)) = elem.value;
                    LOG_DEBUG("Stamped VSRC " + elem.name +
                              " V=" + std::to_string(elem.value));
                    ++vsrc_idx;

                } else if constexpr (std::is_same_v<T, CurrentSource>) {
                    auto na = elem.node_a;
                    auto nb = elem.node_b;
                    stamp_rhs(b, na, -elem.value);
                    stamp_rhs(b, nb, elem.value);
                    LOG_DEBUG("Stamped ISRC " + elem.name +
                              " I=" + std::to_string(elem.value));

                } else if constexpr (std::is_same_v<T, Inductor>) {
                    auto na  = elem.node_a;
                    auto nb  = elem.node_b;
                    auto row = n + num_vsrc + ind_idx;
                    stamp(A, row, na, 1.0);
                    stamp(A, na, row, 1.0);
                    stamp(A, row, nb, -1.0);
                    stamp(A, nb, row, -1.0);
                    b(static_cast<Eigen::Index>(row)) = 0.0;
                    LOG_DEBUG("Stamped IND " + elem.name +
                              " (DC short circuit)");
                    ++ind_idx;

                } else if constexpr (std::is_same_v<T, Capacitor>) {
                    LOG_DEBUG("Skipped CAP " + elem.name +
                              " (DC open circuit)");
                }
            },
            comp);
    }

    LOG_DEBUG("MNA matrix " + std::to_string(mna_size) + "x" +
              std::to_string(mna_size));

    Eigen::ColPivHouseholderQR<Eigen::MatrixXd> qr(A);
    if (!qr.isInvertible()) {
        LOG_ERROR(
            "Singular MNA matrix - possible short circuit of ideal voltage "
            "sources");
        throw std::runtime_error(
            "Singular matrix. Possible short circuit of ideal voltage "
            "sources.");
    }

    Eigen::VectorXd x = qr.solve(b);

    std::vector<double> voltages(n);
    for (std::size_t i = 0; i < n; ++i)
        voltages[i] = x(static_cast<Eigen::Index>(i));
    result.node_voltages.push_back(std::move(voltages));

    std::vector<double> currents(num_vsrc);
    for (std::size_t i = 0; i < num_vsrc; ++i)
        currents[i] = x(static_cast<Eigen::Index>(n + i));
    result.branch_currents.push_back(std::move(currents));

    LOG_INFO("DC analysis complete");
    return result;
}

SimulationResult solve_transient(const Circuit &circuit, double t_start,
                                 double t_end, double t_step) {
    SimulationResult result;
    result.analysis_type = "transient";
    populate_names(circuit, result);

    LOG_WARN("Transient analysis not yet implemented, returning placeholder");

    for (double t = t_start; t <= t_end; t += t_step) {
        result.time_points.push_back(t);

        std::vector<double> voltages(circuit.node_count, 0.0);
        result.node_voltages.push_back(std::move(voltages));

        std::size_t         num_vsrc = count_voltage_sources(circuit);
        std::vector<double> currents(num_vsrc, 0.0);
        result.branch_currents.push_back(std::move(currents));
    }

    return result;
}

} // namespace core
