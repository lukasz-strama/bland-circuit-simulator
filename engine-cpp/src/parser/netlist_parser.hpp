#pragma once

#include <string>

#include "core/circuit.hpp"

namespace parser {

[[nodiscard]] core::Circuit parse_netlist(const std::string &raw);

} // namespace parser
