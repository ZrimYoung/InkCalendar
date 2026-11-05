#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
if [ -n "${INK_CALENDAR_TOOLS:-}" ]; then
    TOOL_ROOT=$INK_CALENDAR_TOOLS
elif [ -d "$PROJECT_DIR/../.tools/ink-calendar" ]; then
    TOOL_ROOT="$PROJECT_DIR/../.tools/ink-calendar"
else
    TOOL_ROOT="$PROJECT_DIR/../../Tools/ink-calendar"
fi
if [ "$#" -eq 0 ]; then
    set -- testDebugUnitTest lintDebug assembleDebug
fi
if [ -x "$TOOL_ROOT/gradle-8.9/bin/gradle" ] && [ -x "$TOOL_ROOT/jdk-17.0.21+9/Contents/Home/bin/java" ]; then
    export JAVA_HOME="$TOOL_ROOT/jdk-17.0.21+9/Contents/Home"
    export ANDROID_HOME="$TOOL_ROOT/android-sdk"
    export ANDROID_USER_HOME="$TOOL_ROOT/android-user"
    export GRADLE_USER_HOME="$TOOL_ROOT/gradle-cache"
    exec "$TOOL_ROOT/gradle-8.9/bin/gradle" -p "$PROJECT_DIR" --no-daemon "$@"
fi
exec "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" "$@"
