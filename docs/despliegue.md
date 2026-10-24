# Propuesta de despliegue · EventosUni

**Responsable:** Tiago Perez
**Versión:** 1.0 · octubre de 2026

## 1. Objetivo

Proponer cómo publicar EventosUni en internet para que la universidad lo use, manteniendo la misma arquitectura que hoy corre en Docker Compose.

## 2. Qué hay que desplegar

| Servicio | Tecnología | Puerto interno | ¿Expuesto a internet? |
|---|---|---|---|
| frontend | React + nginx | 80 | Sí (a través de HTTPS) |
| core | Java Spring Boot | 8080 | No (solo vía nginx en /api) |
| analitica | Python FastAPI | 8000 | No (solo vía nginx en /analitica) |
| db | PostgreSQL 16 | 5432 | No |

## 3. Opciones evaluadas

| Opción | Ventajas | Desventajas | Costo mensual aprox. |
|---|---|---|---|
| A. Servidor virtual (VPS) con Docker Compose | Se usa el mismo docker-compose.yml; control total | Hay que mantener el servidor | USD 24 (DigitalOcean, 4 GB de RAM) |
| B. Plataforma administrada (PaaS) | Menos mantenimiento | Hay que adaptar cada servicio; puede costar más | USD 38 a 69 (Render, según el tamaño de cada servicio) |
| C. Servidor de la universidad | Sin costo extra; datos dentro de la institución | Depende del área de sistemas | USD 0 (usa infraestructura existente) |

## 4. Opción recomendada: A. Servidor virtual (VPS) con Docker Compose

Es la opción más económica entre las pagas y la que menos cambios requiere: se despliega el mismo `docker-compose.yml` que ya usamos, sin adaptar cada servicio a una plataforma distinta. En una PaaS cada servicio se cobra por separado y la instancia más chica (512 MB) queda justa para Spring Boot y para el modelo de scikit-learn, así que conviene pasar a la de 2 GB y el costo sube. La opción C sería ideal por costo y por mantener los datos en la institución, pero depende de los tiempos del área de sistemas; queda como alternativa si la universidad ofrece un servidor.

Pasos generales para opción A:

1. Crear un servidor Linux (Ubuntu) con al menos 4 GB de RAM (la guía de uso del proyecto indica que Docker necesita unos 3–4 GB para levantar todo).
2. Instalar Docker y Docker Compose.
3. Clonar el repositorio.
4. Crear el archivo `.env` con contraseñas nuevas (`DB_PASSWORD` y `JWT_SECRET`).
5. Ejecutar `docker compose up -d --build`.
6. Apuntar un dominio al servidor y activar HTTPS (por ejemplo con un proxy y certificados Let's Encrypt).
7. Programar una copia de seguridad diaria de la base (`pg_dump`).

## 5. Seguridad

- Cambiar las contraseñas de ejemplo antes de publicar.
- Abrir solo los puertos 80 y 443; la base y los microservicios quedan en la red interna de Docker.
- No subir nunca el archivo `.env` al repositorio.
- En producción, quitar del `docker-compose.yml` los `ports` de db (5432), core (8080) y analitica (8000): hoy están publicados para desarrollo, y Docker expone los puertos publicados aunque el firewall del servidor (ufw) los bloquee.
- No cargar los datos de demo (`02_datos_demo.sql`) en producción: crean usuarios con la contraseña `demo1234`, incluido el administrador.
- Actualizar `CORS_ORIGINS` en el `docker-compose.yml` con el dominio real en lugar de `localhost`.

## 6. Costos estimados

| Proveedor | Plan | Precio mensual | Fecha de consulta |
|---|---|---|---|
| DigitalOcean (VPS) | Basic Droplet Regular: 2 vCPU, 4 GiB RAM, 80 GiB SSD | USD 24 | 24/10/2026 |
| Render (PaaS) · configuración mínima | Frontend como sitio estático (USD 0) + core 1 CPU / 2 GB (USD 25) + analitica 0,5 CPU / 512 MB (USD 7) + PostgreSQL 256 MB (USD 6) | USD 38 | 24/10/2026 |
| Render (PaaS) · configuración recomendada | Frontend como sitio estático (USD 0) + core 1 CPU / 2 GB (USD 25) + analitica 1 CPU / 2 GB (USD 25) + PostgreSQL 1 GB (USD 19) | USD 69 | 24/10/2026 |

Para la opción A se suma el dominio (costo anual bajo); el certificado HTTPS de Let's Encrypt es gratuito. Si se quieren los backups automáticos del proveedor, DigitalOcean los cobra aparte (20 % del costo del servidor si son semanales, 30 % si son diarios).

## 7. Riesgos

| Riesgo | Mitigación |
|---|---|
| Caída del servidor | `restart: unless-stopped` en los contenedores, monitoreo con alertas (por ejemplo UptimeRobot) y snapshots del servidor para restaurarlo rápido |
| Pérdida de datos | Copia de seguridad diaria |
| Acceso no autorizado | Firewall con solo 80/443, puertos internos sin publicar, acceso SSH con clave, sin usuarios de demo y `.env` fuera del repositorio |
| Falta de memoria (Java + Python + Postgres) | Servidor de 4 GB y límite de memoria para la JVM |
