import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { hotels } from '../api'
import { Search, Star, MapPin, Hotel } from 'lucide-react'

export default function HotelsPage() {
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [location, setLocation] = useState('')
  const [checkIn, setCheckIn] = useState('')
  const [checkOut, setCheckOut] = useState('')
  const [searched, setSearched] = useState(false)

  useEffect(() => {
    load()
  }, [])

  async function load() {
    setLoading(true)
    try {
      const res = await hotels.search({})
      setResults(res.items || [])
    } catch {
      setError('Failed to load hotels')
    } finally {
      setLoading(false)
    }
  }

  async function handleSearch(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setSearched(true)
    try {
      const res = await hotels.search({
        location: location || undefined,
        checkIn: checkIn || undefined,
        checkOut: checkOut || undefined,
      })
      setResults(res.items || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Find your perfect stay</h1>
        <p className="text-gray-500 text-sm">Search from hundreds of hotels worldwide</p>
      </div>

      <form onSubmit={handleSearch} className="bg-white rounded-2xl border border-gray-200 p-4 mb-8 flex flex-wrap gap-3 items-end">
        <div className="flex-1 min-w-40">
          <label className="block text-xs font-medium text-gray-600 mb-1">Location</label>
          <input
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="New York, Paris…"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="min-w-36">
          <label className="block text-xs font-medium text-gray-600 mb-1">Check-in</label>
          <input
            type="date"
            value={checkIn}
            onChange={(e) => setCheckIn(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="min-w-36">
          <label className="block text-xs font-medium text-gray-600 mb-1">Check-out</label>
          <input
            type="date"
            value={checkOut}
            onChange={(e) => setCheckOut(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <button
          type="submit"
          className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg text-sm font-medium transition"
        >
          <Search size={16} />
          Search
        </button>
      </form>

      {error && <p className="text-red-600 text-sm mb-4">{error}</p>}

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="bg-white rounded-2xl border border-gray-200 p-5 animate-pulse">
              <div className="h-5 bg-gray-200 rounded w-3/4 mb-3" />
              <div className="h-4 bg-gray-200 rounded w-1/2 mb-2" />
              <div className="h-4 bg-gray-200 rounded w-1/3" />
            </div>
          ))}
        </div>
      ) : results.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <Hotel size={40} className="mx-auto mb-3 opacity-30" />
          <p>{searched ? 'No hotels match your search.' : 'No hotels available.'}</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {results.map((h) => (
            <HotelCard key={h.hotelId} hotel={h} />
          ))}
        </div>
      )}
    </div>
  )
}

function HotelCard({ hotel }) {
  return (
    <Link
      to={`/hotels/${hotel.hotelId}`}
      className="bg-white rounded-2xl border border-gray-200 p-5 hover:shadow-md hover:border-blue-300 transition group"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="bg-blue-50 rounded-xl p-2 group-hover:bg-blue-100 transition">
          <Hotel size={20} className="text-blue-600" />
        </div>
        <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full capitalize">
          {hotel.type?.toLowerCase()}
        </span>
      </div>
      <h3 className="font-semibold text-gray-900 mb-1">{hotel.name}</h3>
      <div className="flex items-center gap-1 text-gray-500 text-sm mb-2">
        <MapPin size={13} />
        {hotel.location}
      </div>
      <div className="flex items-center justify-between mt-3">
        <div className="flex items-center gap-0.5">
          {[...Array(5)].map((_, i) => (
            <Star
              key={i}
              size={13}
              className={i < hotel.stars ? 'text-amber-400 fill-amber-400' : 'text-gray-200 fill-gray-200'}
            />
          ))}
        </div>
        {hotel.minPrice > 0 && (
          <span className="text-sm font-semibold text-blue-600">
            from ${hotel.minPrice}<span className="font-normal text-gray-400">/night</span>
          </span>
        )}
      </div>
    </Link>
  )
}
