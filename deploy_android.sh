#!/bin/bash

# Build the Android APK
echo "Building Android App..."
./build_android.sh

if [ $? -ne 0 ]; then
    echo "Build failed. Aborting deployment."
    exit 1
fi

# Define variables
APK_PATH="Android/app/build/outputs/apk/debug/app-debug.apk"
APP_ID="1:967761670755:android:6a7c4b68b7722ce8883e56"
RELEASE_NOTES="Deployed via deploy_android.sh script"

if [ ! -f "$APK_PATH" ]; then
    echo "APK not found at $APK_PATH"
    exit 1
fi

echo "Deploying to Firebase App Distribution..."
firebase appdistribution:distribute "$APK_PATH" \
    --app "$APP_ID" \
    --release-notes "$RELEASE_NOTES"

echo "Deployment complete!"
