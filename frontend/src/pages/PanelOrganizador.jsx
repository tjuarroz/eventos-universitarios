import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Aviso, Cargando, EstadoBadge, Kpi, useCarga, Vacio } from '../components/comunes.jsx'
import { ESTADOS, TIPOS, fechaCorta, hora, pct } from '../util.js'

export default function PanelOrganizador() {
  const { esOrganizador, usuario } = useAuth()
  const nav = useNavigate()
  const [estado, setEstado] = useState('')
  const [q, setQ] = useState('')
  const [msg, setMsg] = useState(null)
  const resumen = useCarga(() => api.get('/api/panel/resumen'), [])
  const tabla = useCarga(() => api.get('/api/panel/eventos', { estado }), [estado])

  const cambiar = async (ev, accion, texto) => {
    setMsg(null)
    try {
      await api.post(`/api/eventos/${ev.id}/${accion}`)
      setMsg({ tipo: 'ok', texto: `"${ev.titulo}" ${texto}` })
      tabla.recargar()
      resumen.recargar()
    } catch (e) {
      setMsg({ tipo: 'error', texto: e.message })
    }
  }
  const eliminar = async (ev) => {
    if (!window.confirm(`¿Eliminar "${ev.titulo}"?`)) return
    try {
      await api.del(`/api/eventos/${ev.id}`)
      tabla.recargar()
      resumen.recargar()
    } catch (e) {
      setMsg({ tipo: 'error', texto: e.message })
    }
  }

  const r = resumen.datos
  const filas = (tabla.datos || []).filter((e) => e.titulo.toLowerCase().includes(q.toLowerCase()))

  return (
    <>
      <div className="cabecera">
        <div>
          <h1>Panel del organizador</h1>
          <p className="sub">{usuario.rol === 'ORGANIZADOR' ? 'Tus eventos' : 'Todos los eventos'}: inscriptos, asistencia real y predicciones.</p>
        </div>
        {esOrganizador && <Link to="/organizador/eventos/nuevo" className="btn btn-pri">+ Nuevo evento</Link>}
      </div>

      {r && (
        <div className="kpis">
          <Kpi titulo="Eventos próximos" valor={r.proximos} detalle={`${r.borradores} en borrador`} />
          <Kpi titulo="Eventos realizados" valor={r.finalizados} detalle={`${r.asistentesTotales.toLocaleString('es-AR')} asistentes`} />
          <Kpi titulo="Tasa de asistencia" valor={pct(r.tasaAsistenciaPromedio)} detalle="asistentes / inscriptos" />
          <Kpi titulo="Sala desbordada" valor={r.eventosConSobrecapacidad} detalle="eventos sobre el aforo" tono="alerta" />
          <Kpi titulo="Sala sobredimensionada" valor={r.eventosConBajaAsistencia} detalle="ocupación menor al 40 %" tono="aviso" />
        </div>
      )}

      <section className="filtros">
        <input className="buscador" placeholder="Buscar evento…" value={q} onChange={(e) => setQ(e.target.value)} />
        <label>Estado
          <select value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(ESTADOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
      </section>
      <Aviso tipo={msg?.tipo} onCerrar={() => setMsg(null)}>{msg?.texto}</Aviso>
      <Aviso>{tabla.error}</Aviso>

      {tabla.cargando && !tabla.datos ? <Cargando /> : filas.length === 0 ? <Vacio>No hay eventos.</Vacio> : (
        <div className="tabla-cont">
          <table className="tabla">
            <thead>
              <tr><th>Título</th><th>Fecha</th><th>Sala</th><th className="num">Inscriptos</th><th className="num">Asistentes</th><th className="num">Estimado</th><th>Estado</th><th>Acciones</th></tr>
            </thead>
            <tbody>
              {filas.map((e) => (
                <tr key={e.id}>
                  <td><Link to={`/eventos/${e.id}`} className="titulo-celda">{e.titulo}</Link><div className="apagado">{TIPOS[e.tipo]} · {e.facultad}</div></td>
                  <td>{fechaCorta(e.fecha)} <span className="apagado">{hora(e.horaInicio)}</span></td>
                  <td>{e.salaNombre ?? '—'}{e.salaCapacidad && <div className="apagado">aforo {e.salaCapacidad}</div>}</td>
                  <td className="num">{e.inscriptos}<span className="apagado">/{e.capacidad}</span></td>
                  <td className="num">{e.estado === 'FINALIZADO' || e.asistentes > 0 ? e.asistentes : '—'}</td>
                  <td className="num">{e.asistenciaEstimada ?? '—'}{e.probSobrecapacidad > 0.3 && <span className="punto-alerta" title="Riesgo de sobrecapacidad">!</span>}</td>
                  <td><EstadoBadge estado={e.estado} /></td>
                  <td>
                    <div className="acciones">
                      {esOrganizador && ['BORRADOR', 'PUBLICADO'].includes(e.estado) && <button className="btn btn-chico btn-sec" onClick={() => nav(`/organizador/eventos/${e.id}/editar`)}>Editar</button>}
                      {e.estado !== 'CANCELADO' && <button className="btn btn-chico btn-sec" onClick={() => nav(`/organizador/eventos/${e.id}/prediccion`)}>Ver predicción</button>}
                      <button className="btn btn-chico btn-sec" onClick={() => nav(`/organizador/eventos/${e.id}/estadisticas`)}>Ver estadísticas</button>
                      {['PUBLICADO', 'FINALIZADO'].includes(e.estado) && <button className="btn btn-chico btn-sec" onClick={() => nav(`/organizador/eventos/${e.id}/asistencia`)}>Asistencia</button>}
                      {esOrganizador && e.estado === 'BORRADOR' && <button className="btn btn-chico btn-pri" onClick={() => cambiar(e, 'publicar', 'publicado')}>Publicar</button>}
                      {esOrganizador && e.estado === 'PUBLICADO' && <button className="btn btn-chico btn-sec" onClick={() => cambiar(e, 'finalizar', 'finalizado')}>Finalizar</button>}
                      {esOrganizador && e.estado === 'BORRADOR' && <button className="btn btn-chico btn-peligro" onClick={() => eliminar(e)}>Eliminar</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
