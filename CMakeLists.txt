cmake_minimum_required(VERSION 3.9)
project(jssc VERSION 2.8.0 DESCRIPTION "Java Simple Serial Connector")

find_package(JNI)

# Kitware-recommended technique for defaulting to 'Release' mode
if(NOT CMAKE_BUILD_TYPE)
	message(STATUS "Setting build type to 'Release' as none was specified.")
	set(CMAKE_BUILD_TYPE Release CACHE STRING "Choose the type of build." FORCE)
	# Set the possible values of build type for cmake-gui
	set_property(CACHE CMAKE_BUILD_TYPE PROPERTY STRINGS "Debug" "Release" "MinSizeRel" "RelWithDebInfo")
endif()

if(APPLE)
	# Locate IOKit framework for serial/ioss.h
	find_library(IOKit IOKit)
	if(IOKit)
		find_path(IOKit_INCLUDE_DIR NAMES IOKitLib.h HINTS ${IOKit_INC_SEARCH_PATH} ${IOKit_PKGC_INCLUDE_DIRS} PATH_SUFFIXES IOKit)
		list(APPEND JSSC_ADDITIONAL_INCLUDES ${IOKit_INCLUDE_DIR})
	endif()
endif()

if(WIN32)
	set(JSSC_PLATFORM "windows")
else()
	set(JSSC_PLATFORM "_nix_based")
endif()

add_library(jssc SHARED
    src/cpp/${JSSC_PLATFORM}/jssc.cpp
)

target_include_directories(jssc PRIVATE ${JNI_INCLUDE_DIRS} ${JSSC_ADDITIONAL_INCLUDES})

set_target_properties(jssc PROPERTIES VERSION ${PROJECT_VERSION})
set_target_properties(jssc PROPERTIES PUBLIC_HEADER src/jssc_SerialNativeInterface.h)
