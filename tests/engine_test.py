#!/usr/bin/env python3

import signal
import sys

signal.signal(signal.SIGINT, lambda *_: (print(f"\n\033[33mInterrupted.\033[0m"), sys.exit(130)))

import argparse
import csv
import io
import math
import subprocess
import time

import requests

CONFIG = {"url": "http://localhost:8081/api/v1/simulate"}

COLORS = {
    "green": "\033[32m",
    "red": "\033[31m",
    "yellow": "\033[33m",
    "cyan": "\033[36m",
    "gray": "\033[90m",
    "bold": "\033[1m",
    "reset": "\033[0m",
}


def c(color: str, text: str) -> str:
    return f"{COLORS[color]}{text}{COLORS['reset']}"


def send_netlist(netlist: str) -> requests.Response:
    return requests.post(CONFIG["url"], data=netlist, timeout=10)


def parse_csv(text: str) -> list[dict[str, str]]:
    reader = csv.DictReader(io.StringIO(text))
    return list(reader)


def print_csv_table(text: str) -> None:
    rows = parse_csv(text)
    if not rows:
        print(c("yellow", "  (empty result)"))
        return
    headers = list(rows[0].keys())
    col_widths = [max(len(h), max((len(r[h]) for r in rows), default=0)) for h in headers]
    header_line = " | ".join(h.rjust(w) for h, w in zip(headers, col_widths))
    print(f"  {c('bold', header_line)}")
    print(f"  {'-+-'.join('-' * w for w in col_widths)}")
    for row in rows:
        line = " | ".join(row[h].rjust(w) for h, w in zip(headers, col_widths))
        print(f"  {line}")


def assert_close(actual: float, expected: float, tol: float = 1e-3, label: str = "") -> bool:
    if abs(expected) > 1e-12:
        err = abs((actual - expected) / expected)
    else:
        err = abs(actual - expected)
    ok = err <= tol
    status = c("green", "PASS") if ok else c("red", "FAIL")
    detail = f"{label}: actual={actual:.6f} expected={expected:.6f} err={err:.6f}"
    print(f"    [{status}] {detail}")
    return ok


PRESETS = {}


def preset(name: str, description: str):
    def decorator(func):
        PRESETS[name] = (func, description)
        return func
    return decorator


@preset("dc_divider", "DC voltage divider (Example A)")
def test_dc_divider(verbose: bool) -> bool:
    netlist = (
        "* Dzielnik napiecia (5V > R1k > OUT > R1k > GND)\n"
        "VSRC V1 IN 0 type=dc val=5.0\n"
        "RES R1 IN OUT val=1000\n"
        "RES R2 OUT 0 val=1000\n"
        ".SIMULATE type=dc\n"
    )
    resp = send_netlist(netlist)
    if resp.status_code != 200:
        print(c("red", f"  HTTP {resp.status_code}: {resp.text}"))
        return False

    if verbose:
        print_csv_table(resp.text)

    rows = parse_csv(resp.text)
    ok = True
    ok &= assert_close(float(rows[0]["V(IN)"]), 5.0, label="V(IN)")
    ok &= assert_close(float(rows[0]["V(OUT)"]), 2.5, label="V(OUT)")
    ok &= assert_close(float(rows[0]["I(V1)"]), -0.0025, label="I(V1)")
    return ok


@preset("dc_isrc", "DC current source (Example C)")
def test_dc_isrc(verbose: bool) -> bool:
    netlist = (
        "* Zrodlo pradowe 20mA w rezystor 500 Ohm\n"
        "ISRC I1 0 OUT type=dc val=0.02\n"
        "RES R1 OUT 0 val=500\n"
        ".SIMULATE type=dc\n"
    )
    resp = send_netlist(netlist)
    if resp.status_code != 200:
        print(c("red", f"  HTTP {resp.status_code}: {resp.text}"))
        return False

    if verbose:
        print_csv_table(resp.text)

    rows = parse_csv(resp.text)
    ok = True
    ok &= assert_close(float(rows[0]["V(OUT)"]), 10.0, label="V(OUT)")
    return ok


@preset("rc_transient", "RC transient charging (Example B)")
def test_rc_transient(verbose: bool) -> bool:
    netlist = (
        "VSRC V1 IN 0 type=dc val=5.0\n"
        "RES R1 IN OUT val=1000\n"
        "CAP C1 OUT 0 val=1e-6\n"
        ".SIMULATE type=trans tstop=0.005 tstep=0.0001\n"
    )
    resp = send_netlist(netlist)
    if resp.status_code != 200:
        print(c("red", f"  HTTP {resp.status_code}: {resp.text}"))
        return False

    if verbose:
        print_csv_table(resp.text)

    rows = parse_csv(resp.text)
    tau = 1000.0 * 1e-6
    ok = True
    for row in rows:
        t = float(row["time"])
        v_out = float(row["V(OUT)"])
        v_expected = 5.0 * (1.0 - math.exp(-t / tau))
        ok &= assert_close(v_out, v_expected, tol=0.05, label=f"V(OUT) @ t={t:.6f}")
    return ok


