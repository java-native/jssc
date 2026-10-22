/* jSSC (Java Simple Serial Connector) - serial port communication library.
 *
 * This file is part of jSSC.
 *
 * jSSC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jSSC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with jSSC.  If not, see <http://www.gnu.org/licenses/>.
 */
#include "version.h"

/*
 * Build ident: version, git revision, target directory and build origin, e.g.
 *
 *   @(#)JSSC-IDENT version=2.10.5 rev=77a3c9d target=linux_64 origin=java-native/jssc
 *
 * It sits in a section of its own and is never referenced, so it is marked as used to survive
 * optimization, stripping and dead code elimination. Read it without loading the library:
 *
 *   grep -a -o 'JSSC-IDENT [[:print:]]*' <lib>      (any platform)
 *   readelf -p .jssc libjssc.so
 *   otool -s __TEXT __jssc libjssc.dylib     (or: what libjssc.dylib)
 *   objdump -s -j .jssc jssc.dll
 */
#if defined(_MSC_VER)
    #pragma section(".jssc", read)
    extern "C" __declspec(allocate(".jssc")) const char jssc_ident[] = JSSC_IDENT;
    #if defined(_M_IX86)
        #pragma comment(linker, "/include:_jssc_ident")
    #else
        #pragma comment(linker, "/include:jssc_ident")
    #endif
#elif defined(__APPLE__)
    extern "C" __attribute__((used, section("__TEXT,__jssc"))) const char jssc_ident[] = JSSC_IDENT;
#else
    extern "C" __attribute__((used, section(".jssc"))) const char jssc_ident[] = JSSC_IDENT;
#endif
