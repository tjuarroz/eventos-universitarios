import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { Aviso, Cargando, useCarga, Vacio } from '../components/comunes.jsx'
import { PUNTAJES, TIPOS, fecha, hora } from '../util.js'

/** Formulario de una encuesta: puntaje 1-5, si lo recomendaría y un comentario opcional. */
function FormEncuesta({ p, onEnviada }) {
  const [puntos, setPuntos] = useState(null)
  const [recomendaria, setRecomendaria] = useState(null)
  const [comentario, setComentario] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)

  const enviar = async (ev) => {
    ev.preventDefault()
    if (!puntos || recomendaria === null) {
      setError('Elegí un puntaje y si lo recomendarías.')
      return
    }
    setEnviando(true)
    setError(null)
    try {
      await api.post('/api/encuestas', { inscripcionId: p.inscripcionId, puntaje: puntos, recomendaria, comentario })
      onEnviada(p)
    } catch (e) {
      setError(e.message)
      setEnviando(false)
    }
  }

  return (
    <article className="tarjeta">
      <div className="tarjeta-top">
        <span className={`tipo tipo-${p.tipo.toLowerCase()}`}>{TIPOS[p.tipo]}</span>
        <span className="facultad">{p.facultad}</span>
      </div>
      <h3>{p.titulo}</h3>
      <div className="meta">
        <span>📅 {fecha(p.fecha)}</span><span>🕒 {hora(p.horaInicio)}</span>
        {p.salaNombre && <span>📍 {p.salaNombre}</span>}<span>👤 {p.organizadorNombre}</span>
      </div>
      <form className="encuesta" onSubmit={enviar}>
        <div>
          <p className="encuesta-pregunta">¿Cómo calificarías el evento?</p>
          <div className="estrellas" role="radiogroup" aria-label="Puntaje">
            {[1, 2, 3, 4, 5].map((n) => (
              <button type="button" key={n} role="radio" aria-checked={puntos === n}
                      className={`estrella ${puntos === n ? 'activa' : ''}`} onClick={() => setPuntos(n)}>
                <b>{n} ★</b><span>{PUNTAJES[n]}</span>
              </button>
            ))}
          </div>
        </div>
        <div>
          <p className="encuesta-pregunta">¿Se lo recomendarías a un compañero?</p>
          <div className="opciones-si-no" role="radiogroup" aria-label="Recomendaría">
            <button type="button" role="radio" aria-checked={recomendaria === true}
                    className={`chip ${recomendaria === true ? 'activa' : ''}`} onClick={() => setRecomendaria(true)}>Sí</button>
            <button type="button" role="radio" aria-checked={recomendaria === false}
                    className={`chip ${recomendaria === false ? 'activa' : ''}`} onClick={() => setRecomendaria(false)}>No</button>
          </div>
        </div>
        <label>
          <span className="encuesta-pregunta">Comentario (opcional)</span>
          <textarea maxLength={500} value={comentario} onChange={(e) => setComentario(e.target.value)}
                    placeholder="¿Qué te gustó? ¿Qué mejorarías?" />
        </label>
        <Aviso onCerrar={() => setError(null)}>{error}</Aviso>
        <div className="tarjeta-acciones">
          <span className="nota">Disponible hasta el {fecha(p.cierra)}</span>
          <button className="btn btn-pri" disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar encuesta'}</button>
        </div>
      </form>
    </article>
  )
}

export default function MisEncuestas() {
  const { datos, error, cargando, setDatos } = useCarga(() => api.get('/api/encuestas/pendientes'), [])
  const [ok, setOk] = useState(null)
  if (cargando && !datos) return <Cargando />

  const enviada = (p) => {
    setDatos((l) => l.filter((x) => x.inscripcionId !== p.inscripcionId))
    setOk(`¡Gracias! Registramos tu opinión sobre "${p.titulo}".`)
    window.dispatchEvent(new Event('encuestas-cambiaron'))
  }

  return (
    <>
      <div className="cabecera">
        <div>
          <h1>Encuestas de satisfacción</h1>
          <p className="sub">Cuando un evento al que asististe finaliza, te pedimos tu opinión. Es anónima para el organizador
            y ayuda a predecir la asistencia de los próximos eventos.</p>
        </div>
      </div>
      <Aviso>{error}</Aviso>
      <Aviso tipo="ok" onCerrar={() => setOk(null)}>{ok}</Aviso>
      {datos && datos.length === 0 ? (
        <Vacio>No tenés encuestas pendientes. <Link to="/mis-inscripciones">Ver mis inscripciones</Link></Vacio>
      ) : (
        <div className="grilla grilla-ancha">
          {(datos || []).map((p) => <FormEncuesta key={p.inscripcionId} p={p} onEnviada={enviada} />)}
        </div>
      )}
    </>
  )
}
