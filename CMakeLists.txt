cmake_minimum_required(VERSION 3.5)
cmake_policy(SET CMP0048 NEW)
cmake_policy(SET CMP0042 NEW)

# Get some architecture information
if(WIN32)
    string(TOLOWER $ENV{PROCESSOR_ARCHITECTURE} OS_ARCH)
    set(OS_TYPE "Windows")
else()
	execute_process(COMMAND uname -m OUTPUT_VARIABLE OS_ARCH OUTPUT_STRIP_TRAILING_WHITESPACE)
	execute_process(COMMAND uname OUTPUT_VARIABLE OS_TYPE OUTPUT_STRIP_TRAILING_WHITESPACE)
endif()

# Determine if compiling on Apple and host arch before starting project
if(APPLE AND NOT CMAKE_OSX_ARCHITECTURES)
	set(CMAKE_OSX_ARCHITECTURES "${OS_ARCH}" CACHE STRING "Build architectures for Mac OS X" FORCE)
endif()

project(jssc VERSION "" LANGUAGES CXX)

# Use JAVA_HOME from Maven
if(JAVA_HOME)
    # Sanitize path for cmake
    file(TO_CMAKE_PATH "${JAVA_HOME}" JAVA_HOME)
    message(STATUS "Picked up JAVA_HOME from Maven: ${JAVA_HOME}")
    # Sanitize accidental jre selection on Linux
    if(JAVA_HOME MATCHES "/jre$")
        STRING(REGEX REPLACE "/jre$" "/jdk" JAVA_HOME "${JAVA_HOME}")
        message(WARNING "Maven JAVA_HOME appears to be a JRE,  trying to fix: ${JAVA_HOME}")
    endif()
endif()

find_package(Java)
# Workaround for FindJNI, per https://stackoverflow.com/a/51764145/3196753
set(JAVA_AWT_LIBRARY HeaderOnly)
set(JAVA_JVM_LIBRARY HeaderOnly)
set(JAVA_AWT_INCLUDE_PATH HeaderOnly)
find_package(JNI REQUIRED)

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

	# Handle edge-case with legacy versioned SDK headers (e.g. usr/include/c++/4.2.1)
	if(CMAKE_OSX_SYSROOT)
	    file(GLOB std_versions "${CMAKE_OSX_SYSROOT}/usr/include/c++/*")
	    foreach(std_version IN LISTS std_versions)
	        if("${std_version}" MATCHES "/v[0-9]*$")
	             # Ignore c++/v1 (should already be included)
	            continue()
	        elseif(IS_DIRECTORY "${std_version}")
	            message(STATUS "Adding legacy include path: ${std_version}")
	            list(APPEND JSSC_ADDITIONAL_INCLUDES "${std_version}")
	        endif()
        endforeach()
	endif()
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Statically link gcc/c++
if(MSVC)
	# /MT = Multithread, static version of the run-time library
	set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /MT")
	# Reproducible DLLs: /Brepro puts a hash of the content where the link time would go
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /Brepro")
	set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} /Brepro")
elseif(WIN32)
	# Assume mingw, use "-static"
	set(CMAKE_CXX_STANDARD_LIBRARIES "-static ${CMAKE_CXX_STANDARD_LIBRARIES}")
	# Reproducible DLLs: no link time in the PE header, export or debug directory
	set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-insert-timestamp")
elseif(CMAKE_CXX_COMPILER_ID STREQUAL "GNU" AND NOT CMAKE_SYSTEM_NAME STREQUAL "SunOS")
	set(CMAKE_CXX_STANDARD_LIBRARIES "-static-libgcc -static-libstdc++ ${CMAKE_CXX_STANDARD_LIBRARIES}")
endif()

if(WIN32)
	set(CPP_SOURCE "windows")
	# Version resource (jssc.rc.in)
	enable_language(RC)
	# Remove "lib" prefix from windows binaries
	set(CMAKE_SHARED_LIBRARY_PREFIX "")
	# Disable optimization for Release builds (XP/Server 2003)
	if(NOT MSVC)
		set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O0")
	else()
		set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} /Od")
	endif()
else()
	set(CPP_SOURCE "_nix_based")
	# Sane level of optimization for Release builds
	set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2")
endif()

