import axios from 'axios'

const BASE = '/api'

// ---------------------------------------------------------------------------
// Lattes URL scraping
// ---------------------------------------------------------------------------

/**
 * Fetch the public Lattes page and return structured curriculum JSON.
 * @param {string} url  — public Lattes profile URL
 * @returns {Promise<object>} Curriculo JSON
 */
export async function scrapeLattesUrl(url) {
  const resp = await axios.post(`${BASE}/scrape`, { url })
  return resp.data
}

/**
 * Fetch the public Lattes page, generate a PDF and trigger browser download.
 * @param {string} url      — public Lattes profile URL
 * @param {string} fileName — suggested download filename
 */
export async function scrapeAndDownloadPdf(url, fileName = 'curriculo.pdf') {
  const resp = await axios.post(`${BASE}/scrape/convert`, { url }, {
    responseType: 'blob',
  })

  const blobUrl = window.URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }))
  const link = document.createElement('a')
  link.href = blobUrl
  link.setAttribute('download', fileName)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(blobUrl)
}

/**
 * Fetch the public Lattes page, generate a Word (.docx) and trigger browser download.
 * @param {string} url      — public Lattes profile URL
 * @param {string} fileName — suggested download filename
 */
export async function scrapeAndDownloadWord(url, fileName = 'curriculo.docx') {
  const resp = await axios.post(`${BASE}/scrape/convert/word`, { url }, {
    responseType: 'blob',
  })

  const wordMime = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const blobUrl = window.URL.createObjectURL(new Blob([resp.data], { type: wordMime }))
  const link = document.createElement('a')
  link.href = blobUrl
  link.setAttribute('download', fileName)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(blobUrl)
}

/**
 * Parse the XML file and get structured JSON for preview.
 * @param {File} file
 * @returns {Promise<object>} Curriculo JSON
 */
export async function parseCurriculo(file) {
  const form = new FormData()
  form.append('file', file)
  const resp = await axios.post(`${BASE}/parse`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return resp.data
}

/**
 * Convert the XML file to a PDF and trigger browser download.
 * @param {File} file
 * @param {string} fileName  — suggested download filename
 */
export async function convertAndDownload(file, fileName = 'curriculo.pdf') {
  const form = new FormData()
  form.append('file', file)
  const resp = await axios.post(`${BASE}/convert`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob',
  })

  const url = window.URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', fileName)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

/**
 * Convert the XML file to a Word (.docx) and trigger browser download.
 * @param {File} file
 * @param {string} fileName  — suggested download filename
 */
export async function convertAndDownloadWord(file, fileName = 'curriculo.docx') {
  const form = new FormData()
  form.append('file', file)
  const resp = await axios.post(`${BASE}/convert/word`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    responseType: 'blob',
  })

  const wordMime = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  const url = window.URL.createObjectURL(new Blob([resp.data], { type: wordMime }))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', fileName)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}
