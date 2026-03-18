import { useRef, useState } from 'react'
import styles from './UploadZone.module.css'

export default function UploadZone({ onFileSelected, disabled }) {
  const inputRef = useRef(null)
  const [dragging, setDragging] = useState(false)

  function handleFiles(files) {
    if (!files || files.length === 0) return
    const file = files[0]
    if (!file.name.toLowerCase().endsWith('.xml')) {
      alert('Selecione um arquivo .xml exportado da Plataforma Lattes.')
      return
    }
    onFileSelected(file)
  }

  function onDrop(e) {
    e.preventDefault()
    setDragging(false)
    handleFiles(e.dataTransfer.files)
  }

  return (
    <div
      className={`${styles.zone} ${dragging ? styles.dragging : ''} ${disabled ? styles.disabled : ''}`}
      onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      onClick={() => !disabled && inputRef.current?.click()}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && !disabled && inputRef.current?.click()}
      aria-label="Área para upload do arquivo XML Lattes"
    >
      <input
        ref={inputRef}
        type="file"
        accept=".xml"
        style={{ display: 'none' }}
        onChange={(e) => handleFiles(e.target.files)}
        disabled={disabled}
      />
      <div className={styles.icon}>📄</div>
      <p className={styles.primary}>
        {dragging ? 'Solte o arquivo aqui' : 'Arraste o XML do Lattes aqui'}
      </p>
      <p className={styles.secondary}>ou clique para selecionar o arquivo</p>
      <p className={styles.hint}>Aceita arquivos .xml exportados da Plataforma Lattes (CNPq)</p>
    </div>
  )
}
