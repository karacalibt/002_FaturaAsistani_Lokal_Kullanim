import { useCallback, useEffect, useState } from 'react'
import {
  deleteInvoice,
  fetchInvoices,
  getExcelExportUrl,
  uploadInvoice,
} from './api.js'
import './App.css'

async function fetchDemoHealth() {
  try {
    const res = await fetch('/api/demo/health')
    if (!res.ok) return null
    return res.json()
  } catch {
    return null
  }
}

function formatMoney(value, currency = 'TRY') {
  if (value == null) return '—'
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: currency || 'TRY',
  }).format(Number(value))
}

function formatDate(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('tr-TR')
  } catch {
    return iso
  }
}

export default function App() {
  const [invoices, setInvoices] = useState([])
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadStatus, setUploadStatus] = useState('')
  const [error, setError] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [dragOver, setDragOver] = useState(false)
  const [demoHealth, setDemoHealth] = useState(null)

  const loadInvoices = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchInvoices()
      setInvoices(data)
      if (data.length > 0 && !selectedId) {
        setSelectedId(data[0].id)
      }
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [selectedId])

  useEffect(() => {
    loadInvoices()
  }, [loadInvoices])

  useEffect(() => {
    fetchDemoHealth().then(setDemoHealth)
    const t = setInterval(() => fetchDemoHealth().then(setDemoHealth), 15000)
    return () => clearInterval(t)
  }, [])

  const handleFile = async (file) => {
    if (!file) return
    const allowed = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg']
    const ext = file.name.toLowerCase()
    const validExt = ext.endsWith('.pdf') || ext.endsWith('.jpg') || ext.endsWith('.jpeg') || ext.endsWith('.png')
    if (!validExt && !allowed.includes(file.type)) {
      setError('Sadece PDF, JPG ve PNG dosyaları desteklenir.')
      return
    }

    setUploading(true)
    setError(null)
    const modelHint = demoHealth?.ollamaModel || 'Ollama'
    setUploadStatus(`OCR + ${modelHint} işleniyor… (ilk seferde 1–3 dk sürebilir)`)
    try {
      const result = await uploadInvoice(file)
      await loadInvoices()
      setSelectedId(result.id)
    } catch (e) {
      setError(e.message)
    } finally {
      setUploading(false)
      setUploadStatus('')
    }
  }

  const onDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    const file = e.dataTransfer.files?.[0]
    handleFile(file)
  }

  const handleDelete = async (id) => {
    if (!confirm('Bu faturayı silmek istediğinize emin misiniz?')) return
    try {
      await deleteInvoice(id)
      if (selectedId === id) setSelectedId(null)
      await loadInvoices()
    } catch (e) {
      setError(e.message)
    }
  }

  const selected = invoices.find((i) => i.id === selectedId)

  return (
    <div className="app">
      <header className="header">
        <div className="header-brand">
          <span className="logo">📄</span>
          <div>
            <h1>AI Fatura Asistanı</h1>
            <p>Lokal demo — PaddleOCR + Ollama (bulut yok)</p>
          </div>
        </div>
        <div className="header-actions">
          {demoHealth && (
            <div className="service-status">
              <span className={demoHealth.paddleocr?.up ? 'up' : 'down'}>
                OCR {demoHealth.paddleocr?.up ? '✓' : '✗'}
              </span>
              <span
                className={
                  demoHealth.ollama?.up && demoHealth.ollamaModelReady !== false ? 'up' : 'down'
                }
                title={
                  demoHealth.ollamaModelReady === false
                    ? `Model hazır değil: ${demoHealth.ollamaModel}`
                    : demoHealth.ollamaModel
                }
              >
                LLM {demoHealth.ollama?.up && demoHealth.ollamaModelReady !== false ? '✓' : '✗'}
              </span>
            </div>
          )}
          <a className="btn btn-secondary" href={getExcelExportUrl()} download="faturalar.xlsx">
            Excel İndir
          </a>
        </div>
      </header>

      <main className="main">
        <section className="upload-panel">
          <div
            className={`dropzone ${dragOver ? 'drag-over' : ''} ${uploading ? 'uploading' : ''}`}
            onDragOver={(e) => {
              e.preventDefault()
              setDragOver(true)
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={onDrop}
          >
            <input
              id="file-input"
              type="file"
              accept=".pdf,.jpg,.jpeg,.png"
              onChange={(e) => handleFile(e.target.files?.[0])}
              disabled={uploading}
            />
            <label htmlFor="file-input" className="dropzone-label">
              {uploading ? (
                <span className="spinner">{uploadStatus || 'İşleniyor…'}</span>
              ) : (
                <>
                  <strong>Fatura yükle</strong>
                  <span>Sürükle-bırak veya tıklayarak PDF / JPG / PNG seçin</span>
                </>
              )}
            </label>
          </div>
          {error && <div className="alert alert-error">{error}</div>}
        </section>

        <div className="content-grid">
          <aside className="list-panel">
            <div className="panel-header">
              <h2>Yüklenen faturalar</h2>
              <button className="btn btn-ghost" onClick={loadInvoices} disabled={loading}>
                Yenile
              </button>
            </div>
            {loading && invoices.length === 0 ? (
              <p className="muted">Yükleniyor…</p>
            ) : invoices.length === 0 ? (
              <p className="muted">Henüz fatura yok. İlk dosyanızı yükleyin.</p>
            ) : (
              <ul className="invoice-list">
                {invoices.map((inv) => (
                  <li key={inv.id}>
                    <button
                      type="button"
                      className={`invoice-item ${selectedId === inv.id ? 'active' : ''}`}
                      onClick={() => setSelectedId(inv.id)}
                    >
                      <span className="file-name">{inv.fileName}</span>
                      <span className="meta">
                        {inv.invoice?.supplierName || 'Tedarikçi belirsiz'} ·{' '}
                        {formatMoney(inv.invoice?.totalAmount, inv.invoice?.currency)}
                      </span>
                    </button>
                    <button
                      type="button"
                      className="btn-delete"
                      title="Sil"
                      onClick={() => handleDelete(inv.id)}
                    >
                      ×
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </aside>

          <section className="detail-panel">
            {!selected ? (
              <div className="empty-detail">
                <p>Detay görmek için listeden bir fatura seçin.</p>
              </div>
            ) : (
              <>
                <div className="panel-header">
                  <h2>{selected.fileName}</h2>
                  <span className="badge">{formatDate(selected.uploadedAt)}</span>
                </div>

                <div className="cards">
                  <div className="card">
                    <h3>Fatura bilgileri</h3>
                    <dl className="info-grid">
                      <dt>Fatura No</dt>
                      <dd>{selected.invoice?.invoiceNumber ?? '—'}</dd>
                      <dt>Tarih</dt>
                      <dd>{selected.invoice?.invoiceDate ?? '—'}</dd>
                      <dt>Tedarikçi</dt>
                      <dd>{selected.invoice?.supplierName ?? '—'}</dd>
                      <dt>VKN</dt>
                      <dd>{selected.invoice?.supplierTaxNumber ?? '—'}</dd>
                      <dt>Vergi Dairesi</dt>
                      <dd>{selected.invoice?.taxOffice ?? '—'}</dd>
                      <dt>Toplam</dt>
                      <dd className="amount">
                        {formatMoney(selected.invoice?.totalAmount, selected.invoice?.currency)}
                      </dd>
                    </dl>
                    {selected.invoice?.warnings?.length > 0 && (
                      <ul className="warnings">
                        {selected.invoice.warnings.map((w, i) => (
                          <li key={i}>{w}</li>
                        ))}
                      </ul>
                    )}
                  </div>

                  {selected.invoice?.lineItems?.length > 0 && (
                    <div className="card">
                      <h3>Kalemler</h3>
                      <table>
                        <thead>
                          <tr>
                            <th>Açıklama</th>
                            <th>Miktar</th>
                            <th>Birim</th>
                            <th>KDV %</th>
                            <th>Toplam</th>
                          </tr>
                        </thead>
                        <tbody>
                          {selected.invoice.lineItems.map((item, idx) => (
                            <tr key={idx}>
                              <td>{item.description ?? '—'}</td>
                              <td>{item.quantity ?? '—'}</td>
                              <td>{formatMoney(item.unitPrice, selected.invoice?.currency)}</td>
                              <td>{item.vatRate != null ? `%${item.vatRate}` : '—'}</td>
                              <td>{formatMoney(item.lineTotal, selected.invoice?.currency)}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  <div className="card">
                    <h3>Ham metin</h3>
                    <pre className="raw-text">{selected.rawText || '—'}</pre>
                  </div>
                </div>
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}
