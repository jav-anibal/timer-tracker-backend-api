# TimerTracker

API REST en Java 21 y Spring Boot para gestionar usuarios y roles. Es un proyecto de práctica para aprender backend con Java.

## Qué hace

- Crea usuarios con username, email y contraseña.
- Guarda la contraseña cifrada con BCrypt. Nunca se devuelve en las respuestas.
- Asigna el rol `EMPLOYEE` a cada usuario nuevo.
- Permite consultar un usuario por id y listar todos.
- Valida los datos de entrada y devuelve errores con códigos HTTP claros.

## Stack

- Java 21, Gradle
- Spring Boot 4.1.1: Spring Web MVC, Spring Data JPA (Hibernate), Spring Security, Bean Validation
- PostgreSQL 16 con Docker Compose
- JUnit 5, Mockito, MockMvc
- GitHub Actions para build y tests

## Arquitectura

```
HTTP -> controller -> service -> repository -> PostgreSQL
           |             |
          DTO          entidad JPA
```

- `controller`: endpoints REST y códigos de respuesta.
- `dto`: datos de entrada y salida. Las entidades no salen de la capa de servicio.
- `service`: reglas de negocio y transacciones.
- `repository`: acceso a datos con Spring Data JPA.
- `model`: entidades JPA.
- `exception`: manejo global de errores en `GlobalExceptionHandler`.
- `config`: BCrypt, cadena de seguridad y carga de roles al arrancar.

## Endpoints

| Método | Ruta | Acceso | Respuesta |
|---|---|---|---|
| POST | `/api/users` | Público | 201 con `Location`, 400 si los datos no son válidos, 409 si el username o el email ya existen |
| GET | `/api/users/{id}` | Autenticado | 200, o 404 si no existe |
| GET | `/api/users` | Autenticado | 200 con la lista de usuarios |

Cuerpo de `POST /api/users`:

```json
{
  "username": "anibal",
  "email": "anibal@mail.com",
  "password": "password123"
}
```

Reglas: username de 3 a 64 caracteres, email válido de hasta 128 y contraseña de 8 a 72.

Los endpoints autenticados usan HTTP Basic con los usuarios guardados en la base de datos.

## Cómo arrancarlo

1. Copia la plantilla de variables y pon tu contraseña:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Levanta PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

3. Arranca la aplicación. Docker Compose toma la conexión a la base de datos del contenedor:

   ```powershell
   .\gradlew.bat bootRun
   ```

La API queda en `http://localhost:8080`.

Ejemplo:

```powershell
curl.exe -X POST http://localhost:8080/api/users -H "Content-Type: application/json" -d "{\"username\":\"anibal\",\"email\":\"anibal@mail.com\",\"password\":\"password123\"}"
curl.exe -u anibal:password123 http://localhost:8080/api/users
```

## Tests

Los tests necesitan PostgreSQL levantado y las variables de conexión, porque no usan Docker Compose:

```powershell
$env:POSTGRES_PASSWORD = "la_contraseña_de_tu_.env"
$env:POSTGRES_DB = "timertracker"
$env:POSTGRES_USER = "timertracker"
.\gradlew.bat test
```

El puerto por defecto es el `5434`. Si lo cambias, define también `POSTGRES_PORT`.

Qué cubren:
- `UserServiceTest`: alta, cifrado de contraseña, duplicados, rol inexistente, búsquedas.
- `UserDetailsServiceImplTest`: carga de usuarios para la autenticación.
- `UserControllerTest`: códigos 201, 400, 401, 404 y 409, y que la contraseña no aparece en la respuesta.
- `TimertrackerApplicationTests`: que el contexto de Spring arranca.

El workflow de GitHub Actions (`.github/workflows/ci.yml`) ejecuta `./gradlew build` con Postgres como servicio.

## Docker

`docker-compose.yml` define un único servicio, `postgres`, con PostgreSQL 16 y los datos en el volumen `postgres_data`.

Publica el puerto `5434` del host. El `5432` suele estar ocupado por una instalación local de PostgreSQL y el `5433` lo usaba otro proyecto. Si cambias el puerto, actualízalo también en `docker-compose.yml`.

Para borrar la base de datos, incluidos los datos: `docker compose down -v`.

## Estado del proyecto

Hecho:
- Alta, consulta por id y listado de usuarios.
- Validación de entrada, manejo global de errores y 400, 404 y 409.
- Contraseñas cifradas con BCrypt.
- Autenticación HTTP Basic con usuarios de la base de datos.
- Roles `ADMIN` y `EMPLOYEE` creados al arrancar.
- Tests unitarios y de controlador, y CI en GitHub Actions.

Limitaciones conocidas:
- No hay login con token (JWT) ni sesiones. Cada petición autenticada envía usuario y contraseña.
- Los endpoints no comprueban el rol: cualquier usuario autenticado puede listar usuarios.
- No hay endpoints para cambiar la contraseña, desactivar usuarios ni paginar la lista.
- Las entidades `TimeEntry` y `Workday` existen, pero no tienen endpoints ni lógica de fichaje.
- El esquema lo crea Hibernate (`ddl-auto: update`). No hay migraciones.
