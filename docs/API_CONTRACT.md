## 1. Założenia
* **Protokół:** HTTP REST, base URL: `http://localhost:8080/api`
* **Kodowanie:** UTF-8.
* **Format danych:** JSON (`Content-Type: application/json`).
* **Odpowiedzi symulacji:** Backend proxy-uje wynik z silnika jako CSV (patrz `ENGINE_CONTRACT.md`).

## 2. Użytkownicy

### 2.1. Rejestracja

`POST /api/users/register`

**Request:**
```json
{
  "username": "jan",
  "email": "jan@polsl.pl",
  "password": "secret123"
}
```

**Response 201 Created:**
```json
{
  "id": 1,
  "username": "jan",
  "email": "jan@polsl.pl"
}
```

### 2.2. Logowanie

`POST /api/users/login`

**Request:**
```json
{
  "username": "jan",
  "password": "secret123"
}
```

**Response 200 OK:**
```json
{
  "id": 1,
  "username": "jan",
  "email": "jan@polsl.pl"
}
```

### 2.3. Pobranie danych użytkownika

`GET /api/users/{id}`

**Response 200 OK:**
```json
{
  "id": 1,
  "username": "jan",
  "email": "jan@polsl.pl"
}
```

## 3. Schematy (CRUD)

### 3.1. Lista schematów

`GET /api/schematics`

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "Dzielnik napięcia",
    "elements": [],
    "wires": [],
    "createdAt": "2026-03-06T12:00:00Z"
  }
]
```

### 3.2. Pobranie schematu

`GET /api/schematics/{id}`

**Response 200 OK:**
```json
{
  "id": 1,
  "name": "Dzielnik napięcia",
  "elements": [
    { "id": "R1", "type": "R", "node1": "IN",  "node2": "OUT", "value": 1000.0, "x": 2, "y": 1, "rotation": 0 },
    { "id": "R2", "type": "R", "node1": "OUT", "node2": "0",   "value": 1000.0, "x": 4, "y": 1, "rotation": 0 },
    { "id": "V1", "type": "V", "node1": "IN",  "node2": "0",   "value": 5.0,    "x": 0, "y": 1, "rotation": 90 }
  ],
  "wires": [
    { "id": "W1", "node": "IN",  "points": [[1,1],[2,1]] },
    { "id": "W2", "node": "OUT", "points": [[3,1],[4,1]] },
    { "id": "W3", "node": "0",   "points": [[5,1],[5,2],[0,2],[0,1]] }
  ],
  "createdAt": "2026-03-06T12:00:00Z"
}
```

### 3.3. Utworzenie schematu

`POST /api/schematics`

**Request:**
```json
{
  "name": "Dzielnik napięcia",
  "elements": [
    { "id": "R1", "type": "R", "node1": "IN",  "node2": "OUT", "value": 1000.0, "x": 2, "y": 1, "rotation": 0 },
    { "id": "R2", "type": "R", "node1": "OUT", "node2": "0",   "value": 1000.0, "x": 4, "y": 1, "rotation": 0 },
    { "id": "V1", "type": "V", "node1": "IN",  "node2": "0",   "value": 5.0,    "x": 0, "y": 1, "rotation": 90 }
  ],
  "wires": [
    { "id": "W1", "node": "IN",  "points": [[1,1],[2,1]] },
    { "id": "W2", "node": "OUT", "points": [[3,1],[4,1]] },
    { "id": "W3", "node": "0",   "points": [[5,1],[5,2],[0,2],[0,1]] }
  ]
}
```

**Response 201 Created:** zwraca zapisany schemat z nadanym `id` i `createdAt`.

### 3.4. Aktualizacja schematu

`PUT /api/schematics/{id}`

**Request:** identyczny jak POST (pełna reprezentacja schematu).

**Response 200 OK:** zwraca zaktualizowany schemat.

### 3.5. Usunięcie schematu

`DELETE /api/schematics/{id}`

**Response 204 No Content**

## 4. Symulacja

### 4.1. Uruchomienie symulacji

`POST /api/simulate`

Backend konwertuje schemat na netlistę (format z `ENGINE_CONTRACT.md`), wysyła do silnika C++ i zwraca wynik CSV.

**Request:**
```json
{
  "schematicId": 1,
  "analysisType": "DC",
  "parameters": {}
}
```

Dla analizy TRANSIENT:
```json
{
  "schematicId": 1,
  "analysisType": "TRANSIENT",
  "parameters": {
    "tstop": 0.01,
    "tstep": 0.0001
  }
}
```

**Response 200 OK** (`Content-Type: text/csv`):
```csv
time,V(IN),V(OUT),I(V1)
0.000,5.000,2.500,-0.0025
```

Backend przekazuje CSV z silnika bez modyfikacji. Frontend parsuje CSV po stronie klienta.

## 5. Kody Błędów

| Kod | Znaczenie |
| :--- | :--- |
| **200** | Sukces |
| **201** | Zasób utworzony |
| **204** | Usunięto (brak ciała) |
| **400** | Błąd walidacji (np. brak wymaganego pola) |
| **401** | Brak autoryzacji |
| **404** | Zasób nie istnieje |
| **422** | Błąd symulacji (przekazany z silnika C++) |
| **500** | Błąd wewnętrzny serwera |

Ciało odpowiedzi błędu:
```json
{
  "error": "Schemat o id=99 nie istnieje"
}
```
