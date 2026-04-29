import { useState, useEffect } from 'react'
import { bookings } from '../api'
import { useAuth } from '../AuthContext'
import { CalendarDays, CreditCard, XCircle, CheckCircle, Clock, Ban } from 'lucide-react'

const STATUS_STYLE = {
  pending:   { bg: 'bg-amber-50',  text: 'text-amber-700',  icon: <Clock size={14} /> },
  confirmed: { bg: 'bg-green-50',  text: 'text-green-700',  icon: <CheckCircle size={14} /> },
  cancelled: { bg: 'bg-red-50',    text: 'text-red-600',    icon: <Ban size={14} /> },
}

export default function BookingsPage() {
  const { user } = useAuth()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [paying, setPaying] = useState(null)
  const [cancelling, setCancelling] = useState(null)
  const [msg, setMsg] = useState(null)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      const res = await bookings.listForUser(user.userId)
      setList(res)
    } catch {
    } finally {
      setLoading(false)
    }
  }

  async function handlePay(bookingId) {
    setPaying(bookingId)
    setMsg(null)
    try {
      const res = await bookings.pay(bookingId)
      setMsg({ type: res.success ? 'success' : 'error', text: res.success ? `Payment successful! Charge: ${res.chargeId}` : res.failureReason })
      await load()
    } catch (err) {
      setMsg({ type: 'error', text: err.message })
    } finally {
      setPaying(null)
    }
  }

  async function handleCancel(bookingId) {
    if (!confirm('Cancel this booking?')) return
    setCancelling(bookingId)
    setMsg(null)
    try {
      await bookings.cancel(bookingId)
      setMsg({ type: 'success', text: 'Booking cancelled.' })
      await load()
    } catch (err) {
      setMsg({ type: 'error', text: err.message })
    } finally {
      setCancelling(null)
    }
  }

  if (loading) return <div className="max-w-3xl mx-auto px-4 py-12 text-center text-gray-500">Loading…</div>

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        <CalendarDays className="text-blue-600" size={22} /> My Bookings
      </h1>

      {msg && (
        <p className={`text-sm px-4 py-3 rounded-xl mb-4 ${msg.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'}`}>
          {msg.text}
        </p>
      )}

      {list.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <CalendarDays size={40} className="mx-auto mb-3 opacity-30" />
          <p>No bookings yet.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {list.map((b) => {
            const s = STATUS_STYLE[b.status] || STATUS_STYLE.pending
            return (
              <div key={b.id} className="bg-white rounded-2xl border border-gray-200 p-5">
                <div className="flex items-start justify-between flex-wrap gap-2">
                  <div>
                    <p className="font-semibold text-gray-900">Booking #{b.id}</p>
                    <p className="text-sm text-gray-500 mt-0.5">Hotel #{b.hotelId} · Room #{b.roomId}</p>
                  </div>
                  <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full ${s.bg} ${s.text}`}>
                    {s.icon}
                    {b.status}
                  </span>
                </div>

                <div className="mt-3 flex flex-wrap gap-4 text-sm text-gray-600">
                  <span>📅 {b.checkIn} → {b.checkOut}</span>
                  <span className="font-semibold text-gray-800">${b.totalPrice}</span>
                </div>

                {b.status === 'pending' && (
                  <div className="mt-4 flex gap-2">
                    <button
                      onClick={() => handlePay(b.id)}
                      disabled={paying === b.id}
                      className="flex items-center gap-1.5 bg-blue-600 hover:bg-blue-700 text-white text-sm px-4 py-2 rounded-lg transition disabled:opacity-50"
                    >
                      <CreditCard size={15} />
                      {paying === b.id ? 'Processing…' : 'Pay now'}
                    </button>
                    <button
                      onClick={() => handleCancel(b.id)}
                      disabled={cancelling === b.id}
                      className="flex items-center gap-1.5 border border-red-300 text-red-600 hover:bg-red-50 text-sm px-4 py-2 rounded-lg transition disabled:opacity-50"
                    >
                      <XCircle size={15} />
                      {cancelling === b.id ? 'Cancelling…' : 'Cancel'}
                    </button>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
