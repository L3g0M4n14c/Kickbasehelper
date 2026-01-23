#!/bin/bash
cd KickbaseCore
echo "Building Swift Package..."
swift build
if [ $? -ne 0 ]; then
    echo "Swift Build Failed"
    exit 1
fi

cd ../Android
echo "Building Android App..."
./gradlew assembleDebug
