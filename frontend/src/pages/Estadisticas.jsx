import { Link, useParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api.js'
import { Aviso, Cargando, Kpi, useCarga, Vacio } from '../components/comunes.jsx'
import { REFERENCIA, SERIES, TablaDatos, TooltipCaja, ejeProps, grillaProps } from '../components/graficos.jsx'
import { CATEGORIAS, METODOS, PUNTAJES, pct, puntaje } from '../util.js'

/** Resultados de la encuesta de satisfacción del evento. */
function Encuesta({ id }) {
  const { datos: r, error } = useCarga(() => api.get(`/api/eventos/${id}/encuestas`), [id])
  if (error) return <Aviso>{error}</Aviso>
  if (!r) return null
  const max = Math.max(1, ...r.distribucion)
  return (
    <section className="tarjeta">
      <h2>Encuesta de satisfacción</h2>
      {r.respuestas === 0 ? (
        <Vacio>{r.asistentes === 0 ? 'La encuesta se habilita para los asistentes cuando el evento finaliza.'
          : 'Todavía nadie respondió la encuesta.'}</Vacio>
      ) : (
        <>
          <div className="kpis">
            <Kpi titulo="Satisfacción" valor={`${puntaje(r.promedio)} / 5`} detalle={PUNTAJES[Math.round(r.promedio)]}
                 tono={r.promedio >= 4 ? 'ok' : r.promedio < 3 ? 'alerta' : 'aviso'} />
            <Kpi titulo="Lo recomendarían" valor={pct(r.porcentajeRecomienda)} detalle="de quienes respondieron" />
            <Kpi titulo="Respuestas" valor={r.respuestas} detalle={`${pct(r.tasaRespuesta)} de ${r.asistentes} asistentes`} />
          </div>
          <div className="dos-columnas">
            <div>
              <h3>Distribución de puntajes</h3>
              <div className="distribucion">
                {[5, 4, 3, 2, 1].map((n) => (
                  <div key={n} className="distribucion-fila">
                    <span>{'★'.repeat(n)}</span>
                    <div className="cupo-barra"><div className="cupo-lleno" style={{ width: `${(r.distribucion[n - 1] / max) * 100}%` }} /></div>
                    <span className="num">{r.distribucion[n - 1]}</span>
                  </div>
                ))}
              </div>
            </div>
            <div>
              <h3>Comentarios</h3>
              {r.comentarios.length === 0 ? <Vacio>Sin comentarios.</Vacio> : (
                <ul className="comentarios">{r.comentarios.map((c, i) => <li key={i}>“{c}”</li>)}</ul>
              )}
            </div>
          </div>
          <p className="nota">Las respuestas son anónimas. El puntaje medio se usa como variable del modelo predictivo
            (satisfacción previa del organizador) para estimar la asistencia de sus próximos eventos.</p>
        </>
      )}
    </section>
  )
}

function Comparativo({ titulo, datos, nombres = {} }) {
  const filas = datos.map((d) => ({ ...d, etiqueta: nombres[d.nombre] ?? d.nombre }))
  return (
    <section className="tarjeta">
      <h2>{titulo}</h2>
      {filas.length === 0 ? <Vacio>Sin datos.</Vacio> : (
        <>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={filas} barGap={2} barCategoryGap="28%">
              <CartesianGrid {...grillaProps} />
              <XAxis dataKey="etiqueta" {...ejeProps} interval={0} tickFormatter={(v) => v.replace("Ciencias ", "C. ")} />
              <YAxis {...ejeProps} allowDecimals={false} width={36} />
              <Tooltip content={<TooltipCaja />} cursor={{ fill: 'rgba(11,11,11,0.04)' }} />
              <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              <Bar dataKey="inscriptos" name="Inscriptos" fill={REFERENCIA} radius={[4, 4, 0, 0]} isAnimationActive={false} />
              <Bar dataKey="asistentes" name="Asistentes" fill={SERIES[0]} radius={[4, 4, 0, 0]} isAnimationActive={false} />
            </BarChart>
          </ResponsiveContainer>
          <TablaDatos columnas={[{ k: 'etiqueta', t: 'Grupo' }, { k: 'inscriptos', t: 'Inscriptos', num: true }, { k: 'asistentes', t: 'Asistentes', num: true }]} filas={filas} />
        </>
      )}
    </section>
  )
}

export default function Estadisticas() {
  const { id } = useParams()
  const { datos: s, error, cargando } = useCarga(() => api.get(`/api/eventos/${id}/estadisticas`), [id])
  if (cargando && !s) return <Cargando />
  if (error) return <Aviso>{error}</Aviso>
  const p = s.ultimaPrediccion
  const desvio = p && s.asistentes ? s.asistentes - p.asistenciaEstimada : null

  return (
    <>
      <Link to="/organizador" className="volver">← Volver al panel</Link>
      <div className="cabecera"><div><h1>Estadísticas de participación</h1><p className="sub">{s.titulo}</p></div></div>
      <div className="kpis">
        <Kpi titulo="Inscriptos" valor={s.inscriptos} detalle={`Cupo ${s.capacidad}`} />
        <Kpi titulo="Asistentes" valor={s.asistentes} detalle={`Tasa ${pct(s.tasaAsistencia)}`} />
        <Kpi titulo="Ocupación de sala" valor={s.ocupacionSala === null || s.ocupacionSala === undefined ? '—' : pct(s.ocupacionSala)}
             detalle={s.salaCapacidad ? `Aforo ${s.salaCapacidad}` : 'Sin sala'} tono={s.ocupacionSala > 1 ? 'alerta' : s.ocupacionSala < 0.4 ? 'aviso' : null} />
        <Kpi titulo="Predicción vs. real" valor={p ? p.asistenciaEstimada : '—'}
             detalle={desvio === null ? 'Estimado' : `Real ${s.asistentes} (${desvio >= 0 ? '+' : ''}${desvio})`} />
      </div>
      <div className="dos-columnas">
        <Comparativo titulo="Por categoría" datos={s.porCategoria} nombres={CATEGORIAS} />
        <Comparativo titulo="Por facultad del asistente" datos={s.porFacultad} />
      </div>
      <Encuesta id={id} />
      <section className="tarjeta">
        <h2>Método de registro</h2>
        {s.porMetodo.length === 0 ? <Vacio>Todavía no se registró asistencia.</Vacio> : (
          <div className="metodos">
            {s.porMetodo.map((m) => (
              <div key={m.nombre} className="metodo">
                <span className="metodo-nombre">{METODOS[m.nombre]}</span>
                <div className="cupo-barra"><div className="cupo-lleno" style={{ width: `${(m.asistentes / s.asistentes) * 100}%` }} /></div>
                <span className="num">{m.asistentes} ({pct(m.asistentes / s.asistentes)})</span>
              </div>
            ))}
          </div>
        )}
      </section>
    </>
  )
}