@preset("rl_transient", "RL transient (inductor current build-up)")
def test_rl_transient(verbose: bool) -> bool:
    netlist = (
        "VSRC V1 IN 0 type=dc val=10.0\n"
        "RES R1 IN OUT val=100\n"
        "IND L1 OUT 0 val=0.1\n"
        ".SIMULATE type=trans tstop=0.005 tstep=0.0001\n"
    )
    resp = send_netlist(netlist)
    if resp.status_code != 200:
        print(c("red", f"  HTTP {resp.status_code}: {resp.text}"))
        return False

    if verbose:
        print_csv_table(resp.text)

    rows = parse_csv(resp.text)
    R = 100.0
    L = 0.1
    V = 10.0
    tau = L / R
    i_ss = V / R
    ok = True
    for row in rows:
        t = float(row["time"])
        i_v1 = float(row["I(V1)"])
        i_expected = -i_ss * (1.0 - math.exp(-t / tau))
        ok &= assert_close(i_v1, i_expected, tol=0.05, label=f"I(V1) @ t={t:.4f}")
    ok &= assert_close(float(rows[0]["I(V1)"]), 0.0, tol=1e-6, label="I(V1) @ t=0 == 0 (zero IC)")
    v_out_last = float(rows[-1]["V(OUT)"])
    ok &= assert_close(v_out_last, 0.0, tol=0.15, label="V(OUT) final ~0 (approaching steady state)")
    return ok


@preset("dc_inductor", "DC inductor as short circuit")
def test_dc_inductor(verbose: bool) -> bool:
    netlist = (
        "VSRC V1 IN 0 type=dc val=12.0\n"
        "RES R1 IN OUT val=200\n"
        "IND L1 OUT 0 val=0.01\n"
        ".SIMULATE type=dc\n"
    )
    resp = send_netlist(netlist)
    if resp.status_code != 200:
        print(c("red", f"  HTTP {resp.status_code}: {resp.text}"))
        return False

    if verbose:
        print_csv_table(resp.text)

    rows = parse_csv(resp.text)
    ok = True
    ok &= assert_close(float(rows[0]["V(IN)"]), 12.0, label="V(IN)")
    ok &= assert_close(float(rows[0]["V(OUT)"]), 0.0, tol=1e-6, label="V(OUT) ~0 (short)")
    ok &= assert_close(float(rows[0]["I(V1)"]), -0.06, label="I(V1) = -12/200")
    return ok


@preset("dc_cap_open", "DC capacitor as open circuit")
def test_dc_cap_open(verbose: bool) -> bool:
    netlist = (
        "VSRC V1 IN 0 type=dc val=5.0\n"
        "RES R1 IN OUT val=1000\n"
        "CAP C1 OUT 0 val=1e-6\n"
        ".SIMULATE type=dc\n"
    )
    resp = send_netlist(netlist)
    if resp.status_code != 200:
        print(c("red", f"  HTTP {resp.status_code}: {resp.text}"))
        return False

    if verbose:
        print_csv_table(resp.text)

    rows = parse_csv(resp.text)
    ok = True
    ok &= assert_close(float(rows[0]["V(IN)"]), 5.0, label="V(IN)")
    return ok


@preset("sine_vsrc", "Sine voltage source transient")
def test_sine_vsrc(verbose: bool) -> bool:
    netlist = (
        "VSRC V1 IN 0 type=sine val=5.0 freq=1000\n"
        "RES R1 IN 0 val=1000\n"
        ".SIMULATE type=trans tstop=0.002 tstep=0.0001\n"
    )
    resp = send_netlist(netlist)
    if resp.status_code != 200:
        print(c("red", f"  HTTP {resp.status_code}: {resp.text}"))
        return False

    if verbose:
        print_csv_table(resp.text)

    rows = parse_csv(resp.text)
    ok = True
    for row in rows:
        t = float(row["time"])
        v_in = float(row["V(IN)"])
        v_expected = 5.0 * math.sin(2.0 * math.pi * 1000.0 * t)
        ok &= assert_close(v_in, v_expected, tol=0.01, label=f"V(IN) @ t={t:.6f}")
    return ok


@preset("error_empty", "Empty netlist returns 400")
def test_error_empty(verbose: bool) -> bool:
    resp = send_netlist("")
    ok = resp.status_code == 400
    status = c("green", "PASS") if ok else c("red", "FAIL")
    print(f"    [{status}] HTTP status={resp.status_code} (expected 400)")
    if verbose and resp.text:
        print(f"    {c('gray', resp.text.strip())}")
    return ok


@preset("error_badline", "Malformed netlist returns 400")
def test_error_badline(verbose: bool) -> bool:
    resp = send_netlist("GARBAGE LINE\n.SIMULATE type=dc\n")
    ok = resp.status_code == 400
    status = c("green", "PASS") if ok else c("red", "FAIL")
    print(f"    [{status}] HTTP status={resp.status_code} (expected 400)")
    if verbose and resp.text:
        print(f"    {c('gray', resp.text.strip())}")
    return ok


