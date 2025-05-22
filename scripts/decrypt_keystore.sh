#!/bin/bash

# MarFaNet Keystore Decryption Script
# Decrypts release signing keystore from encrypted storage

set -e

echo "🔐 Decrypting MarFaNet release keystore..."

# Check required environment variables
if [ -z "$KEYSTORE_PASSWORD" ]; then
    echo "❌ KEYSTORE_PASSWORD environment variable not set"
    exit 1
fi

if [ -z "$KEY_PASSWORD" ]; then
    echo "❌ KEY_PASSWORD environment variable not set"
    exit 1
fi

# Create keystore directory
mkdir -p app/keystore

# For CI/CD: keystore should be base64 encoded and stored as secret
if [ ! -z "$KEYSTORE_BASE64" ]; then
    echo "📦 Decoding keystore from base64..."
    echo "$KEYSTORE_BASE64" | base64 -d > app/keystore/marfanet-release.jks
else
    echo "⚠️  KEYSTORE_BASE64 not found - assuming local development"
    
    # For local development, copy from secure location
    if [ -f ~/.android/marfanet-release.jks ]; then
        cp ~/.android/marfanet-release.jks app/keystore/marfanet-release.jks
    else
        echo "❌ Release keystore not found in ~/.android/marfanet-release.jks"
        echo "📝 Please generate keystore with:"
        echo "   keytool -genkey -v -keystore ~/.android/marfanet-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias marfanet"
        exit 1
    fi
fi

# Verify keystore
if [ ! -f app/keystore/marfanet-release.jks ]; then
    echo "❌ Failed to create keystore file"
    exit 1
fi

# Test keystore access
keytool -list -keystore app/keystore/marfanet-release.jks -storepass "$KEYSTORE_PASSWORD" -alias marfanet > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Keystore decrypted and verified successfully"
else
    echo "❌ Keystore verification failed - check password"
    exit 1
fi

echo "🚀 Ready for release build!"