# Plan de pruebas · EventosUni

**Responsable:** Horacio Agustin Capdevila Fuentes
**Versión:** 1.0 · octubre de 2026

## 1. Objetivo

Verificar que EventosUni cumple los casos de uso de la consigna y que el modelo predictivo responde de forma coherente, antes de la entrega del 12/11.

## 2. Alcance

- **Incluye:** los 5 casos de uso (CRUD de eventos, inscripción con QR, asistencia, panel del organizador, módulo predictivo), la seguridad con JWT y roles, y la encuesta de satisfacción.
- **No incluye:** pruebas de carga ni de seguridad avanzadas (quedan como mejora futura).

## 3. Entorno de prueba

- Proyecto levantado con `docker compose up --build -d`.
- Navegador: Chrome actualizado; también se revisa en el celular.
- Usuarios de demo (contraseña `demo1234`): `admin@`, `organizador@`, `apoyo@` y `estudiante@uni.edu.ar`.
- Datos: los de demo que se cargan al crear la base.

## 4. Pruebas automáticas existentes

Se ejecutan con `cd analitica-python && pytest -q` y `cd core-java && mvn test`.

| # | Archivo | Prueba | Qué verifica |
|---|---|---|---|
| 1 | test_api.py | test_requiere_token | La API de analítica rechaza pedidos sin token |
| 2 | test_api.py | test_estado_y_metricas | El modelo se entrena y devuelve métricas |
| 3 | test_api.py | test_sala_chica_da_sobrecapacidad_y_recomienda_otra | Sala chica: alerta de sobrecupo y sala sugerida |
| 4 | test_api.py | test_sala_grande_da_baja_asistencia | Sala grande: alerta de baja asistencia |
| 5 | test_api.py | test_lluvia_reduce_asistencia | La lluvia baja la asistencia estimada |
| 6 | test_api.py | test_lote_y_tendencias | Predicción en lote y tendencias |
| 7 | test_api.py | test_holt_sigue_tendencia | El pronóstico sigue la tendencia |
| 8 | test_encuestas.py | test_satisfaccion_previa_no_usa_el_propio_evento_ni_posteriores | La encuesta no "filtra" datos del futuro |
| 9 | test_encuestas.py | test_sin_encuestas_usa_valor_por_defecto | Sin encuestas se usa 4,0 |
| 10 | test_encuestas.py | test_organizador_mejor_valorado_tiene_mas_asistencia | Mejor valoración, más asistencia |
| 11 | JwtServiceTest.java | generaYValidaToken | Java genera y valida el token |
| 12 | JwtServiceTest.java | rechazaTokenAlterado | Un token modificado se rechaza |
| 13 | JwtServiceTest.java | exigeSecretoLargo | El secreto debe ser largo |

## 5. Casos de prueba manuales

Estado: ✅ OK · ❌ Falla · ⏳ Pendiente

| ID | Caso de uso | Pasos | Resultado esperado | Resultado obtenido | Estado |
|---|---|---|---|---|---|
| CP-01 | Seguridad | Ingresar con estudiante@ y contraseña incorrecta | Mensaje de error, no entra | | ⏳ |
| CP-02 | Seguridad | Como estudiante, abrir /organizador/eventos | No tiene acceso | | ⏳ |
| CP-03 | CU1 CRUD | Como organizador, crear un evento con sala y horario libre | Se crea en estado borrador | | ⏳ |
| CP-04 | CU1 CRUD | Crear otro evento en la misma sala y horario | Rechazo por superposición | | ⏳ |
| CP-05 | CU1 CRUD | Publicar, editar y cancelar el evento | Cambia el estado en cada paso | | ⏳ |
| CP-06 | CU2 Inscripción | Como estudiante, inscribirse a un evento publicado | Aparece en "Mis inscripciones" con QR | | ⏳ |
| CP-07 | CU2 Inscripción | Inscribirse a un evento con cupo lleno | Mensaje de cupo completo | | ⏳ |
| CP-08 | CU3 Asistencia | Como apoyo, registrar asistencia por código | Queda como presente | | ⏳ |
| CP-09 | CU3 Asistencia | Registrar dos veces la misma asistencia | No se duplica | | ⏳ |
| CP-10 | CU4 Panel | Como organizador, abrir el Panel | Indicadores y lista de eventos | | ⏳ |
| CP-11 | CU5 Predicción | Abrir "Ver predicción" de un evento próximo | Asistencia estimada, intervalo y sala recomendada | | ⏳ |
| CP-12 | Encuestas | Como estudiante, responder una encuesta pendiente | "¡Gracias!" y baja el contador | | ⏳ |
| CP-13 | Encuestas | Como organizador, ver estadísticas de un evento finalizado | Promedio, distribución y comentarios | | ⏳ |

## 6. Criterio de aprobación

El sistema se considera aprobado si pasan las 13 pruebas automáticas y todos los casos manuales de prioridad alta (CP-01 a CP-12).

## 7. Registro de defectos

| ID | Caso | Descripción | Gravedad | Estado |
|---|---|---|---|---|
| | | | | |

