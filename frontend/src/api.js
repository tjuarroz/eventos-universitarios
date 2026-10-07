// Cliente REST: core Java (/api) y microservicio Python de analítica (/analitica/api/v1)
const CORE = import.meta.env.VITE_API_URL ?? ''
const ANALITICA = import.meta.env.VITE_ANALITICA_URL ?? '/analitica'
const CLAVE = 'eventos.sesion'

export const sesion = {
  leer() {
    try { return JSON.parse(localStorage.getItem(CLAVE)) } catch { return null }
  },
  guardar(s) {
    try { localStorage.setItem(CLAVE, JSON.stringify(s)) } catch { /* sin storage */ }
  },
  borrar() {
    try { localStorage.removeItem(CLAVE) } catch { /* sin storage */ }
  },
}

export class ApiError extends Error {
  constructor(estado, mensaje, campos) {
    super(mensaje)
    this.estado = estado
    this.campos = campos
  }
}

async function pedir(base, ruta, { metodo = 'GET', cuerpo, params } = {}) {
  const url = new URL(base + ruta, window.location.origin)
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v)
    })
  }
  const headers = { Accept: 'application/json' }
  const token = sesion.leer()?.token
  if (token) headers.Authorization = `Bearer ${token}`
  if (cuerpo !== undefined) headers['Content-Type'] = 'application/json'

  let res
  try {
    res = await fetch(url, { method: metodo, headers, body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor')
  }
  if (res.status === 401 && token) {
    sesion.borrar()
    window.dispatchEvent(new Event('sesion-expirada'))
  }
  if (res.status === 204) return null
  const texto = await res.text()
  const datos = texto ? JSON.parse(texto) : null
  if (!res.ok) {
    const msg = datos?.mensaje || datos?.detail || `Error ${res.status}`
    throw new ApiError(res.status, typeof msg === 'string' ? msg : 'Datos inválidos', datos?.campos)
  }
  return datos
}

export const api = {
  get: (r, params) => pedir(CORE, r, { params }),
  post: (r, cuerpo) => pedir(CORE, r, { metodo: 'POST', cuerpo: cuerpo ?? {} }),
  put: (r, cuerpo) => pedir(CORE, r, { metodo: 'PUT', cuerpo }),
  del: (r) => pedir(CORE, r, { metodo: 'DELETE' }),
}

export const analitica = {
  get: (r, params) => pedir(ANALITICA, '/api/v1' + r, { params }),
}
