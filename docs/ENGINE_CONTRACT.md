## 1. Założenia

| Parametr | Wartość |
|---|---|
| **Protokół** | HTTP POST |
| **URL produkcyjny** | `https://bland-circuit-engine.onrender.com/api/v1/simulate` |
| **URL lokalny** | `http://localhost:8081/api/v1/simulate` |
| **Kodowanie** | UTF-8 |
| **Separatory dziesiętne** | Kropka `.` |
| **Jednostki** | SI (Ohm, Farad, Henr, Wolt, Amper) |
| **Autoryzacja** | Nagłówek `X-Engine-API-Key` |

> Ze względu na darmowy plan Render.com, pierwsze zapytanie po dłuższej nieaktywności może zająć kilkanaście sekund (cold start).

---

## 2. Autoryzacja (API Key)

Silnik C++ jest zabezpieczony wewnętrznym kluczem API. **Tylko backend** zna ten klucz - klienci GUI nigdy nie komunikują się z silnikiem bezpośrednio.

### Wymagany nagłówek

```
X-Engine-API-Key: <tajny_klucz>
```

| Parametr | Opis |
|---|---|
| **Nazwa nagłówka** | `X-Engine-API-Key` |
| **Wartość** | Tajny klucz znany tylko backendowi (konfiguracja: zmienna środowiskowa `ENGINE_API_KEY`) |
| **Walidacja** | Silnik porównuje wartość nagłówka z własną zmienną środowiskową `ENGINE_API_KEY` |

### Zachowanie przy braku / błędnym kluczu

| Sytuacja | Odpowiedź silnika |
|---|---|
| Brak nagłówka `X-Engine-API-Key` | `401 Unauthorized` - `"Missing X-Engine-API-Key header"` |
| Nieprawidłowa wartość klucza | `401 Unauthorized` - `"Invalid API key"` |
| Prawidłowy klucz | Przetwarzanie żądania |

### Przykład żądania z backendu

```http
POST /api/v1/simulate HTTP/1.1
Host: bland-circuit-engine.onrender.com
X-Engine-API-Key: supersecretkey123
Content-Type: text/plain; charset=utf-8

* Dzielnik napięcia
VSRC V1 IN 0 type=dc val=5.0
RES R1 IN OUT val=1000
RES R2 OUT 0 val=1000
.SIMULATE type=dc
```

---

## 3. Format Wejściowy: Netlista BCS

Payload żądania z backendu Java do silnika C++ to wieloliniowy ciąg znaków (`text/plain`) zawierający netlistę w formacie BCS.

### 3.1. Zasady składni
1. Każdy element to jedna linia tekstu.
2. Elementy w linii oddzielone są spacją.
3. Struktura linii: `TYP NAZWA WĘZEŁ_A WĘZEŁ_B [parametry_klucz=wartość …]`
4. Wielkość liter nie ma znaczenia dla parsera, ale zalecane są WIELKIE litery dla typów.
5. Linie zaczynające się od `*` to komentarze (silnik je ignoruje).

### 3.2. Definicja węzłów (Nodes)
* Węzły są reprezentowane jako ciągi znaków (String).
* **Masa (GND) to ZAWSZE węzeł o nazwie `0`.**

### 3.3. Słownik Komponentów

| Typ | Opis | Węzły | Wymagane parametry | Przykłady |
| :--- | :--- | :--- | :--- | :--- |
| **RES** | Rezystor | `N+`, `N-` | `val` (Rezystancja w Ohmach) | `RES R1 IN OUT val=1000` |
| **CAP** | Kondensator | `N+`, `N-` | `val` (Pojemność w Faradach) | `CAP C1 OUT 0 val=1e-6` |
| **IND** | Cewka | `N+`, `N-` | `val` (Indukcyjność w Henrach) | `IND L1 IN 0 val=1e-3` |
| **VSRC** | Źródło Napięcia | `N+`, `N-` | `type` (dc/sine), `val` (Amplituda w Voltach) | `VSRC V1 IN 0 type=dc val=5.0`<br>`VSRC V2 IN 0 type=sine val=2.0 freq=50.0` |
| **ISRC** | Źródło Prądu | `N+`, `N-` | `type` (dc/sine), `val` (Amplituda w Amperach) | `ISRC I1 IN OUT type=dc val=0.01` |

*Prąd/napięcie płynie od (N+) do (N-).*

### 3.4. Polecenia Symulacji
Na końcu netlisty musi znaleźć się jedna z poniższych dyrektyw.

