import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Bar, BarChart, Cell, LabelList, ResponsiveContainer, XAxis, YAxis } from 'recharts'
import { api } from '../api.js'
import { Aviso, Cargando, EstadoBadge, Kpi, useCarga } from '../components/comunes.jsx'
import { ESTADO, REFERENCIA, SERIES, TINTA, ejeProps } from '../components/graficos.jsx'
import { TIPOS, fecha, hora, pct } from '../util.js'

export function riesgo(p) {
  if (p >= 0.5) return { tono: 'alerta', texto: 'Alto', color: ESTADO.critico }
  if (p >= 0.2) return { tono: 'aviso', texto: 'Medio', color: ESTADO.serio }
  return { tono: 'ok', texto: 'Bajo', color: ESTADO.bien }
}

export default function PrediccionEvento() {
  const { id } = useParams()
  const evento = useCarga(() => api.get(`/api/eventos/${id}`), [id])
  const pred = useCarga(async () => (await api.get(`/api/eventos/${id}/prediccion`)) ?? api.post(`/api/eventos/${id}/prediccion`), [id])
  const [recalculando, setRecalculando] = useState(false)
  const [error, setError] = useState('')

  const recalcular = async () => {
    setRecalculando(true)
    setError('')
    try {
      pred.setDatos(await api.post(`/api/eventos/${id}/prediccion`))
    } catch (e) {
      setError(e.message)
    } finally {
      setRecalculando(false)
    }
  }

  if (evento.cargando && !evento.datos) return <Cargando />
  if (evento.error) return <Aviso>{evento.error}</Aviso>
  const ev = evento.datos
  const p = pred.datos
  const d = p?.detalle

  const barras = p ? [
    { nombre: 'Cupo de inscripción', valor: p.capacidad, color: REFERENCIA },
    { nombre: 'Inscriptos proyectados', valor: d?.inscriptos_proyectados ?? p.inscriptos, color: REFERENCIA },
    { nombre: 'Asistencia estimada', valor: p.asistenciaEstimada, color: SERIES[0] },
    ...(p.salaActualCapacidad ? [{ nombre: `Aforo ${p.salaActualNombre}`, valor: p.salaActualCapacidad, color: REFERENCIA }] : []),
    ...(p.salaRecomendadaCapacidad && p.salaRecomendadaId !== p.salaActualId ? [{ nombre: `Aforo ${p.salaRecomendadaNombre} (recomendada)`, valor: p.salaRecomendadaCapacidad, color: SERIES[2] }] : []),
  ] : []
  const rs = p && riesgo(p.probSobrecapacidad)
  const rb = p && riesgo(p.probBajaAsistencia)

  return (
    <>
      <Link to="/organizador" className="volver">← Volver al panel</Link>
      <div className="cabecera">
        <div>
          <h1>Predicción de asistencia</h1>
          <p className="sub">{ev.titulo} · {TIPOS[ev.tipo]} · {fecha(ev.fecha)} {hora(ev.horaInicio)} · <EstadoBadge estado={ev.estado} /></p>
        </div>
        <button className="btn btn-pri" onClick={recalcular} disabled={recalculando}>{recalculando ? 'Calculando…' : 'Recalcular'}</button>
      </div>
      <Aviso>{error || pred.error}</Aviso>
      {pred.cargando && !p ? <Cargando texto="Consultando el modelo predictivo…" /> : p && (
        <>
          <div className="kpis">
            <Kpi titulo="Asistencia estimada" valor={p.asistenciaEstimada} detalle={`Rango 80 %: ${p.intervaloInferior}–${p.intervaloSuperior} personas`} />
            <Kpi titulo="Prob. de sobrecapacidad" valor={pct(p.probSobrecapacidad)} detalle={`Riesgo ${rs.texto.toLowerCase()} · supera el aforo`} tono={rs.tono} />
            <Kpi titulo="Prob. de baja asistencia" valor={pct(p.probBajaAsistencia)} detalle={`Riesgo ${rb.texto.toLowerCase()} · menos del 40 % del aforo`} tono={rb.tono} />
            <Kpi titulo={p.salaRecomendadaId && p.salaRecomendadaId === p.salaActualId ? 'Sala recomendada (la actual)' : 'Sala recomendada'} valor={p.salaRecomendadaNombre ?? '—'}
                 detalle={p.salaRecomendadaCapacidad ? `${p.salaRecomendadaCapacidad} lugares · ocupación ${pct(d?.sala_recomendada?.ocupacion_estimada)}` : ''}
                 tono={d?.sala_actual_adecuada === false ? 'aviso' : null} />
          </div>

          <div className="dos-columnas">
            <section className="tarjeta">
              <h2>Asistencia estimada vs. capacidad</h2>
              <ResponsiveContainer width="100%" height={barras.length * 46 + 20}>
                <BarChart data={barras} layout="vertical" margin={{ left: 8, right: 48 }} barCategoryGap={10}>
                  <XAxis type="number" hide domain={[0, 'dataMax']} />
                  <YAxis type="category" dataKey="nombre" width={190} {...ejeProps} tick={{ fill: TINTA.secundaria, fontSize: 12 }} />
                  <Bar dataKey="valor" radius={[0, 4, 4, 0]} isAnimationActive={false}>
                    {barras.map((b) => <Cell key={b.nombre} fill={b.color} />)}
                    <LabelList dataKey="valor" position="right" fill={TINTA.primaria} fontSize={12} />
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
              <p className="nota">Tasa de asistencia estimada: <b>{pct(d?.tasa_estimada)}</b> de los inscriptos · Modelo: {p.modelo === 'random_forest' ? 'Random Forest' : 'Regresión lineal (Ridge)'}</p>
            </section>
            <section className="tarjeta">
              <h2>¿Por qué esta estimación?</h2>
              <ul className="explicacion">
                {(d?.explicacion ?? []).map((m) => <li key={m}>{m}</li>)}
              </ul>
              <p className="nota">Generada el {new Date(p.generadaEn).toLocaleString('es-AR')}. Se recalcula al cambiar inscriptos, sala o clima.</p>
              {d?.sala_actual_adecuada === false && p.salaRecomendadaId && ev.estado !== 'FINALIZADO' && (
                <Link className="btn btn-sec" to={`/organizador/eventos/${id}/editar`}>Cambiar sala del evento</Link>
              )}
            </section>
          </div>
        </>
      )}
    </>
  )
}
