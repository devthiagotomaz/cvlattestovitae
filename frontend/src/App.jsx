import { useState, useCallback } from 'react'
import UploadZone from './components/UploadZone'
import CurriculoPreview from './components/CurriculoPreview'
import { parseCurriculo, convertAndDownload, convertAndDownloadWord } from './api/curriculoApi'
import './App.css'

const STATUS = { IDLE: 'idle', LOADING: 'loading', PREVIEW: 'preview', ERROR: 'error' }

export default function App() {
  const [status, setStatus] = useState(STATUS.IDLE)
  const [curriculo, setCurriculo] = useState(null)
  const [selectedFile, setSelectedFile] = useState(null)
  const [errorMsg, setErrorMsg] = useState('')
  const [downloading, setDownloading] = useState(false)
  const [downloadingWord, setDownloadingWord] = useState(false)

  const handleFileSelected = useCallback(async (file) => {
    setSelectedFile(file)
    setStatus(STATUS.LOADING)
    setErrorMsg('')
    try {
      const data = await parseCurriculo(file)
      setCurriculo(data)
      setStatus(STATUS.PREVIEW)
    } catch (err) {
      const msg = err.response?.status === 400
        ? 'Arquivo inválido. Certifique-se de que é um XML exportado da Plataforma Lattes.'
        : 'Erro ao processar o arquivo. Verifique se o servidor backend está rodando em localhost:8080.'
      setErrorMsg(msg)
      setStatus(STATUS.ERROR)
    }
  }, [])

  const handleDownload = useCallback(async () => {
    if (!selectedFile) return
    setDownloading(true)
    try {
      const fileName = curriculo?.nomeCompleto
        ? `${curriculo.nomeCompleto.replace(/\s+/g, '_')}_curriculo.pdf`
        : 'curriculo.pdf'
      await convertAndDownload(selectedFile, fileName)
    } catch {
      alert('Erro ao gerar o PDF. Tente novamente.')
    } finally {
      setDownloading(false)
    }
  }, [selectedFile, curriculo])

  const handleDownloadWord = useCallback(async () => {
    if (!selectedFile) return
    setDownloadingWord(true)
    try {
      const fileName = curriculo?.nomeCompleto
        ? `${curriculo.nomeCompleto.replace(/\s+/g, '_')}_curriculo.docx`
        : 'curriculo.docx'
      await convertAndDownloadWord(selectedFile, fileName)
    } catch {
      alert('Erro ao gerar o Word. Tente novamente.')
    } finally {
      setDownloadingWord(false)
    }
  }, [selectedFile, curriculo])

  const handleReset = () => {
    setStatus(STATUS.IDLE)
    setCurriculo(null)
    setSelectedFile(null)
    setErrorMsg('')
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header-inner">
          <div className="app-logo">📋</div>
          <div>
            <h1 className="app-title">CV Lattes → Currículo Vitae</h1>
            <p className="app-subtitle">
              Converta seu currículo Lattes (CNPq) em um PDF profissional
            </p>
          </div>
        </div>
      </header>

      <main className="app-main">
        {/* ---- UPLOAD ---- */}
        {status === STATUS.IDLE && (
          <div className="upload-wrapper">
            <UploadZone onFileSelected={handleFileSelected} disabled={false} />
          </div>
        )}

        {/* ---- LOADING ---- */}
        {status === STATUS.LOADING && (
          <div className="center-message">
            <div className="spinner" aria-label="Carregando" />
            <p>Processando o arquivo XML...</p>
          </div>
        )}

        {/* ---- ERROR ---- */}
        {status === STATUS.ERROR && (
          <div className="error-box">
            <p className="error-text">⚠️ {errorMsg}</p>
            <button className="btn btn-secondary" onClick={handleReset}>
              Tentar novamente
            </button>
          </div>
        )}

        {/* ---- PREVIEW ---- */}
        {status === STATUS.PREVIEW && curriculo && (
          <>
            <div className="preview-toolbar">
              <div className="preview-file-info">
                <span className="file-icon">📄</span>
                <span className="file-name">{selectedFile?.name}</span>
              </div>
              <div className="preview-actions">
                <button className="btn btn-secondary" onClick={handleReset}>
                  ← Novo arquivo
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleDownload}
                  disabled={downloading}
                >
                  {downloading ? 'Gerando PDF...' : '⬇ Baixar PDF'}
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={handleDownloadWord}
                  disabled={downloadingWord}
                >
                  {downloadingWord ? 'Gerando Word...' : '📝 Baixar Word'}
                </button>
              </div>
            </div>

            <CurriculoPreview curriculo={curriculo} />

            <div className="preview-footer-actions">
              <button
                className="btn btn-primary btn-lg"
                onClick={handleDownload}
                disabled={downloading}
              >
                {downloading ? 'Gerando PDF...' : '⬇ Baixar Currículo em PDF'}
              </button>
              <button
                className="btn btn-secondary btn-lg"
                onClick={handleDownloadWord}
                disabled={downloadingWord}
              >
                {downloadingWord ? 'Gerando Word...' : '📝 Baixar Currículo em Word'}
              </button>
            </div>
          </>
        )}
      </main>

      <footer className="app-footer">
        Gerado a partir da Plataforma Lattes — CNPq
      </footer>
    </div>
  )
}
