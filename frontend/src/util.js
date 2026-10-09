export const TIPOS = {
  CHARLA: 'Charla', SEMINARIO: 'Seminario', TALLER: 'Taller', JORNADA: 'Jornada', INSTITUCIONAL: 'Institucional',
}
export const CLIMAS = { SOLEADO: 'Soleado', NUBLADO: 'Nublado', LLUVIA: 'Lluvia' }
export const ESTADOS = { BORRADOR: 'Borrador', PUBLICADO: 'Publicado', FINALIZADO: 'Finalizado', CANCELADO: 'Cancelado' }
export const CATEGORIAS = { ESTUDIANTE: 'Estudiante', DOCENTE: 'Docente', PERSONAL: 'Personal' }
export const METODOS = { LISTA: 'Lista', CODIGO: 'Código', QR: 'QR' }
export const ROLES = { ADMIN: 'Administrador', ORGANIZADOR: 'Organizador', APOYO: 'Personal de apoyo', USUARIO: 'Usuario' }
export const FACULTADES = ['Ingeniería', 'Ciencias Económicas', 'Derecho', 'Ciencias Sociales', 'Ciencias Exactas']

const aFecha = (iso) => {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export const fecha = (iso) =>
  iso ? aFecha(iso).toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }) : '—'

export const fechaCorta = (iso) =>
  iso ? aFecha(iso).toLocaleDateString('es-AR', { day: '2-digit', month: '2-digit', year: '2-digit' }) : '—'

export const hora = (h) => (h ? h.slice(0, 5) : '')

export const pct = (v, dec = 0) => (v === null || v === undefined ? '—' : `${(v * 100).toFixed(dec)}%`)

export const hoyIso = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export const mesCorto = (periodo) => {
  const [y, m] = periodo.split('-').map(Number)
  return new Date(y, m - 1, 1).toLocaleDateString('es-AR', { month: 'short', year: '2-digit' })
}

// Encuesta de satisfacción
export const PUNTAJES = { 1: 'Muy malo', 2: 'Malo', 3: 'Regular', 4: 'Bueno', 5: 'Excelente' }
export const puntaje = (v) => (v === null || v === undefined ? '—' : v.toFixed(1).replace('.', ','))
