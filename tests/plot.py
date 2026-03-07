#!/usr/bin/env python3
import argparse
import csv
import io
import sys

import requests

try:
    import matplotlib.pyplot as plt
except ImportError:
    print("matplotlib is required: pip install matplotlib")
    sys.exit(1)

ENGINE_URL = "http://localhost:8081/api/v1/simulate"


def send_netlist(path: str, url: str) -> str:
    with open(path, "r") as f:
        netlist = f.read()
    print(f"Sending netlist from {path} ...")
    resp = requests.post(url, data=netlist, timeout=30)
    if resp.status_code != 200:
        print(f"Engine error (HTTP {resp.status_code}): {resp.text}")
        sys.exit(1)
    return resp.text


def parse_csv_text(text: str) -> tuple[list[str], list[list[float]]]:
    reader = csv.reader(io.StringIO(text))
    headers = next(reader)
    data = []
    for row in reader:
        data.append([float(v) for v in row])
    return headers, data


def main():
    parser = argparse.ArgumentParser(description="Plot circuit simulation results")
    parser.add_argument("netlist", help="Path to .netlist file")
    parser.add_argument("--url", default=ENGINE_URL, help="Engine URL")
    parser.add_argument(
        "--only",
        choices=["V", "I"],
        default=None,
        help="Plot only voltages (V) or currents (I)",
    )
    parser.add_argument(
        "--cols",
        default=None,
        help='Comma-separated column names to plot, e.g. "V(IN),V(OUT)"',
    )
    parser.add_argument(
        "--no-grid", action="store_true", help="Disable grid"
    )
    args = parser.parse_args()

    csv_text = send_netlist(args.netlist, args.url)
    headers, data = parse_csv_text(csv_text)

    if not data:
        print("No data points returned.")
        sys.exit(1)

    time_col = [row[0] for row in data]
    columns = {}
    for i, h in enumerate(headers):
        if h == "time":
            continue
        columns[h] = [row[i] for row in data]

    if args.cols:
        selected = [c.strip() for c in args.cols.split(",")]
        columns = {k: v for k, v in columns.items() if k in selected}
        if not columns:
            print(f"No matching columns found. Available: {', '.join(headers[1:])}")
            sys.exit(1)
    elif args.only:
        prefix = f"{args.only}("
        columns = {k: v for k, v in columns.items() if k.startswith(prefix)}

    v_cols = {k: v for k, v in columns.items() if k.startswith("V(")}
    i_cols = {k: v for k, v in columns.items() if k.startswith("I(")}

    has_v = bool(v_cols)
    has_i = bool(i_cols)
    num_plots = has_v + has_i

    if num_plots == 0:
        print("No columns to plot.")
        sys.exit(1)

    fig, axes = plt.subplots(num_plots, 1, figsize=(12, 5 * num_plots), sharex=True)
    if num_plots == 1:
        axes = [axes]

    # Scale time axis
    t_max = max(time_col) if time_col else 0
    if t_max > 0 and t_max < 1e-3:
        t_scale, t_unit = 1e6, "µs"
    elif t_max < 1:
        t_scale, t_unit = 1e3, "ms"
    else:
        t_scale, t_unit = 1, "s"

    time_scaled = [t * t_scale for t in time_col]
    ax_idx = 0

    if has_v:
        ax = axes[ax_idx]
        for name, values in v_cols.items():
            ax.plot(time_scaled, values, label=name, linewidth=1.2)
        ax.set_ylabel("Voltage [V]")
        ax.legend(loc="best", fontsize=8)
        if not args.no_grid:
            ax.grid(True, alpha=0.3)
        ax_idx += 1

    if has_i:
        ax = axes[ax_idx]
        # Scale current axis
        all_i = [v for vals in i_cols.values() for v in vals]
        i_max = max(abs(v) for v in all_i) if all_i else 0
        if i_max > 0 and i_max < 1e-3:
            i_scale, i_unit = 1e6, "µA"
        elif i_max < 1:
            i_scale, i_unit = 1e3, "mA"
        else:
            i_scale, i_unit = 1, "A"

        for name, values in i_cols.items():
            ax.plot(time_scaled, [v * i_scale for v in values], label=name, linewidth=1.2)
        ax.set_ylabel(f"Current [{i_unit}]")
        ax.legend(loc="best", fontsize=8)
        if not args.no_grid:
            ax.grid(True, alpha=0.3)
        ax_idx += 1

    axes[-1].set_xlabel(f"Time [{t_unit}]")

    title = args.netlist.rsplit("/", 1)[-1].replace(".netlist", "")
    fig.suptitle(title, fontsize=14, fontweight="bold")
    fig.tight_layout()

    print(f"Plotted {len(columns)} signals, {len(data)} time points.")
    plt.show()


if __name__ == "__main__":
    main()
