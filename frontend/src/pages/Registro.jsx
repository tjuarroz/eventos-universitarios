import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { Aviso } from '../components/comunes.jsx'
import { CATEGORIAS, FACULTADES } from '../util.js'

export default function Registro() {
  const { registrar } = useAuth()
  const nav = useNavigate()
  const [f, setF] = useState({ nombre: '', apellido: '', email: '', password: '', categoria: 'ESTUDIANTE', facultad: FACULTADES[0] })
  const [error, setError] = useState('')
  const [campos, setCampos] = useState({})
  const [enviando, setEnviando] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    setCampos({})
    if (f.password.length < 8) { setCampos({ password: 'Mínimo 8 caracteres' }); return }
    setEnviando(true)
    try {
      await registrar(f)
      nav('/eventos', { replace: true })
    } catch (err) {
      setError(err.message)
      setCampos(err.campos || {})
    } finally {
      setEnviando(false)
    }
  }

  const Err = ({ k }) => (campos[k] ? <small className="campo-error">{campos[k]}</small> : null)

  return (
    <div className="acceso">
      <div className="acceso-panel">
        <h1 className="acceso-titulo">Crear cuenta</h1>
        <form onSubmit={enviar} className="form">
          <div className="fila2">
            <label>Nombre<input value={f.nombre} onChange={set('nombre')} required /><Err k="nombre" /></label>
            <label>Apellido<input value={f.apellido} onChange={set('apellido')} required /><Err k="apellido" /></label>
          </div>
          <label>Email institucional<input type="email" value={f.email} onChange={set('email')} required /><Err k="email" /></label>
          <label>Contraseña<input type="password" value={f.password} onChange={set('password')} required minLength={8} /><Err k="password" /></label>
          <div className="fila2">
            <label>Soy
              <select value={f.categoria} onChange={set('categoria')}>
                {Object.entries(CATEGORIAS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </label>
            <label>Facultad
              <select value={f.facultad} onChange={set('facultad')}>
                {FACULTADES.map((x) => <option key={x}>{x}</option>)}
              </select>
            </label>
          </div>
          <Aviso>{error}</Aviso>
          <button className="btn btn-pri btn-bloque" disabled={enviando}>{enviando ? 'Creando…' : 'Registrarme'}</button>
        </form>
        <p className="acceso-alt">¿Ya tenés cuenta? <Link to="/login">Iniciá sesión</Link></p>
      </div>
    </div>
  )
}
