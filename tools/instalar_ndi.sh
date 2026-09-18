#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
    echo "Uso: ./tools/instalar_ndi.sh '/caminho/NDI SDK for Android'"
    exit 1
fi

SDK_DIR=$1
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
HEADER=$(find "$SDK_DIR" -type f -name 'Processing.NDI.Lib.h' -print -quit)

if [ -z "$HEADER" ]; then
    echo "Processing.NDI.Lib.h nao foi encontrado."
    exit 1
fi

HEADER_DIR=$(dirname "$HEADER")
mkdir -p "$PROJECT_DIR/app/src/main/cpp/include"
cp "$HEADER_DIR"/*.h "$PROJECT_DIR/app/src/main/cpp/include/"

FOUND_ARM64=0
FOUND_ARM32=0
find "$SDK_DIR" -type f -name 'libndi.so' | while IFS= read -r LIB; do
    case "$(printf '%s' "$LIB" | tr '[:upper:]' '[:lower:]')" in
        *arm64-v8a*|*aarch64*|*arm64*)
            mkdir -p "$PROJECT_DIR/app/src/main/jniLibs/arm64-v8a"
            cp "$LIB" "$PROJECT_DIR/app/src/main/jniLibs/arm64-v8a/libndi.so"
            ;;
        *armeabi-v7a*|*armv7*|*armhf*|*arm32*)
            mkdir -p "$PROJECT_DIR/app/src/main/jniLibs/armeabi-v7a"
            cp "$LIB" "$PROJECT_DIR/app/src/main/jniLibs/armeabi-v7a/libndi.so"
            ;;
    esac
done

rm -f "$PROJECT_DIR/app/src/main/cpp/include/COLOQUE_OS_HEADERS_NDI_AQUI.txt"
test -f "$PROJECT_DIR/app/src/main/jniLibs/arm64-v8a/libndi.so" && FOUND_ARM64=1
test -f "$PROJECT_DIR/app/src/main/jniLibs/armeabi-v7a/libndi.so" && FOUND_ARM32=1

if [ "$FOUND_ARM64" -ne 1 ]; then
    echo "libndi.so ARM64 nao foi encontrada."
    exit 1
fi

rm -f "$PROJECT_DIR/app/src/main/jniLibs/arm64-v8a/COLOQUE_LIBNDI_SO_AQUI.txt"
if [ "$FOUND_ARM32" -eq 1 ]; then
    rm -f "$PROJECT_DIR/app/src/main/jniLibs/armeabi-v7a/COLOQUE_LIBNDI_SO_AQUI.txt"
fi

rm -rf "$PROJECT_DIR/app/.cxx" "$PROJECT_DIR/app/build"
touch "$PROJECT_DIR/app/src/main/cpp/CMakeLists.txt"

echo "Integracao copiada. Sincronize e compile o projeto no Android Studio."
