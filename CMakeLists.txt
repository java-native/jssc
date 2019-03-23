cmake_minimum_required(VERSION 3.9)
project(jssc VERSION 2.8.0 DESCRIPTION "Java Simple Serial Connector")

find_package(JNI)

add_library(jssc SHARED
    src/cpp/_nix_based/jssc.cpp
)

target_include_directories(jssc PRIVATE ${JNI_INCLUDE_DIRS})

set_target_properties(jssc PROPERTIES VERSION ${PROJECT_VERSION})
set_target_properties(jssc PROPERTIES PUBLIC_HEADER src/jssc_SerialNativeInterface.h)