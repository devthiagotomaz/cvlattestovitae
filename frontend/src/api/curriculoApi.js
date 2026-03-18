import axios from 'axios'

const BASE = '/api'

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
