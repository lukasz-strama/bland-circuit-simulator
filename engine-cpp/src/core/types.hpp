#pragma once

#include <cstddef>
#include <variant>

namespace core {

struct Resistor {
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

struct Capacitor {
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

struct Inductor {
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

struct VoltageSource {
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

struct CurrentSource {
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

using Component =
    std::variant<Resistor, Capacitor, Inductor, VoltageSource, CurrentSource>;

} // namespace core
