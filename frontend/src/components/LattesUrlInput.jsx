import { useState, useCallback } from 'react'
import styles from './LattesUrlInput.module.css'

const LATTES_DOMAIN = 'lattes.cnpq.br'

function isValidLattesUrl(url) {
  try {
    const parsed = new URL(url.trim())
    return parsed.hostname.endsWith(LATTES_DOMAIN)
  } catch {
    return false
  }
}

/**
 * Input component for entering a public Lattes profile URL.
 * Calls `onUrlSubmit(url)` when the user clicks the search button.
 */
export default function LattesUrlInput({ onUrlSubmit, disabled }) {
  const [url, setUrl] = useState('')
  const [validationError, setValidationError] = useState('')

  const handleChange = useCallback((e) => {
    setUrl(e.target.value)
    if (validationError) setValidationError('')
  }, [validationError])

  const handleSubmit = useCallback((e) => {
    e.preventDefault()
    const trimmed = url.trim()
    if (!trimmed) {
      setValidationError('Por favor, insira a URL do currículo Lattes.')
      return
    }
    if (!isValidLattesUrl(trimmed)) {
      setValidationError(
        `URL inválida. A URL deve pertencer ao domínio ${LATTES_DOMAIN} ` +
        '(ex: http://lattes.cnpq.br/1234567890123456).'
      )
      return
    }
    setValidationError('')
    onUrlSubmit(trimmed)
  }, [url, onUrlSubmit])

  return (
    <form className={styles.container} onSubmit={handleSubmit} noValidate>
      <label htmlFor="lattes-url" className={styles.label}>
        🔗 URL do currículo Lattes
      </label>
      <div className={styles.inputRow}>
        <input
          id="lattes-url"
          type="url"
          className={`${styles.input} ${validationError ? styles.inputError : ''}`}
          placeholder="http://lattes.cnpq.br/1234567890123456"
          value={url}
          onChange={handleChange}
          disabled={disabled}
          autoComplete="off"
          spellCheck={false}
        />
        <button
          type="submit"
          className={styles.btn}
          disabled={disabled || !url.trim()}
        >
          Buscar e Converter
        </button>
      </div>
      {validationError && (
        <p className={styles.errorText} role="alert">
          ⚠️ {validationError}
        </p>
      )}
      <p className={styles.hint}>
        Cole a URL pública do seu perfil Lattes (CNPq). O perfil deve estar
        configurado como público para que a busca funcione.
      </p>
    </form>
  )
}
