# TimerTracker — Backend API

API REST para un sistema de fichaje horario y control de accesos empresarial: gestión de usuarios, roles, registros de entrada/salida y cálculo de jornadas laborales.

---

## Índice

- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Puesta en marcha](#puesta-en-marcha)
- [Variables de entorno](#variables-de-entorno)
- [Tests](#tests)
- [Flujo de trabajo con Git](#flujo-de-trabajo-con-git)
- [Hitos](#hitos)

---

## Stack tecnológico

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 21 (toolchain de Gradle) |
| Framework | Spring Boot 4.1.1 |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 16 (Docker) |
| Seguridad | Spring Security |
| Validación | Jakarta Bean Validation |
| Contenedores | Docker + Docker Compose |
| Build | Gradle (wrapper incluido) |
| Tests | JUnit 5, Spring Boot Test |

---

## Arquitectura

Arquitectura por capas clásica:

```
HTTP -> Controller -> DTO -> Service -> Repository -> PostgreSQL
```

| Paquete | Responsabilidad |
|---------|------------------|
| `controller` | Endpoints HTTP, códigos de estado, recibe/devuelve DTOs |
| `dto` | Contratos de entrada y salida, aísla las entidades del exterior |
| `service` | Reglas de negocio y transacciones |
| `repository` | Acceso a datos vía Spring Data JPA |
| `model` | Entidades JPA |
| `exception` | Manejo global de errores (`@ControllerAdvice`) |
| `config` | Seguridad, JWT, CORS |

Ubicación: `src/main/java/com/company/timertracker/`.

---

## Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/jav-anibal/timer-tracker-backend-api.git
cd timer-tracker-backend-api
```

### 2. Crear el archivo `.env`

```powershell
Copy-Item .env.example .env
```

Editar `.env` con credenciales locales (ver [Variables de entorno](#variables-de-entorno)).

### 3. Levantar PostgreSQL con Docker

```powershell
docker compose up -d postgres
docker compose ps
```

> Postgres se publica en el puerto **5434** del host (`5434:5432`). El 5432 lo usa el PostgreSQL instalado en Windows y el 5433 lo usaba otro proyecto. Si cambias el puerto, actualízalo también en `docker-compose.yml`.

### 4. Compilar y ejecutar

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

La aplicación arranca en `http://localhost:8080`.

### Comandos útiles

| Comando | Descripción |
|---|---|
| `.\gradlew.bat clean` | Borra artefactos previos |
| `.\gradlew.bat test` | Ejecuta los tests |
| `.\gradlew.bat check` | Compilación + tests |
| `.\gradlew.bat bootRun` | Arranca la aplicación |
| `docker compose up -d postgres` | Levanta PostgreSQL en segundo plano |
| `docker compose down` | Detiene los contenedores sin borrar datos |
| `docker compose down -v` | Detiene y borra el volumen (usar con cuidado) |

---

## Variables de entorno

`.env.example` (versionado, es la plantilla):

```env
POSTGRES_DB=timertracker2_db
POSTGRES_USER=admin_postgres
POSTGRES_PASSWORD=admin_docker
```

`.env` no se versiona (protegido por `.gitignore`) y contiene las credenciales reales locales. `docker-compose.yml` carga `.env` vía `env_file`, y la dependencia `spring-boot-docker-compose` detecta el contenedor en ejecución y configura la conexión a la base de datos automáticamente: no hace falta exportar variables al proceso de Java.

---

## Tests

Los tests necesitan Postgres levantado (`docker compose up -d postgres`) y la contraseña en la variable `DB_PASSWORD`:

```powershell
$env:DB_PASSWORD = "la_contraseña_del_.env"
.\gradlew.bat test
```

---

## Flujo de trabajo con Git

- Rama estable: `main`.
- Ramas de trabajo con prefijos: `feat/`, `fix/`, `test/`, `docs/`, `chore/`.
- Commits pequeños y descriptivos.
- Nunca se suben `.env`, `build/`, `.gradle/` ni credenciales.

---

## Hitos

Registro de hitos alcanzados, en orden. Se agrega una entrada por hito completado; las anteriores no se reescriben.

- **2026-09-23** — Bootstrap: proyecto Spring Boot 4.1.1 sobre Java 21, PostgreSQL vía Docker Compose, estructura de paquetes por capas (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `config`).

- **2026-09-23** — Modelo de datos: entidades `User`, `Role`, `TimeEntry` y `Workday` con JPA; enums `RoleName` y `TimeEntryType` en paquete `enums`; repositorios Spring Data (`RoleRepository`, `UserRepository`, `TimeEntryRepository`, `WorkdayRepository`). Hibernate crea cinco tablas: `users`, `roles`, `user_roles`, `time_entries`, `workdays`.