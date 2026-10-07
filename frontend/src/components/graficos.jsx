// Paleta y piezas comunes de gráficos (Recharts).
// Paleta categórica validada (orden fijo, nunca cíclico) — ver documentación del proyecto.
export const SERIES = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100', '#e87ba4', '#008300', '#4a3aa7', '#e34948']
export const REFERENCIA = '#b5b3ab'   // barras de referencia (aforo / capacidad)
export const TINTA = { primaria: '#0b0b0b', secundaria: '#52514e', muted: '#898781', grilla: '#e1e0d9', eje: '#c3c2b7' }
export const ESTADO = { bien: '#0ca30c', aviso: '#fab219', serio: '#ec835a', critico: '#d03b3b' }

export const ejeProps = {
  tick: { fill: TINTA.muted, fontSize: 12 },
  axisLine: { stroke: TINTA.eje },
  tickLine: false,
}

export const grillaProps = { stroke: TINTA.grilla, strokeDasharray: '0', vertical: false }

/** Tooltip con tinta neutra y muestra de color por serie. */
export function TooltipCaja({ active, payload, label, formato = (v) => v, etiqueta = (l) => l }) {
  if (!active || !payload?.length) return null
  return (
    <div className="tooltip">
      <div className="tooltip-titulo">{etiqueta(label, payload)}</div>
      {payload.filter((p) => p.value !== null && p.value !== undefined).map((p) => (
        <div key={p.dataKey + p.name} className="tooltip-fila">
          <span className="tooltip-muestra" style={{ background: p.color }} />
          <span>{p.name}</span>
          <b>{formato(p.value, p)}</b>
        </div>
      ))}
    </div>
  )
}

/** Tabla accesible alternativa a un gráfico. */
export function TablaDatos({ columnas, filas }) {
  return (
    <details className="tabla-datos">
      <summary>Ver datos en tabla</summary>
      <div className="tabla-cont">
        <table className="tabla tabla-compacta">
          <thead><tr>{columnas.map((c) => <th key={c.k} className={c.num ? 'num' : ''}>{c.t}</th>)}</tr></thead>
          <tbody>
            {filas.map((f, i) => (
              <tr key={i}>{columnas.map((c) => <td key={c.k} className={c.num ? 'num' : ''}>{c.f ? c.f(f[c.k], f) : (f[c.k] ?? '—')}</td>)}</tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  )
}
