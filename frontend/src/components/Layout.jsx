import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { ROLES } from '../util.js'

/** Cantidad de encuestas de satisfacción pendientes; se actualiza al responder una. */
function usePendientes() {
  const [n, setN] = useState(0)
  const cargar = useCallback(() => {
    api.get('/api/encuestas/pendientes').then((l) => setN(l?.length ?? 0)).catch(() => setN(0))
  }, [])
  useEffect(() => {
    cargar()
    window.addEventListener('encuestas-cambiaron', cargar)
    return () => window.removeEventListener('encuestas-cambiaron', cargar)
  }, [cargar])
  return n
}

export default function Layout() {
  const { usuario, logout, esStaff, esOrganizador, esAdmin } = useAuth()
  const [abierto, setAbierto] = useState(false)
  const [avisoCerrado, setAvisoCerrado] = useState(false)
  const pendientes = usePendientes()
  const nav = useNavigate()
  const loc = useLocation()
  const cerrar = () => setAbierto(false)

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-in">
          <NavLink to="/eventos" className="marca" onClick={cerrar}>
            <img src="/favicon.svg" alt="" width="28" height="28" />
            <span>Eventos<b>Uni</b></span>
          </NavLink>
          <button className="hamburguesa" onClick={() => setAbierto(!abierto)} aria-label="Menú">☰</button>
          <nav className={`menu ${abierto ? 'abierto' : ''}`} onClick={cerrar}>
            <NavLink to="/eventos">Eventos</NavLink>
            <NavLink to="/mis-inscripciones">Mis inscripciones</NavLink>
            <NavLink to="/mis-encuestas">
              Encuestas{pendientes > 0 && <span className="menu-badge" aria-label={`${pendientes} pendientes`}>{pendientes}</span>}
            </NavLink>
            {esStaff && <NavLink to="/organizador">Panel</NavLink>}
            {esStaff && <NavLink to="/dashboard">Dashboard predictivo</NavLink>}
            {esAdmin && <NavLink to="/admin/salas">Salas</NavLink>}
            {esAdmin && <NavLink to="/admin/usuarios">Usuarios</NavLink>}
          </nav>
          <div className="usuario">
            <div className="usuario-datos">
              <span className="usuario-nombre">{usuario.nombre} {usuario.apellido}</span>
              <span className="usuario-rol">{ROLES[usuario.rol]}</span>
            </div>
            <button className="btn btn-sec btn-chico" onClick={() => { logout(); nav('/login') }}>Salir</button>
          </div>
        </div>
      </header>
      <main className="contenido">
        {pendientes > 0 && !avisoCerrado && loc.pathname !== '/mis-encuestas' && (
          <div className="aviso aviso-info" role="status">
            <span>
              {pendientes === 1 ? 'Tenés 1 encuesta de satisfacción pendiente' : `Tenés ${pendientes} encuestas de satisfacción pendientes`}
              {' '}de eventos a los que asististe. <Link to="/mis-encuestas"><b>Responder ahora</b></Link>
            </span>
            <button className="aviso-x" onClick={() => setAvisoCerrado(true)} aria-label="Cerrar">×</button>
          </div>
        )}
        <Outlet />
      </main>
      <footer className="pie">Sistema de gestión de eventos universitarios con predicción de asistencia · {esOrganizador ? 'Modo organizador' : 'Proyecto integrador'}</footer>
    </div>
  )
}
