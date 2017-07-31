cmake_minimum_required(VERSION 3.8)
project(jqfakeclient)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES
        libqfakeclient/include/channel.h
        libqfakeclient/include/client.h
        libqfakeclient/include/command_buffer.h
        libqfakeclient/include/command_parser.h
        libqfakeclient/include/common.h
        libqfakeclient/include/console.h
        libqfakeclient/include/message_parser.h
        libqfakeclient/include/network_address.h
        libqfakeclient/include/protocol_executor.h
        libqfakeclient/include/socket.h
        libqfakeclient/include/system.h
        libqfakeclient/src/channel.cpp
        libqfakeclient/src/client.cpp
        libqfakeclient/src/command_buffer.cpp
        libqfakeclient/src/command_parser.cpp
        libqfakeclient/src/console.cpp
        libqfakeclient/src/message_parser.cpp
        libqfakeclient/src/network_address.cpp
        libqfakeclient/src/protocol_executor.cpp
        libqfakeclient/src/socket.cpp
        libqfakeclient/src/system.cpp
        libqfakeclient/main.cpp
        library/build/generated/jni_headers/jqfakeclient.h
        library/src/main/cpp/jqfakeclient.cpp)

include_directories(libqfakeclient/include)
include_directories(library/build/generated/jni_headers)

add_executable(jqfakeclient ${SOURCE_FILES})