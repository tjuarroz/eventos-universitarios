import { useEffect, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'

/** Lector de QR con la cámara del dispositivo. Llama a onLeer(texto) por cada código detectado. */
export default function EscanerQR({ onLeer }) {
  const ref = useRef(null)
  const ultimo = useRef({ texto: '', t: 0 })
  const [error, setError] = useState('')

  useEffect(() => {
    const lector = new Html5Qrcode('lector-qr')
    ref.current = lector
    lector.start({ facingMode: 'environment' }, { fps: 10, qrbox: { width: 220, height: 220 } }, (texto) => {
      const ahora = Date.now()
      if (texto === ultimo.current.texto && ahora - ultimo.current.t < 3000) return // evita lecturas repetidas
      ultimo.current = { texto, t: ahora }
      onLeer(texto)
    }).catch(() => setError('No se pudo acceder a la cámara. Verificá los permisos o usá el ingreso por código.'))
    return () => {
      if (lector.isScanning) lector.stop().catch(() => {})
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="escaner">
      <div id="lector-qr" />
      {error && <p className="campo-error">{error}</p>}
    </div>
  )
}
