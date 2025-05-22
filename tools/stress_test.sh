#!/bin/bash

# MarFaNet Connection Stress Test
# Target: ≤1 disconnect error in 24h (vs 7 baseline)

set -e

PACKAGE_NAME="net.marfanet.android"
LOG_DIR="logs/stress"
TEST_DURATION_HOURS=24
CYCLE_DURATION_SECONDS=300  # 5 minutes per cycle
TOTAL_CYCLES=$((TEST_DURATION_HOURS * 60 / (CYCLE_DURATION_SECONDS / 60)))

echo "🔥 MarFaNet 24h Connection Stress Test"
echo "📊 Target: ≤1 disconnect error (vs 7 baseline)"
echo "⏱️  Duration: ${TEST_DURATION_HOURS}h (${TOTAL_CYCLES} cycles)"
echo "🔄 Cycle: ${CYCLE_DURATION_SECONDS}s connect/disconnect"

# Setup
mkdir -p "$LOG_DIR"
rm -f "$LOG_DIR"/*.log

# Counters
disconnect_errors=0
successful_cycles=0
start_time=$(date +%s)

# Log files
MAIN_LOG="$LOG_DIR/stress_test_$(date +%Y%m%d_%H%M%S).log"
ERROR_LOG="$LOG_DIR/disconnect_errors.log"
STATS_LOG="$LOG_DIR/stats.json"

echo "📝 Logs: $LOG_DIR"
echo "{\"start_time\": \"$(date -Iseconds)\", \"target_cycles\": $TOTAL_CYCLES}" > "$STATS_LOG"

# Functions
log_info() {
    echo "[$(date +'%H:%M:%S')] INFO: $1" | tee -a "$MAIN_LOG"
}

log_error() {
    echo "[$(date +'%H:%M:%S')] ERROR: $1" | tee -a "$MAIN_LOG" >> "$ERROR_LOG"
    ((disconnect_errors++))
}

check_vpn_status() {
    local status=$(adb shell dumpsys connectivity | grep -i "vpn" | grep -i "connected" | wc -l)
    echo $status
}

start_vpn_connection() {
    # Launch app
    adb shell am start -W -n "$PACKAGE_NAME/.MainActivity" > /dev/null 2>&1
    sleep 3
    
    # Simulate VPN toggle (adjust coordinates for your UI)
    adb shell input tap 540 960  # VPN toggle button
    sleep 5
    
    # Verify connection
    local connected=$(check_vpn_status)
    if [ "$connected" -eq 0 ]; then
        log_error "Failed to establish VPN connection in cycle $1"
        return 1
    fi
    
    return 0
}

stop_vpn_connection() {
    # Toggle VPN off
    adb shell input tap 540 960  # VPN toggle button
    sleep 3
    
    # Force stop to ensure clean state
    adb shell am force-stop "$PACKAGE_NAME" 2>/dev/null || true
    sleep 2
    
    return 0
}

# Main stress test loop
log_info "🚀 Starting 24h stress test - $TOTAL_CYCLES cycles"

for cycle in $(seq 1 $TOTAL_CYCLES); do
    cycle_start=$(date +%s)
    
    log_info "Cycle $cycle/$TOTAL_CYCLES - Starting VPN connection"
    
    # Start connection
    if start_vpn_connection $cycle; then
        log_info "Cycle $cycle - VPN connected successfully"
        
        # Monitor for half the cycle duration
        monitor_duration=$((CYCLE_DURATION_SECONDS / 2))
        sleep $monitor_duration
        
        # Check if still connected
        if [ "$(check_vpn_status)" -eq 0 ]; then
            log_error "VPN disconnected unexpectedly in cycle $cycle"
        else
            log_info "Cycle $cycle - Connection stable after ${monitor_duration}s"
        fi
        
        # Stop connection
        log_info "Cycle $cycle - Stopping VPN connection"
        stop_vpn_connection
        
        ((successful_cycles++))
    else
        log_error "Cycle $cycle - Failed to start VPN connection"
    fi
    
    # Wait for remainder of cycle
    cycle_end=$(date +%s)
    cycle_elapsed=$((cycle_end - cycle_start))
    remaining_wait=$((CYCLE_DURATION_SECONDS - cycle_elapsed))
    
    if [ $remaining_wait -gt 0 ]; then
        sleep $remaining_wait
    fi
    
    # Progress update every 10 cycles
    if [ $((cycle % 10)) -eq 0 ]; then
        elapsed_hours=$(( ($(date +%s) - start_time) / 3600 ))
        log_info "Progress: $cycle/$TOTAL_CYCLES cycles (${elapsed_hours}h elapsed)"
        log_info "Stats: $successful_cycles successful, $disconnect_errors errors"
    fi
done

# Final results
end_time=$(date +%s)
total_duration=$((end_time - start_time))
total_hours=$((total_duration / 3600))

log_info "🏁 Stress test completed!"
log_info "📊 Final Results:"
log_info "   Total cycles: $TOTAL_CYCLES"
log_info "   Successful: $successful_cycles"
log_info "   Disconnect errors: $disconnect_errors"
log_info "   Duration: ${total_hours}h"

# Update stats JSON
cat > "$STATS_LOG" << EOF
{
    "start_time": "$(date -d @$start_time -Iseconds)",
    "end_time": "$(date -d @$end_time -Iseconds)",
    "duration_hours": $total_hours,
    "total_cycles": $TOTAL_CYCLES,
    "successful_cycles": $successful_cycles,
    "disconnect_errors": $disconnect_errors,
    "target_errors": 1,
    "baseline_errors": 7,
    "test_passed": $([ $disconnect_errors -le 1 ] && echo "true" || echo "false")
}
EOF

# Exit code based on results
if [ $disconnect_errors -le 1 ]; then
    log_info "✅ STRESS TEST PASSED: $disconnect_errors ≤ 1 disconnect errors"
    exit 0
else
    log_info "❌ STRESS TEST FAILED: $disconnect_errors > 1 disconnect errors"
    exit 1
fi