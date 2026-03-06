cmake_minimum_required(VERSION 3.28)
project(bland-circuit-simulator LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

include(FetchContent)

FetchContent_Declare(
    asio
    GIT_REPOSITORY https://github.com/chriskohlhoff/asio.git
    GIT_TAG        asio-1-30-2
    GIT_SHALLOW    TRUE
    EXCLUDE_FROM_ALL
)

FetchContent_Declare(
    Crow
    GIT_REPOSITORY https://github.com/CrowCpp/Crow.git
    GIT_TAG        v1.2.0
    GIT_SHALLOW    TRUE
    EXCLUDE_FROM_ALL
)

FetchContent_Declare(
    Eigen3
    GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
    GIT_TAG        3.4.0
    GIT_SHALLOW    TRUE
)



FetchContent_MakeAvailable(asio)
set(ASIO_INCLUDE_DIR "${asio_SOURCE_DIR}/asio/include" CACHE PATH "" FORCE)

set(CROW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(CROW_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(Crow)

cmake_policy(SET CMP0169 OLD)
FetchContent_GetProperties(Eigen3)
if(NOT eigen3_POPULATED)
    FetchContent_Populate(Eigen3)
endif()

add_library(Eigen3_headers INTERFACE)
add_library(Eigen3::Eigen ALIAS Eigen3_headers)
target_include_directories(Eigen3_headers INTERFACE "${eigen3_SOURCE_DIR}")

add_executable(${PROJECT_NAME}
    src/main.cpp
    src/api/handlers.cpp
    src/parser/netlist_parser.cpp
    src/core/solver.cpp
)

target_include_directories(${PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/src)

get_target_property(CROW_INC_DIRS Crow::Crow INTERFACE_INCLUDE_DIRECTORIES)
target_include_directories(${PROJECT_NAME} SYSTEM PRIVATE ${CROW_INC_DIRS})

target_link_libraries(${PROJECT_NAME}
    PRIVATE
        Crow::Crow
        Eigen3::Eigen
)
