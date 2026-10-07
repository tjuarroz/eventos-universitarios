import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { Aviso, Cargando, Kpi, useCarga, Vacio } from '../components/comunes.jsx'
import EscanerQR from '../components/EscanerQR.jsx'
import { CATEGORIAS, METODOS, fecha, hora, pct } from '../util.js'

export default function Asistencia() {
  const { id } = useParams()
  const evento = useCarga(() => api.get(`/api/eventos/${id}`), [id])
  const lista = useCarga(() => api.get(`/api/eventos/${id}/inscriptos`), [id])
  const [modo, setModo] = useState('lista')
  const [codigo, setCodigo] = useState('')
  const [q, setQ] = useState('')
  const [ultimo, setUltimo] = useState(null)

  const registrar = async (cuerpo) => {
    try {
      const r = await api.post(`/api/eventos/${id}/asistencias`, cuerpo)
      setUltimo({ tipo: r.yaRegistrada ? 'info' : 'ok', texto: r.yaRegistrada ? `${r.nombreCompleto} ya tenía la asistencia registrada.` : `✓ Asistencia registrada: ${r.nombreCompleto}` })
      lista.setDatos((prev) => prev?.map((i) => (i.id === r.inscripcionId ? { ...i, asistio: true, metodo: r.metodo, registradaEn: r.registradaEn } : i)))
    } catch (e) {
      setUltimo({ tipo: 'error', texto: e.message })
    }
  }
  const anular = async (i) => {
    try {
      await api.del(`/api/eventos/${id}/asistencias/${i.id}`)
      lista.setDatos((prev) => prev.map((x) => (x.id === i.id ? { ...x, asistio: false, metodo: null } : x)))
    } catch (e) {
      setUltimo({ tipo: 'error', texto: e.message })
    }
  }

  const filtradas = useMemo(() => (lista.datos || []).filter((i) =>
    `${i.apellido} ${i.nombre} ${i.email} ${i.codigo}`.toLowerCase().includes(q.toLowerCase())), [lista.datos, q])

  if ((evento.cargando && !evento.datos) || (lista.cargando && !lista.datos)) return <Cargando />
  if (evento.error || lista.error) return <Aviso>{evento.error || lista.error}</Aviso>
  const ev = evento.datos
  const total = lista.datos.length
  const presentes = lista.datos.filter((i) => i.asistio).length

  return (
    <>
      <Link to="/organizador" className="volver">← Volver al panel</Link>
      <div className="cabecera">
        <div>
          <h1>Registro de asistencia</h1>
          <p className="sub">{ev.titulo} · {fecha(ev.fecha)} {hora(ev.horaInicio)} · {ev.salaNombre ?? 'Sala a confirmar'}</p>
        </div>
      </div>
      <div className="kpis">
        <Kpi titulo="Inscriptos" valor={total} />
        <Kpi titulo="Presentes" valor={presentes} tono="ok" />
        <Kpi titulo="Asistencia" valor={pct(total ? presentes / total : 0)} />
        <Kpi titulo="Aforo de sala" valor={ev.salaCapacidad ?? '—'} detalle={ev.salaCapacidad && presentes > ev.salaCapacidad ? '¡Sala excedida!' : null} tono={ev.salaCapacidad && presentes > ev.salaCapacidad ? 'alerta' : null} />
      </div>

      <div className="pestanas" role="tablist">
        {[['lista', 'Lista'], ['codigo', 'Código'], ['qr', 'Escanear QR']].map(([k, v]) => (
          <button key={k} role="tab" aria-selected={modo === k} className={modo === k ? 'activa' : ''} onClick={() => setModo(k)}>{v}</button>
        ))}
      </div>
      <Aviso tipo={ultimo?.tipo} onCerrar={() => setUltimo(null)}>{ultimo?.texto}</Aviso>

      {modo === 'codigo' && (
        <form className="tarjeta form codigo-form" onSubmit={(e) => { e.preventDefault(); if (codigo.trim()) { registrar({ codigo: codigo.trim(), metodo: 'CODIGO' }); setCodigo('') } }}>
          <label>Código de inscripción
            <input value={codigo} onChange={(e) => setCodigo(e.target.value.toUpperCase())} placeholder="Ej: K7P2QX9M" autoFocus maxLength={12} className="input-codigo" />
          </label>
          <button className="btn btn-pri">Registrar</button>
        </form>
      )}
      {modo === 'qr' && (
        <div className="tarjeta">
          <EscanerQR onLeer={(texto) => registrar({ codigo: texto, metodo: 'QR' })} />
          <p className="nota">Apuntá la cámara al QR que el asistente muestra en “Mis inscripciones”.</p>
        </div>
      )}

      <section className="filtros">
        <input className="buscador" placeholder="Buscar por nombre, email o código…" value={q} onChange={(e) => setQ(e.target.value)} />
      </section>
      {filtradas.length === 0 ? <Vacio>No hay inscriptos{q && ' que coincidan'}.</Vacio> : (
        <div className="tabla-cont">
          <table className="tabla">
            <thead><tr><th>Presente</th><th>Apellido y nombre</th><th>Categoría</th><th>Código</th><th>Registro</th></tr></thead>
            <tbody>
              {filtradas.map((i) => (
                <tr key={i.id} className={i.asistio ? 'fila-ok' : ''}>
                  <td>
                    <input type="checkbox" className="check" checked={i.asistio} aria-label={`Presente ${i.apellido}`}
                           onChange={() => (i.asistio ? anular(i) : registrar({ inscripcionId: i.id, metodo: 'LISTA' }))} />
                  </td>
                  <td>{i.apellido}, {i.nombre}<div className="apagado">{i.email}</div></td>
                  <td>{CATEGORIAS[i.categoria]}</td>
                  <td><code>{i.codigo}</code></td>
                  <td>{i.asistio ? <span className="ok">{METODOS[i.metodo]}{i.registradaEn && ` · ${i.registradaEn.slice(11, 16)}`}</span> : <span className="apagado">—</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
