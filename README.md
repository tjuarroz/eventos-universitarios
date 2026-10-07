# Sistema de gestión de eventos universitarios con predicción de asistencia

Proyecto integrador · Programación de Vanguardia (2026)

Una plataforma única para **crear y publicar eventos**, **inscribirse**, **registrar asistencia (lista, código o QR)** y **estimar cuánta gente va a ir**. Sirve para asignar bien las salas y tomar decisiones con datos históricos.

## Arquitectura

| Componente | Tecnología | Puerto | Carpeta |
|---|---|---|---|
| Front-end | React 18 + Vite + Recharts (servido por nginx) | 3000 | `frontend/` |
| Microservicio 1: core del negocio | Java 21 + Spring Boot 3.3 + Spring Security (JWT) + JPA | 8080 | `core-java/` |
| Microservicio 2: analítica predictiva | Python 3.12 + FastAPI + scikit-learn | 8000 | `analitica-python/` |
| Base de datos | PostgreSQL 16 | 5432 | `db/init/` |

```mermaid
flowchart LR
  FE[Front-end React] -- REST /api + JWT --> J[Core Java · Spring Boot]
  FE -- REST /analitica + JWT --> P[Analítica Python · FastAPI]
  J -- histórico / eventos a predecir --> P
  J -- JPA --> DB[(PostgreSQL)]
```

El front-end habla con los **dos** microservicios. Con el core Java hace el login, los eventos, las inscripciones, la asistencia y el panel. Con el servicio Python pide las tendencias y las métricas del modelo. El core le manda al servicio Python los datos históricos para entrenar y los eventos a predecir, y guarda cada predicción en la tabla `predicciones`. Los tres componentes validan el mismo JWT (secreto HMAC compartido).

## Cómo ejecutarlo

### Opción A: Docker (recomendada)

```bash
docker compose up --build
```

Después abrí **http://localhost:3000**.

- La primera vez PostgreSQL crea el esquema y carga los datos de demo: unos 140 eventos finalizados en los últimos 18 meses, 16 eventos próximos y 420 usuarios.
- Al arrancar, el core Java entrena el modelo automáticamente.
- Swagger del core: http://localhost:8080/swagger-ui.html
- Swagger de la analítica: http://localhost:8000/docs

Para volver a cargar los datos de demo desde cero: `docker compose down -v && docker compose up --build`.

### Opción B: cada servicio por separado (desarrollo)

```bash
# 1) Base de datos: crear la BD "eventos" (usuario/clave eventos) y ejecutar los scripts
psql -U eventos -d eventos -f db/init/01_esquema.sql -f db/init/02_datos_demo.sql

# 2) Analítica (Python)
cd analitica-python && python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt && uvicorn app.main:app --port 8000

# 3) Core (Java 21 + Maven)
cd core-java && mvn spring-boot:run

# 4) Front-end
cd frontend && npm install && npm run dev      # http://localhost:5173
```

## Usuarios de prueba

La contraseña de todos es `demo1234`. En la pantalla de login hay botones para completarlos.

| Rol | Email | Qué puede hacer |
|---|---|---|
| Administrador | admin@uni.edu.ar | Todo, más salas, usuarios y roles |
| Organizador | organizador@uni.edu.ar | Crear, editar y publicar sus eventos, panel, predicciones y asistencia |
| Personal de apoyo | apoyo@uni.edu.ar | Registrar asistencia y ver estadísticas |
| Estudiante | estudiante@uni.edu.ar | Ver eventos, inscribirse y mostrar su QR |

## Casos de uso cubiertos

1. **Gestión de eventos (CRUD):** alta, edición, baja, publicación, finalización y cancelación. Controla que dos eventos no ocupen la misma sala en horarios superpuestos.
2. **Inscripción:** listado con filtros por fecha, tipo, facultad y texto. La inscripción genera un código único que el usuario ve como QR. Se valida el cupo.
3. **Registro de asistencia:** por lista (checkbox), código tipeado o **QR escaneado con la cámara**. Es idempotente.
4. **Panel del organizador:** muestra inscriptos, asistentes, estado, historial, indicadores, estadísticas por evento y acceso a las predicciones.
5. **Módulo predictivo (Python):** calcula la asistencia estimada con un intervalo del 80 %, la probabilidad de sobrecapacidad y la de baja asistencia, recomienda sala según el aforo y muestra tendencias por tipo y por facultad con pronóstico.

## Módulo predictivo (resumen)

- **Datos:** un registro por evento finalizado (vista `v_historico_eventos`): tipo, facultad, fecha, horario, duración, clima, aforo, inscriptos y asistentes.
- **Modelo de inscriptos (Random Forest):** proyecta los inscriptos al cierre de la inscripción cuando el evento todavía está lejos.
- **Modelo de tasa de asistencia:** se comparan una **regresión Ridge** y un **Random Forest** con validación cruzada de 5 folds, y se queda el de menor error medio absoluto. También se reporta una línea base (promedio por tipo).
- **Incertidumbre:** se usa una aproximación normal con el error de validación cruzada más la dispersión entre árboles. Con eso se calculan el intervalo del 80 %, `P(asistentes > aforo)` y `P(asistentes < 40 % del aforo)`.
- **Sala recomendada:** la más chica, de la facultad o de uso general, que cubra el escenario alto (percentil 90).
- **Series temporales:** media móvil de 3 meses por grupo y pronóstico a 3 meses con suavizado exponencial de **Holt**.

## Pruebas

```bash
cd analitica-python && pytest -q        # 7 pruebas: seguridad, predicción, lluvia, sala, lote, tendencias
cd core-java && mvn test                # pruebas de JwtService
```

## Estructura

```
db/init/             01_esquema.sql (7 tablas + vista) · 02_datos_demo.sql (generado)
tools/               generar_datos_demo.py (regenera los datos de demo)
core-java/           Spring Boot: domain, repository, service, web, security, dto, exception
analitica-python/    FastAPI: modelo.py, tendencias.py, main.py, seguridad.py, tests/
frontend/            React: pages/, components/, api.js, auth.jsx
docs/                documentación, diagramas (Mermaid) y capturas de pantalla
```
