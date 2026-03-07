#pragma once

#include <cstddef>
#include <limits>
#include <string>
#include <variant>

namespace core {

inline constexpr std::size_t GROUND_NODE =
    std::numeric_limits<std::size_t>::max();

} // namespace core

namespace core {

struct Resistor {
    std::string name;
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

struct Capacitor {
    std::string name;
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

struct Inductor {
    std::string name;
    std::size_t node_a;
    std::size_t node_b;
    double      value;
};

struct VoltageSource {
    std::string name;
    std::size_t node_a;
    std::size_t node_b;
    std::string source_type;
    double      value;
    double      frequency;
};

struct CurrentSource {
    std::string name;
    std::size_t node_a;
    std::size_t node_b;
    std::string source_type;
    double      value;
    double      frequency;
};

using Component =
    std::variant<Resistor, Capacitor, Inductor, VoltageSource, CurrentSource>;

} // namespace core
