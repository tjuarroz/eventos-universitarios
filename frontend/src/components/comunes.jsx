import { useCallback, useEffect, useState } from 'react'
import { ESTADOS } from '../util.js'

/** Carga datos asíncronos con estados de carga y error. */
export function useCarga(fn, deps = []) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const recargar = useCallback(async () => {
    setCargando(true)
    setError(null)
    try {
      setDatos(await fn())
    } catch (e) {
      setError(e.message)
    } finally {
      setCargando(false)
    }
  }, deps)
  useEffect(() => { recargar() }, [recargar])
  return { datos, error, cargando, recargar, setDatos }
}

export const Cargando = ({ texto = 'Cargando…' }) => <div className="cargando"><span className="spinner" />{texto}</div>

export const Aviso = ({ tipo = 'error', children, onCerrar }) =>
  children ? (
    <div className={`aviso aviso-${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      <span>{children}</span>
      {onCerrar && <button className="aviso-x" onClick={onCerrar} aria-label="Cerrar">×</button>}
    </div>
  ) : null

export const EstadoBadge = ({ estado }) => <span className={`badge badge-${estado?.toLowerCase()}`}>{ESTADOS[estado] ?? estado}</span>

export const Kpi = ({ titulo, valor, detalle, tono }) => (
  <div className={`kpi ${tono ? 'kpi-' + tono : ''}`}>
    <div className="kpi-titulo">{titulo}</div>
    <div className="kpi-valor">{valor}</div>
    {detalle && <div className="kpi-detalle">{detalle}</div>}
  </div>
)

export const BarraCupo = ({ ocupados, total }) => {
  const p = total ? Math.min(100, (ocupados / total) * 100) : 0
  return (
    <div className="cupo" title={`${ocupados} de ${total}`}>
      <div className="cupo-barra"><div className={`cupo-lleno ${p >= 100 ? 'completo' : p > 80 ? 'alto' : ''}`} style={{ width: `${p}%` }} /></div>
      <span className="cupo-texto">{ocupados}/{total} inscriptos</span>
    </div>
  )
}

export const Vacio = ({ children }) => <div className="vacio">{children}</div>
