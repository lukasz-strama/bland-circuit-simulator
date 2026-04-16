#!/usr/bin/env python3
"""
  python3 tests/integration_test.py                          # domyślnie localhost:8080
  python3 tests/integration_test.py --url http://host:port   # inny backend
"""

import argparse
import json
import sys
import time
import requests
from datetime import datetime

# --- Kolory terminala ---
GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
CYAN   = "\033[96m"
BOLD   = "\033[1m"
RESET  = "\033[0m"

passed = 0
failed = 0


def header(msg):
    print(f"\n{BOLD}{CYAN}{'═' * 60}")
    print(f"  {msg}")
    print(f"{'═' * 60}{RESET}\n")


def step(num, msg):
    print(f"{BOLD}[{num}]{RESET} {msg}")


def ok(msg, data=None):
    global passed
    passed += 1
    print(f"    {GREEN}{msg}{RESET}")
    if data:
        for line in data.split("\n"):
            print(f"    {GREEN}   {line}{RESET}")


def fail(msg, data=None):
    global failed
    failed += 1
    print(f"    {RED}{msg}{RESET}")
    if data:
        for line in data.split("\n"):
            print(f"    {RED}   {line}{RESET}")


def warn(msg):
    print(f"    {YELLOW}{msg}{RESET}")


def pretty(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False)


# --- Netlisty testowe ---

NETLIST_DC = """\
* Dzielnik napięcia 5V
* R1=1k, R2=1k V(OUT) = 2.5V
VSRC V1 IN 0 type=dc val=5.0
RES R1 IN OUT val=1000
RES R2 OUT 0 val=1000
.SIMULATE type=dc"""

NETLIST_TRANSIENT = """\
* Ładowanie RC: R=1k, C=1uF, tau=1ms
* V(OUT) powinno rosnąć od 0 do ~5V
VSRC V1 IN 0 type=dc val=5.0
RES R1 IN OUT val=1000
CAP C1 OUT 0 val=1e-6
.SIMULATE type=trans tstop=0.005 tstep=0.001"""

NETLIST_BAD = """\
* Brakujący węzeł masy - powinien zwrócić błąd
RES R1 A B val=1000
.SIMULATE type=dc"""


