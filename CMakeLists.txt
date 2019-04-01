cmake_minimum_required(VERSION 2.7)
cmake_policy(SET CMP0048 NEW)
cmake_policy(SET CMP0042 NEW)
project(jssc VERSION 2.7.1 LANGUAGES CXX)

find_package(Java)
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

# Statically link gcc/c++
if(CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
	if(WIN32)
		# Assume mingw, use "-static"
		set(CMAKE_CXX_STANDARD_LIBRARIES "-static ${CMAKE_CXX_STANDARD_LIBRARIES}")
	else()
		set(CMAKE_CXX_STANDARD_LIBRARIES "-static-libgcc -static-libstdc++ ${CMAKE_CXX_STANDARD_LIBRARIES}")
	endif()
	execute_process(COMMAND uname OUTPUT_VARIABLE OS_TYPE OUTPUT_STRIP_TRAILING_WHITESPACE)
	if(OS_TYPE STREQUAL "Darwin")
		# Try to locate JNI again for edge-case (e.g. cross-compiling)
		if(NOT JNI_FOUND)
			execute_process(COMMAND /usr/libexec/java_home OUTPUT_VARIABLE JAVA_HOME OUTPUT_STRIP_TRAILING_WHITESPACE)
			set(ENV{JAVA_HOME} ${JAVA_HOME})
			find_package(JNI)
		endif()
	endif()
elseif(MSVC)
	set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MT")
endif()

if(WIN32)
	set(JSSC_PLATFORM "windows")
	# Disable optimization for Release builds (XP/Server 2003)
	if(NOT MSVC)
		set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O0")
	else()
		set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /Od")
	endif()
else()
	set(JSSC_PLATFORM "_nix_based")
	# Sane level of optimization for Release builds
	set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2")
endif()


# append the generated header file (which should not be generated in to a source folder).
list(APPEND JSSC_ADDITIONAL_INCLUDES target/nar/javah-include/)

# output to maven conventions to target/generated-sources/<generator>
# The other part will be in conventions to native-lib-loader.

# linux, macos, etc
string(TOLOWER "${CMAKE_SYSTEM_NAME}" OS_NAME)
if(APPLE)
	# scijava expects "darwin", not "osx"
	set(OS_NAME "darwin")
endif()

# 32-bit or 64-bit
if("${CMAKE_SIZEOF_VOID_P}" EQUAL "8")
	set(OS_BITS 64)
else()
	set(OS_BITS 32)
endif()

# library (e.g. linux_64.so) TODO: Handle arm, hardfloat, etc
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ../classes/META-INF/lib/${OS_NAME}_${OS_BITS})

add_library(jssc SHARED
    src/cpp/${JSSC_PLATFORM}/jssc.cpp
)

target_include_directories(jssc PRIVATE ${JNI_INCLUDE_DIRS} ${JSSC_ADDITIONAL_INCLUDES})

set_target_properties(jssc PROPERTIES VERSION ${PROJECT_VERSION})
set_target_properties(jssc PROPERTIES PUBLIC_HEADER target/nar/javah-include/jssc_SerialNativeInterface.h)
set_target_properties(jssc PROPERTIES POSITION_INDEPENDENT_CODE ON)


# Call strip on non-debug builds
if(CMAKE_STRIP AND NOT CMAKE_BUILD_TYPE MATCHES "Deb")
	if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
		set(STRIP_ARGS -x)
	endif()
	add_custom_command(TARGET jssc POST_BUILD COMMAND "${CMAKE_STRIP}" ${STRIP_ARGS} $<TARGET_FILE:jssc>)
endif()