# Detect platform if -DNATIVE_LIB_DIR is not provided
# TODO: Handle arm, hardfloat, etc
if(NOT NATIVE_LIB_DIR OR "${NATIVE_LIB_DIR}" STREQUAL "")
	# windows, linux, darwin, etc
	string(TOLOWER "${CMAKE_SYSTEM_NAME}" OS_NAME)
	if(OS_NAME MATCHES "darwin")
		set(OS_NAME "osx")
	endif()

	# bitness/suffix
	if(CMAKE_OSX_ARCHITECTURES)
        # can occur when calling cmake directly
        if(CMAKE_OSX_ARCHITECTURES STREQUAL "arm64")
            set(OS_SUFFIX "arm64")
        else()
            set(OS_SUFFIX 64)
        endif()
	elseif("${OS_ARCH}" STREQUAL "aarch64" OR "${OS_ARCH}" STREQUAL "arm64")
		set(OS_SUFFIX "arm64")
	elseif("${OS_ARCH}" MATCHES "arm")
		set(OS_SUFFIX "arm")
	elseif("${OS_ARCH}" MATCHES "riscv32")
		set(OS_SUFFIX "riscv32")
	elseif("${OS_ARCH}" MATCHES "riscv64")
		set(OS_SUFFIX "riscv64")
	elseif(FORCE_M32)
		set(OS_SUFFIX 32)
	elseif(FORCE_M64)
		set(OS_SUFFIX 64)
	else()
		if("${CMAKE_SIZEOF_VOID_P}" EQUAL "8")
			set(OS_SUFFIX 64)
		else()
			set(OS_SUFFIX 32)
		endif()
	endif()

	# handle warnings
	if(OS_SUFFIX STREQUAL "arm")
        if(${CMAKE_VERSION} VERSION_LESS "3.10.0")
            message(WARNING "Cmake < 3.10.0 can't detect HAS_FPU.  This only affects arm 32-bit builds.")
        else()
            cmake_host_system_information(RESULT OS_SUFFIX_SF QUERY HAS_FPU)
            # warn of native-lib-loader shortcomming
            IF(NOT OS_SUFFIX_SF EQUAL 1)
                message(WARNING "Soft float detected. We can't determine this at runtime; Compatibility will suffer.")
            ENDIF()
        endif()
    endif()

	set(NATIVE_LIB_DIR ${OS_NAME}_${OS_SUFFIX})
endif()
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/natives/${NATIVE_LIB_DIR})

# version.h using #cmakedefine for version from pom.xml.
if(NOT JSSC_VERSION)
    # Fallback to parsing pom.xml if not provided
    set(JSSC_VERSION "0.0.0-UNKNOWN")
    file(STRINGS ${CMAKE_CURRENT_SOURCE_DIR}/pom.xml POM_FILE)
    foreach(POM_LINE ${POM_FILE})
        # Assume first "<version>" is the project version
        if(POM_LINE MATCHES "<version>")
            string(REGEX REPLACE "^[ \t]+|<[^>]*>" "" DETECTED_VERSION "${POM_LINE}")
            string(STRIP "${DETECTED_VERSION}" DETECTED_VERSION)
            if(DETECTED_VERSION STREQUAL "")
                message(WARNING "Could not parse JSSC version from pom.xml, defaulting to \"${JSSC_VERSION}\"")
            else()
                set(JSSC_VERSION "${DETECTED_VERSION}")
                message(STATUS "Found JSSC version \"${JSSC_VERSION}\" in pom.xml")
            endif()
            break()
        endif()
    endforeach()
else()
    message(STATUS "JSSC version passed in: \"${JSSC_VERSION}\"")
endif()
# Build ident (src/main/cpp/ident.cpp). ant/build.xml passes the revision and origin; when blank,
# the revision is taken from git ("-dirty" if native sources have uncommitted changes) and origin=local.
if(NOT JSSC_BUILD_REV)
    set(JSSC_BUILD_REV "unknown")
    find_package(Git QUIET)
    if(GIT_FOUND)
        execute_process(COMMAND ${GIT_EXECUTABLE} rev-parse --short HEAD
            WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
            RESULT_VARIABLE GIT_RESULT OUTPUT_VARIABLE GIT_REV
            OUTPUT_STRIP_TRAILING_WHITESPACE ERROR_QUIET)
        if(GIT_RESULT EQUAL 0)
            set(JSSC_BUILD_REV "${GIT_REV}")
            execute_process(COMMAND ${GIT_EXECUTABLE} status --porcelain -- src/main/cpp toolchain CMakeLists.txt
                WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
                OUTPUT_VARIABLE GIT_DIRTY OUTPUT_STRIP_TRAILING_WHITESPACE ERROR_QUIET)
            if(GIT_DIRTY)
                set(JSSC_BUILD_REV "${JSSC_BUILD_REV}-dirty")
            endif()
        endif()
    endif()
endif()
if(NOT JSSC_BUILD_ORIGIN)
    set(JSSC_BUILD_ORIGIN "local")
