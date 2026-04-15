#pragma once

#include <stdexcept>
#include <string>

namespace core {

/// 400 — netlist syntax / validation error
class ParseError : public std::runtime_error {
  public:
    explicit ParseError(const std::string &msg) : std::runtime_error(msg) {}
};

/// 422 — mathematically/physically invalid circuit
class SimulationError : public std::runtime_error {
  public:
    explicit SimulationError(const std::string &msg)
        : std::runtime_error(msg) {}
};

} // namespace core
