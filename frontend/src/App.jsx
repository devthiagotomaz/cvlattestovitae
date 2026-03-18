import { useState, useCallback } from "react";
import UploadZone from "./components/UploadZone";
import CurriculoPreview from "./components/CurriculoPreview";
import LattesUrlInput from "./components/LattesUrlInput";
import {
  parseCurriculo,
  convertAndDownload,
  convertAndDownloadWord,
  scrapeLattesUrl,
  scrapeAndDownloadPdf,
  scrapeAndDownloadWord,
} from "./api/curriculoApi";
import "./App.css";

const STATUS = {
  IDLE: "idle",
  LOADING: "loading",
  PREVIEW: "preview",
  ERROR: "error",
  CAPTCHA: "captcha",
};
const MODE = { XML: "xml", URL: "url" };

export default function App() {
  const [mode, setMode] = useState(MODE.XML);
  const [status, setStatus] = useState(STATUS.IDLE);
  const [curriculo, setCurriculo] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [lattesUrl, setLattesUrl] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [captchaUrl, setCaptchaUrl] = useState("");
  const [downloading, setDownloading] = useState(false);
  const [downloadingWord, setDownloadingWord] = useState(false);

  // ---- mode switching ----
  const switchMode = (newMode) => {
    if (newMode === mode) return;
    setMode(newMode);
    handleReset();
  };

  // ---- XML upload flow ----
  const handleFileSelected = useCallback(async (file) => {
    setSelectedFile(file);
    setStatus(STATUS.LOADING);
    setErrorMsg("");
    try {
      const data = await parseCurriculo(file);
      setCurriculo(data);
      setStatus(STATUS.PREVIEW);
    } catch (err) {
      const msg =
        err.response?.status === 400
          ? "Arquivo inválido. Certifique-se de que é um XML exportado da Plataforma Lattes."
          : "Erro ao processar o arquivo. Verifique se o servidor backend está rodando em localhost:8080.";
      setErrorMsg(msg);
      setStatus(STATUS.ERROR);
    }
  }, []);

  // ---- URL scraping flow ----
  const handleUrlSubmit = useCallback(async (url) => {
    setLattesUrl(url);
    setStatus(STATUS.LOADING);
    setErrorMsg("");
    setCaptchaUrl("");
    try {
      const data = await scrapeLattesUrl(url);
      setCurriculo(data);
      setStatus(STATUS.PREVIEW);
    } catch (err) {
      let msg;
      const httpStatus = err.response?.status;
      if (httpStatus === 400) {
        msg =
          "URL inválida. Verifique se a URL pertence ao domínio lattes.cnpq.br.";
      } else if (httpStatus === 403) {
        msg =
          "Acesso negado. O perfil Lattes pode estar configurado como privado.";
      } else if (httpStatus === 404) {
        msg = "Currículo não encontrado. Verifique se a URL está correta.";
      } else if (httpStatus === 503) {
        const responseData = err.response?.data;
        const urlFromResponse =
          typeof responseData === "object" ? responseData?.captchaUrl : null;
        if (urlFromResponse) {
          setCaptchaUrl(urlFromResponse);
          setStatus(STATUS.CAPTCHA);
          return;
        }
        msg =
          "O Lattes está exigindo verificação de segurança (CAPTCHA). Por favor, acesse o perfil manualmente no navegador e tente novamente em alguns minutos.";
      } else if (
        err.code === "ECONNABORTED" ||
        err.message?.includes("timeout")
      ) {
        msg =
          "Tempo limite excedido. O servidor Lattes pode estar lento. Tente novamente.";
      } else {
        msg =
          "Não foi possível acessar o currículo. Verifique se a URL está correta e o perfil é público.";
      }
      setErrorMsg(msg);
      setStatus(STATUS.ERROR);
    }
  }, []);

  // ---- downloads ----
  const handleDownload = useCallback(async () => {
    const fileName = curriculo?.nomeCompleto
      ? `${curriculo.nomeCompleto.replace(/\s+/g, "_")}_curriculo.pdf`
      : "curriculo.pdf";
    setDownloading(true);
    try {
      if (mode === MODE.URL) {
        await scrapeAndDownloadPdf(lattesUrl, fileName);
      } else {
        if (!selectedFile) return;
        await convertAndDownload(selectedFile, fileName);
      }
    } catch {
      alert("Erro ao gerar o PDF. Tente novamente.");
    } finally {
      setDownloading(false);
    }
  }, [mode, selectedFile, lattesUrl, curriculo]);

  const handleDownloadWord = useCallback(async () => {
    const fileName = curriculo?.nomeCompleto
      ? `${curriculo.nomeCompleto.replace(/\s+/g, "_")}_curriculo.docx`
      : "curriculo.docx";
    setDownloadingWord(true);
    try {
      if (mode === MODE.URL) {
        await scrapeAndDownloadWord(lattesUrl, fileName);
      } else {
        if (!selectedFile) return;
        await convertAndDownloadWord(selectedFile, fileName);
      }
    } catch {
      alert("Erro ao gerar o Word. Tente novamente.");
    } finally {
      setDownloadingWord(false);
    }
  }, [mode, selectedFile, lattesUrl, curriculo]);

  const handleReset = () => {
    setStatus(STATUS.IDLE);
    setCurriculo(null);
    setSelectedFile(null);
    setLattesUrl("");
    setErrorMsg("");
    setCaptchaUrl("");
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="app-header-inner">
          <div className="app-logo">📋</div>
          <div>
            <h1 className="app-title">CV Lattes → Currículo Vitae</h1>
            <p className="app-subtitle">
              Converta seu currículo Lattes (CNPq) em um PDF ou Word
              profissional
            </p>
          </div>
        </div>
      </header>

      <main className="app-main">
        {/* ---- MODE TABS ---- */}
        {status === STATUS.IDLE && (
          <div className="mode-tabs">
            <button
              className={`mode-tab ${mode === MODE.XML ? "mode-tab--active" : ""}`}
              onClick={() => switchMode(MODE.XML)}
            >
              📤 Upload XML
            </button>
            <button
              className={`mode-tab ${mode === MODE.URL ? "mode-tab--active" : ""}`}
              onClick={() => switchMode(MODE.URL)}
            >
              🔗 URL do Lattes
            </button>
          </div>
        )}

        {/* ---- IDLE: XML UPLOAD ---- */}
        {status === STATUS.IDLE && mode === MODE.XML && (
          <div className="upload-wrapper">
            <UploadZone onFileSelected={handleFileSelected} disabled={false} />
          </div>
        )}

        {/* ---- IDLE: URL INPUT ---- */}
        {status === STATUS.IDLE && mode === MODE.URL && (
          <div className="upload-wrapper">
            <LattesUrlInput onUrlSubmit={handleUrlSubmit} disabled={false} />
          </div>
        )}

        {/* ---- LOADING ---- */}
        {status === STATUS.LOADING && (
          <div className="center-message">
            <div className="spinner" aria-label="Carregando" />
            <p>
              {mode === MODE.URL
                ? "Buscando dados do Lattes..."
                : "Processando o arquivo XML..."}
            </p>
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

        {/* ---- CAPTCHA REQUIRED ---- */}
        {status === STATUS.CAPTCHA && (
          <div className="captcha-box">
            <div className="captcha-icon">🔒</div>
            <h2 className="captcha-title">Verificação de Segurança Necessária</h2>
            <p className="captcha-desc">
              O Lattes está exigindo que você comprove que não é um robô antes
              de exibir o currículo.
            </p>
            <ol className="captcha-steps">
              <li>
                Clique em <strong>"Abrir verificação no Lattes"</strong> abaixo
              </li>
              <li>Na nova aba, resolva o desafio de segurança (CAPTCHA)</li>
              <li>Volte aqui e clique em <strong>"Tentar novamente"</strong></li>
            </ol>
            <div className="captcha-actions">
              <a
                href={captchaUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="btn btn-primary"
              >
                🔗 Abrir verificação no Lattes
              </a>
              <button
                className="btn btn-secondary"
                onClick={() => handleUrlSubmit(lattesUrl)}
              >
                🔄 Tentar novamente
              </button>
              <button className="btn btn-secondary" onClick={handleReset}>
                ← Cancelar
              </button>
            </div>
            <p className="captcha-hint">
              URL da verificação:{" "}
              <code className="captcha-url">{captchaUrl}</code>
            </p>
          </div>
        )}

        {/* ---- PREVIEW ---- */}
        {status === STATUS.PREVIEW && curriculo && (
          <>
            <div className="preview-toolbar">
              <div className="preview-file-info">
                <span className="file-icon">
                  {mode === MODE.URL ? "🔗" : "📄"}
                </span>
                <span className="file-name">
                  {mode === MODE.URL ? lattesUrl : selectedFile?.name}
                </span>
              </div>
              <div className="preview-actions">
                <button className="btn btn-secondary" onClick={handleReset}>
                  ← {mode === MODE.URL ? "Nova busca" : "Novo arquivo"}
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleDownload}
                  disabled={downloading}
                >
                  {downloading ? "Gerando PDF..." : "⬇ Baixar PDF"}
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={handleDownloadWord}
                  disabled={downloadingWord}
                >
                  {downloadingWord ? "Gerando Word..." : "📝 Baixar Word"}
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
                {downloading ? "Gerando PDF..." : "⬇ Baixar Currículo em PDF"}
              </button>
              <button
                className="btn btn-secondary btn-lg"
                onClick={handleDownloadWord}
                disabled={downloadingWord}
              >
                {downloadingWord
                  ? "Gerando Word..."
                  : "📝 Baixar Currículo em Word"}
              </button>
            </div>
          </>
        )}
      </main>

      <footer className="app-footer">
        Gerado a partir da Plataforma Lattes — CNPq
      </footer>
    </div>
  );
}