endif()
set(JSSC_BUILD_INFO "version=${JSSC_VERSION} rev=${JSSC_BUILD_REV} target=${NATIVE_LIB_DIR} origin=${JSSC_BUILD_ORIGIN}")
set(JSSC_IDENT "@(#)JSSC-IDENT ${JSSC_BUILD_INFO}")
message(STATUS "Native library ident: ${JSSC_IDENT}")
configure_file(src/main/cpp/version.h.in ${CMAKE_CURRENT_BINARY_DIR}/version.h @ONLY)

add_library(jssc SHARED src/main/cpp/${CPP_SOURCE}/jssc.cpp src/main/cpp/ident.cpp)

if(WIN32)
	# VERSIONINFO wants numbers: 2.10.5-SNAPSHOT becomes 2,10,5,0
	string(REGEX MATCH "^([0-9]+)\\.([0-9]+)\\.([0-9]+)" JSSC_VERSION_NUMERIC "${JSSC_VERSION}")
	set(JSSC_VERSION_MAJOR ${CMAKE_MATCH_1})
	set(JSSC_VERSION_MINOR ${CMAKE_MATCH_2})
	set(JSSC_VERSION_PATCH ${CMAKE_MATCH_3})
	# Vendor and copyright are fixed in pom.xml, so read them from there for maven and plain cmake builds alike
	file(READ ${CMAKE_CURRENT_SOURCE_DIR}/pom.xml POM_CONTENT)
	string(REGEX MATCH "<organization>[ \t\r\n]*<name>([^<]*)</name>" POM_MATCH "${POM_CONTENT}")
	set(JSSC_VENDOR "${CMAKE_MATCH_1}")
	string(REGEX MATCH "<jssc.copyright>([^<]*)</jssc.copyright>" POM_MATCH "${POM_CONTENT}")
	set(JSSC_COPYRIGHT "${CMAKE_MATCH_1}")
	if(NOT JSSC_VENDOR OR NOT JSSC_COPYRIGHT)
		message(WARNING "Could not read vendor or copyright from pom.xml, VERSIONINFO will be incomplete")
	endif()
	configure_file(src/main/cpp/windows/jssc.rc.in ${CMAKE_CURRENT_BINARY_DIR}/jssc.rc @ONLY)
	target_sources(jssc PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/jssc.rc)
endif()

# Fall-back header for when maven is not available
list(APPEND JSSC_ADDITIONAL_INCLUDES src/main/cpp/)
target_include_directories(jssc PRIVATE ${JNI_INCLUDE_DIRS} ${CMAKE_CURRENT_BINARY_DIR} ${JSSC_ADDITIONAL_INCLUDES})

set_target_properties(jssc PROPERTIES PUBLIC_HEADER ${CMAKE_CURRENT_BINARY_DIR}/jssc_SerialNativeInterface.h)
set_target_properties(jssc PROPERTIES POSITION_INDEPENDENT_CODE ON)
if(WIN32)
	# Fix paths for MSVC (Debug/Release) and MINGW
	set_target_properties(jssc PROPERTIES RUNTIME_OUTPUT_DIRECTORY_DEBUG "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
	set_target_properties(jssc PROPERTIES RUNTIME_OUTPUT_DIRECTORY_RELEASE "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
endif()

if(CMAKE_CXX_COMPILER_ID STREQUAL "GNU" AND NOT WIN32 AND NOT FORCE_IGNORE)
    if(FORCE_M32)
        # Build 32-bit binary on Linux
        set_target_properties(jssc PROPERTIES COMPILE_FLAGS -m32 LINK_FLAGS -m32)
    elseif(FORCE_M64)
        # Build 64-bit binary on Linux
        set_target_properties(jssc PROPERTIES COMPILE_FLAGS -m64 LINK_FLAGS -m64)
    endif()
endif()

# Call strip on non-debug builds
if(CMAKE_STRIP AND NOT CMAKE_BUILD_TYPE MATCHES "Deb")
	if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
		set(STRIP_ARGS -x)
	endif()
	add_custom_command(TARGET jssc POST_BUILD COMMAND "${CMAKE_STRIP}" ${STRIP_ARGS} $<TARGET_FILE:jssc>)
endif()

# Copy native library to target/classes for processing by junit, maven
add_custom_command(TARGET jssc POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_directory ${CMAKE_CURRENT_BINARY_DIR}/natives/ ${CMAKE_CURRENT_BINARY_DIR}/../classes/natives/
)

# Handle compiler warnings
if(MSVC)
	#TODO Treat warnings as errors /WX
	target_compile_options(jssc PRIVATE /W4)
else()
	#TODO Treat warnings as errors -Werror
	target_compile_options(jssc PRIVATE -Wall -Wextra -pedantic -Wno-long-long)
endif()
