import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api.js'
import { Aviso, Cargando } from '../components/comunes.jsx'
import { CLIMAS, FACULTADES, TIPOS, hora } from '../util.js'

const VACIO = { titulo: '', descripcion: '', materiales: '', tipo: 'CHARLA', facultad: FACULTADES[0], fecha: '', horaInicio: '18:00', horaFin: '19:30', salaId: '', capacidad: 50, clima: 'SOLEADO' }

export default function EventoForm() {
  const { id } = useParams()
  const nav = useNavigate()
  const [f, setF] = useState(VACIO)
  const [salas, setSalas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [campos, setCampos] = useState({})
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    (async () => {
      try {
        setSalas(await api.get('/api/salas', { soloActivas: true }))
        if (id) {
          const e = await api.get(`/api/eventos/${id}`)
          setF({ ...e, horaInicio: hora(e.horaInicio), horaFin: hora(e.horaFin), salaId: e.salaId ?? '', descripcion: e.descripcion ?? '', materiales: e.materiales ?? '' })
        }
      } catch (e) {
        setError(e.message)
      } finally {
        setCargando(false)
      }
    })()
  }, [id])

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  const sala = salas.find((s) => String(s.id) === String(f.salaId))

  const guardar = async (publicar) => {
    setError('')
    setCampos({})
    setGuardando(true)
    const cuerpo = { ...f, salaId: f.salaId ? Number(f.salaId) : null, capacidad: Number(f.capacidad) }
    try {
      const ev = id ? await api.put(`/api/eventos/${id}`, cuerpo) : await api.post('/api/eventos', cuerpo)
      if (publicar && ev.estado === 'BORRADOR') await api.post(`/api/eventos/${ev.id}/publicar`)
      nav(`/organizador/eventos/${ev.id}/prediccion`)
    } catch (e) {
      setError(e.message)
      setCampos(e.campos || {})
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <Cargando />
  const Err = ({ k }) => (campos[k] ? <small className="campo-error">{campos[k]}</small> : null)

  return (
    <>
      <Link to="/organizador" className="volver">← Volver al panel</Link>
      <div className="cabecera"><h1>{id ? 'Editar evento' : 'Nuevo evento'}</h1></div>
      <form className="form tarjeta form-ancho" onSubmit={(e) => { e.preventDefault(); guardar(false) }}>
        <label>Título<input value={f.titulo} onChange={set('titulo')} required maxLength={150} /><Err k="titulo" /></label>
        <div className="fila3">
          <label>Tipo de actividad
            <select value={f.tipo} onChange={set('tipo')}>{Object.entries(TIPOS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
          </label>
          <label>Facultad
            <select value={f.facultad} onChange={set('facultad')}>{FACULTADES.map((x) => <option key={x}>{x}</option>)}</select>
          </label>
          <label>Clima pronosticado
            <select value={f.clima} onChange={set('clima')}>{Object.entries(CLIMAS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select>
          </label>
        </div>
        <div className="fila3">
          <label>Fecha<input type="date" value={f.fecha} onChange={set('fecha')} required /><Err k="fecha" /></label>
          <label>Hora de inicio<input type="time" value={f.horaInicio} onChange={set('horaInicio')} required /></label>
          <label>Hora de fin<input type="time" value={f.horaFin} onChange={set('horaFin')} required /></label>
        </div>
        <div className="fila2">
          <label>Sala
            <select value={f.salaId} onChange={set('salaId')}>
              <option value="">A confirmar</option>
              {salas.map((s) => <option key={s.id} value={s.id}>{s.nombre} — {s.edificio} (aforo {s.capacidad})</option>)}
            </select>
          </label>
          <label>Cupo de inscripción<input type="number" min="1" value={f.capacidad} onChange={set('capacidad')} required /><Err k="capacidad" /></label>
        </div>
        {sala && Number(f.capacidad) > sala.capacidad && (
          <Aviso tipo="info">El cupo ({f.capacidad}) supera el aforo de la sala ({sala.capacidad}). Es válido si esperás ausentismo: después de guardar, revisá la predicción para ver el riesgo de sobrecapacidad.</Aviso>
        )}
        <label>Descripción<textarea rows="4" value={f.descripcion} onChange={set('descripcion')} /></label>
        <label>Materiales / descripción ampliada<textarea rows="3" value={f.materiales} onChange={set('materiales')} /></label>
        <Aviso>{error}</Aviso>
        <div className="form-acciones">
          <button type="submit" className="btn btn-sec" disabled={guardando}>Guardar{!id || f.estado === 'BORRADOR' ? ' como borrador' : ''}</button>
          {(!id || f.estado === 'BORRADOR') && <button type="button" className="btn btn-pri" disabled={guardando} onClick={() => guardar(true)}>Guardar y publicar</button>}
        </div>
      </form>
    </>
  )
}