def run_interactive() -> None:
    print(c("cyan", "=== Interactive Netlist Tester ==="))
    print("Enter netlist lines. End with an empty line or EOF (Ctrl+D).")
    print(c("gray", "Tip: paste a full netlist including .SIMULATE directive.\n"))

    lines = []
    try:
        while True:
            line = input(c("gray", "> "))
            if line == "":
                break
            lines.append(line)
    except EOFError:
        pass

    if not lines:
        print(c("yellow", "No netlist entered."))
        return

    netlist = "\n".join(lines) + "\n"
    print(f"\n{c('cyan', '--- Sending netlist ---')}")
    print(c("gray", netlist))

    try:
        resp = send_netlist(netlist)
    except requests.ConnectionError:
        print(c("red", f"Connection refused. Is the engine running at {CONFIG['url']}?"))
        return
    except requests.ReadTimeout:
        print(c("red", f"Request timed out. The engine at {CONFIG['url']} took too long to respond."))
        return

    print(f"\n{c('cyan', '--- Response ---')}")
    print(f"  HTTP {resp.status_code}")
    if resp.status_code == 200:
        print_csv_table(resp.text)
    else:
        print(c("red", f"  {resp.text}"))


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Test tool for bland-circuit-simulator engine (C++)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="Examples:\n"
               "  %(prog)s --all                  Run all preset tests\n"
               "  %(prog)s -t dc_divider -v       Run one test verbosely\n"
               "  %(prog)s -i                     Interactive netlist mode\n"
               "  %(prog)s --list                  List available presets\n",
    )
    parser.add_argument("-t", "--test", nargs="+", metavar="NAME", help="Run specific preset test(s)")
    parser.add_argument("-a", "--all", action="store_true", help="Run all preset tests")
    parser.add_argument("-i", "--interactive", action="store_true", help="Interactive mode: type a netlist and see result")
    parser.add_argument("-v", "--verbose", action="store_true", help="Show full CSV result tables")
    parser.add_argument("-l", "--list", action="store_true", help="List available preset tests")
    parser.add_argument("--url", default=CONFIG["url"], help=f"Engine URL (default: {CONFIG['url']})")
    parser.add_argument("-f", "--file", metavar="PATH", help="Send netlist from a file")

    args = parser.parse_args()
    CONFIG["url"] = args.url

    if args.list:
        print(c("cyan", "Available preset tests:"))
        for name, (_, desc) in sorted(PRESETS.items()):
            print(f"  {c('bold', name):30s}  {desc}")
        return

    if args.interactive:
        run_interactive()
        return

    if args.file:
        with open(args.file) as f:
            netlist = f.read()
        print(f"{c('cyan', '--- Sending netlist from')} {args.file} {c('cyan', '---')}")
        print(c("gray", netlist))
        try:
            resp = send_netlist(netlist)
        except requests.ConnectionError:
            print(c("red", f"Connection refused. Is the engine running at {CONFIG['url']}?"))
            sys.exit(1)
        except requests.ReadTimeout:
            print(c("red", f"Request timed out. The engine at {CONFIG['url']} took too long to respond."))
            sys.exit(1)
        print(f"\n  HTTP {resp.status_code}")
        if resp.status_code == 200:
            print_csv_table(resp.text)
        else:
            print(c("red", f"  {resp.text}"))
        return

    tests_to_run = []
    if args.all:
        tests_to_run = list(PRESETS.keys())
    elif args.test:
        for name in args.test:
            if name not in PRESETS:
                print(c("red", f"Unknown test: {name}"))
                print(f"Available: {', '.join(sorted(PRESETS.keys()))}")
                sys.exit(1)
        tests_to_run = args.test
    else:
        parser.print_help()
        return

    try:
        requests.get(CONFIG["url"].rsplit("/", 2)[0], timeout=2)
    except requests.ConnectionError:
        print(c("red", f"Cannot connect to engine at {CONFIG['url']}"))
        print(c("yellow", "Start the engine first: ./build/bland-circuit-simulator"))
        sys.exit(1)
    except Exception:
        pass

    passed = 0
    failed = 0
    for name in tests_to_run:
        func, desc = PRESETS[name]
        print(f"\n{c('cyan', '>>>')} {c('bold', name)} — {desc}")
        try:
            ok = func(args.verbose)
        except requests.ConnectionError:
            print(c("red", f"  Connection lost to {CONFIG['url']}"))
            ok = False
        except requests.ReadTimeout:
            print(c("red", f"  Request timed out"))
            ok = False
        except Exception as e:
            print(c("red", f"  Exception: {e}"))
            ok = False
        if ok:
            passed += 1
        else:
            failed += 1

    print(f"\n{'=' * 50}")
    summary = f"  {passed} passed, {failed} failed, {passed + failed} total"
    print(c("green" if failed == 0 else "red", summary))
    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
