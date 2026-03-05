#include <crow.h>

#include "api/handlers.hpp"

int main() {
    crow::SimpleApp app;

    CROW_ROUTE(app, "/api/v1/simulate")
        .methods(crow::HTTPMethod::POST)(
            [](const crow::request &req) { return api::handle_simulate(req); });

    app.port(8080).multithreaded().run();

    return 0;
}
