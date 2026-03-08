#include "core/solver.hpp"

#include <cmath>
#include <numbers>
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
        std::visit(
            [&](const auto &elem) {
                result.current_names.push_back(elem.name);
            },
            comp);
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

static std::size_t count_capacitors(const Circuit &circuit) {
    std::size_t count = 0;
    for (const auto &comp : circuit.components) {
        if (std::holds_alternative<Capacitor>(comp))
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

static double vsrc_value(const VoltageSource &src, double t) {
    if (src.source_type == "sine" || src.source_type == "sin")
        return src.value * std::sin(2.0 * std::numbers::pi * src.frequency * t);
    return src.value;
}

static double isrc_value(const CurrentSource &src, double t) {
    if (src.source_type == "sine" || src.source_type == "sin")
        return src.value * std::sin(2.0 * std::numbers::pi * src.frequency * t);
    return src.value;
}

static double node_voltage(const std::vector<double> &voltages,
                           std::size_t                node) {
    if (node == GROUND_NODE)
        return 0.0;
    return voltages[node];
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
        LOG_ERROR("Singular MNA matrix in DC analysis");
        throw std::runtime_error(
            "Singular matrix. Possible causes: short circuit of ideal "
            "voltage sources, floating node, missing ground reference, "
            "or open circuit with no DC path.");
    }

    Eigen::VectorXd x = qr.solve(b);

    std::vector<double> voltages(n);
    for (std::size_t i = 0; i < n; ++i)
        voltages[i] = x(static_cast<Eigen::Index>(i));

    std::vector<double> vsrc_currents(num_vsrc);
    for (std::size_t i = 0; i < num_vsrc; ++i)
        vsrc_currents[i] = x(static_cast<Eigen::Index>(n + i));

    std::vector<double> all_currents;
    {
        std::size_t vs_i = 0, in_i = 0;
        for (const auto &comp : circuit.components) {
            std::visit(
                [&](const auto &elem) {
                    using T = std::decay_t<decltype(elem)>;
                    if constexpr (std::is_same_v<T, Resistor>) {
                        double va = node_voltage(voltages, elem.node_a);
                        double vb = node_voltage(voltages, elem.node_b);
                        all_currents.push_back((va - vb) / elem.value);
                    } else if constexpr (std::is_same_v<T, VoltageSource>) {
                        all_currents.push_back(vsrc_currents[vs_i]);
                        ++vs_i;
                    } else if constexpr (std::is_same_v<T, CurrentSource>) {
                        all_currents.push_back(elem.value);
                    } else if constexpr (std::is_same_v<T, Inductor>) {
                        all_currents.push_back(
                            x(static_cast<Eigen::Index>(n + num_vsrc + in_i)));
                        ++in_i;
                    } else if constexpr (std::is_same_v<T, Capacitor>) {
                        all_currents.push_back(0.0);
                    }
                },
                comp);
        }
    }

    result.node_voltages.push_back(std::move(voltages));
    result.branch_currents.push_back(std::move(vsrc_currents));
    result.component_currents.push_back(std::move(all_currents));

    LOG_INFO("DC analysis complete");
    return result;
}

SimulationResult solve_transient(const Circuit &circuit, double t_start,
                                 double t_end, double t_step) {
    const auto n        = circuit.node_count;
    const auto num_vsrc = count_voltage_sources(circuit);
    const auto num_cap  = count_capacitors(circuit);
    const auto num_ind  = count_inductors(circuit);

    SimulationResult result;
    result.analysis_type = "transient";
    populate_names(circuit, result);

    LOG_INFO("Transient analysis: " + std::to_string(n) + " nodes, " +
             std::to_string(num_vsrc) + " vsrc, " + std::to_string(num_cap) +
             " cap, " + std::to_string(num_ind) + " ind, " + "tstep=" +
             std::to_string(t_step) + " tstop=" + std::to_string(t_end));

    if (n == 0) {
        LOG_WARN("Empty circuit for transient analysis");
        return result;
    }

    std::vector<double> cap_voltages(num_cap, 0.0);
    std::vector<double> ind_currents(num_ind, 0.0);

    {
        auto            init_size = n + num_vsrc + num_cap;
        auto            sz        = static_cast<Eigen::Index>(init_size);
        Eigen::MatrixXd A         = Eigen::MatrixXd::Zero(sz, sz);
        Eigen::VectorXd b         = Eigen::VectorXd::Zero(sz);

        std::size_t vsrc_idx = 0;
        std::size_t cap_idx  = 0;

        for (const auto &comp : circuit.components) {
            std::visit(
                [&](const auto &elem) {
                    using T = std::decay_t<decltype(elem)>;

                    if constexpr (std::is_same_v<T, Resistor>) {
                        double g = 1.0 / elem.value;
                        stamp(A, elem.node_a, elem.node_a, g);
                        stamp(A, elem.node_b, elem.node_b, g);
                        stamp(A, elem.node_a, elem.node_b, -g);
                        stamp(A, elem.node_b, elem.node_a, -g);

                    } else if constexpr (std::is_same_v<T, VoltageSource>) {
                        auto row = n + vsrc_idx;
                        stamp(A, row, elem.node_a, 1.0);
                        stamp(A, elem.node_a, row, 1.0);
                        stamp(A, row, elem.node_b, -1.0);
                        stamp(A, elem.node_b, row, -1.0);
                        b(static_cast<Eigen::Index>(row)) =
                            vsrc_value(elem, 0.0);
                        ++vsrc_idx;

                    } else if constexpr (std::is_same_v<T, CurrentSource>) {
                        double i = isrc_value(elem, 0.0);
                        stamp_rhs(b, elem.node_a, -i);
                        stamp_rhs(b, elem.node_b, i);

                    } else if constexpr (std::is_same_v<T, Capacitor>) {
                        auto row = n + num_vsrc + cap_idx;
                        stamp(A, row, elem.node_a, 1.0);
                        stamp(A, elem.node_a, row, 1.0);
                        stamp(A, row, elem.node_b, -1.0);
                        stamp(A, elem.node_b, row, -1.0);
                        b(static_cast<Eigen::Index>(row)) = 0.0;
                        LOG_DEBUG("IC: CAP " + elem.name + " V=0");
                        ++cap_idx;

                    } else if constexpr (std::is_same_v<T, Inductor>) {
                        LOG_DEBUG("IC: IND " + elem.name +
                                  " I=0 (open circuit)");
                    }
                },
                comp);
        }

        Eigen::ColPivHouseholderQR<Eigen::MatrixXd> qr(A);
        if (!qr.isInvertible()) {
            LOG_ERROR("Singular MNA matrix at initial conditions (t=0)");
            throw std::runtime_error(
                "Singular matrix at initial conditions. Possible causes: "
                "short circuit of ideal voltage sources, floating node, "
                "missing ground reference, or open circuit with no DC path.");
        }

        Eigen::VectorXd x = qr.solve(b);

        result.time_points.push_back(0.0);

        std::vector<double> voltages(n);
        for (std::size_t i = 0; i < n; ++i)
            voltages[i] = x(static_cast<Eigen::Index>(i));

        std::vector<double> currents(num_vsrc);
        for (std::size_t i = 0; i < num_vsrc; ++i)
            currents[i] = x(static_cast<Eigen::Index>(n + i));

        std::vector<double> all_currents_t0;
        {
            std::size_t vs_i = 0, cp_i = 0;
            for (const auto &comp : circuit.components) {
                std::visit(
                    [&](const auto &elem) {
                        using T = std::decay_t<decltype(elem)>;
                        if constexpr (std::is_same_v<T, Resistor>) {
                            double va = node_voltage(voltages, elem.node_a);
                            double vb = node_voltage(voltages, elem.node_b);
                            all_currents_t0.push_back((va - vb) / elem.value);
                        } else if constexpr (std::is_same_v<T, VoltageSource>) {
                            all_currents_t0.push_back(currents[vs_i]);
                            ++vs_i;
                        } else if constexpr (std::is_same_v<T, CurrentSource>) {
                            all_currents_t0.push_back(isrc_value(elem, 0.0));
                        } else if constexpr (std::is_same_v<T, Inductor>) {
                            all_currents_t0.push_back(0.0);
                        } else if constexpr (std::is_same_v<T, Capacitor>) {
                            all_currents_t0.push_back(
                                x(static_cast<Eigen::Index>(n + num_vsrc +
                                                            cp_i)));
                            ++cp_i;
                        }
                    },
                    comp);
            }
        }

        result.node_voltages.push_back(voltages);
        result.branch_currents.push_back(currents);
        result.component_currents.push_back(std::move(all_currents_t0));

        cap_idx = 0;
        for (const auto &c : circuit.components) {
            if (const auto *cap = std::get_if<Capacitor>(&c)) {
                double va             = node_voltage(voltages, cap->node_a);
                double vb             = node_voltage(voltages, cap->node_b);
                cap_voltages[cap_idx] = va - vb;
                ++cap_idx;
            }
        }

        LOG_DEBUG("t=0 initial conditions solved (CAP V=0, IND I=0)");
    }

    auto trans_mna_size = n + num_vsrc;
    auto sz             = static_cast<Eigen::Index>(trans_mna_size);

    for (double t = t_start + t_step; t <= t_end + t_step * 0.5; t += t_step) {
        Eigen::MatrixXd A = Eigen::MatrixXd::Zero(sz, sz);
        Eigen::VectorXd b = Eigen::VectorXd::Zero(sz);

        std::size_t vsrc_idx = 0;
        std::size_t cap_idx  = 0;
        std::size_t ind_idx  = 0;

        for (const auto &comp : circuit.components) {
            std::visit(
                [&](const auto &elem) {
                    using T = std::decay_t<decltype(elem)>;

                    if constexpr (std::is_same_v<T, Resistor>) {
                        double g = 1.0 / elem.value;
                        stamp(A, elem.node_a, elem.node_a, g);
                        stamp(A, elem.node_b, elem.node_b, g);
                        stamp(A, elem.node_a, elem.node_b, -g);
                        stamp(A, elem.node_b, elem.node_a, -g);

                    } else if constexpr (std::is_same_v<T, VoltageSource>) {
                        auto row = n + vsrc_idx;
                        stamp(A, row, elem.node_a, 1.0);
                        stamp(A, elem.node_a, row, 1.0);
                        stamp(A, row, elem.node_b, -1.0);
                        stamp(A, elem.node_b, row, -1.0);
                        b(static_cast<Eigen::Index>(row)) = vsrc_value(elem, t);
                        ++vsrc_idx;

                    } else if constexpr (std::is_same_v<T, CurrentSource>) {
                        double i = isrc_value(elem, t);
                        stamp_rhs(b, elem.node_a, -i);
                        stamp_rhs(b, elem.node_b, i);

                    } else if constexpr (std::is_same_v<T, Capacitor>) {
                        double g_c = elem.value / t_step;
                        stamp(A, elem.node_a, elem.node_a, g_c);
                        stamp(A, elem.node_b, elem.node_b, g_c);
                        stamp(A, elem.node_a, elem.node_b, -g_c);
                        stamp(A, elem.node_b, elem.node_a, -g_c);
                        double i_hist = g_c * cap_voltages[cap_idx];
                        stamp_rhs(b, elem.node_a, i_hist);
                        stamp_rhs(b, elem.node_b, -i_hist);
                        ++cap_idx;

                    } else if constexpr (std::is_same_v<T, Inductor>) {
                        double g_l = t_step / elem.value;
                        stamp(A, elem.node_a, elem.node_a, g_l);
                        stamp(A, elem.node_b, elem.node_b, g_l);
                        stamp(A, elem.node_a, elem.node_b, -g_l);
                        stamp(A, elem.node_b, elem.node_a, -g_l);
                        double i_hist = ind_currents[ind_idx];
                        stamp_rhs(b, elem.node_a, -i_hist);
                        stamp_rhs(b, elem.node_b, i_hist);
                        ++ind_idx;
                    }
                },
                comp);
        }

        Eigen::ColPivHouseholderQR<Eigen::MatrixXd> qr(A);
        if (!qr.isInvertible()) {
            LOG_ERROR("Singular MNA matrix at t=" + std::to_string(t));
            throw std::runtime_error("Singular matrix at t=" +
                                     std::to_string(t));
        }

        Eigen::VectorXd x = qr.solve(b);

        result.time_points.push_back(t);

        std::vector<double> voltages(n);
        for (std::size_t i = 0; i < n; ++i)
            voltages[i] = x(static_cast<Eigen::Index>(i));

        std::vector<double> currents(num_vsrc);
        for (std::size_t i = 0; i < num_vsrc; ++i)
            currents[i] = x(static_cast<Eigen::Index>(n + i));

        auto cap_voltages_prev = cap_voltages;

        cap_idx = 0;
        ind_idx = 0;
        for (const auto &c : circuit.components) {
            if (const auto *cap = std::get_if<Capacitor>(&c)) {
                double va             = node_voltage(voltages, cap->node_a);
                double vb             = node_voltage(voltages, cap->node_b);
                cap_voltages[cap_idx] = va - vb;
                ++cap_idx;
            } else if (const auto *ind = std::get_if<Inductor>(&c)) {
                double va             = node_voltage(voltages, ind->node_a);
                double vb             = node_voltage(voltages, ind->node_b);
                double g_l            = t_step / ind->value;
                ind_currents[ind_idx] = ind_currents[ind_idx] + g_l * (va - vb);
                ++ind_idx;
            }
        }

        std::vector<double> all_currents;
        {
            std::size_t vs_i = 0, cp_i = 0, in_i = 0;
            for (const auto &comp : circuit.components) {
                std::visit(
                    [&](const auto &elem) {
                        using T = std::decay_t<decltype(elem)>;
                        if constexpr (std::is_same_v<T, Resistor>) {
                            double va = node_voltage(voltages, elem.node_a);
                            double vb = node_voltage(voltages, elem.node_b);
                            all_currents.push_back((va - vb) / elem.value);
                        } else if constexpr (std::is_same_v<T, VoltageSource>) {
                            all_currents.push_back(currents[vs_i]);
                            ++vs_i;
                        } else if constexpr (std::is_same_v<T, CurrentSource>) {
                            all_currents.push_back(isrc_value(elem, t));
                        } else if constexpr (std::is_same_v<T, Inductor>) {
                            all_currents.push_back(ind_currents[in_i]);
                            ++in_i;
                        } else if constexpr (std::is_same_v<T, Capacitor>) {
                            double va    = node_voltage(voltages, elem.node_a);
                            double vb    = node_voltage(voltages, elem.node_b);
                            double v_new = va - vb;
                            double v_old = cap_voltages_prev[cp_i];
                            all_currents.push_back(elem.value *
                                                   (v_new - v_old) / t_step);
                            ++cp_i;
                        }
                    },
                    comp);
            }
        }

        result.node_voltages.push_back(std::move(voltages));
        result.branch_currents.push_back(std::move(currents));
        result.component_currents.push_back(std::move(all_currents));
    }

    LOG_INFO("Transient analysis complete: " +
             std::to_string(result.time_points.size()) + " time points");
    return result;
}

} // namespace core
