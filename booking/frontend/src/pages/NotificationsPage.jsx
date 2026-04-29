import { useState, useEffect } from 'react'
import { notifications } from '../api'
import { useAuth } from '../AuthContext'
import { Bell, CheckCheck } from 'lucide-react'

export default function NotificationsPage() {
  const { user } = useAuth()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      const res = await notifications.list(user.userId)
      setList(res.items || [])
    } catch {
    } finally {
      setLoading(false)
    }
  }

  async function handleRead(notifId) {
    try {
      await notifications.markRead(user.userId, notifId)
      setList((prev) => prev.map((n) => n.id === notifId ? { ...n, readAt: new Date().toISOString() } : n))
    } catch {}
  }

  if (loading) return <div className="max-w-2xl mx-auto px-4 py-12 text-center text-gray-500">Loading…</div>

  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        <Bell className="text-blue-600" size={22} /> Notifications
      </h1>

      {list.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <Bell size={40} className="mx-auto mb-3 opacity-30" />
          <p>No notifications yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {list.map((n) => (
            <div
              key={n.id}
              className={`bg-white rounded-2xl border p-4 transition ${n.readAt ? 'border-gray-100 opacity-60' : 'border-blue-200 shadow-sm'}`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-medium bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                      {n.type?.replace(/_/g, ' ')}
                    </span>
                    {!n.readAt && <span className="w-2 h-2 rounded-full bg-blue-500 flex-shrink-0" />}
                  </div>
                  <p className="text-sm text-gray-800">{n.message}</p>
                  <p className="text-xs text-gray-400 mt-1">{new Date(n.createdAt).toLocaleString()}</p>
                </div>
                {!n.readAt && (
                  <button
                    onClick={() => handleRead(n.id)}
                    className="flex-shrink-0 flex items-center gap-1 text-xs text-blue-600 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded-lg transition"
                  >
                    <CheckCheck size={13} /> Mark read
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
