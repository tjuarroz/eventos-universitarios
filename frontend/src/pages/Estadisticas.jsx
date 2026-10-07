import { Link, useParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api.js'
import { Aviso, Cargando, Kpi, useCarga, Vacio } from '../components/comunes.jsx'
import { REFERENCIA, SERIES, TablaDatos, TooltipCaja, ejeProps, grillaProps } from '../components/graficos.jsx'
import { CATEGORIAS, METODOS, pct } from '../util.js'

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
              <XAxis dataKey="etiqueta" {...ejeProps} />
              <YAxis {...ejeProps} allowDecimals={false} width={36} />
              <Tooltip content={<TooltipCaja />} cursor={{ fill: 'rgba(11,11,11,0.04)' }} />
              <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              <Bar dataKey="inscriptos" name="Inscriptos" fill={REFERENCIA} radius={[4, 4, 0, 0]} />
              <Bar dataKey="asistentes" name="Asistentes" fill={SERIES[0]} radius={[4, 4, 0, 0]} />
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
