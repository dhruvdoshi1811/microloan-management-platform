import { useEffect, useState } from 'react'
import { api } from '../api/client'

export default function AdminPage({ token }) {
  const [applications, setApplications] = useState([])
  const [outboxEvents, setOutboxEvents] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [overdueResult, setOverdueResult] = useState(null)
  const [rejectingId, setRejectingId] = useState(null)
  const [rejectionReason, setRejectionReason] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    refreshApplications()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    refreshOutbox()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter])

  async function refreshApplications() {
    setError('')
    try {
      const page = await api.get('/loan-applications?size=1000', token)
      setApplications(page.content)
    } catch (err) {
      setError(err.message)
    }
  }

  async function refreshOutbox() {
    try {
      const suffix = statusFilter ? `&status=${statusFilter}` : ''
      const page = await api.get(`/admin/outbox?size=1000${suffix}`, token)
      setOutboxEvents(page.content)
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleApprove(id) {
    setError('')
    setBusy(true)
    try {
      await api.post(`/loan-applications/${id}/approve`, null, token)
      setMessage(`Application #${id} approved.`)
      refreshApplications()
      refreshOutbox()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleReject(id) {
    setError('')
    setBusy(true)
    try {
      await api.post(`/loan-applications/${id}/reject`, { rejectionReason }, token)
      setMessage(`Application #${id} rejected.`)
      setRejectingId(null)
      setRejectionReason('')
      refreshApplications()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleRunOverdueCheck() {
    setError('')
    setBusy(true)
    try {
      const result = await api.post('/admin/run-overdue-check', null, token)
      setOverdueResult(result)
      refreshOutbox()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const pending = applications.filter((a) => a.status === 'PENDING')
  const decided = applications.filter((a) => a.status !== 'PENDING')

  return (
    <div className="space-y-8">
      {error && <Banner kind="error">{error}</Banner>}
      {message && <Banner kind="success">{message}</Banner>}

      <section>
        <h2 className="font-medium text-gray-900 mb-2">Pending applications</h2>
        {pending.length === 0 && <p className="text-sm text-gray-500">Nothing pending.</p>}
        <ul className="space-y-2">
          {pending.map((a) => (
            <li key={a.id} className="border border-gray-200 rounded p-3 text-sm space-y-2">
              <div className="flex justify-between items-center">
                <span>
                  App #{a.id} - borrower #{a.borrowerId}, product #{a.productId}, {a.requestedAmount} over{' '}
                  {a.requestedTenureMonths} months
                </span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => handleApprove(a.id)}
                    className="bg-green-600 text-white rounded px-3 py-1 text-xs disabled:opacity-50 cursor-pointer"
                  >
                    Approve
                  </button>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => setRejectingId(rejectingId === a.id ? null : a.id)}
                    className="bg-red-600 text-white rounded px-3 py-1 text-xs disabled:opacity-50 cursor-pointer"
                  >
                    Reject
                  </button>
                </div>
              </div>
              {rejectingId === a.id && (
                <div className="flex gap-2">
                  <input
                    placeholder="Rejection reason"
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    className="flex-1 border border-gray-300 rounded px-3 py-1 text-xs"
                  />
                  <button
                    type="button"
                    disabled={busy || !rejectionReason}
                    onClick={() => handleReject(a.id)}
                    className="bg-gray-900 text-white rounded px-3 py-1 text-xs disabled:opacity-50 cursor-pointer"
                  >
                    Confirm reject
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h2 className="font-medium text-gray-900 mb-2">Decided applications</h2>
        {decided.length === 0 && <p className="text-sm text-gray-500">None yet.</p>}
        <ul className="space-y-1 text-sm">
          {decided.map((a) => (
            <li key={a.id} className="flex justify-between border-b border-gray-100 py-1">
              <span>
                App #{a.id} - borrower #{a.borrowerId}
              </span>
              <span>
                {a.status}
                {a.rejectionReason ? ` (${a.rejectionReason})` : ''}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <div className="flex items-center justify-between mb-2">
          <h2 className="font-medium text-gray-900">Outbox events</h2>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="border border-gray-300 rounded px-2 py-1 text-sm"
          >
            <option value="">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="PUBLISHED">Published</option>
          </select>
        </div>
        {outboxEvents.length === 0 && <p className="text-sm text-gray-500">No events.</p>}
        <table className="text-xs w-full border-collapse">
          <thead>
            <tr className="text-left text-gray-500">
              <th className="pr-3">ID</th>
              <th className="pr-3">Type</th>
              <th className="pr-3">Aggregate</th>
              <th className="pr-3">Status</th>
              <th>Published at</th>
            </tr>
          </thead>
          <tbody>
            {outboxEvents.map((e) => (
              <tr key={e.id} className="border-t border-gray-100">
                <td className="pr-3 py-1">{e.id}</td>
                <td className="pr-3">{e.eventType}</td>
                <td className="pr-3">
                  {e.aggregateType}#{e.aggregateId}
                </td>
                <td className="pr-3">{e.status}</td>
                <td>{e.publishedAt || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2 className="font-medium text-gray-900 mb-2">Overdue check</h2>
        <p className="text-xs text-gray-500 mb-2">
          Manually runs the same batch job the daily schedule runs, for demo purposes.
        </p>
        <button
          type="button"
          disabled={busy}
          onClick={handleRunOverdueCheck}
          className="bg-gray-900 text-white rounded px-4 py-1.5 text-sm disabled:opacity-50 cursor-pointer"
        >
          Run overdue check
        </button>
        {overdueResult && (
          <p className="text-sm text-gray-700 mt-2">
            Scanned {overdueResult.loansScanned} loans - {overdueResult.loansMarkedOverdue} marked overdue,{' '}
            {overdueResult.installmentsMarkedOverdue} installments overdue, {overdueResult.penaltiesApplied} penalties
            applied.
          </p>
        )}
      </section>
    </div>
  )
}

function Banner({ kind, children }) {
  const classes = kind === 'error' ? 'bg-red-50 text-red-700 border-red-200' : 'bg-green-50 text-green-700 border-green-200'
  return <div className={`text-sm border rounded px-3 py-2 ${classes}`}>{children}</div>
}
