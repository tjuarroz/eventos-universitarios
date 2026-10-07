import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import { analitica, api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Aviso, Cargando, Kpi, useCarga, Vacio } from '../components/comunes.jsx'
import { REFERENCIA, SERIES, TablaDatos, TooltipCaja, ejeProps, grillaProps } from '../components/graficos.jsx'
import { TIPOS, fechaCorta, mesCorto, pct } from '../util.js'
import { riesgo } from './PrediccionEvento.jsx'

const corto = (t, n = 18) => (t.length > n ? t.slice(0, n - 1) + '…' : t)

function BarrasSimples({ datos, clave, formato, alto = 200 }) {
  return (
    <ResponsiveContainer width="100%" height={alto}>
      <BarChart data={datos} barCategoryGap="30%">
        <CartesianGrid {...grillaProps} />
        <XAxis dataKey="grupo" {...ejeProps} interval={0} />
        <YAxis {...ejeProps} width={40} tickFormatter={formato} />
        <Tooltip content={<TooltipCaja formato={formato} />} cursor={{ fill: 'rgba(11,11,11,0.04)' }} />
        <Bar dataKey={clave} name="Tasa de asistencia" fill={SERIES[0]} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}

export default function Dashboard() {
  const { esOrganizador } = useAuth()
  const proximos = useCarga(() => api.get('/api/predicciones/proximos'), [])
  const [agrupar, setAgrupar] = useState('tipo')
  const [metrica, setMetrica] = useState('tasa')
  const serie = useCarga(() => analitica.get('/tendencias', { agrupar, metrica, horizonte: 3 }), [agrupar, metrica])
  const resumen = useCarga(() => analitica.get('/tendencias/resumen'), [])
  const modelo = useCarga(() => analitica.get('/modelo/estado'), [])
  const [sel, setSel] = useState(null)
  const [msg, setMsg] = useState(null)
  const [entrenando, setEntrenando] = useState(false)

  const preds = proximos.datos || []
  const actual = preds.find((p) => p.eventoId === sel) ?? preds[0]

  const barras = preds.map((p) => ({
    nombre: corto(p.eventoTitulo), titulo: p.eventoTitulo, fecha: p.fecha,
    estimada: p.asistenciaEstimada, capacidad: p.salaActualCapacidad ?? p.capacidad,
    rango: `${p.intervaloInferior}–${p.intervaloSuperior}`, prob: p.probSobrecapacidad, id: p.eventoId,
  }))

  // serie real + pronóstico en un solo arreglo (claves "<grupo>" y "<grupo>__p")
  const lineas = useMemo(() => {
    const s = serie.datos
    if (!s) return []
    const filas = s.datos.map((d) => ({ ...d }))
    s.pronostico.forEach((p, i) => {
      let f = filas.find((x) => x.periodo === p.periodo)
      if (!f) { f = { periodo: p.periodo }; filas.push(f) }
      s.grupos.forEach((g) => { f[`${g}__p`] = p[g] })
      if (i === 0) s.grupos.forEach((g) => { f[`${g}__p`] = f[g] })
    })
    return filas.slice(-15)
  }, [serie.datos])

  const fmtMetrica = metrica === 'tasa' ? (v) => pct(v) : (v) => Math.round(v)
  const nombreGrupo = (g) => (agrupar === 'tipo' ? TIPOS[g] : g)

  const reentrenar = async () => {
    setEntrenando(true)
    setMsg(null)
    try {
      const r = await api.post('/api/analitica/entrenar')
      setMsg({ tipo: 'ok', texto: `Modelo reentrenado con ${r.registros} eventos. Elegido: ${r.modelo_elegido}.` })
      modelo.recargar(); serie.recargar(); resumen.recargar()
      proximos.setDatos(await api.get('/api/predicciones/proximos', { regenerar: true }))
    } catch (e) {
      setMsg({ tipo: 'error', texto: e.message })
    } finally {
      setEntrenando(false)
    }
  }

  const m = modelo.datos
  const elegido = m?.metricas?.[m.modelo_elegido]

  return (
    <>
      <div className="cabecera">
        <div>
          <h1>Dashboard predictivo</h1>
          <p className="sub">Estimaciones del microservicio de analítica (Python) para los próximos eventos y tendencias históricas.</p>
        </div>
        {esOrganizador && <button className="btn btn-sec" onClick={reentrenar} disabled={entrenando}>{entrenando ? 'Entrenando…' : 'Reentrenar modelo'}</button>}
      </div>
      <Aviso tipo={msg?.tipo} onCerrar={() => setMsg(null)}>{msg?.texto}</Aviso>
      <Aviso>{proximos.error}</Aviso>

      {/* ---- Indicadores del evento seleccionado ---- */}
      {proximos.cargando && !proximos.datos ? <Cargando texto="Calculando predicciones…" /> : preds.length === 0 ? (
        <Vacio>No hay eventos próximos para predecir.</Vacio>
      ) : (
        <>
          <section className="tarjeta seleccion">
            <label>Evento
              <select value={actual.eventoId} onChange={(e) => setSel(Number(e.target.value))}>
                {preds.map((p) => <option key={p.eventoId} value={p.eventoId}>{fechaCorta(p.fecha)} · {p.eventoTitulo}</option>)}
              </select>
            </label>
            <Link to={`/organizador/eventos/${actual.eventoId}/prediccion`} className="btn btn-sec btn-chico">Ver detalle</Link>
          </section>
          <div className="kpis">
            <Kpi titulo="Asistencia estimada" valor={actual.asistenciaEstimada} detalle={`Rango ${actual.intervaloInferior}–${actual.intervaloSuperior} · ${actual.inscriptos} inscriptos hoy`} />
            <Kpi titulo="Probabilidad de sobrecapacidad" valor={pct(actual.probSobrecapacidad)} detalle={`Riesgo ${riesgo(actual.probSobrecapacidad).texto.toLowerCase()} · aforo ${actual.salaActualCapacidad ?? 'sin sala'}`} tono={riesgo(actual.probSobrecapacidad).tono} />
            <Kpi titulo="Sala recomendada" valor={actual.salaRecomendadaNombre ?? '—'} detalle={actual.salaRecomendadaCapacidad ? `${actual.salaRecomendadaCapacidad} lugares${actual.salaActualNombre ? ` · hoy: ${actual.salaActualNombre}` : ''}` : ''} />
          </div>

          <section className="tarjeta">
            <h2>Asistencia estimada vs. capacidad de sala</h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={barras} barGap={2} barCategoryGap="22%" onClick={(e) => e?.activePayload && setSel(e.activePayload[0].payload.id)}>
                <CartesianGrid {...grillaProps} />
                <XAxis dataKey="nombre" {...ejeProps} interval={0} angle={-30} textAnchor="end" height={80} />
                <YAxis {...ejeProps} width={40} allowDecimals={false} />
                <Tooltip cursor={{ fill: 'rgba(11,11,11,0.04)' }}
                         content={<TooltipCaja etiqueta={(l, p) => `${p?.[0]?.payload.titulo} · ${fechaCorta(p?.[0]?.payload.fecha)}`}
                                               formato={(v, p) => (p.dataKey === 'estimada' ? `${v} (rango ${p.payload.rango})` : v)} />} />
                <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                <Bar dataKey="estimada" name="Asistencia estimada" fill={SERIES[0]} radius={[4, 4, 0, 0]} />
                <Bar dataKey="capacidad" name="Capacidad (aforo de sala)" fill={REFERENCIA} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
            <TablaDatos columnas={[
              { k: 'titulo', t: 'Evento' }, { k: 'fecha', t: 'Fecha', f: fechaCorta }, { k: 'estimada', t: 'Estimada', num: true },
              { k: 'rango', t: 'Rango 80 %', num: true }, { k: 'capacidad', t: 'Capacidad', num: true }, { k: 'prob', t: 'P(sobrecap.)', num: true, f: (v) => pct(v) },
            ]} filas={barras} />
          </section>
        </>
      )}

      {/* ---- Tendencias ---- */}
      <section className="tarjeta">
        <div className="tarjeta-cabecera">
          <h2>Tendencia de asistencia por {agrupar === 'tipo' ? 'tipo de evento' : 'facultad'}</h2>
          <div className="controles">
            <select value={agrupar} onChange={(e) => setAgrupar(e.target.value)} aria-label="Agrupar por">
              <option value="tipo">Por tipo de evento</option><option value="facultad">Por facultad</option>
            </select>
            <select value={metrica} onChange={(e) => setMetrica(e.target.value)} aria-label="Métrica">
              <option value="tasa">Tasa de asistencia</option><option value="asistentes">Asistentes promedio</option>
            </select>
          </div>
        </div>
        <Aviso>{serie.error}</Aviso>
        {serie.datos && (
          <>
            <ResponsiveContainer width="100%" height={320}>
              <ComposedChart data={lineas}>
                <CartesianGrid {...grillaProps} />
                <XAxis dataKey="periodo" {...ejeProps} tickFormatter={mesCorto} />
                <YAxis {...ejeProps} width={44} tickFormatter={fmtMetrica} domain={metrica === 'tasa' ? [0.3, 1] : ['auto', 'auto']} />
                <Tooltip content={<TooltipCaja etiqueta={mesCorto} formato={fmtMetrica} />} />
                {serie.datos.grupos.map((g, i) => (
                  <Line key={g} dataKey={g} name={nombreGrupo(g)} stroke={SERIES[i]} strokeWidth={2} dot={false} activeDot={{ r: 4 }} connectNulls isAnimationActive={false} />
                ))}
                {serie.datos.grupos.map((g, i) => (
                  <Line key={g + 'p'} dataKey={`${g}__p`} name={`${nombreGrupo(g)} (pronóstico)`} legendType="none" stroke={SERIES[i]} strokeWidth={2} strokeDasharray="5 4" dot={false} isAnimationActive={false} />
                ))}
              </ComposedChart>
            </ResponsiveContainer>
            <div className="leyenda">
              {serie.datos.grupos.map((g, i) => <span key={g}><i style={{ background: SERIES[i] }} />{nombreGrupo(g)}</span>)}
              <span><i className="punteada" />Pronóstico</span>
            </div>
            <p className="nota">Media móvil de 3 meses; la línea punteada es el pronóstico a 3 meses (suavizado exponencial de Holt).</p>
          </>
        )}
      </section>

      {/* ---- Patrones históricos ---- */}
      {resumen.datos && (
        <div className="tres-columnas">
          <section className="tarjeta"><h2>Asistencia por horario</h2><BarrasSimples datos={resumen.datos.por_horario} clave="tasa_asistencia" formato={(v) => pct(v)} /></section>
          <section className="tarjeta"><h2>Asistencia por día</h2><BarrasSimples datos={resumen.datos.por_dia.map((d) => ({ ...d, grupo: d.grupo.slice(0, 3) }))} clave="tasa_asistencia" formato={(v) => pct(v)} /></section>
          <section className="tarjeta"><h2>Asistencia según clima</h2><BarrasSimples datos={resumen.datos.por_clima.map((d) => ({ ...d, grupo: d.grupo[0] + d.grupo.slice(1).toLowerCase() }))} clave="tasa_asistencia" formato={(v) => pct(v)} /></section>
        </div>
      )}

      {/* ---- Modelo ---- */}
      {m?.entrenado && (
        <section className="tarjeta">
          <h2>Modelo predictivo</h2>
          <div className="dos-columnas sin-margen">
            <div>
              <p className="texto">Entrenado con <b>{m.registros}</b> eventos finalizados el {new Date(m.entrenado_en).toLocaleString('es-AR')}.
                Modelo elegido por validación cruzada: <b>{m.modelo_elegido === 'random_forest' ? 'Random Forest' : 'Regresión lineal (Ridge)'}</b>.</p>
              <div className="tabla-cont">
                <table className="tabla tabla-compacta">
                  <thead><tr><th>Modelo</th><th className="num">Error medio (personas)</th><th className="num">R²</th></tr></thead>
                  <tbody>
                    {[['regresion_ridge', 'Regresión Ridge'], ['random_forest', 'Random Forest'], ['linea_base_media_por_tipo', 'Línea base (promedio por tipo)']].map(([k, t]) => (
                      <tr key={k} className={k === m.modelo_elegido ? 'fila-ok' : ''}><td>{t}</td><td className="num">{m.metricas[k]?.mae_asistentes}</td><td className="num">{m.metricas[k]?.r2_asistentes}</td></tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {elegido && <p className="nota">En promedio, la estimación se desvía ±{elegido.mae_asistentes} personas de la asistencia real.</p>}
            </div>
            <div>
              <h3>Variables más influyentes</h3>
              <ResponsiveContainer width="100%" height={m.importancias.length * 30 + 10}>
                <BarChart data={m.importancias} layout="vertical" margin={{ right: 40 }}>
                  <XAxis type="number" hide domain={[0, 'dataMax']} />
                  <YAxis type="category" dataKey="nombre" width={150} {...ejeProps} />
                  <Tooltip content={<TooltipCaja formato={(v) => pct(v, 1)} />} cursor={{ fill: 'rgba(11,11,11,0.04)' }} />
                  <Bar dataKey="importancia" name="Importancia" fill={SERIES[0]} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </section>
      )}
    </>
  )
}
