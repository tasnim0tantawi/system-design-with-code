import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { hotels, bookings, reviews } from '../api'
import { useAuth } from '../AuthContext'
import { MapPin, Star, Bed, Calendar, CreditCard, MessageSquare, ChevronLeft } from 'lucide-react'

export default function HotelDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [hotel, setHotel] = useState(null)
  const [reviewList, setReviewList] = useState([])
  const [loading, setLoading] = useState(true)
  const [bookingForm, setBookingForm] = useState({ roomId: '', checkIn: '', checkOut: '' })
  const [bookingMsg, setBookingMsg] = useState(null)
  const [reviewForm, setReviewForm] = useState({ bookingId: '', rating: 5, comment: '' })
  const [reviewMsg, setReviewMsg] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    Promise.all([hotels.get(id), hotels.reviews(id)])
      .then(([h, r]) => { setHotel(h); setReviewList(r.items || []) })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [id])

  async function handleBook(e) {
    e.preventDefault()
    setSubmitting(true)
    setBookingMsg(null)
    try {
      const b = await bookings.create(id, Number(bookingForm.roomId), bookingForm.checkIn, bookingForm.checkOut)
      setBookingMsg({ type: 'success', text: `Booking #${b.id} created! Total: $${b.totalPrice}. Go to My Bookings to pay.` })
      setBookingForm({ roomId: '', checkIn: '', checkOut: '' })
    } catch (err) {
      setBookingMsg({ type: 'error', text: err.message })
    } finally {
      setSubmitting(false)
    }
  }

  async function handleReview(e) {
    e.preventDefault()
    setSubmitting(true)
    setReviewMsg(null)
    try {
      await reviews.create(id, Number(reviewForm.bookingId), reviewForm.rating, reviewForm.comment)
      setReviewMsg({ type: 'success', text: 'Review submitted!' })
      const r = await hotels.reviews(id)
      setReviewList(r.items || [])
      setReviewForm({ bookingId: '', rating: 5, comment: '' })
    } catch (err) {
      setReviewMsg({ type: 'error', text: err.message })
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div className="max-w-4xl mx-auto px-4 py-12 text-center text-gray-500">Loading…</div>
  if (!hotel) return <div className="max-w-4xl mx-auto px-4 py-12 text-center text-gray-500">Hotel not found.</div>

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <button onClick={() => navigate(-1)} className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-800 mb-5 transition">
        <ChevronLeft size={16} /> Back
      </button>

      <div className="bg-white rounded-2xl border border-gray-200 p-6 mb-6">
        <div className="flex items-start justify-between flex-wrap gap-3">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{hotel.name}</h1>
            <div className="flex items-center gap-1 text-gray-500 text-sm mt-1">
              <MapPin size={14} /> {hotel.location}
            </div>
          </div>
          <span className="bg-green-100 text-green-700 text-xs font-medium px-3 py-1 rounded-full capitalize">
            {hotel.status}
          </span>
        </div>

        <div className="flex items-center gap-1 mt-3">
          {[...Array(5)].map((_, i) => (
            <Star key={i} size={16} className={i < hotel.stars ? 'text-amber-400 fill-amber-400' : 'text-gray-200 fill-gray-200'} />
          ))}
          <span className="text-sm text-gray-500 ml-1">{hotel.stars}-star {hotel.type?.toLowerCase()}</span>
        </div>

        {hotel.description && <p className="text-gray-600 text-sm mt-3">{hotel.description}</p>}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {user && (
          <div className="bg-white rounded-2xl border border-gray-200 p-6">
            <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Bed size={18} className="text-blue-600" /> Book a Room
            </h2>
            <form onSubmit={handleBook} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Room ID</label>
                <input
                  type="number"
                  placeholder="e.g. 1"
                  value={bookingForm.roomId}
                  onChange={(e) => setBookingForm({ ...bookingForm, roomId: e.target.value })}
                  required
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1 flex items-center gap-1"><Calendar size={12} />Check-in</label>
                  <input
                    type="date"
                    value={bookingForm.checkIn}
                    onChange={(e) => setBookingForm({ ...bookingForm, checkIn: e.target.value })}
                    required
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1 flex items-center gap-1"><Calendar size={12} />Check-out</label>
                  <input
                    type="date"
                    value={bookingForm.checkOut}
                    onChange={(e) => setBookingForm({ ...bookingForm, checkOut: e.target.value })}
                    required
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
              {bookingMsg && (
                <p className={`text-sm px-3 py-2 rounded-lg ${bookingMsg.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'}`}>
                  {bookingMsg.text}
                </p>
              )}
              <button
                type="submit"
                disabled={submitting}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg text-sm font-medium transition disabled:opacity-50 flex items-center justify-center gap-2"
              >
                <CreditCard size={16} /> {submitting ? 'Booking…' : 'Book now'}
              </button>
            </form>
          </div>
        )}

        {user && (
          <div className="bg-white rounded-2xl border border-gray-200 p-6">
            <h2 className="font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <MessageSquare size={18} className="text-blue-600" /> Write a Review
            </h2>
            <form onSubmit={handleReview} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Booking ID</label>
                <input
                  type="number"
                  placeholder="e.g. 1"
                  value={reviewForm.bookingId}
                  onChange={(e) => setReviewForm({ ...reviewForm, bookingId: e.target.value })}
                  required
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Rating</label>
                <div className="flex gap-1">
                  {[1, 2, 3, 4, 5].map((s) => (
                    <button
                      key={s}
                      type="button"
                      onClick={() => setReviewForm({ ...reviewForm, rating: s })}
                    >
                      <Star size={24} className={s <= reviewForm.rating ? 'text-amber-400 fill-amber-400' : 'text-gray-300 fill-gray-300'} />
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">Comment</label>
                <textarea
                  rows={3}
                  value={reviewForm.comment}
                  onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value })}
                  placeholder="Share your experience…"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>
              {reviewMsg && (
                <p className={`text-sm px-3 py-2 rounded-lg ${reviewMsg.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'}`}>
                  {reviewMsg.text}
                </p>
              )}
              <button
                type="submit"
                disabled={submitting}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg text-sm font-medium transition disabled:opacity-50"
              >
                {submitting ? 'Submitting…' : 'Submit review'}
              </button>
            </form>
          </div>
        )}
      </div>

      <div className="mt-6 bg-white rounded-2xl border border-gray-200 p-6">
        <h2 className="font-semibold text-gray-900 mb-4">
          Guest Reviews <span className="text-gray-400 font-normal">({reviewList.length})</span>
        </h2>
        {reviewList.length === 0 ? (
          <p className="text-sm text-gray-400">No reviews yet. Be the first!</p>
        ) : (
          <div className="space-y-4">
            {reviewList.map((r) => (
              <div key={r.id} className="border-b border-gray-100 last:border-0 pb-4 last:pb-0">
                <div className="flex items-center gap-2 mb-1">
                  <div className="flex">
                    {[...Array(5)].map((_, i) => (
                      <Star key={i} size={13} className={i < r.rating ? 'text-amber-400 fill-amber-400' : 'text-gray-200 fill-gray-200'} />
                    ))}
                  </div>
                  <span className="text-xs text-gray-400">{new Date(r.createdAt).toLocaleDateString()}</span>
                </div>
                <p className="text-sm text-gray-700">{r.comment}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
