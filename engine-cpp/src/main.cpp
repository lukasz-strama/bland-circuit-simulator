#include <crow.h>

#include <cstdlib>

#include "api/handlers.hpp"

int main() {
    crow::SimpleApp app;

    CROW_ROUTE(app, "/api/v1/simulate")
        .methods(crow::HTTPMethod::POST)(
            [](const crow::request &req) { return api::handle_simulate(req); });

    const char *port_env = std::getenv("PORT");
    uint16_t    port =
        port_env ? static_cast<uint16_t>(std::stoi(port_env)) : 8081;

    app.port(port).multithreaded().run();

    return 0;
}
