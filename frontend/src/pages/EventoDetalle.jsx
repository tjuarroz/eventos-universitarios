import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Aviso, BarraCupo, Cargando, EstadoBadge, useCarga } from '../components/comunes.jsx'
import { CLIMAS, TIPOS, fecha, hora } from '../util.js'

export default function EventoDetalle() {
  const { id } = useParams()
  const { usuario, esStaff } = useAuth()
  const { datos: ev, error, cargando, recargar } = useCarga(() => api.get(`/api/eventos/${id}`), [id])
  const [msg, setMsg] = useState(null)
  const [trabajando, setTrabajando] = useState(false)

  if (cargando && !ev) return <Cargando />
  if (error) return <Aviso>{error}</Aviso>

  const accion = async (fn, ok) => {
    setTrabajando(true)
    setMsg(null)
    try {
      await fn()
      await recargar()
      setMsg({ tipo: 'ok', texto: ok })
    } catch (e) {
      setMsg({ tipo: 'error', texto: e.message })
    } finally {
      setTrabajando(false)
    }
  }
  const lleno = ev.inscriptos >= ev.capacidad
  const esDueno = usuario.id === ev.organizadorId || usuario.rol === 'ADMIN'

  return (
    <>
      <Link to="/eventos" className="volver">← Volver a eventos</Link>
      <div className="detalle">
        <section className="detalle-principal">
          <div className="tarjeta-top">
            <span className={`tipo tipo-${ev.tipo.toLowerCase()}`}>{TIPOS[ev.tipo]}</span>
            <EstadoBadge estado={ev.estado} />
          </div>
          <h1>{ev.titulo}</h1>
          <p className="sub">Organiza: {ev.organizadorNombre} · Facultad de {ev.facultad}</p>
          <h2>Descripción</h2>
          <p className="texto">{ev.descripcion || 'Sin descripción.'}</p>
          <h2>Materiales</h2>
          <p className="texto">{ev.materiales || 'El organizador todavía no cargó materiales.'}</p>
        </section>

        <aside className="detalle-lateral">
          <dl className="datos">
            <dt>Fecha</dt><dd>{fecha(ev.fecha)}</dd>
            <dt>Horario</dt><dd>{hora(ev.horaInicio)} a {hora(ev.horaFin)}</dd>
            <dt>Sala</dt><dd>{ev.salaNombre ? `${ev.salaNombre} (${ev.salaEdificio})` : 'A confirmar'}</dd>
            <dt>Aforo de sala</dt><dd>{ev.salaCapacidad ?? '—'}</dd>
            <dt>Pronóstico</dt><dd>{CLIMAS[ev.clima]}</dd>
          </dl>
          <BarraCupo ocupados={ev.inscriptos} total={ev.capacidad} />
          <p className="nota">Cupo de inscripción: {ev.capacidad} · Quedan {Math.max(0, ev.capacidad - ev.inscriptos)} lugares</p>

          {ev.inscripto ? (
            <div className="qr-caja">
              <p><b>Ya estás inscripto.</b> Mostrá este QR al ingresar:</p>
              <QRCodeSVG value={ev.codigoInscripcion} size={150} />
              <code className="codigo">{ev.codigoInscripcion}</code>
              {ev.estado === 'PUBLICADO' && (
                <button className="btn btn-link" disabled={trabajando}
                        onClick={() => accion(() => api.del(`/api/eventos/${id}/inscripcion`), 'Cancelaste tu inscripción')}>
                  Cancelar inscripción
                </button>
              )}
            </div>
          ) : ev.estado === 'PUBLICADO' ? (
            <button className="btn btn-pri btn-bloque" disabled={lleno || trabajando}
                    onClick={() => accion(() => api.post(`/api/eventos/${id}/inscripcion`), '¡Inscripción confirmada!')}>
              {lleno ? 'Sin cupo disponible' : 'Inscribirme'}
            </button>
          ) : null}

          {esStaff && (
            <div className="acciones-staff">
              {esDueno && ['BORRADOR', 'PUBLICADO'].includes(ev.estado) && <Link className="btn btn-sec" to={`/organizador/eventos/${id}/editar`}>Editar</Link>}
              <Link className="btn btn-sec" to={`/organizador/eventos/${id}/asistencia`}>Asistencia</Link>
              <Link className="btn btn-sec" to={`/organizador/eventos/${id}/prediccion`}>Predicción</Link>
            </div>
          )}
          <Aviso tipo={msg?.tipo}>{msg?.texto}</Aviso>
        </aside>
      </div>
    </>
  )
}
