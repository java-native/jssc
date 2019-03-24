cmake_minimum_required(VERSION 3.9)
project(jssc VERSION 2.8.0 DESCRIPTION "Java Simple Serial Connector")

find_package(JNI)

if(APPLE)
    # Locate IOKit framework for serial/ioss.h
    find_library(IOKit IOKit)
    if(IOKit)
        find_path(IOKit_INCLUDE_DIR NAMES IOKitLib.h HINTS ${IOKit_INC_SEARCH_PATH} ${IOKit_PKGC_INCLUDE_DIRS} PATH_SUFFIXES IOKit)
        list(APPEND JSSC_ADDITIONAL_INCLUDES ${IOKit_INCLUDE_DIR})
    endif(IOKit)
endif(APPLE)

if(WIN32)
	SET(JSSC_PLATFORM "windows")
else()
	SET(JSSC_PLATFORM "_nix_based")
endif()

add_library(jssc SHARED
    src/cpp/${JSSC_PLATFORM}/jssc.cpp
)

target_include_directories(jssc PRIVATE ${JNI_INCLUDE_DIRS} ${JSSC_ADDITIONAL_INCLUDES})

set_target_properties(jssc PROPERTIES VERSION ${PROJECT_VERSION})
set_target_properties(jssc PROPERTIES PUBLIC_HEADER src/jssc_SerialNativeInterface.h)
