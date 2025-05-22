#!/bin/bash

# MarFaNet Post-Release Monitoring Setup
# Configures monitoring, alerting, and telemetry for GA release

set -e

echo "📊 MarFaNet Post-Release Monitoring Setup"
echo "=========================================="

# Create monitoring configuration
MONITOR_DIR="monitoring"
mkdir -p "$MONITOR_DIR"

echo "🚨 Setting up monitoring thresholds..."

# 1. Crashlytics Alert Configuration
cat > "$MONITOR_DIR/crashlytics_config.json" << EOF
{
  "alert_thresholds": {
    "crash_rate": {
      "critical": 0.002,
      "warning": 0.001,
      "description": "1 crash per 500 sessions = critical"
    },
    "crash_free_sessions": {
      "critical": 0.95,
      "warning": 0.97,
      "description": "Below 95% triggers critical alert"
    },
    "anr_rate": {
      "critical": 0.005,
      "warning": 0.003,
      "description": "ANR rate threshold"
    }
  },
  "notification_channels": [
    "slack://monitoring",
    "email://dev-team@marfanet.com"
  ]
}
EOF

# 2. Performance Monitoring Configuration
cat > "$MONITOR_DIR/performance_config.json" << EOF
{
  "kpi_targets": {
    "cold_start_ms": {
      "target": 1200,
      "alert_threshold": 1500,
      "baseline": 1100
    },
    "cpu_percent": {
      "target": 11.0,
      "alert_threshold": 15.0,
      "baseline": 10.5
    },
    "memory_mb": {
      "target": 180,
      "alert_threshold": 220,
      "baseline": 175
    },
    "battery_percent_hour": {
      "target": 2.0,
      "alert_threshold": 3.0,
      "baseline": 1.8
    }
  },
  "anomaly_detection": {
    "deviation_threshold": 0.20,
    "description": "Alert on ±20% baseline deviation"
  }
}
EOF

# 3. Network RTT Monitoring
cat > "$MONITOR_DIR/network_config.json" << EOF
{
  "endpoints": [
    {
      "name": "primary_servers",
      "urls": ["server1.marfanet.com", "server2.marfanet.com"],
      "expected_rtt_ms": 50,
      "alert_threshold_ms": 200
    },
    {
      "name": "fallback_servers", 
      "urls": ["fallback1.marfanet.com", "fallback2.marfanet.com"],
      "expected_rtt_ms": 100,
      "alert_threshold_ms": 300
    }
  ],
  "check_interval_minutes": 5,
  "uptime_target": 0.999
}
EOF

# 4. Store Monitoring Configuration
cat > "$MONITOR_DIR/store_config.json" << EOF
{
  "google_play": {
    "package_name": "net.marfanet.android",
    "rating_threshold": 4.0,
    "review_sentiment_threshold": 0.7,
    "install_rate_alert": "10% drop in 24h"
  },
  "fdroid": {
    "package_name": "net.marfanet.android",
    "update_status": "active",
    "build_status_check": true
  },
  "github_releases": {
    "download_tracking": true,
    "asset_integrity_check": true
  }
}
EOF

echo "✅ Monitoring configurations created"

# 5. Generate Monitoring Dashboard URLs
cat > "$MONITOR_DIR/dashboard_links.md" << EOF
# MarFaNet Monitoring Dashboards

## 📱 App Performance
- **Firebase Console**: https://console.firebase.google.com/project/marfanet-android
- **Crashlytics**: https://console.firebase.google.com/project/marfanet-android/crashlytics
- **Performance**: https://console.firebase.google.com/project/marfanet-android/performance

## 🛡️ Security & Compliance
- **OWASP Dashboard**: Internal security portal
- **Dependency Updates**: GitHub Dependabot alerts
- **License Monitoring**: GitHub license compliance

## 📊 Store Analytics
- **Google Play Console**: https://play.google.com/console/developers
- **F-Droid Status**: https://f-droid.org/packages/net.marfanet.android/
- **GitHub Insights**: https://github.com/marfanet/android/pulse

## 🚨 Alert Channels
- **Slack**: #marfanet-alerts
- **Email**: dev-team@marfanet.com
- **PagerDuty**: Production incidents only
EOF

echo "📋 Dashboard links generated"

# 6. Post-Release Support Schedule
cat > "$MONITOR_DIR/support_schedule.md" << EOF
# MarFaNet Post-Release Support Schedule

## Release Cadence
| Release Type | Frequency | Branch | Criteria |
|--------------|-----------|--------|----------|
| **Patch (1.0.x)** | As-needed | hotfix/* → release/1.0.x | Blocker/Major only |
| **Minor (1.x.0)** | ~8 weeks | develop | New features, backwards-compatible |
| **Major (x.0.0)** | TBD | roadmap planning | Breaking changes |

## Triage Labels
- \`regression\` - Something that worked in previous version
- \`security\` - Security vulnerability or concern
- \`enhancement\` - New feature request
- \`bug\` - Functional issue
- \`blocker\` - Prevents core functionality
- \`major\` - Significant impact on users

## Response SLA
- **Blocker**: 4 hours
- **Major**: 24 hours  
- **Minor**: 1 week
- **Enhancement**: Next minor release

## Hotfix Process
1. Create \`hotfix/issue-description\` branch from \`release/1.0.x\`
2. Implement minimal fix with tests
3. PR review + approval required
4. Merge to release branch
5. Tag new patch version (1.0.1, 1.0.2, etc.)
6. Auto-deploy via CI pipeline
EOF

echo "📅 Support schedule documented"

# 7. Beta Channel Sunset Plan
cat > "$MONITOR_DIR/beta_sunset.md" << EOF
# Beta Channel Sunset Plan

## Timeline
- **Day 0**: GA release v1.0.0 published
- **Day 1**: Notify beta testers of GA availability
- **Day 7**: Close public beta GitHub releases
- **Day 14**: Archive beta releases in releases/archives
- **Day 30**: Migrate Firebase beta testers to production opt-in

## Migration Steps
1. **Firebase App Distribution**:
   - Send notification to beta testers
   - Provide GA download links
   - Disable new beta builds

2. **GitHub Releases**:
   - Archive beta releases
   - Update README to remove beta links
   - Add deprecation notice

3. **Google Play Console**:
   - Promote closed beta users to production
   - Disable internal testing track
   - Clean up test accounts

## Data Preservation
- **Beta crash reports**: Export and archive
- **Performance metrics**: Save baseline data
- **User feedback**: Compile insights document
EOF

echo "🌅 Beta sunset plan ready"

echo ""
echo "✅ POST-RELEASE MONITORING SETUP COMPLETE"
echo "=========================================="
echo "📊 Monitoring configs: $MONITOR_DIR/"
echo "🚨 Alert thresholds: Configured for production"
echo "📅 Support schedule: Documented and ready"
echo "🌅 Beta sunset: Planned for gradual migration"

echo ""
echo "🎯 READY FOR GA LAUNCH!"
echo "Next: Run ./scripts/ga_readiness_check.sh to validate all criteria"