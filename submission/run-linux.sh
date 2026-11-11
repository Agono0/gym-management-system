#!/bin/sh
# Compile-and-run script for Linux. Usage: ./run-linux.sh  (or: sh run-linux.sh)

cd "$(dirname "$0")" || exit 1

# Java uses ';' as classpath separator on Windows and ':' everywhere else,
# so the script also works from Git Bash on Windows
case "$(uname -s)" in
    CYGWIN*|MINGW*|MSYS*) CP_SEP=";" ;;
    *) CP_SEP=":" ;;
esac

if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
    echo
    echo "[ERROR] JDK 21 or newer is required. Install it and make sure javac and java are on PATH."
    exit 1
fi

if [ ! -d "out" ]; then
    mkdir out
fi
find src -name "*.java" > out/sources.txt

echo "[1/2] Compiling..."
if ! javac --release 21 -encoding UTF-8 -Xlint:all -cp "lib/*" -d out @out/sources.txt; then
    echo
    echo "[ERROR] Compilation failed. Verify JDK 21 or newer is installed on PATH."
    exit 1
fi

echo "[2/2] Starting GYM Management System..."
java --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -cp "out${CP_SEP}lib/*" com.gym.Main
status=$?
if [ $status -ne 0 ]; then
    echo
    echo "[ERROR] GYM Management System exited with code $status."
fi
exit $status
