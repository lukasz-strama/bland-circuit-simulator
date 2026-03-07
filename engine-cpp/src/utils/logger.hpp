#pragma once

#include <cstdint>
#include <source_location>
#include <string_view>

namespace utils {

enum class LogLevel : std::uint8_t {
    Debug   = 0,
    Info    = 1,
    Warning = 2,
    Error   = 3,
};

class Logger {
  public:
    Logger(const Logger &)            = delete;
    Logger &operator=(const Logger &) = delete;

    static Logger         &instance();
    void                   set_level(LogLevel level);
    [[nodiscard]] LogLevel level() const;

    void log(LogLevel level, std::string_view message,
             const std::source_location &loc = std::source_location::current());

  private:
    Logger() = default;

    LogLevel min_level_{LogLevel::Debug};
};

} // namespace utils

// ---------------------------------------------------------------------------
// usage:  LOG_INFO("Server started on port {}", 8081);
// ---------------------------------------------------------------------------

#define LOG_DEBUG(msg)                                                         \
    ::utils::Logger::instance().log(::utils::LogLevel::Debug, (msg),           \
                                    std::source_location::current())

#define LOG_INFO(msg)                                                          \
    ::utils::Logger::instance().log(::utils::LogLevel::Info, (msg),            \
                                    std::source_location::current())

#define LOG_WARN(msg)                                                          \
    ::utils::Logger::instance().log(::utils::LogLevel::Warning, (msg),         \
                                    std::source_location::current())

#define LOG_ERROR(msg)                                                         \
    ::utils::Logger::instance().log(::utils::LogLevel::Error, (msg),           \
                                    std::source_location::current())
