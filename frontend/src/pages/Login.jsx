import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import { Aviso } from '../components/comunes.jsx'

const DEMO = [
  ['Organizador', 'organizador@uni.edu.ar'],
  ['Estudiante', 'estudiante@uni.edu.ar'],
  ['Apoyo', 'apoyo@uni.edu.ar'],
  ['Admin', 'admin@uni.edu.ar'],
]

export default function Login() {
  const { login } = useAuth()
  const nav = useNavigate()
  const loc = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  const enviar = async (e) => {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await login(email, password)
      nav(loc.state?.desde || '/eventos', { replace: true })
    } catch (err) {
      setError(err.estado === 401 ? 'Email o contraseña incorrectos' : err.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="acceso">
      <div className="acceso-panel">
        <div className="acceso-marca">
          <img src="/favicon.svg" alt="" width="44" height="44" />
          <div>
            <h1>Eventos<b>Uni</b></h1>
            <p>Gestión de eventos universitarios con predicción de asistencia</p>
          </div>
        </div>
        <form onSubmit={enviar} className="form" noValidate>
          <label>Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required autoFocus />
          </label>
          <label>Contraseña
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
          </label>
          <Aviso>{error}</Aviso>
          <button className="btn btn-pri btn-bloque" disabled={enviando || !email || !password}>
            {enviando ? 'Ingresando…' : 'Iniciar sesión'}
          </button>
        </form>
        <p className="acceso-alt">¿No tenés cuenta? <Link to="/registro">Registrate</Link></p>
        <div className="demo">
          <span>Usuarios de prueba (contraseña <code>demo1234</code>):</span>
          <div className="demo-botones">
            {DEMO.map(([r, m]) => (
              <button key={m} type="button" className="chip" onClick={() => { setEmail(m); setPassword('demo1234') }}>{r}</button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
