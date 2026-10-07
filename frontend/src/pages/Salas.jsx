import { useState } from 'react'
import { api } from '../api.js'
import { Aviso, Cargando, useCarga } from '../components/comunes.jsx'
import { FACULTADES } from '../util.js'

const NUEVA = { nombre: '', edificio: '', facultad: '', capacidad: 30, equipamiento: '', activa: true }

export default function Salas() {
  const { datos, error, cargando, recargar } = useCarga(() => api.get('/api/salas'), [])
  const [f, setF] = useState(NUEVA)
  const [msg, setMsg] = useState(null)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const guardar = async (e) => {
    e.preventDefault()
    setMsg(null)
    try {
      const cuerpo = { ...f, capacidad: Number(f.capacidad), facultad: f.facultad || null }
      if (f.id) await api.put(`/api/salas/${f.id}`, cuerpo)
      else await api.post('/api/salas', cuerpo)
      setMsg({ tipo: 'ok', texto: `Sala "${f.nombre}" guardada` })
      setF(NUEVA)
      recargar()
    } catch (err) {
      setMsg({ tipo: 'error', texto: err.message })
    }
  }
  const eliminar = async (s) => {
    if (!window.confirm(`¿Eliminar la sala "${s.nombre}"?`)) return
    try {
      await api.del(`/api/salas/${s.id}`)
      recargar()
    } catch (err) {
      setMsg({ tipo: 'error', texto: err.estado === 409 ? 'La sala tiene eventos asociados: desactivala en lugar de eliminarla.' : err.message })
    }
  }

  return (
    <>
      <div className="cabecera"><div><h1>Salas</h1><p className="sub">Espacios disponibles y su aforo. El modelo recomienda salas de esta lista.</p></div></div>
      <div className="dos-columnas lateral">
        <form className="tarjeta form" onSubmit={guardar}>
          <h2>{f.id ? 'Editar sala' : 'Nueva sala'}</h2>
          <label>Nombre<input value={f.nombre} onChange={set('nombre')} required /></label>
          <label>Edificio<input value={f.edificio} onChange={set('edificio')} required /></label>
          <div className="fila2">
            <label>Facultad
              <select value={f.facultad ?? ''} onChange={set('facultad')}>
                <option value="">Uso general</option>
                {FACULTADES.map((x) => <option key={x}>{x}</option>)}
              </select>
            </label>
            <label>Aforo<input type="number" min="1" value={f.capacidad} onChange={set('capacidad')} required /></label>
          </div>
          <label>Equipamiento<input value={f.equipamiento ?? ''} onChange={set('equipamiento')} /></label>
          <label className="check-linea"><input type="checkbox" checked={f.activa} onChange={set('activa')} /> Activa</label>
          <Aviso tipo={msg?.tipo}>{msg?.texto}</Aviso>
          <div className="form-acciones">
            {f.id && <button type="button" className="btn btn-sec" onClick={() => setF(NUEVA)}>Cancelar</button>}
            <button className="btn btn-pri">Guardar</button>
          </div>
        </form>
        <div>
          <Aviso>{error}</Aviso>
          {cargando && !datos ? <Cargando /> : (
            <div className="tabla-cont">
              <table className="tabla">
                <thead><tr><th>Sala</th><th>Facultad</th><th className="num">Aforo</th><th>Estado</th><th /></tr></thead>
                <tbody>
                  {datos?.map((s) => (
                    <tr key={s.id}>
                      <td>{s.nombre}<div className="apagado">{s.edificio}{s.equipamiento && ` · ${s.equipamiento}`}</div></td>
                      <td>{s.facultad ?? 'General'}</td>
                      <td className="num">{s.capacidad}</td>
                      <td>{s.activa ? <span className="ok">Activa</span> : <span className="apagado">Inactiva</span>}</td>
                      <td><div className="acciones">
                        <button className="btn btn-chico btn-sec" onClick={() => setF(s)}>Editar</button>
                        <button className="btn btn-chico btn-peligro" onClick={() => eliminar(s)}>Eliminar</button>
                      </div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
