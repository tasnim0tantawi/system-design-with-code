import { useState } from 'react'
import {
  Link as LinkIcon,
  Copy,
  Check,
  BarChart3,
  ExternalLink,
  Loader2,
  Search,
} from 'lucide-react'

export default function App() {
  return (
    <div className="min-h-full px-4 py-10 max-w-3xl mx-auto">
      <header className="mb-10 text-center">
        <h1 className="text-4xl font-bold tracking-tight text-white flex items-center justify-center gap-2">
          <LinkIcon className="w-8 h-8 text-indigo-400" />
          URL Shortener
        </h1>
        <p className="text-slate-400 mt-2 text-sm">
          Spring Boot 4 + Cassandra + Kafka backend
        </p>
      </header>

      <div className="space-y-6">
        <ShortenCard />
        <LookupCard />
      </div>
    </div>
  )
}

function Card({ children }) {
  return (
    <div className="bg-slate-800/60 backdrop-blur border border-slate-700 rounded-xl p-6 shadow-lg">
      {children}
    </div>
  )
}

function Label({ children }) {
  return <label className="block text-sm font-medium text-slate-300 mb-1.5">{children}</label>
}

function Input(props) {
  return (
    <input
      {...props}
      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
    />
  )
}

function Button({ loading, children, ...rest }) {
  return (
    <button
      {...rest}
      disabled={loading || rest.disabled}
      className="w-full bg-indigo-500 hover:bg-indigo-400 disabled:bg-slate-600 disabled:cursor-not-allowed text-white font-medium rounded-lg px-4 py-2.5 flex items-center justify-center gap-2 transition"
    >
      {loading && <Loader2 className="w-4 h-4 animate-spin" />}
      {children}
    </button>
  )
}

function ErrorBanner({ message }) {
  if (!message) return null
  return (
    <div className="bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm rounded-lg px-3 py-2 mt-3">
      {message}
    </div>
  )
}

