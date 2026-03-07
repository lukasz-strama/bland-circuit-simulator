#include "utils/logger.hpp"

#include <chrono>
#include <ctime>
#include <filesystem>
#include <iomanip>
#include <iostream>
#include <mutex>
#include <sstream>

namespace utils {

static constexpr std::string_view kReset  = "\033[0m";
static constexpr std::string_view kGray   = "\033[90m";
static constexpr std::string_view kGreen  = "\033[32m";
static constexpr std::string_view kYellow = "\033[33m";
static constexpr std::string_view kRed    = "\033[31m";
static constexpr std::string_view kBold   = "\033[1m";

static std::mutex g_log_mutex; // NOLINT

static std::string_view level_tag(LogLevel level) {
    switch (level) {
    case LogLevel::Debug:
        return "DEBUG";
    case LogLevel::Info:
        return "INFO ";
    case LogLevel::Warning:
        return "WARN ";
    case LogLevel::Error:
        return "ERROR";
    }
    return "?????";
}

static std::string_view level_color(LogLevel level) {
    switch (level) {
    case LogLevel::Debug:
        return kGray;
    case LogLevel::Info:
        return kGreen;
    case LogLevel::Warning:
        return kYellow;
    case LogLevel::Error:
        return kRed;
    }
    return kReset;
}

static std::string timestamp_now() {
    using clock    = std::chrono::system_clock;
    const auto now = clock::now();
    const auto ms  = std::chrono::duration_cast<std::chrono::milliseconds>(
                        now.time_since_epoch()) %
                    1000;
    const auto time = clock::to_time_t(now);

    std::tm buf{};
    localtime_r(&time, &buf);

    std::ostringstream oss;
    oss << std::put_time(&buf, "%H:%M:%S") << '.' << std::setfill('0')
        << std::setw(3) << ms.count();
    return oss.str();
}

static std::string short_file(const char *path) {
    return std::filesystem::path(path).filename().string();
}

Logger &Logger::instance() {
    static Logger logger;
    return logger;
}

void Logger::set_level(LogLevel level) {
    min_level_ = level;
}

LogLevel Logger::level() const {
    return min_level_;
}

void Logger::log(LogLevel level, std::string_view message,
                 const std::source_location &loc) {
    if (level < min_level_)
        return;

    const auto ts    = timestamp_now();
    const auto tag   = level_tag(level);
    const auto color = level_color(level);
    const auto file  = short_file(loc.file_name());
    const auto line  = loc.line();

    std::ostringstream out;
    out << kGray << ts << kReset << ' ' << color << kBold << tag << kReset
        << ' ' << kGray << '[' << file << ':' << line << ']' << kReset << ' '
        << message << '\n';

    // Thread-safe write
    {
        std::lock_guard lock(g_log_mutex);
        if (level >= LogLevel::Error) {
            std::cerr << out.str();
            std::cerr.flush();
        } else {
            std::cout << out.str();
            std::cout.flush();
        }
    }
}

} // namespace utils
