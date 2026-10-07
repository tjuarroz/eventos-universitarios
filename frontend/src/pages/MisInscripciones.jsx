import { useState } from 'react'
import { Link } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import { api } from '../api.js'
import { Aviso, Cargando, EstadoBadge, useCarga, Vacio } from '../components/comunes.jsx'
import { TIPOS, fecha, hora, hoyIso } from '../util.js'

export default function MisInscripciones() {
  const { datos, error, cargando } = useCarga(() => api.get('/api/inscripciones/mias'), [])
  const [qr, setQr] = useState(null)
  if (cargando && !datos) return <Cargando />

  const activas = (datos || []).filter((i) => i.estado === 'ACTIVA')
  const proximas = activas.filter((i) => i.fecha >= hoyIso() && i.estadoEvento === 'PUBLICADO').reverse()
  const pasadas = activas.filter((i) => !proximas.includes(i))

  return (
    <>
      <div className="cabecera"><h1>Mis inscripciones</h1></div>
      <Aviso>{error}</Aviso>
      <h2>Próximos</h2>
      {proximas.length === 0 ? <Vacio>No tenés inscripciones a eventos próximos. <Link to="/eventos">Ver eventos</Link></Vacio> : (
        <div className="grilla">
          {proximas.map((i) => (
            <article key={i.id} className="tarjeta">
              <div className="tarjeta-top"><span className={`tipo tipo-${i.tipo.toLowerCase()}`}>{TIPOS[i.tipo]}</span><span className="facultad">{i.facultad}</span></div>
              <h3><Link to={`/eventos/${i.eventoId}`}>{i.eventoTitulo}</Link></h3>
              <div className="meta"><span>📅 {fecha(i.fecha)}</span><span>🕒 {hora(i.horaInicio)}</span>{i.salaNombre && <span>📍 {i.salaNombre}</span>}</div>
              <div className="tarjeta-acciones">
                <code className="codigo">{i.codigo}</code>
                <button className="btn btn-pri" onClick={() => setQr(i)}>Mostrar QR</button>
              </div>
            </article>
          ))}
        </div>
      )}
      <h2>Historial</h2>
      {pasadas.length === 0 ? <Vacio>Todavía no participaste de eventos.</Vacio> : (
        <div className="tabla-cont">
          <table className="tabla">
            <thead><tr><th>Evento</th><th>Fecha</th><th>Tipo</th><th>Estado</th><th>Asistencia</th></tr></thead>
            <tbody>
              {pasadas.map((i) => (
                <tr key={i.id}>
                  <td>{i.eventoTitulo}</td><td>{fecha(i.fecha)}</td><td>{TIPOS[i.tipo]}</td>
                  <td><EstadoBadge estado={i.estadoEvento} /></td>
                  <td>{i.asistio ? <span className="ok">✓ Asististe</span> : <span className="apagado">Sin registro</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {qr && (
        <div className="modal-fondo" onClick={() => setQr(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} role="dialog" aria-label="Código QR">
            <h3>{qr.eventoTitulo}</h3>
            <p className="sub">{fecha(qr.fecha)} · {hora(qr.horaInicio)}</p>
            <QRCodeSVG value={qr.codigo} size={220} />
            <code className="codigo grande">{qr.codigo}</code>
            <button className="btn btn-sec" onClick={() => setQr(null)}>Cerrar</button>
          </div>
        </div>
      )}
    </>
  )
}
