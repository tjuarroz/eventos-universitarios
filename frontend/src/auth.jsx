import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, sesion } from './api.js'

const Ctx = createContext(null)

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => sesion.leer()?.usuario ?? null)

  useEffect(() => {
    const salir = () => setUsuario(null)
    window.addEventListener('sesion-expirada', salir)
    return () => window.removeEventListener('sesion-expirada', salir)
  }, [])

  const entrar = useCallback((resp) => {
    sesion.guardar(resp)
    setUsuario(resp.usuario)
  }, [])

  const login = useCallback(async (email, password) => {
    entrar(await api.post('/api/auth/login', { email, password }))
  }, [entrar])

  const registrar = useCallback(async (datos) => {
    entrar(await api.post('/api/auth/registro', datos))
  }, [entrar])

  const logout = useCallback(() => {
    sesion.borrar()
    setUsuario(null)
  }, [])

  const valor = useMemo(() => {
    const rol = usuario?.rol
    return {
      usuario, login, registrar, logout,
      esAdmin: rol === 'ADMIN',
      esOrganizador: rol === 'ORGANIZADOR' || rol === 'ADMIN',
      esStaff: ['ADMIN', 'ORGANIZADOR', 'APOYO'].includes(rol),
    }
  }, [usuario, login, registrar, logout])

  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>
}

export const useAuth = () => useContext(Ctx)
