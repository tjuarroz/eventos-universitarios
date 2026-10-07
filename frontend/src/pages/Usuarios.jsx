import { useState } from 'react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { Aviso, Cargando, useCarga } from '../components/comunes.jsx'
import { CATEGORIAS, ROLES } from '../util.js'

export default function Usuarios() {
  const { usuario } = useAuth()
  const [q, setQ] = useState('')
  const [pagina, setPagina] = useState(0)
  const { datos, error, cargando, setDatos } = useCarga(() => api.get('/api/usuarios', { q, pagina, tamanio: 15 }), [q, pagina])
  const [msg, setMsg] = useState(null)

  const actualizar = async (u, ruta, cuerpo) => {
    setMsg(null)
    try {
      const nuevo = await api.put(`/api/usuarios/${u.id}/${ruta}`, cuerpo)
      setDatos({ ...datos, content: datos.content.map((x) => (x.id === u.id ? nuevo : x)) })
      setMsg({ tipo: 'ok', texto: `${nuevo.nombre} ${nuevo.apellido} actualizado` })
    } catch (e) {
      setMsg({ tipo: 'error', texto: e.message })
    }
  }

  return (
    <>
      <div className="cabecera"><div><h1>Usuarios y roles</h1><p className="sub">Asigná quién organiza eventos y quién registra asistencia.</p></div></div>
      <section className="filtros">
        <input className="buscador" placeholder="Buscar por nombre o email…" value={q} onChange={(e) => { setQ(e.target.value); setPagina(0) }} />
      </section>
      <Aviso tipo={msg?.tipo} onCerrar={() => setMsg(null)}>{msg?.texto}</Aviso>
      <Aviso>{error}</Aviso>
      {cargando && !datos ? <Cargando /> : datos && (
        <>
          <div className="tabla-cont">
            <table className="tabla">
              <thead><tr><th>Usuario</th><th>Categoría</th><th>Facultad</th><th>Rol</th><th>Activo</th></tr></thead>
              <tbody>
                {datos.content.map((u) => (
                  <tr key={u.id}>
                    <td>{u.apellido}, {u.nombre}<div className="apagado">{u.email}</div></td>
                    <td>{CATEGORIAS[u.categoria]}</td>
                    <td>{u.facultad ?? '—'}</td>
                    <td>
                      <select value={u.rol} disabled={u.id === usuario.id} onChange={(e) => actualizar(u, 'rol', { rol: e.target.value })}>
                        {Object.entries(ROLES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                      </select>
                    </td>
                    <td><input type="checkbox" className="check" checked={u.activo} disabled={u.id === usuario.id} onChange={(e) => actualizar(u, 'activo', { activo: e.target.checked })} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="paginador">
            <button className="btn btn-sec btn-chico" disabled={datos.first} onClick={() => setPagina(pagina - 1)}>← Anterior</button>
            <span>Página {datos.number + 1} de {Math.max(1, datos.totalPages)} · {datos.totalElements} usuarios</span>
            <button className="btn btn-sec btn-chico" disabled={datos.last} onClick={() => setPagina(pagina + 1)}>Siguiente →</button>
          </div>
        </>
      )}
    </>
  )
}
