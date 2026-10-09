import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Registro from './pages/Registro.jsx'
import Eventos from './pages/Eventos.jsx'
import EventoDetalle from './pages/EventoDetalle.jsx'
import MisInscripciones from './pages/MisInscripciones.jsx'
import MisEncuestas from './pages/MisEncuestas.jsx'
import PanelOrganizador from './pages/PanelOrganizador.jsx'
import EventoForm from './pages/EventoForm.jsx'
import Asistencia from './pages/Asistencia.jsx'
import Estadisticas from './pages/Estadisticas.jsx'
import PrediccionEvento from './pages/PrediccionEvento.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Salas from './pages/Salas.jsx'
import Usuarios from './pages/Usuarios.jsx'

function Protegida({ children, requiere }) {
  const auth = useAuth()
  const loc = useLocation()
  if (!auth.usuario) return <Navigate to="/login" replace state={{ desde: loc.pathname }} />
  if (requiere && !auth[requiere]) return <Navigate to="/eventos" replace />
  return children
}

export default function App() {
  const { usuario } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={usuario ? <Navigate to="/eventos" replace /> : <Login />} />
      <Route path="/registro" element={usuario ? <Navigate to="/eventos" replace /> : <Registro />} />
      <Route element={<Protegida><Layout /></Protegida>}>
        <Route path="/eventos" element={<Eventos />} />
        <Route path="/eventos/:id" element={<EventoDetalle />} />
        <Route path="/mis-inscripciones" element={<MisInscripciones />} />
        <Route path="/mis-encuestas" element={<MisEncuestas />} />
        <Route path="/organizador" element={<Protegida requiere="esStaff"><PanelOrganizador /></Protegida>} />
        <Route path="/organizador/eventos/nuevo" element={<Protegida requiere="esOrganizador"><EventoForm /></Protegida>} />
        <Route path="/organizador/eventos/:id/editar" element={<Protegida requiere="esOrganizador"><EventoForm /></Protegida>} />
        <Route path="/organizador/eventos/:id/asistencia" element={<Protegida requiere="esStaff"><Asistencia /></Protegida>} />
        <Route path="/organizador/eventos/:id/estadisticas" element={<Protegida requiere="esStaff"><Estadisticas /></Protegida>} />
        <Route path="/organizador/eventos/:id/prediccion" element={<Protegida requiere="esStaff"><PrediccionEvento /></Protegida>} />
        <Route path="/dashboard" element={<Protegida requiere="esStaff"><Dashboard /></Protegida>} />
        <Route path="/admin/salas" element={<Protegida requiere="esAdmin"><Salas /></Protegida>} />
        <Route path="/admin/usuarios" element={<Protegida requiere="esAdmin"><Usuarios /></Protegida>} />
      </Route>
      <Route path="*" element={<Navigate to={usuario ? '/eventos' : '/login'} replace />} />
    </Routes>
  )
}
