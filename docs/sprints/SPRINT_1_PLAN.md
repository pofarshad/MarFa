# Sprint 1 - MarFaNet v1.1.0 Development Cycle

**Duration**: 2 weeks (10 working days)  
**Sprint Goal**: Deliver split-tunneling MVP, WireGuard core integration POC, and real-time stats overlay skeleton

## 🎯 Sprint Objectives

### Core Deliverables
1. **Split-Tunneling**: App selector with Room DB integration
2. **WireGuard Engine**: Native library integration and JNI bridge
3. **Stats Overlay**: Real-time performance monitoring
4. **Compose Migration**: HomeScreen modernization
5. **Tooling Upgrade**: AGP 9 + Kotlin 2.0 implementation

## 📋 Epic Breakdown

### STU-001: Split-Tunneling UI
**Goal**: Enable per-app VPN routing control

| Task | Description | Owner | Estimate | Dependencies |
|------|-------------|-------|----------|--------------|
| STU-1 | Design Figma flow for App Selector | UX | 0.5d | - |
| STU-2 | Implement Room schema `AppRule` | Android Dev 1 | 0.5d | STU-1 |
| STU-3 | Compose UI + ViewModel | Android Dev 1 | 1d | STU-2 |
| STU-4 | Integrate into VPNService builder | Android Dev 2 | 1d | STU-3 |

**Acceptance Criteria**:
- User can select ≥3 apps for tunneling
- Xray config JSON updates on connect
- Unit tests pass with 90%+ coverage

### WG-002: WireGuard Engine
**Goal**: Integrate WireGuard as alternative to Xray

| Task | Description | Owner | Estimate | Dependencies |
|------|-------------|-------|----------|--------------|
| WG-1 | Cross-compile wireguard-go libs | Network Dev | 1d | - |
| WG-2 | Create JNI bridge `WgNative.kt` | Network Dev | 0.5d | WG-1 |
| WG-3 | Settings toggle + engine factory | Android Dev 2 | 0.5d | WG-2 |
| WG-4 | Throughput benchmark script | QA | 0.5d | WG-3 |

**Acceptance Criteria**:
- Demo connects to public WG endpoint
- Throughput benchmark ≥30% faster than Xray
- ARM64 and x86_64 native libraries included

### RSO-003: Stats Overlay
**Goal**: Real-time VPN performance monitoring

| Task | Description | Owner | Estimate | Dependencies |
|------|-------------|-------|----------|--------------|
| RSO-1 | Overlay permission flow | Android Dev 1 | 0.5d | - |
| RSO-2 | Stats polling coroutine | Android Dev 1 | 0.5d | RSO-1 |
| RSO-3 | Compose overlay UI | Android Dev 1 | 0.5d | RSO-2 |

**Acceptance Criteria**:
- Overlay appears while VPN active
- Shows RTT and up/down speed
- Hides on tap, lifecycle unit tests pass

## 📅 Sprint Timeline

### Week 1 (Days 1-5)
```
Day 1: TOOL-1 (AGP/Kotlin upgrade)    | WG-1 (WireGuard libs)     | STU-1 (Design)
Day 2: WG-2 (JNI bridge)              | RSO-1 (Permissions)       | STU-2 (Room schema)
Day 3: WG-3 (Settings toggle)         | RSO-2 (Stats polling)     | STU-3 (Compose UI)
Day 4: RSO-3 (Overlay UI)             | STU-4 (VPN integration)   | CMP-1 (HomeScreen)
Day 5: WG-4 (Benchmarks)              | Integration testing       | Code review
```

### Week 2 (Days 6-10)
```
Day 6: QA regression testing          | Bug fixes                 | Documentation
Day 7: Performance testing            | Security review           | Demo preparation
Day 8: Final integration              | Alpha build               | Testing cleanup
Day 9: Demo rehearsal                 | Stakeholder review        | Release prep
Day 10: Sprint demo                   | Retrospective             | Next sprint planning
```

## ✅ Definition of Done

### Technical Requirements
- [ ] All tasks merged to `develop` branch
- [ ] CI pipeline green with all checks passing
- [ ] Demo APK `v1.1.0-alpha1` available in GitHub Actions artifacts
- [ ] Crash-free automated test run on Firebase Test Lab
- [ ] Unit test coverage ≥90% for new code
- [ ] Security scan passes with no critical issues

### Documentation Requirements
- [ ] `SPLIT_TUNNELLING.md` updated with implementation details
- [ ] `WIREGUARD.md` created with integration guide
- [ ] API documentation updated for new components
- [ ] User guide updated with new features

## 🚧 Risk Management

### High-Risk Items
1. **WireGuard Legal/IP Review**
   - *Mitigation*: Quick legal review before merge
   - *Owner*: Legal team
   - *Deadline*: Day 3

2. **AGP 9 Stability Issues**
   - *Mitigation*: Keep fallback branch on AGP 8
   - *Owner*: DevOps
   - *Rollback plan*: Revert if blocking issues

3. **Overlay Permission OEM Compatibility**
   - *Mitigation*: Early testing on MIUI & EMUI
   - *Owner*: QA team
   - *Timeline*: Days 2-3

## 📊 Success Metrics

### Performance Targets
- **Build Time**: ≤20% increase from current
- **APK Size**: ≤5MB increase
- **Memory Usage**: ≤10% increase
- **Battery Drain**: ≤2% increase

### Feature Adoption (Post-Release)
- **Split-Tunneling**: ≥15% user adoption
- **WireGuard**: ≥25% adoption on 5G networks
- **Stats Overlay**: ≥60% engagement rate

---

**Sprint kickoff ready! Awaiting Patch 1.0.1 metrics green for 72h before full execution.**