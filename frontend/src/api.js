const API_BASE = import.meta.env.VITE_API_URL || ''
const UPLOAD_TIMEOUT_MS = 5 * 60 * 1000

async function parseApiError(response) {
  const text = await response.text()
  try {
    const data = JSON.parse(text)
    return data.message || data.detail || data.error || text
  } catch {
    return text || `HTTP ${response.status}`
  }
}

function wrapNetworkError(err) {
  const msg = err?.message || String(err)
  if (msg.includes('Failed to fetch') || msg.includes('ECONNREFUSED') || msg.includes('NetworkError')) {
    return new Error(
      'Backend\'e bağlanılamadı (port 8080). Yeni terminalde: cd backend && mvn spring-boot:run'
    )
  }
  if (err?.name === 'AbortError') {
    return new Error('İşlem zaman aşımına uğradı (5 dk). Daha hafif Ollama modeli deneyin: ollama pull qwen2.5:3b')
  }
  return err instanceof Error ? err : new Error(msg)
}

async function fetchWithTimeout(url, options = {}, timeoutMs = UPLOAD_TIMEOUT_MS) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(url, { ...options, signal: controller.signal })
  } finally {
    clearTimeout(timer)
  }
}

export async function uploadInvoice(file) {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetchWithTimeout(`${API_BASE}/api/invoices/upload`, {
      method: 'POST',
      body: formData,
    })

    if (!response.ok) {
      throw new Error(await parseApiError(response))
    }

    return response.json()
  } catch (e) {
    throw wrapNetworkError(e)
  }
}

export async function fetchInvoices() {
  try {
    const response = await fetchWithTimeout(`${API_BASE}/api/invoices`, {}, 15000)
    if (!response.ok) {
      throw new Error(await parseApiError(response))
    }
    return response.json()
  } catch (e) {
    throw wrapNetworkError(e)
  }
}

export async function deleteInvoice(id) {
  const response = await fetch(`${API_BASE}/api/invoices/${id}`, {
    method: 'DELETE',
  })
  if (!response.ok) {
    throw new Error('Silme işlemi başarısız')
  }
}

export function getExcelExportUrl() {
  return `${API_BASE}/api/invoices/export/excel`
}
