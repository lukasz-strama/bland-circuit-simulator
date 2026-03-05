#pragma once

#include <crow.h>

namespace api {

crow::response handle_simulate(const crow::request &req);

} // namespace api
