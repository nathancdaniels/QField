set(VCPKG_TARGET_ARCHITECTURE arm)
set(VCPKG_CRT_LINKAGE static)
set(VCPKG_LIBRARY_LINKAGE static)
set(VCPKG_CMAKE_SYSTEM_NAME Android)

if(PORT STREQUAL "openssl")
  set(VCPKG_LIBRARY_LINKAGE dynamic)
endif()
