import { useEffect, useState } from 'react'
import { api } from '../api/client'

const BORROWER_ID_KEY = 'microloan.borrowerId'

export default function BorrowerPage({ token }) {
  const [borrowerId, setBorrowerId] = useState(() => localStorage.getItem(BORROWER_ID_KEY) || '')
  const [borrower, setBorrower] = useState(null)
  const [products, setProducts] = useState([])
  const [applications, setApplications] = useState([])
  const [loans, setLoans] = useState([])
  const [installments, setInstallments] = useState({})
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (borrowerId) {
      refreshAll(borrowerId)
    }
  }, [borrowerId])

  async function refreshAll(id) {
    setError('')
    try {
      const [borrowerData, productPage, applicationPage, loanPage] = await Promise.all([
        api.get(`/borrowers/${id}`, token),
        api.get('/loan-products?size=1000', token),
        api.get('/loan-applications?size=1000', token),
        api.get('/loans?size=1000', token),
      ])
      setBorrower(borrowerData)
      setProducts(productPage.content.filter((p) => p.isActive))
      setApplications(applicationPage.content.filter((a) => String(a.borrowerId) === String(id)))
      setLoans(loanPage.content.filter((l) => String(l.borrowerId) === String(id)))
    } catch (err) {
      setError(err.message)
    }
  }

  function useThisBorrowerId(id) {
    localStorage.setItem(BORROWER_ID_KEY, String(id))
    setBorrowerId(String(id))
  }

  function forgetBorrowerId() {
    localStorage.removeItem(BORROWER_ID_KEY)
    setBorrowerId('')
    setBorrower(null)
  }

  if (!borrowerId) {
    return (
      <ResolveBorrowerId
        token={token}
        onResolved={useThisBorrowerId}
      />
    )
  }

  if (!borrower) {
    return <p className="text-gray-500">{error || 'Loading...'}</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">{borrower.fullName}</h2>
          <p className="text-sm text-gray-500">
            Borrower #{borrower.id} - KYC level: <strong>{borrower.kycLevel}</strong>
          </p>
        </div>
        <button type="button" onClick={forgetBorrowerId} className="text-sm text-gray-500 underline cursor-pointer">
          Switch borrower
        </button>
      </div>

      {error && <Banner kind="error">{error}</Banner>}
      {message && <Banner kind="success">{message}</Banner>}

      {borrower.kycLevel === 'NONE' && (
        <KycPanel
          token={token}
          borrowerId={borrower.id}
          onVerified={() => refreshAll(borrowerId)}
          setMessage={setMessage}
          setError={setError}
        />
      )}

      {borrower.kycLevel !== 'NONE' && (
        <ApplyForLoanPanel
          token={token}
          borrowerId={borrower.id}
          products={products}
          onSubmitted={() => refreshAll(borrowerId)}
          setMessage={setMessage}
          setError={setError}
        />
      )}

      <section>
        <h3 className="font-medium text-gray-900 mb-2">My applications</h3>
        {applications.length === 0 && <p className="text-sm text-gray-500">No applications yet.</p>}
        <ul className="space-y-2">
          {applications.map((a) => (
            <li key={a.id} className="border border-gray-200 rounded p-3 text-sm flex justify-between items-center">
              <span>
                Application #{a.id} - {a.requestedAmount} over {a.requestedTenureMonths} months
              </span>
              <StatusBadge status={a.status} />
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h3 className="font-medium text-gray-900 mb-2">My loans</h3>
        {loans.length === 0 && <p className="text-sm text-gray-500">No loans yet.</p>}
        <ul className="space-y-3">
          {loans.map((loan) => (
            <LoanCard
              key={loan.id}
              loan={loan}
              token={token}
              installments={installments[loan.id]}
              onLoadInstallments={async () => {
                const list = await api.get(`/loans/${loan.id}/installments`, token)
                setInstallments((prev) => ({ ...prev, [loan.id]: list }))
              }}
              onChanged={() => refreshAll(borrowerId)}
              setMessage={setMessage}
              setError={setError}
            />
          ))}
        </ul>
      </section>
    </div>
  )
}

function ResolveBorrowerId({ token, onResolved }) {
  const [existingId, setExistingId] = useState('')
  const [error, setError] = useState('')
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [dob, setDob] = useState('')
  const [monthlyIncome, setMonthlyIncome] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleUseExisting(event) {
    event.preventDefault()
    setError('')
    try {
      const borrower = await api.get(`/borrowers/${existingId}`, token)
      onResolved(borrower.id)
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCreate(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const borrower = await api.post(
        '/borrowers',
        { fullName, phone, email, dob, monthlyIncome: Number(monthlyIncome) },
        token,
      )
      onResolved(borrower.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid gap-6 sm:grid-cols-2">
      <form onSubmit={handleUseExisting} className="border border-gray-200 rounded-lg p-4 space-y-3">
        <h2 className="font-medium text-gray-900">I already have a Borrower ID</h2>
        <p className="text-xs text-gray-500">
          Borrower profiles aren't linked to login accounts yet, so this app asks for the ID directly.
        </p>
        <input
          type="number"
          required
          placeholder="Borrower ID"
          value={existingId}
          onChange={(e) => setExistingId(e.target.value)}
          className="w-full border border-gray-300 rounded px-3 py-1.5"
        />
        <button type="submit" className="w-full bg-gray-900 text-white rounded py-2 cursor-pointer">
          Use this borrower
        </button>
      </form>

      <form onSubmit={handleCreate} className="border border-gray-200 rounded-lg p-4 space-y-3">
        <h2 className="font-medium text-gray-900">Create a borrower profile</h2>
        <input
          required
          placeholder="Full name"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          className="w-full border border-gray-300 rounded px-3 py-1.5"
        />
        <input
          required
          placeholder="Phone"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          className="w-full border border-gray-300 rounded px-3 py-1.5"
        />
        <input
          type="email"
          required
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full border border-gray-300 rounded px-3 py-1.5"
        />
        <input
          type="date"
          required
          value={dob}
          onChange={(e) => setDob(e.target.value)}
          className="w-full border border-gray-300 rounded px-3 py-1.5"
        />
        <input
          type="number"
          required
          placeholder="Monthly income"
          value={monthlyIncome}
          onChange={(e) => setMonthlyIncome(e.target.value)}
          className="w-full border border-gray-300 rounded px-3 py-1.5"
        />
        <button type="submit" disabled={busy} className="w-full bg-gray-900 text-white rounded py-2 disabled:opacity-50 cursor-pointer">
          {busy ? 'Creating...' : 'Create profile'}
        </button>
      </form>

      {error && <Banner kind="error">{error}</Banner>}
    </div>
  )
}

function KycPanel({ token, borrowerId, onVerified, setMessage, setError }) {
  const [documentType, setDocumentType] = useState('PAN')
  const [documentNumber, setDocumentNumber] = useState('')
  const [otpCode, setOtpCode] = useState('')
  const [issuedOtp, setIssuedOtp] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleInitiate(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const otp = await api.post(`/borrowers/${borrowerId}/kyc/initiate`, { documentType, documentNumber }, token)
      setIssuedOtp(otp.otpCode)
      setMessage(`OTP issued: ${otp.otpCode} (auto-filled below - a real gateway would text this instead)`)
      setOtpCode(otp.otpCode)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleVerify(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api.post(`/borrowers/${borrowerId}/kyc/verify-otp`, { documentType, otpCode }, token)
      setMessage('KYC verified.')
      onVerified()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="border border-gray-200 rounded-lg p-4 space-y-3">
      <h3 className="font-medium text-gray-900">Complete KYC to apply for a loan</h3>
      <form onSubmit={issuedOtp ? handleVerify : handleInitiate} className="flex flex-wrap gap-3 items-end">
        <div>
          <label className="block text-xs text-gray-500 mb-1">Document type</label>
          <select
            value={documentType}
            disabled={!!issuedOtp}
            onChange={(e) => setDocumentType(e.target.value)}
            className="border border-gray-300 rounded px-3 py-1.5"
          >
            <option value="PAN">PAN</option>
            <option value="AADHAAR">Aadhaar</option>
          </select>
        </div>
        {!issuedOtp && (
          <div>
            <label className="block text-xs text-gray-500 mb-1">Document number</label>
            <input
              required
              value={documentNumber}
              onChange={(e) => setDocumentNumber(e.target.value)}
              className="border border-gray-300 rounded px-3 py-1.5"
            />
          </div>
        )}
        {issuedOtp && (
          <div>
            <label className="block text-xs text-gray-500 mb-1">OTP code</label>
            <input
              required
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              className="border border-gray-300 rounded px-3 py-1.5"
            />
          </div>
        )}
        <button type="submit" disabled={busy} className="bg-gray-900 text-white rounded px-4 py-1.5 disabled:opacity-50 cursor-pointer">
          {issuedOtp ? 'Verify OTP' : 'Send OTP'}
        </button>
      </form>
    </section>
  )
}

function ApplyForLoanPanel({ token, borrowerId, products, onSubmitted, setMessage, setError }) {
  const [productId, setProductId] = useState(products[0]?.id ?? '')
  const [amount, setAmount] = useState('')
  const [tenureMonths, setTenureMonths] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api.post(
        '/loan-applications',
        {
          borrowerId: Number(borrowerId),
          productId: Number(productId),
          requestedAmount: Number(amount),
          requestedTenureMonths: Number(tenureMonths),
        },
        token,
      )
      setMessage('Application submitted.')
      setAmount('')
      setTenureMonths('')
      onSubmitted()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="border border-gray-200 rounded-lg p-4 space-y-3">
      <h3 className="font-medium text-gray-900">Apply for a loan</h3>
      <form onSubmit={handleSubmit} className="flex flex-wrap gap-3 items-end">
        <div>
          <label className="block text-xs text-gray-500 mb-1">Product</label>
          <select
            required
            value={productId}
            onChange={(e) => setProductId(e.target.value)}
            className="border border-gray-300 rounded px-3 py-1.5"
          >
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.minPrincipal}-{p.maxPrincipal}, {p.interestRate}%)
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Amount</label>
          <input
            type="number"
            required
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="border border-gray-300 rounded px-3 py-1.5 w-32"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">Tenure (months)</label>
          <input
            type="number"
            required
            value={tenureMonths}
            onChange={(e) => setTenureMonths(e.target.value)}
            className="border border-gray-300 rounded px-3 py-1.5 w-28"
          />
        </div>
        <button type="submit" disabled={busy || products.length === 0} className="bg-gray-900 text-white rounded px-4 py-1.5 disabled:opacity-50 cursor-pointer">
          Submit application
        </button>
      </form>
    </section>
  )
}

function LoanCard({ loan, token, installments, onLoadInstallments, onChanged, setMessage, setError }) {
  const [paymentAmount, setPaymentAmount] = useState('')
  const [paymentMode, setPaymentMode] = useState('UPI')
  const [busy, setBusy] = useState(false)

  async function handleAcknowledge() {
    setError('')
    setBusy(true)
    try {
      await api.post(`/loans/${loan.id}/agreement/acknowledge`, null, token)
      setMessage(`Loan #${loan.id} acknowledged - installment schedule generated.`)
      onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleRepay(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const paymentReference = `WEB-${Date.now()}-${Math.floor(Math.random() * 1e6)}`
      await api.post(
        '/repayments',
        { loanId: loan.id, amount: Number(paymentAmount), paymentReference, paymentMode },
        token,
      )
      setMessage(`Repayment of ${paymentAmount} recorded for loan #${loan.id}.`)
      setPaymentAmount('')
      onChanged()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const outstanding = (Number(loan.totalPayable) - Number(loan.totalPaid)).toFixed(2)

  return (
    <li className="border border-gray-200 rounded-lg p-4 space-y-3">
      <div className="flex justify-between items-center text-sm">
        <span>
          Loan #{loan.id} - principal {loan.principalAmount}, EMI {loan.emiAmount}, outstanding {outstanding}
        </span>
        <StatusBadge status={loan.status} />
      </div>

      {loan.status === 'AGREEMENT_PENDING' && (
        <button type="button" onClick={handleAcknowledge} disabled={busy} className="bg-gray-900 text-white rounded px-4 py-1.5 text-sm disabled:opacity-50 cursor-pointer">
          Acknowledge agreement
        </button>
      )}

      {(loan.status === 'ACTIVE' || loan.status === 'OVERDUE' || loan.status === 'CLOSED') && (
        <div className="space-y-2">
          <button type="button" onClick={onLoadInstallments} className="text-sm text-gray-600 underline cursor-pointer">
            {installments ? 'Refresh installments' : 'View installments'}
          </button>
          {installments && (
            <table className="text-xs w-full border-collapse">
              <thead>
                <tr className="text-left text-gray-500">
                  <th className="pr-3">#</th>
                  <th className="pr-3">Due date</th>
                  <th className="pr-3">Due</th>
                  <th className="pr-3">Paid</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {installments.map((i) => (
                  <tr key={i.id} className="border-t border-gray-100">
                    <td className="pr-3 py-1">{i.installmentNo}</td>
                    <td className="pr-3">{i.dueDate}</td>
                    <td className="pr-3">{i.totalDue}</td>
                    <td className="pr-3">{i.amountPaid}</td>
                    <td>{i.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {(loan.status === 'ACTIVE' || loan.status === 'OVERDUE') && (
        <form onSubmit={handleRepay} className="flex flex-wrap gap-3 items-end pt-2 border-t border-gray-100">
          <div>
            <label className="block text-xs text-gray-500 mb-1">Amount</label>
            <input
              type="number"
              required
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(e.target.value)}
              className="border border-gray-300 rounded px-3 py-1.5 w-32"
            />
          </div>
          <div>
            <label className="block text-xs text-gray-500 mb-1">Mode</label>
            <select value={paymentMode} onChange={(e) => setPaymentMode(e.target.value)} className="border border-gray-300 rounded px-3 py-1.5">
              <option value="UPI">UPI</option>
              <option value="NETBANKING">Net banking</option>
              <option value="CARD">Card</option>
            </select>
          </div>
          <button type="submit" disabled={busy} className="bg-gray-900 text-white rounded px-4 py-1.5 text-sm disabled:opacity-50 cursor-pointer">
            Make repayment
          </button>
        </form>
      )}
    </li>
  )
}

function StatusBadge({ status }) {
  return <span className="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-700">{status}</span>
}

function Banner({ kind, children }) {
  const classes = kind === 'error' ? 'bg-red-50 text-red-700 border-red-200' : 'bg-green-50 text-green-700 border-green-200'
  return <div className={`text-sm border rounded px-3 py-2 ${classes}`}>{children}</div>
}
