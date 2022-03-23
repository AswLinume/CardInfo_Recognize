cmake_minimum_required(VERSION 3.10.2)

project("idcardrecognize")

include_directories(include)
file(GLOB my_source_path *.cpp *.c)
add_library(
        IDCardReg

        SHARED

        ${my_source_path})
add_library(lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so)
find_library(
        log-lib
        log
)
find_library(
        jnigraphics-lib
        jnigraphics)


target_link_libraries(
        IDCardReg

        ${log-lib}
        ${jnigraphics-lib}
        lib_opencv)