def main():
    parser = argparse.ArgumentParser(description="Test integracyjny: User  Backend  Engine")
    parser.add_argument("--url", default="http://localhost:8080",
                        help="Base URL backendu Spring (domyślnie http://localhost:8080)")
    args = parser.parse_args()

    base = args.url.rstrip("/")
    ts = int(time.time())
    username = f"testuser_{ts}"
    email = f"test_{ts}@polsl.pl"
    password = "TestPass123!"

    header(f"Test integracyjny")
    print(f"  Backend URL:  {base}")
    print(f"  Użytkownik:   {username}")
    print(f"  Czas:         {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    # ----------------------------------------------
    # 1. Rejestracja
    # ----------------------------------------------
    step(1, "Rejestracja użytkownika")
    try:
        r = requests.post(f"{base}/api/auth/register", json={
            "username": username,
            "email": email,
            "password": password
        }, timeout=10)
        if r.status_code == 201:
            data = r.json()
            ok(f"HTTP 201 - użytkownik '{data.get('username')}' (id={data.get('id')})")
        else:
            fail(f"HTTP {r.status_code}", r.text)
    except Exception as e:
        fail(f"Błąd połączenia: {e}")
        print(f"\n{RED}Backend nie jest dostępny pod {base}. Uruchom go i spróbuj ponownie.{RESET}")
        sys.exit(1)

    # ----------------------------------------------
    # 2. Logowanie
    # ----------------------------------------------
    step(2, "Logowanie (JWT)")
    token = None
    try:
        r = requests.post(f"{base}/api/auth/login", json={
            "username": username,
            "password": password
        }, timeout=10)
        if r.status_code == 200:
            data = r.json()
            token = data.get("token")
            expires = data.get("expiresIn")
            ok(f"HTTP 200 - token JWT otrzymany (wygasa za {expires}s)")
            ok(f"Token: {token[:40]}...")
        else:
            fail(f"HTTP {r.status_code}", r.text)
    except Exception as e:
        fail(f"Błąd: {e}")

    if not token:
        print(f"\n{RED}Nie udało się zalogować - przerywam testy.{RESET}")
        sys.exit(1)

    auth = {"Authorization": f"Bearer {token}"}

    # ----------------------------------------------
    # 3. Tworzenie projektu
    # ----------------------------------------------
    step(3, "Tworzenie projektu")
    project_id = None
    try:
        r = requests.post(f"{base}/api/projects", json={
            "name": "Test integracyjny",
            "elements": [
                {"id": "R1", "type": "R", "node1": "IN", "node2": "OUT", "value": 1000},
                {"id": "R2", "type": "R", "node1": "OUT", "node2": "0", "value": 1000},
                {"id": "V1", "type": "V", "node1": "IN", "node2": "0", "value": 5.0}
            ],
            "wires": []
        }, headers=auth, timeout=10)
        if r.status_code == 201:
            data = r.json()
            project_id = data.get("id")
            ok(f"HTTP 201 - projekt '{data.get('name')}' (id={project_id})")
        else:
            fail(f"HTTP {r.status_code}", r.text)
    except Exception as e:
        fail(f"Błąd: {e}")

    if not project_id:
        print(f"\n{RED}Nie udało się utworzyć projektu - przerywam testy.{RESET}")
        sys.exit(1)

    # ----------------------------------------------
    # 4. Symulacja DC
    # ----------------------------------------------
    step(4, "Symulacja DC - dzielnik napięcia 5V (R1=R2=1kΩ)")
    print(f"     Netlist wysyłany przez backend do silnika na Render.com")
    sim1_id = None
    try:
        r = requests.post(f"{base}/api/projects/{project_id}/simulate", json={
            "version": "1.0",
            "author": username,
            "layout": {},
            "netlist_bcs": NETLIST_DC
        }, headers=auth, timeout=90)  # cold start Render

        if r.status_code == 200:
            data = r.json()
            sim1_id = data.get("simulationId")
            csv = data.get("data_csv", "")
            ok(f"HTTP 200 - symulacja #{sim1_id} zakończona sukcesem")

            # Parsuj CSV i sprawdź wynik
            lines = [l for l in csv.strip().split("\n") if l]
            if len(lines) >= 2:
                headers_csv = lines[0].split(",")
                values = lines[1].split(",")
                result = dict(zip(headers_csv, values))
                v_out = float(result.get("V(OUT)", 0))
                ok(f"V(OUT) = {v_out:.6f}V  (oczekiwane: 2.500000V)")
                if abs(v_out - 2.5) < 0.001:
                    ok("Wynik poprawny")
                else:
                    fail(f"Wynik niepoprawny. Oczekiwano 2.5V, otrzymano {v_out}V")
            else:
                warn(f"CSV ma {len(lines)} linii - nie da się zweryfikować")

            # Pokaż surowy CSV
            print(f"\n    {CYAN}Surowe dane CSV:{RESET}")
            for line in lines:
                print(f"    {line}")
        else:
            fail(f"HTTP {r.status_code}", r.text[:200])
    except requests.Timeout:
        fail("Timeout 90s - silnik na Render.com nie odpowiedział (cold start?)")
    except Exception as e:
        fail(f"Błąd: {e}")

    # ----------------------------------------------
    # 5. Symulacja transient
    # ----------------------------------------------
    step(5, "Symulacja transient - ładowanie RC (R=1kΩ, C=1µF, τ=1ms)")
    sim2_id = None
    try:
        r = requests.post(f"{base}/api/projects/{project_id}/simulate", json={
            "version": "1.0",
            "author": username,
            "layout": {},
            "netlist_bcs": NETLIST_TRANSIENT
        }, headers=auth, timeout=90)

        if r.status_code == 200:
            data = r.json()
            sim2_id = data.get("simulationId")
            csv = data.get("data_csv", "")
            lines = [l for l in csv.strip().split("\n") if l]
            ok(f"HTTP 200 - symulacja #{sim2_id} zakończona sukcesem")
            ok(f"Punkty danych: {len(lines) - 1}")

            # Pokaż CSV
            print(f"\n    {CYAN}Przebieg ładowania RC:{RESET}")
            print(f"    {'t [s]':>12}  {'V(OUT) [V]':>12}  {'I(R1) [A]':>12}")
            print(f"    {'-' * 40}")
            for line in lines[1:]:
                cols = line.split(",")
                if len(cols) >= 5:
                    t = float(cols[0])
                    v_out = float(cols[2])
                    i_r1 = float(cols[4])
                    print(f"    {t:>12.6f}  {v_out:>12.6f}  {i_r1:>12.6f}")

            # Sprawdź czy napięcie rośnie
            if len(lines) >= 3:
                first_v = float(lines[1].split(",")[2])
                last_v = float(lines[-1].split(",")[2])
                if last_v > first_v:
                    ok(f"V(OUT) rośnie: {first_v:.3f}V  {last_v:.3f}V (ładowanie)")
                else:
                    fail(f"V(OUT) nie rośnie: {first_v}V  {last_v}V")
        else:
            fail(f"HTTP {r.status_code}", r.text[:200])
    except Exception as e:
        fail(f"Błąd: {e}")

    # ----------------------------------------------
    # 6. Błędna netlista (walidacja)
    # ----------------------------------------------
    step(6, "Błędna netlista - test obsługi błędów")
    try:
        r = requests.post(f"{base}/api/projects/{project_id}/simulate", json={
            "version": "1.0",
            "author": username,
            "layout": {},
            "netlist_bcs": NETLIST_BAD
        }, headers=auth, timeout=90)

        if r.status_code in (422, 500):
            ok(f"HTTP {r.status_code} - silnik poprawnie odrzucił błędną netlistę")
            try:
                data = r.json()
                ok(f"Komunikat: {data.get('error', r.text[:100])}")
            except:
                ok(f"Odpowiedź: {r.text[:100]}")
        elif r.status_code == 200:
            warn("HTTP 200 - silnik zaakceptował netlistę bez masy (może to być poprawne)")
        else:
            fail(f"HTTP {r.status_code}", r.text[:200])
    except Exception as e:
        fail(f"Błąd: {e}")

    # ----------------------------------------------
    # 7. Historia symulacji
    # ----------------------------------------------
    step(7, "Pobranie historii symulacji projektu")
    try:
        r = requests.get(f"{base}/api/projects/{project_id}/simulations",
                         headers=auth, timeout=10)
        if r.status_code == 200:
            data = r.json()
            ok(f"HTTP 200 - {len(data)} symulacji w historii")
            for sim in data:
                print(f"      • #{sim['simulationId']}  status={sim['status']}  "
                      f"ts={sim['timestamp']}")
        else:
            fail(f"HTTP {r.status_code}", r.text[:200])
    except Exception as e:
        fail(f"Błąd: {e}")

    # ----------------------------------------------
    # 8. Szczegóły symulacji
    # ----------------------------------------------
    if sim1_id:
        step(8, f"Pobranie szczegółów symulacji #{sim1_id}")
        try:
            r = requests.get(f"{base}/api/projects/{project_id}/simulations/{sim1_id}",
                             headers=auth, timeout=10)
            if r.status_code == 200:
                data = r.json()
                ok(f"HTTP 200 - status={data.get('status')}, "
                   f"CSV={len(data.get('data_csv', ''))} bajtów")
            else:
                fail(f"HTTP {r.status_code}", r.text[:200])
        except Exception as e:
            fail(f"Błąd: {e}")

    # ----------------------------------------------
    # 9. Test bezpieczeństwa - request bez tokenu
    # ----------------------------------------------
    step(9, "Test bezpieczeństwa - request bez JWT tokenu")
    try:
        r = requests.get(f"{base}/api/projects", timeout=10)
        if r.status_code in (401, 403):
            ok(f"HTTP {r.status_code} - dostęp poprawnie zablokowany bez tokenu")
        else:
            fail(f"HTTP {r.status_code} - powinno być 401/403!", r.text[:100])
    except Exception as e:
        fail(f"Błąd: {e}")

    # ----------------------------------------------
    # Podsumowanie
    # ----------------------------------------------
    header("PODSUMOWANIE")
    total = passed + failed
    print(f"  Testy:     {total}")
    print(f"  {GREEN}Passed:    {passed}{RESET}")
    if failed:
        print(f"  {RED}Failed:    {failed}{RESET}")
    else:
        print(f"  Failed:    0")

    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
