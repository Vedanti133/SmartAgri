import subprocess
import sys
import time

SUITES = [
    ("Phase 5", "test_phase5.py"),
    ("Phase 6", "test_phase6.py"),
    ("Phase 7", "test_phase7.py"),
    ("Phase 8", "test_phase8.py"),
    ("Phase 9", "test_phase9.py"),
    ("Phase 10", "test_phase10.py"),
    ("Phase 11", "test_phase11.py"),
    ("Phase 12", "test_phase12.py"),
    ("Phase 13", "test_phase13.py"),
    ("Phase 14", "test_phase14.py"),
    ("Phase 15", "test_phase15.py"),
    ("Phase 16", "test_phase16.py"),
    ("Phase 17", "test_phase17.py")
]

def main():
    print("=" * 80)
    print("SMARTAGRI COMPLETE ENTERPRISE REGRESSION TEST HARNESS (PHASES 5 - 17)")
    print("=" * 80)

    total_passed = 0
    total_failed = 0
    results = []

    start_all = time.time()

    for phase_name, script_name in SUITES:
        print(f"\n>>> Running {phase_name} ({script_name})...")
        t0 = time.time()
        res = subprocess.run([sys.executable, script_name], capture_output=True, text=True)
        dur = time.time() - t0

        passed = 0
        failed = 0
        for line in res.stdout.splitlines():
            line_s = line.strip()
            if "[PASS]" in line_s:
                passed += 1
            elif "[FAIL]" in line_s:
                failed += 1

        total_passed += passed
        total_failed += failed

        status = "PASSED" if (res.returncode == 0 and failed == 0) else "FAILED"
        results.append((phase_name, script_name, passed, failed, dur, status))
        print(f"    [{status}] {phase_name}: {passed} passed, {failed} failed in {dur:.2f}s")
        if status == "FAILED":
            print("--- STDOUT (tail) ---")
            print("\n".join(res.stdout.splitlines()[-15:]))
            print("--- STDERR ---")
            print(res.stderr)

    total_dur = time.time() - start_all
    print("\n" + "=" * 80)
    print("FINAL REGRESSION SUITE CERTIFICATION SUMMARY")
    print("=" * 80)
    print(f"{'Phase':<12} | {'Script':<18} | {'Passed':<8} | {'Failed':<8} | {'Duration':<10} | {'Status'}")
    print("-" * 80)
    for phase_name, script_name, passed, failed, dur, status in results:
        print(f"{phase_name:<12} | {script_name:<18} | {passed:<8} | {failed:<8} | {dur:<10.2f} | {status}")
    print("-" * 80)
    print(f"TOTAL: {total_passed} PASSED, {total_failed} FAILED across {len(SUITES)} suites in {total_dur:.2f}s")
    if total_failed == 0:
        print(">>> 100% REGRESSION CERTIFICATION ACHIEVED! ZERO REGRESSIONS FOUND.")
    else:
        print(f">>> WARNING: {total_failed} TESTS FAILED. INVESTIGATION REQUIRED.")
    print("=" * 80)

if __name__ == "__main__":
    main()
