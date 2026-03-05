#pragma once

#include <cstddef>
#include <string>
#include <vector>

#include "core/types.hpp"

namespace core {

struct Circuit {
    std::vector<Component> components;
    std::size_t            node_count{0};
    std::string            name;
};

} // namespace core
