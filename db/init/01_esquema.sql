-- =====================================================================
--  Sistema de gestión de eventos universitarios con predicción de asistencia
--  Esquema de base de datos (PostgreSQL 16)
-- =====================================================================

CREATE TABLE roles (
    id          SERIAL PRIMARY KEY,
    nombre      VARCHAR(30)  NOT NULL UNIQUE,      -- ADMIN, ORGANIZADOR, APOYO, USUARIO
    descripcion VARCHAR(200)
);

CREATE TABLE usuarios (
    id             SERIAL PRIMARY KEY,
    nombre         VARCHAR(80)  NOT NULL,
    apellido       VARCHAR(80)  NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    categoria      VARCHAR(20)  NOT NULL DEFAULT 'ESTUDIANTE'
                   CHECK (categoria IN ('ESTUDIANTE','DOCENTE','PERSONAL')),
    facultad       VARCHAR(80),
    rol_id         INT          NOT NULL REFERENCES roles(id),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE salas (
    id            SERIAL PRIMARY KEY,
    nombre        VARCHAR(80)  NOT NULL UNIQUE,
    edificio      VARCHAR(80)  NOT NULL,
    facultad      VARCHAR(80),
    capacidad     INT          NOT NULL CHECK (capacidad > 0),
    equipamiento  VARCHAR(300),
    activa        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE eventos (
    id              SERIAL PRIMARY KEY,
    titulo          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    materiales      TEXT,
    tipo            VARCHAR(20)  NOT NULL
                    CHECK (tipo IN ('CHARLA','SEMINARIO','TALLER','JORNADA','INSTITUCIONAL')),
    facultad        VARCHAR(80)  NOT NULL,
    fecha           DATE         NOT NULL,
    hora_inicio     TIME         NOT NULL,
    hora_fin        TIME         NOT NULL,
    sala_id         INT          REFERENCES salas(id),
    capacidad       INT          NOT NULL CHECK (capacidad > 0),   -- cupo de inscripción
    clima           VARCHAR(20)  NOT NULL DEFAULT 'SOLEADO'
                    CHECK (clima IN ('SOLEADO','NUBLADO','LLUVIA')),
    estado          VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR'
                    CHECK (estado IN ('BORRADOR','PUBLICADO','FINALIZADO','CANCELADO')),
    organizador_id  INT          NOT NULL REFERENCES usuarios(id),
    creado_en       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CHECK (hora_fin > hora_inicio)
);
CREATE INDEX idx_eventos_fecha  ON eventos(fecha);
CREATE INDEX idx_eventos_estado ON eventos(estado);

CREATE TABLE inscripciones (
    id                 SERIAL PRIMARY KEY,
    evento_id          INT          NOT NULL REFERENCES eventos(id) ON DELETE CASCADE,
    usuario_id         INT          NOT NULL REFERENCES usuarios(id),
    codigo             VARCHAR(12)  NOT NULL UNIQUE,     -- código corto / contenido del QR
    estado             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA'
                       CHECK (estado IN ('ACTIVA','CANCELADA')),
    fecha_inscripcion  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (evento_id, usuario_id)
);
CREATE INDEX idx_inscripciones_evento ON inscripciones(evento_id);

CREATE TABLE asistencias (
    id              SERIAL PRIMARY KEY,
    inscripcion_id  INT         NOT NULL UNIQUE REFERENCES inscripciones(id) ON DELETE CASCADE,
    metodo          VARCHAR(10) NOT NULL CHECK (metodo IN ('LISTA','CODIGO','QR')),
    registrada_por  INT         REFERENCES usuarios(id),
    registrada_en   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE predicciones (
    id                    SERIAL PRIMARY KEY,
    evento_id             INT          NOT NULL REFERENCES eventos(id) ON DELETE CASCADE,
    asistencia_estimada   INT          NOT NULL,
    intervalo_inferior    INT,
    intervalo_superior    INT,
    prob_sobrecapacidad   NUMERIC(5,4) NOT NULL,
    prob_baja_asistencia  NUMERIC(5,4) NOT NULL,
    sala_recomendada_id   INT          REFERENCES salas(id),
    modelo                VARCHAR(60)  NOT NULL,
    detalle               TEXT,                        -- JSON con explicación / métricas
    generada_en           TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_predicciones_evento ON predicciones(evento_id, generada_en DESC);

-- Vista usada para entrenar el modelo: un registro por evento finalizado
CREATE VIEW v_historico_eventos AS
SELECT e.id                       AS evento_id,
       e.tipo,
       e.facultad,
       e.fecha,
       e.hora_inicio,
       EXTRACT(EPOCH FROM (e.hora_fin - e.hora_inicio)) / 60 AS duracion_min,
       e.clima,
       COALESCE(s.capacidad, e.capacidad) AS capacidad_sala,
       COUNT(i.id) FILTER (WHERE i.estado = 'ACTIVA') AS inscriptos,
       COUNT(a.id)                                    AS asistentes
FROM eventos e
LEFT JOIN salas s          ON s.id = e.sala_id
LEFT JOIN inscripciones i  ON i.evento_id = e.id
LEFT JOIN asistencias a    ON a.inscripcion_id = i.id
WHERE e.estado = 'FINALIZADO'
GROUP BY e.id, s.capacidad;