**Analiza stałoprądowa (DC):**
Oblicza tylko punkt pracy.
```
.SIMULATE type=dc
```

**Analiza czasowa (Transient):**
```
.SIMULATE type=trans tstop=0.01 tstep=0.0001
```
* `tstop` - czas końcowy symulacji w sekundach.
* `tstep` - krok całkowania w sekundach.

### 3.5 Przykładowe netlisty

- Przykład A
```
* Dzielnik napięcia (5V > R1k > OUT > R1k > GND)
VSRC V1 IN 0 type=dc val=5.0
RES R1 IN OUT val=1000
RES R2 OUT 0 val=1000
.SIMULATE type=dc
```
- Przykład B
```
* Szeregowy układ RC (ładowanie kondensatora)
VSRC V1 IN 0 type=dc val=5.0
RES R1 IN OUT val=1000
CAP C1 OUT 0 val=1e-6
.SIMULATE type=trans tstop=0.005 tstep=0.001
```
- Przykład C
```
* Źródło prądowe 20mA w rezystor 500 Ohm
ISRC I1 0 OUT type=dc val=0.02
RES R1 OUT 0 val=500
.SIMULATE type=dc
```

---

## 4. Format Wyjściowy: Wyniki Symulacji (CSV)

Silnik zwraca wyniki jako tekst CSV. Backend opakowuje ten string w JSON i przekazuje do GUI (patrz `API_CONTRACT.md`).

### 4.1. Formatowanie CSV
* **Separator kolumn:** Przecinek `,`
* **Znak dziesiętny:** Kropka `.`
* **Nagłówki:** Pierwszy wiersz zawsze zawiera nazwy kolumn.
* **Jednostki:** Wszystkie wartości są w podstawowych jednostkach SI (Sekunda, Wolt, Amper).

### 4.2. Struktura Kolumn
1. Pierwsza kolumna to ZAWSZE `time` (w analizie DC będzie to jeden wiersz z `time=0`).
2. Następne kolumny to napięcia na WSZYSTKICH węzłach w formacie `V(nazwa_wezla)`. Węzeł `0` jest pomijany.
3. Ostatnie kolumny to prądy płynące przez WSZYSTKIE komponenty w formacie `I(nazwa_komponentu)`, w kolejności występowania w netliście. Obejmuje to rezystory, źródła napięciowe, źródła prądowe, cewki i kondensatory (w analizie transient).

### 4.3. Przykład prawidłowej odpowiedzi (HTTP 200 OK)

- Odpowiedź dla Przykładu A
```csv
time,V(IN),V(OUT),I(V1),I(R1),I(R2)
0.000,5.000,2.500,-0.0025,0.0025,0.0025
```

- Odpowiedź dla Przykładu B
```csv
time,V(IN),V(OUT),I(V1),I(R1),I(C1)
0.000,5.000,0.000,-0.0050,0.0050,0.0050
0.001,5.000,3.160,-0.0018,0.0018,0.0018
0.002,5.000,4.323,-0.0006,0.0006,0.0006
0.003,5.000,4.751,-0.0002,0.0002,0.0002
```

- Odpowiedź dla Przykładu C
```csv
time,V(OUT),I(I1),I(R1)
0.000,10.000,0.0200,0.0200
```

---

## 5. Kody Błędów (HTTP Response)

Jeśli silnik C++ napotka problem, odpowiada odpowiednim kodem HTTP i prostym ciałem w formacie `text/plain` z opisem błędu.

| Kod | Kiedy | Przykład odpowiedzi |
|---|---|---|
| **200 OK** | Symulacja zakończona sukcesem | CSV z wynikami |
| **400 Bad Request** | Błąd składni netlisty | `"Brak parametru 'val' w elemencie R1"` |
| **401 Unauthorized** | Brak / nieprawidłowy `X-Engine-API-Key` | `"Invalid API key"` |
| **422 Unprocessable Entity** | Błąd matematyczny / fizyczny obwodu | `"Macierz osobliwa. Prawdopodobne zwarcie idealnych źródeł napięcia."` |
| **500 Internal Server Error** | Krytyczny błąd silnika | `"Brak pamięci RAM na alokację macierzy MNA"` |

> Backend mapuje kody błędów silnika na odpowiedzi JSON dla GUI:
> - Silnik `400` / `422` → Backend `422` z `{"error": "<treść z silnika>"}`
> - Silnik `500` - Backend `500` z `{"error": "Błąd wewnętrzny silnika symulacji"}`
> - Silnik `401` - nigdy nie powinien wystąpić (klucz API jest w konfiguracji backendu)
