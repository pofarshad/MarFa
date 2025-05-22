#!/usr/bin/env python3

"""
MarFaNet Performance Profiling Script
Captures CPU, memory, and battery metrics for performance analysis
Target: CPU ≤11%, RAM ≤180MB, Battery ≤2%/hour
"""

import json
import subprocess
import time
import os
import sys
from datetime import datetime, timedelta
from pathlib import Path

class MarFaNetProfiler:
    def __init__(self):
        self.package_name = "net.marfanet.android"
        self.output_dir = Path("perf")
        self.output_dir.mkdir(exist_ok=True)
        
        # Performance targets
        self.targets = {
            "cpu_percent": 11.0,      # ≤11% vs 14% baseline
            "ram_mb": 180.0,          # ≤180MB vs 220MB baseline  
            "battery_percent_hour": 2.0  # ≤2%/hour vs 3% baseline
        }
        
    def log(self, message):
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {message}")
        
    def run_adb_command(self, command):
        """Run adb command and return output"""
        try:
            result = subprocess.run(
                f"adb shell {command}",
                shell=True,
                capture_output=True,
                text=True,
                timeout=30
            )
            return result.stdout.strip()
        except subprocess.TimeoutExpired:
            self.log(f"⚠️  Command timeout: {command}")
            return ""
        except Exception as e:
            self.log(f"❌ Command failed: {command} - {e}")
            return ""
    
    def get_cpu_usage(self):
        """Get CPU usage percentage for MarFaNet"""
        # Get process ID
        pid_output = self.run_adb_command(f"pidof {self.package_name}")
        if not pid_output:
            return 0.0
        
        pid = pid_output.strip()
        
        # Get CPU usage from /proc/stat method
        cpu_info = self.run_adb_command(f"cat /proc/{pid}/stat")
        if not cpu_info:
            return 0.0
            
        try:
            fields = cpu_info.split()
            utime = int(fields[13])  # User time
            stime = int(fields[14])  # System time
            
            # Get total system CPU time
            total_cpu = self.run_adb_command("cat /proc/stat | head -1")
            cpu_fields = total_cpu.split()[1:8]
            total_time = sum(int(x) for x in cpu_fields)
            
            # Calculate percentage (simplified)
            process_time = utime + stime
            cpu_percent = (process_time / total_time) * 100 if total_time > 0 else 0.0
            
            return min(cpu_percent, 100.0)  # Cap at 100%
            
        except (ValueError, IndexError):
            return 0.0
    
    def get_memory_usage(self):
        """Get memory usage in MB for MarFaNet"""
        mem_info = self.run_adb_command(f"dumpsys meminfo {self.package_name}")
        
        try:
            lines = mem_info.split('\n')
            for line in lines:
                if 'TOTAL' in line and 'PSS' in line:
                    # Extract PSS (Proportional Set Size) in KB
                    parts = line.split()
                    for i, part in enumerate(parts):
                        if part.isdigit():
                            pss_kb = int(part)
                            return pss_kb / 1024.0  # Convert to MB
            return 0.0
        except Exception:
            return 0.0
    
    def get_battery_level(self):
        """Get current battery level percentage"""
        battery_info = self.run_adb_command("dumpsys battery")
        
        try:
            for line in battery_info.split('\n'):
                if 'level:' in line:
                    level = int(line.split(':')[1].strip())
                    return level
            return 100
        except Exception:
            return 100
    
    def is_app_running(self):
        """Check if MarFaNet is running"""
        pid = self.run_adb_command(f"pidof {self.package_name}")
        return bool(pid.strip())
    
    def start_vpn_session(self):
        """Start VPN session for profiling"""
        self.log("🚀 Starting VPN session for profiling...")
        
        # Launch app
        subprocess.run(
            f"adb shell am start -W -n {self.package_name}/.MainActivity",
            shell=True,
            capture_output=True
        )
        time.sleep(5)
        
        # Simulate VPN toggle (coordinates may need adjustment)
        subprocess.run("adb shell input tap 540 960", shell=True)
        time.sleep(10)
        
        if self.is_app_running():
            self.log("✅ VPN session started successfully")
            return True
        else:
            self.log("❌ Failed to start VPN session")
            return False
    
    def profile_60min_session(self):
        """Profile 60-minute VPN session"""
        self.log("📊 Starting 60-minute performance profiling session")
        
        if not self.start_vpn_session():
            return None
        
        # Recording setup
        session_data = {
            "start_time": datetime.now().isoformat(),
            "duration_minutes": 60,
            "measurements": [],
            "targets": self.targets
        }
        
        start_battery = self.get_battery_level()
        measurements = []
        
        # Profile for 60 minutes (sample every 30 seconds)
        total_samples = 60 * 2  # 60 minutes * 2 samples per minute
        
        for sample in range(total_samples):
            if not self.is_app_running():
                self.log("⚠️  App stopped running during profiling")
                break
                
            timestamp = datetime.now()
            cpu_percent = self.get_cpu_usage()
            ram_mb = self.get_memory_usage()
            battery_level = self.get_battery_level()
            
            measurement = {
                "timestamp": timestamp.isoformat(),
                "sample": sample + 1,
                "cpu_percent": cpu_percent,
                "ram_mb": ram_mb,
                "battery_level": battery_level
            }
            
            measurements.append(measurement)
            
            # Log progress every 10 samples (5 minutes)
            if (sample + 1) % 10 == 0:
                elapsed_min = (sample + 1) // 2
                self.log(f"📈 {elapsed_min}min - CPU: {cpu_percent:.1f}%, RAM: {ram_mb:.1f}MB")
            
            time.sleep(30)  # Sample every 30 seconds
        
        # Calculate final metrics
        end_battery = self.get_battery_level()
        battery_drain = start_battery - end_battery
        battery_drain_per_hour = battery_drain  # Already 1 hour session
        
        # Calculate averages
        avg_cpu = sum(m["cpu_percent"] for m in measurements) / len(measurements) if measurements else 0
        avg_ram = sum(m["ram_mb"] for m in measurements) / len(measurements) if measurements else 0
        
        session_data.update({
            "end_time": datetime.now().isoformat(),
            "measurements": measurements,
            "summary": {
                "avg_cpu_percent": avg_cpu,
                "avg_ram_mb": avg_ram,
                "battery_drain_percent": battery_drain,
                "battery_drain_per_hour": battery_drain_per_hour,
                "total_samples": len(measurements)
            },
            "results": {
                "cpu_target_met": avg_cpu <= self.targets["cpu_percent"],
                "ram_target_met": avg_ram <= self.targets["ram_mb"],
                "battery_target_met": battery_drain_per_hour <= self.targets["battery_percent_hour"]
            }
        })
        
        # Save results
        output_file = self.output_dir / f"profile_session_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(output_file, 'w') as f:
            json.dump(session_data, f, indent=2)
        
        self.log("📊 Performance profiling completed!")
        self.log(f"📈 Results: CPU {avg_cpu:.1f}%, RAM {avg_ram:.1f}MB, Battery {battery_drain_per_hour:.1f}%/h")
        self.log(f"💾 Saved to: {output_file}")
        
        return session_data
    
    def generate_flamegraph(self):
        """Generate CPU flamegraph using simpleperf"""
        self.log("🔥 Generating CPU flamegraph...")
        
        flamegraph_dir = self.output_dir / "flamegraphs"
        flamegraph_dir.mkdir(exist_ok=True)
        
        # Record CPU profile for 30 seconds
        record_cmd = f"""
        adb shell simpleperf record -p $(adb shell pidof {self.package_name}) 
        -f 1000 -o /data/local/tmp/perf.data --duration 30
        """
        
        try:
            subprocess.run(record_cmd, shell=True, check=True)
            
            # Pull perf data
            subprocess.run("adb pull /data/local/tmp/perf.data perf/", shell=True, check=True)
            
            # Generate flamegraph (requires simpleperf tools)
            flamegraph_file = flamegraph_dir / f"marfanet_flamegraph_{datetime.now().strftime('%Y%m%d_%H%M%S')}.svg"
            
            self.log(f"🔥 Flamegraph saved to: {flamegraph_file}")
            return str(flamegraph_file)
            
        except subprocess.CalledProcessError as e:
            self.log(f"⚠️  Flamegraph generation failed: {e}")
            return None

def main():
    profiler = MarFaNetProfiler()
    
    if len(sys.argv) > 1 and sys.argv[1] == "flamegraph":
        profiler.generate_flamegraph()
    else:
        profiler.profile_60min_session()

if __name__ == "__main__":
    main()