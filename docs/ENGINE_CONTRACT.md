## 1. Założenia
* **Protokół:** HTTP POST do silnika na endpoint `/api/v1/simulate`.
* **Kodowanie:** Zawsze UTF-8.
* **Separatory dziesiętne:** Kropka `.`.
* **Jednostki:** Wszystkie wartości liczbowe wysyłane do silnika muszą być podane w podstawowych jednostkach SI (Ohm, Farad, Henr, Wolt, Amper) jako liczby zmiennoprzecinkowe lub w notacji naukowej (np. `1000` lub `1e3` zamiast `1k`).

## 2. Format Wejściowy: Netlista

Payload żądania z Javy do C++ zawiera wieloliniowy ciąg znaków (string). 

### 2.1. Zasady składni
1. Każdy element to jedna linia tekstu.
2. Elementy oddzielone są spacją.
3. Struktura linii: `TYP NAZWA WĘZEŁ_A WĘZEŁ_B [parametry_klucz_wartosc...]`
4. Wielkość liter nie ma znaczenia dla parsera, ale zalecane są WIELKIE litery dla typów.
5. Linie zaczynające się od `*` to komentarze (silnik je ignoruje).

### 2.2. Definicja węzłów (Nodes)
* Węzły są reprezentowane jako ciągi znaków (String).
* **Masa (GND) to ZAWSZE węzeł o nazwie `0`.**

### 2.3. Słownik Komponentów

| Typ | Opis | Węzły | Wymagane parametry | Przykłady |
| :--- | :--- | :--- | :--- | :--- |
| **RES** | Rezystor | `N+`, `N-` | `val` (Rezystancja w Ohmach) | `RES R1 IN OUT val=1000` |
| **CAP** | Kondensator | `N+`, `N-` | `val` (Pojemność w Faradach) | `CAP C1 OUT 0 val=1e-6` |
| **IND** | Cewka | `N+`, `N-` | `val` (Indukcyjność w Henrach) | `IND L1 IN 0 val=1e-3` |
| **VSRC** | Źródło Napięcia | `N+`, `N-` | `type` (dc/sine), `val` (Amplituda w Voltach) | `VSRC V1 IN 0 type=dc val=5.0`<br>`VSRC V2 IN 0 type=sine val=2.0 freq=50.0` |
| **ISRC** | Źródło Prądu | `N+`, `N-` | `type` (dc/sine), `val` (Amplituda w Amperach) | `ISRC I1 IN OUT type=dc val=0.01` |

*Prąd/napięcie płynie od (N+) do (N-).*
*Prąd przemienny należy zaimplentować jako placeholder.*

### 2.4. Polecenia Symulacji
Na końcu netlisty musi znaleźć się jedna z poniższych dyrektyw.

**Analiza stałoprądowa (DC):**
Oblicza tylko punkt pracy.
`.SIMULATE type=dc`

**Analiza czasowa (Transient):**
`.SIMULATE type=trans tstop=0.01 tstep=0.0001`
* `tstop` - czas końcowy symulacji w sekundach.
* `tstep` - krok całkowania w sekundach.

### 2.5 Przykładowe netlisty

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
## 3. Format Wyjściowy: Wyniki Symulacji (CSV)

Aby zmniejszyć narzut na serializację/deserializację, silnik zwraca wyniki jako tekst w formacie CSV. Java przesyła ten string bezpośrednio na Frontend, który parsuje CSV do wykresów/wyników.

### 3.1. Formatowanie CSV
* **Separator kolumn:** Przecinek `,`
* **Znak dziesiętny:** Kropka `.`
* **Nagłówki:** Pierwszy wiersz zawsze zawiera nazwy kolumn.
* **Jednostki:** Wszystkie wartości są w podstawowych jednostkach SI (Sekunda, Wolt, Amper).

### 3.2. Struktura Kolumn
1. Pierwsza kolumna to ZAWSZE `time` (w analizie DC będzie to jeden wiersz z `time=0`).
2. Następne kolumny to napięcia na WSZYSTKICH węzłach w formacie `V(nazwa_wezla)`. Węzeł `0` jest pomijany.
3. Ostatnie kolumny to prądy płynące przez źródła napięciowe w formacie `I(nazwa_zrodla)`. Wymaga tego metoda MNA.

### 3.3. Przykład prawidłowej odpowiedzi (HTTP 200 OK)

- Odpowiedź dla Przykładu A
```csv
time,V(IN),V(OUT),I(V1)
0.000,5.000,2.500,-0.0025
```

- Odpowiedź dla Przykładu B
```csv
time,V(IN),V(OUT),I(V1)
0.000,5.000,0.000,-0.0050
0.001,5.000,3.160,-0.0018
0.002,5.000,4.323,-0.0006
0.003,5.000,4.751,-0.0002
```

- Odpowiedź dla Przykładu C
```csv
time,V(OUT)
0.000,10.000
```

## 4. Kody Błędów (HTTP Response)

Jeśli silnik C++ napotka problem, odpowiada odpowiednim kodem HTTP i prostym 'ciałem' w formacie plain/text z opisem błędu.

- **200 OK**: Symulacja zakończona sukcesem (zwraca CSV).
- **400 Bad Request**: Błąd składni netlisty (np. Brak parametru 'val' w elemencie R1).
- **422 Unprocessable Entity**: Błąd matematyczny fizyki obwodu (np. Macierz osobliwa. Prawdopodobne zwarcie idealnych źródeł napięcia.).
- **500 Internal Server Error**: Krytyczny błąd silnika (np. brak pamięci RAM na alokację macierzy MNA).