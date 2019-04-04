# Precompiled libraries

This folder contains precompiled libraries for distribution.

Whenever the interface of `SerialNativeInterface.java` changes, developers
are to recompile binaries for each platform.

## Valid paths

Note: Valid starting from `native-lib-loader` version 2.4.0 or later.

The current list of valid paths can be seen on the `native-lib-loader`
github page. At the time of writing, the supported list is:

  * `linux-arm-32`
  * `linux-x86-32`
  * `windows-x86-32`
  * `aix-ppc64-64`
  * `linux-amd64-64` (also alias of `linux-i386-64`)
  * `linux-aarch64-64` (instead of `linux-arm64-64`)
  * `linux-ppc64le-64`
  * `mac-amd64-64`
  * `mac-ppc64le-64`
  * `mac-x86_64-64`
  * `windows-amd64-64`

If a library is missing, please consider a pull request or open an issue.

## Legacy paths

The old path structure is ambigious, but supported for compatiblity.
This structure must be used before `native-lib-loader` version 2.4.0.

  * `linux_32`
  * `linux_64`
  * `linux_arm`
  * `linux_arm64`
  * `windows_32`
  * `windows_64`

## Activation

Compile using `mvn clean install -Ppackage-only` to activate this directory
for inclusion into the final jar.
