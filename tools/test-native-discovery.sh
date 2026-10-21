#!/usr/bin/env sh
set -eu
monitor_project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
monitor_test_dir=$(mktemp -d)
trap 'rm -rf "$monitor_test_dir"' EXIT HUP INT TERM
"${CXX:-c++}" -std=c++17 -Wall -Wextra -Werror -pthread \
    -I"$monitor_project_dir/tests/native/mocks" \
    -I"$monitor_project_dir/app/src/main/cpp" \
    "$monitor_project_dir/tests/native/source_finder_test.cpp" \
    -o "$monitor_test_dir/source-finder-test"
"$monitor_test_dir/source-finder-test"
