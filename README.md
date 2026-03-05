**1. Tytuł projektu:** Wieloplatformowy system symulacji obwodów elektrycznych RLC w architekturze rozproszonej.

**2. Cel projektu:**
Celem projektu jest stworzenie oprogramowania do projektowania i symulacji obwodów elektronicznych. System umożliwi budowanie schematów z podstawowych elementów pasywnych (R, C, L) oraz źródeł zasilania, a następnie przeprowadzenie analizy prądu stałego (DC) oraz analizy czasowej (stanów przejściowych). Projekt zakłada stworzenie silnika obliczeniowego w języku C++ oraz dwóch w pełni funkcjonalnych, zsynchronizowanych aplikacji klienckich (Desktop i Web) w języku Java, które będą współdzielić dane za pomocą bazy danych.

**3. Zakres prac i architektura systemu:**
System będzie oparty na architekturze klient-serwer z wykorzystaniem mikroserwisów. Zakres prac obejmuje:

* **Silnik Obliczeniowy (Microservice):** Aplikacja w C++ działająca jako serwer HTTP, przyjmująca parametry obwodu (tzw. netlistę) i zwracająca wyniki symulacji prądów i napięć w czasie (w formacie CSV). Algorytm będzie oparty na Zmodyfikowanej Analizie Węzłowej (MNA) oraz metodach całkowania numerycznego dla elementów reaktancyjnych.
* **Backend i Baza Danych (API Gateway):** Centralny serwer w Javie, zarządzający autoryzacją użytkowników, zapisem/odczytem schematów (w formacie JSON) w relacyjnej bazie danych oraz pośredniczący w komunikacji między aplikacjami klienckimi a silnikiem C++.
* **Aplikacje Klienckie (Web & Desktop):** Dwie niezależne aplikacje z graficznym interfejsem użytkownika (GUI), umożliwiające rysowanie schematów, wysyłanie ich do symulacji oraz wizualizację wyników na wykresach.

**4. Podział pracy:**

* **Osoba 1: Silnik obliczeniowy**
    * Silnik symulacyjny w C/C++.
    * Opracowanie parsera netlisty.
    * Implementacja algorytmów numerycznych (MNA, rozwiązywanie układów równań).
    * Wystawienie silnika jako serwera HTTP (Crow).
    * Implementacja logiki integracyjnej komunikującej się z backendem w Javie.

* **Osoba 2: Backend i baza danych**
    * Projekt i wdrożenie bazy danych.
    * Stworzenie REST API do obsługi kont użytkowników i zapisu projektów (JSON).
    * Implementacja logiki integracyjnej komunikującej się z silnikiem C++.

* **Osoba 3: Aplikacja desktopowa**
    * Stworzenie interfejsu użytkownika aplikacji okienkowej.
    * Implementacja interaktywnego edytora schematów obwodów (Canvas).
    * Integracja z REST API (logowanie, zapis, symulacja) i generowanie wykresów.

* **Osoba 4: Aplikacja webowa**
    * Stworzenie interfejsu użytkownika w przeglądarce, powielającego funkcje wersji Desktop.
    * Implementacja webowego edytora schematów.
    * Integracja z REST API oraz wizualizacja wyników w przeglądarce.

**5. Technologie i narzędzia:**

* **Języki programowania:** Java 21, C++ 23.
* **Aplikacja Desktopowa:** JavaFX.
* **Aplikacja Webowa:** Java Spring Boot / Vaadin.
* **Serwer / Backend:** Java Spring Boot, REST API, JSON.
* **Silnik Symulacyjny:** C++, biblioteka numeryczna (Eigen), biblioteka serwera HTTP (Crow).
* **Baza Danych:** PostgreSQL.
* **Narzędzia budowania:** Maven (dla Javy), CMake (dla C++).
* **Kontrola wersji i praca grupowa:** Git, GitHub.