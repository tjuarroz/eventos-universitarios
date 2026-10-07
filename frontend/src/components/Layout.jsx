import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../auth.jsx'
import { ROLES } from '../util.js'

export default function Layout() {
  const { usuario, logout, esStaff, esOrganizador, esAdmin } = useAuth()
  const [abierto, setAbierto] = useState(false)
  const nav = useNavigate()
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
        <Outlet />
      </main>
      <footer className="pie">Sistema de gestión de eventos universitarios con predicción de asistencia · {esOrganizador ? 'Modo organizador' : 'Proyecto integrador'}</footer>
    </div>
  )
}
