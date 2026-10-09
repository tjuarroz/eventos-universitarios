-- =====================================================================
--  Migración: encuestas de satisfacción (octubre 2026)
--  Sirve para agregar la funcionalidad a una base que YA existe, sin borrar datos:
--    docker compose exec -T db psql -U eventos -d eventos < db/migraciones/2026-10_encuestas.sql
--  En una base nueva no hace falta: 01_esquema.sql ya incluye todo esto.
-- =====================================================================

-- Encuesta de satisfacción: la responde cada asistente una vez, cuando el evento se finaliza
CREATE TABLE IF NOT EXISTS encuestas (
    id              SERIAL PRIMARY KEY,
    inscripcion_id  INT          NOT NULL UNIQUE REFERENCES inscripciones(id) ON DELETE CASCADE,
    puntaje         SMALLINT     NOT NULL CHECK (puntaje BETWEEN 1 AND 5),   -- 1 = muy malo … 5 = excelente
    recomendaria    BOOLEAN      NOT NULL,
    comentario      VARCHAR(500),
    respondida_en   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Vista usada para entrenar el modelo: un registro por evento finalizado.
-- Las columnas de la encuesta (organizador, satisfacción y respuestas) van al final para poder
-- actualizar la vista con CREATE OR REPLACE en bases ya creadas (ver db/migraciones).
CREATE OR REPLACE VIEW v_historico_eventos AS
SELECT e.id                       AS evento_id,
       e.tipo,
       e.facultad,
       e.fecha,
       e.hora_inicio,
       EXTRACT(EPOCH FROM (e.hora_fin - e.hora_inicio)) / 60 AS duracion_min,
       e.clima,
       COALESCE(s.capacidad, e.capacidad) AS capacidad_sala,
       COUNT(i.id) FILTER (WHERE i.estado = 'ACTIVA') AS inscriptos,
       COUNT(a.id)                                    AS asistentes,
       e.organizador_id,
       ROUND(AVG(en.puntaje), 2)                      AS satisfaccion,   -- NULL si nadie respondió
       COUNT(en.id)                                   AS respuestas
FROM eventos e
LEFT JOIN salas s          ON s.id = e.sala_id
LEFT JOIN inscripciones i  ON i.evento_id = e.id
LEFT JOIN asistencias a    ON a.inscripcion_id = i.id
LEFT JOIN encuestas en     ON en.inscripcion_id = i.id
WHERE e.estado = 'FINALIZADO'
GROUP BY e.id, s.capacidad;