function ShortenCard() {
  const [longUrl, setLongUrl] = useState('')
  const [ttlHours, setTtlHours] = useState(24)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const [copied, setCopied] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setResult(null)
    setCopied(false)
    try {
      const res = await fetch('/api/urls', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ longUrl, ttlHours: Number(ttlHours) }),
      })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || `HTTP ${res.status}`)
      }
      setResult(await res.json())
    } catch (err) {
      setError(err.message || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  async function copy() {
    if (!result?.shortUrl) return
    await navigator.clipboard.writeText(result.shortUrl)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <Card>
      <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
        <LinkIcon className="w-5 h-5 text-indigo-400" />
        Shorten a URL
      </h2>
      <form onSubmit={onSubmit} className="space-y-4">
        <div>
          <Label>Long URL</Label>
          <Input
            type="url"
            required
            placeholder="https://example.com/some/very/long/path"
            value={longUrl}
            onChange={(e) => setLongUrl(e.target.value)}
          />
        </div>
        <div>
          <Label>TTL (hours)</Label>
          <Input
            type="number"
            min="1"
            required
            value={ttlHours}
            onChange={(e) => setTtlHours(e.target.value)}
          />
        </div>
        <Button type="submit" loading={loading}>
          {loading ? 'Shortening…' : 'Shorten'}
        </Button>
      </form>

      <ErrorBanner message={error} />

      {result && (
        <div className="mt-5 bg-slate-900/60 border border-slate-700 rounded-lg p-4">
          <div className="text-xs uppercase tracking-wide text-slate-400 mb-1">Short URL</div>
          <div className="flex items-center gap-2">
            <a
              href={result.shortUrl}
              target="_blank"
              rel="noreferrer"
              className="font-mono text-indigo-300 hover:text-indigo-200 break-all flex-1"
            >
              {result.shortUrl}
            </a>
            <button
              onClick={copy}
              className="p-2 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
              title="Copy"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
            <a
              href={result.shortUrl}
              target="_blank"
              rel="noreferrer"
              className="p-2 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
              title="Open"
            >
              <ExternalLink className="w-4 h-4" />
            </a>
          </div>
          <dl className="mt-3 text-xs text-slate-400 grid grid-cols-2 gap-y-1">
            <dt>Code</dt>
            <dd className="font-mono text-slate-300">{result.shortCode}</dd>
            <dt>Created</dt>
            <dd className="font-mono text-slate-300">{formatDate(result.createdAt)}</dd>
            <dt>Expires</dt>
            <dd className="font-mono text-slate-300">{formatDate(result.expiresAt)}</dd>
          </dl>
        </div>
      )}
    </Card>
  )
}

function LookupCard() {
  const [shortCode, setShortCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [info, setInfo] = useState(null)
  const [stats, setStats] = useState(null)

  async function onSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    setInfo(null)
    setStats(null)
    try {
      const code = shortCode.trim()
      const [infoRes, statsRes] = await Promise.all([
        fetch(`/api/urls/${code}`),
        fetch(`/api/urls/${code}/stats`),
      ])
      if (!infoRes.ok) throw new Error(`Not found: ${code}`)
      setInfo(await infoRes.json())
      if (statsRes.ok) setStats(await statsRes.json())
    } catch (err) {
      setError(err.message || 'Lookup failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card>
      <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
        <BarChart3 className="w-5 h-5 text-indigo-400" />
        Lookup &amp; Stats
      </h2>
      <form onSubmit={onSubmit} className="flex gap-2">
        <Input
          required
          placeholder="short code (e.g. mO)"
          value={shortCode}
          onChange={(e) => setShortCode(e.target.value)}
        />
        <button
          type="submit"
          disabled={loading}
          className="px-4 bg-indigo-500 hover:bg-indigo-400 disabled:bg-slate-600 text-white rounded-lg flex items-center gap-2 transition"
        >
          {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
        </button>
      </form>

      <ErrorBanner message={error} />

      {info && (
        <div className="mt-5 bg-slate-900/60 border border-slate-700 rounded-lg p-4 space-y-2">
          <div className="text-xs uppercase tracking-wide text-slate-400">Long URL</div>
          <a
            href={info.longUrl}
            target="_blank"
            rel="noreferrer"
            className="font-mono text-indigo-300 hover:text-indigo-200 break-all block"
          >
            {info.longUrl}
          </a>
          <dl className="text-xs text-slate-400 grid grid-cols-2 gap-y-1 pt-2">
            <dt>Created</dt>
            <dd className="font-mono text-slate-300">{formatDate(info.createdAt)}</dd>
            <dt>Expires</dt>
            <dd className="font-mono text-slate-300">{formatDate(info.expiresAt)}</dd>
            <dt>Creator IP</dt>
            <dd className="font-mono text-slate-300">{info.creatorIp || '—'}</dd>
          </dl>
        </div>
      )}

      {stats && (
        <div className="mt-4 bg-slate-900/60 border border-slate-700 rounded-lg p-4">
          <div className="flex items-baseline justify-between mb-3">
            <div className="text-xs uppercase tracking-wide text-slate-400">Total Clicks</div>
            <div className="text-2xl font-bold text-white">{stats.totalClicks}</div>
          </div>
          {stats.dailyCounts?.length > 0 ? (
            <DailyChart counts={stats.dailyCounts} />
          ) : (
            <div className="text-sm text-slate-500">No clicks yet.</div>
          )}
        </div>
      )}
    </Card>
  )
}

function DailyChart({ counts }) {
  const max = Math.max(...counts.map((c) => c.count), 1)
  return (
    <div className="space-y-1.5">
      {counts.map((c) => (
        <div key={c.day} className="flex items-center gap-2 text-xs">
          <div className="w-24 font-mono text-slate-400">{c.day}</div>
          <div className="flex-1 bg-slate-800 rounded h-5 overflow-hidden">
            <div
              className="h-full bg-indigo-500"
              style={{ width: `${(c.count / max) * 100}%` }}
            />
          </div>
          <div className="w-10 text-right font-mono text-slate-300">{c.count}</div>
        </div>
      ))}
    </div>
  )
}

function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}
