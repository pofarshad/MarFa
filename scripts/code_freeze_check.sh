#!/bin/bash

# MarFaNet Code Freeze Enforcement Script
# Ensures only bug fixes are committed after code freeze

set -e

echo "🔒 MarFaNet Code Freeze Enforcement Check"
echo "=========================================="

# Check if we're in code freeze period
CODE_FREEZE_DATE="2025-05-29"  # T+7 days from beta launch
CURRENT_DATE=$(date +%Y-%m-%d)

if [[ "$CURRENT_DATE" > "$CODE_FREEZE_DATE" ]]; then
    echo "🚫 CODE FREEZE ACTIVE since $CODE_FREEZE_DATE"
    FREEZE_ACTIVE=true
else
    echo "✅ Pre-freeze period - normal development allowed"
    FREEZE_ACTIVE=false
fi

# If freeze is active, check commit requirements
if [ "$FREEZE_ACTIVE" = true ]; then
    echo ""
    echo "🔍 Checking commit compliance..."
    
    # Get the current commit message
    COMMIT_MSG=$(git log -1 --pretty=%B)
    
    # Check if commit references a GitHub issue
    if ! echo "$COMMIT_MSG" | grep -qE "#[0-9]+|fixes #[0-9]+|closes #[0-9]+"; then
        echo "❌ FREEZE VIOLATION: Commit must reference GitHub issue ID"
        echo "   Format: 'fix: resolve connection timeout (fixes #123)'"
        exit 1
    fi
    
    # Extract issue number
    ISSUE_NUM=$(echo "$COMMIT_MSG" | grep -oE "#[0-9]+" | head -1 | tr -d '#')
    
    if [ -n "$ISSUE_NUM" ]; then
        echo "📋 Referenced issue: #$ISSUE_NUM"
        
        # Check if issue has required labels (would need GitHub API)
        echo "⚠️  Manual verification required:"
        echo "   - Issue #$ISSUE_NUM must have label 'bug' or 'docs'"
        echo "   - Issue must be approved for code freeze"
    fi
    
    # Check for new lint/detekt warnings
    echo ""
    echo "🔍 Checking for new warnings..."
    
    # Run lint and detekt
    ./gradlew lint detekt --quiet
    
    # Check lint results
    if [ -f app/build/reports/lint-results-debug.xml ]; then
        WARNINGS=$(grep -o 'severity="Warning"' app/build/reports/lint-results-debug.xml | wc -l)
        if [ "$WARNINGS" -gt 0 ]; then
            echo "❌ FREEZE VIOLATION: $WARNINGS new lint warnings found"
            echo "   All warnings must be resolved during code freeze"
            exit 1
        fi
    fi
    
    # Check detekt results
    if [ -f app/build/reports/detekt/detekt.xml ]; then
        DETEKT_ISSUES=$(grep -o '<error' app/build/reports/detekt/detekt.xml | wc -l)
        if [ "$DETEKT_ISSUES" -gt 0 ]; then
            echo "❌ FREEZE VIOLATION: $DETEKT_ISSUES detekt issues found"
            exit 1
        fi
    fi
    
    echo "✅ No new warnings detected"
    
    # Check if files modified are allowed during freeze
    MODIFIED_FILES=$(git diff --name-only HEAD~1)
    
    echo ""
    echo "📁 Modified files:"
    echo "$MODIFIED_FILES"
    
    # Restricted patterns during freeze
    if echo "$MODIFIED_FILES" | grep -qE "build\.gradle|settings\.gradle|dependencies"; then
        echo "⚠️  WARNING: Build configuration changes detected"
        echo "   Only bug fix dependencies allowed during freeze"
    fi
    
    if echo "$MODIFIED_FILES" | grep -qE "src/main/.*\.kt$" | grep -v test; then
        echo "⚠️  WARNING: Source code changes detected"
        echo "   Only bug fixes allowed - no new features"
    fi
    
    echo ""
    echo "✅ Code freeze compliance check completed"
    echo "📋 Manual review required for issue #$ISSUE_NUM"
    
else
    echo ""
    echo "✅ Normal development mode - all changes allowed"
fi

echo ""
echo "🚀 Ready to proceed"