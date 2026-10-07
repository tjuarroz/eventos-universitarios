import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { Aviso, BarraCupo, Cargando, useCarga, Vacio } from '../components/comunes.jsx'
import { FACULTADES, TIPOS, fecha, hora } from '../util.js'

export default function Eventos() {
  const [filtros, setFiltros] = useState({ desde: '', hasta: '', tipo: '', facultad: '', q: '' })
  const { datos, error, cargando, setDatos } = useCarga(() => api.get('/api/eventos', filtros), [JSON.stringify(filtros)])
  const [aviso, setAviso] = useState(null)
  const set = (k) => (e) => setFiltros({ ...filtros, [k]: e.target.value })

  const inscribir = async (ev) => {
    setAviso(null)
    try {
      const r = await api.post(`/api/eventos/${ev.id}/inscripcion`)
      setDatos(datos.map((x) => (x.id === ev.id ? { ...x, inscripto: true, codigoInscripcion: r.codigo, inscriptos: x.inscriptos + 1 } : x)))
      setAviso({ tipo: 'ok', texto: `Te inscribiste a "${ev.titulo}". Tu código de acceso es ${r.codigo} (lo ves como QR en Mis inscripciones).` })
    } catch (e) {
      setAviso({ tipo: 'error', texto: e.message })
    }
  }

  return (
    <>
      <div className="cabecera">
        <div>
          <h1>Próximos eventos</h1>
          <p className="sub">Charlas, seminarios, talleres, jornadas y eventos institucionales de todas las facultades.</p>
        </div>
      </div>

      <section className="filtros" aria-label="Filtros">
        <input className="buscador" placeholder="Buscar por título…" value={filtros.q} onChange={set('q')} />
        <label>Desde<input type="date" value={filtros.desde} onChange={set('desde')} /></label>
        <label>Hasta<input type="date" value={filtros.hasta} onChange={set('hasta')} /></label>
        <label>Tipo
          <select value={filtros.tipo} onChange={set('tipo')}>
            <option value="">Todos</option>
            {Object.entries(TIPOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label>Facultad
          <select value={filtros.facultad} onChange={set('facultad')}>
            <option value="">Todas</option>
            {FACULTADES.map((f) => <option key={f}>{f}</option>)}
          </select>
        </label>
      </section>

      <Aviso tipo={aviso?.tipo} onCerrar={() => setAviso(null)}>{aviso?.texto}</Aviso>
      <Aviso>{error}</Aviso>
      {cargando && !datos ? <Cargando /> : datos?.length === 0 ? (
        <Vacio>No hay eventos que coincidan con los filtros.</Vacio>
      ) : (
        <div className="grilla">
          {datos?.map((ev) => {
            const lleno = ev.inscriptos >= ev.capacidad
            return (
              <article key={ev.id} className="tarjeta">
                <div className="tarjeta-top">
                  <span className={`tipo tipo-${ev.tipo.toLowerCase()}`}>{TIPOS[ev.tipo]}</span>
                  <span className="facultad">{ev.facultad}</span>
                </div>
                <h3>{ev.titulo}</h3>
                <div className="meta">
                  <span>📅 {fecha(ev.fecha)}</span>
                  <span>🕒 {hora(ev.horaInicio)} – {hora(ev.horaFin)}</span>
                  {ev.salaNombre && <span>📍 {ev.salaNombre}</span>}
                </div>
                <BarraCupo ocupados={ev.inscriptos} total={ev.capacidad} />
                <div className="tarjeta-acciones">
                  <Link className="btn btn-sec" to={`/eventos/${ev.id}`}>Ver detalle</Link>
                  {ev.inscripto
                    ? <span className="inscripto">✓ Inscripto</span>
                    : <button className="btn btn-pri" disabled={lleno} onClick={() => inscribir(ev)}>{lleno ? 'Sin cupo' : 'Inscribirme'}</button>}
                </div>
              </article>
            )
          })}
        </div>
      )}
    </>
  )
}
