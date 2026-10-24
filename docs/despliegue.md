Propuesta de despliegue · EventosUni
Responsable: Tiago Perez
Versión: 1.0 · octubre de 2026
1. Objetivo
Proponer cómo publicar EventosUni en internet para que la universidad lo use, manteniendo la misma arquitectura que hoy corre en Docker Compose.
2. Qué hay que desplegar
Servicio	Tecnología	Puerto interno	¿Expuesto a internet?
frontend	React + nginx	80	Sí (a través de HTTPS)
core	Java Spring Boot	8080	No (solo vía nginx en /api)
analitica	Python FastAPI	8000	No
db	PostgreSQL 16	5432	No
3. Opciones evaluadas
Opción	Ventajas	Desventajas	Costo mensual aprox.
A. Servidor virtual (VPS) con Docker Compose	Se usa el mismo docker-compose.yml; control total	Hay que mantener el servidor	(completar)
B. Plataforma administrada (PaaS)	Menos mantenimiento	Hay que adaptar cada servicio; puede costar más	(completar)
C. Servidor de la universidad	Sin costo extra; datos dentro de la institución	Depende del área de sistemas	(completar)
4. Opción recomendada: (completar)
Pasos generales si se elige la opción A:
Crear un servidor Linux (Ubuntu) con al menos 2 GB de RAM.
Instalar Docker y Docker Compose.
Clonar el repositorio.
Crear el archivo `.env` con contraseñas nuevas (`DB_PASSWORD` y `JWT_SECRET`).
Ejecutar `docker compose up -d --build`.
Apuntar un dominio al servidor y activar HTTPS (por ejemplo con un proxy y certificados Let's Encrypt).
Programar una copia de seguridad diaria de la base (`pg_dump`).
5. Seguridad
Cambiar las contraseñas de ejemplo antes de publicar.
Abrir solo los puertos 80 y 443; la base y los microservicios quedan en la red interna de Docker.
No subir nunca el archivo `.env` al repositorio.
6. Costos estimados
(completar con la investigación: proveedor, plan, precio y fecha de consulta)
7. Riesgos
Riesgo	Mitigación
Caída del servidor	(completar)
Pérdida de datos	Copia de seguridad diaria
	
