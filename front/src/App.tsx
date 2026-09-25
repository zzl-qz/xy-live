import { lazy, Suspense, useCallback, useEffect, useRef, useState } from 'react'
import { Link, Route, Routes, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowRight, ArrowUpRight, Bell, Check, ChevronDown, Coins, Compass, Flame, Gamepad2, Headphones, Heart, Menu, Radio, Search, Sparkles, Users, Video, X } from 'lucide-react'
import { api } from './lib/api'
import type { HomePage, LiveRoom, Wallet } from './lib/types'
import { previewNames, previewRooms } from './demo'
const RoomPage = lazy(() => import('./RoomPage'))

export type AppActions = {
  user: HomePage | null
  openLogin: (next?: string) => void
  notify: (message: string, kind?: 'success' | 'error') => void
  openWallet: () => void
}

type Notice = { id: number; message: string; kind: 'success' | 'error' }

function Brand() {
  return <Link className="brand" to="/" aria-label="XY LIVE 首页"><span className="brand-mark"><span /></span><span className="brand-word">XY<span>LIVE</span></span></Link>
}

function Avatar({ src, name, size = 'md' }: { src?: string | null; name?: string | null; size?: 'sm' | 'md' | 'lg' }) {
  return <span className={`avatar avatar-${size}`}>{src ? <img src={src} alt="" onError={event => { event.currentTarget.style.display = 'none' }} /> : null}<span>{(name || 'X').slice(0, 1).toUpperCase()}</span></span>
}

function Header({ user, search, onSearch, onLogin, onWallet, onStart, onLogout }: {
  user: HomePage | null
  search: string
  onSearch: (value: string) => void
  onLogin: () => void
  onWallet: () => void
  onStart: () => void
  onLogout: () => void
}) {
  const location = useLocation()
  const [mobileOpen, setMobileOpen] = useState(false)
  const searchRef = useRef<HTMLInputElement>(null)
  useEffect(() => {
    const focusSearch = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        searchRef.current?.focus()
      }
    }
    window.addEventListener('keydown', focusSearch)
    return () => window.removeEventListener('keydown', focusSearch)
  }, [])
  const onHome = location.pathname === '/'
  return <header className="site-header">
    <div className="header-inner">
      <Brand />
      <nav className={`main-nav ${mobileOpen ? 'mobile-open' : ''}`} aria-label="主导航">
        <Link className={onHome && location.search !== '?type=2' ? 'active' : ''} to="/" onClick={() => setMobileOpen(false)}><Compass size={17} /> 发现</Link>
        <Link className={onHome && location.search === '?type=2' ? 'active' : ''} to="/?type=2" onClick={() => setMobileOpen(false)}><Flame size={17} /> PK 对战</Link>
      </nav>
      {onHome && <label className="header-search"><Search size={18} /><input ref={searchRef} value={search} onChange={event => onSearch(event.target.value)} placeholder="搜索直播间" aria-label="搜索直播间" /><kbd>Ctrl K</kbd></label>}
      <div className="header-actions">
        {user?.loginStatus && <button className="icon-button wallet-button" onClick={onWallet} title="我的钱包"><Coins size={19} /></button>}
        <button className="start-button" onClick={onStart}><Radio size={17} /> 开启直播</button>
        {user?.loginStatus ? <div className="user-dropdown"><button className="user-trigger" title="账号菜单"><Avatar src={user.avatar} name={user.nickName} size="sm" /><span>{user.nickName || '我的账号'}</span><ChevronDown size={14} /></button><div className="user-menu"><button onClick={onWallet}><Coins size={16} /> 我的钱包</button><button onClick={onLogout}><X size={16} /> 退出登录</button></div></div> : <button className="login-button" onClick={onLogin}>登录 / 注册 <ArrowUpRight size={15} /></button>}
        <button className="mobile-menu-button icon-button" onClick={() => setMobileOpen(!mobileOpen)} aria-label="打开菜单"><Menu size={21} /></button>
      </div>
    </div>
  </header>
}

function formatCount(value?: number | null) {
  if (!value) return '0'
  return value >= 10000 ? `${(value / 10000).toFixed(1)}万` : value.toLocaleString('zh-CN')
}

function Cover({ room, index, featured = false }: { room: LiveRoom; index: number; featured?: boolean }) {
  const [failed, setFailed] = useState(false)
  const palette = index % 6
  return <div className={`room-cover art-${palette} ${featured ? 'featured-cover' : ''}`}>
    {room.covertImg && !failed ? <img className="cover-image" src={room.covertImg} alt="" onError={() => setFailed(true)} /> : <div className="cover-illustration" aria-hidden="true"><span className="cover-orbit orbit-one" /><span className="cover-orbit orbit-two" /><span className="cover-glow" /><span className="cover-glyph">{room.type === 2 ? 'PK' : ['♪', '✦', '◈', '◐', '♫', '✳'][palette]}</span></div>}
    <span className="live-badge"><i /> LIVE</span>
    <span className="cover-viewers"><Users size={13} /> {formatCount(room.watchNum)}</span>
    <span className="cover-play" aria-hidden="true"><ArrowUpRight size={20} /></span>
  </div>
}

function RoomCard({ room, index, onClick }: { room: LiveRoom; index: number; onClick: () => void }) {
  const name = previewRooms.includes(room) ? previewNames[previewRooms.indexOf(room)] : `主播 ${String(room.anchorId).slice(-4)}`
  return <button className="room-card" onClick={onClick}>
    <Cover room={room} index={index} />
    <div className="card-bottom"><Avatar name={name} size="sm" /><div className="card-copy"><strong>{room.roomName || `直播间 ${room.id}`}</strong><span>{name} <span className="card-dot">·</span> {room.type === 2 ? 'PK 对战' : '正在直播'}</span></div><ArrowUpRight className="card-arrow" size={18} /></div>
  </button>
}

function Home({ search, onOpenRoom, notify }: { search: string; onOpenRoom: (room: LiveRoom, preview: boolean) => void; notify: AppActions['notify'] }) {
  const [params, setParams] = useSearchParams()
  const type: 1 | 2 = params.get('type') === '2' ? 2 : 1
  const [rooms, setRooms] = useState<LiveRoom[]>([])
  const [page, setPage] = useState(1)
  const [hasNext, setHasNext] = useState(false)
  const [loading, setLoading] = useState(true)
  const [preview, setPreview] = useState(false)
  const [error, setError] = useState('')
  const roomSection = useRef<HTMLElement>(null)
  const requestSeq = useRef(0)

  const fetchRooms = useCallback(async (nextPage: number, append = false) => {
    const request = ++requestSeq.current
    setLoading(true)
    setError('')
    if (!append) setRooms([])
    try {
      const result = await api.rooms(type, nextPage, 12)
      if (request !== requestSeq.current) return
      setPreview(false)
      const typedRooms = result.list.slice(0, 12).map(item => ({ ...item, type }))
      setRooms(current => append ? [...current, ...typedRooms].filter((item, index, all) => all.findIndex(other => other.id === item.id) === index) : typedRooms)
      setHasNext(result.hasNext)
      setPage(nextPage)
    } catch (cause) {
      if (request !== requestSeq.current) return
      const message = cause instanceof Error ? cause.message : '暂时无法获取直播间'
      if (!append) {
        setRooms(previewRooms.filter(room => room.type === type))
        setPreview(true)
      }
      setError(message)
    } finally {
      if (request === requestSeq.current) setLoading(false)
    }
  }, [type])

  useEffect(() => { void fetchRooms(1) }, [fetchRooms])
  const visible = rooms.filter(room => `${room.roomName} ${room.anchorId}`.toLowerCase().includes(search.trim().toLowerCase()))
  return <main className="home-page">
    <section className="hero-section">
      <div className="hero-halo halo-one" /><div className="hero-halo halo-two" />
      <div className="hero-copy"><span className="eyebrow"><span className="pulse-dot" /> LIVE NOW · 每一刻都在发生</span><h1>热爱正在<br /><em>此刻发生。</em></h1><p>遇见同频的人，走进正在发生的故事。<br />音乐、游戏、分享与惊喜，都在这里实时上演。</p><div className="hero-actions"><button className="primary-button" onClick={() => roomSection.current?.scrollIntoView({ behavior: 'smooth' })}>探索直播间 <ArrowRight size={19} /></button><span className="hero-side-note"><span className="online-avatars"><i>鹿</i><i>北</i><i>满</i></span><span>一起，加入现场</span></span></div><div className="hero-metrics"><div><strong>24/7</strong><span>精彩不间断</span></div><div><strong>LIVE</strong><span>真实互动体验</span></div><div><strong>∞</strong><span>总有新的可能</span></div></div></div>
      <div className="hero-showcase" aria-hidden="true"><div className="showcase-ambient" /><div className="showcase-card back-card"><span>LIVE SESSION</span><strong>THE NIGHT<br />IS OURS</strong></div><div className="showcase-card main-card"><div className="showcase-top"><span className="showcase-live"><i /> LIVE</span><span>↗&nbsp; 12.8K</span></div><div className="showcase-art"><span className="showcase-ring ring-a" /><span className="showcase-ring ring-b" /><span className="showcase-core">✦</span></div><div className="showcase-bottom"><span>NOW PLAYING</span><strong>在这里，遇见正在发光的你</strong><small>XY LIVE · 频道精选</small></div></div><div className="floating-note note-one"><Headphones size={17} /> 音乐正在播放</div><div className="floating-note note-two"><span>✦</span> 连接每一种热爱</div></div>
    </section>

    <section className="discover-section" ref={roomSection}>
      <div className="section-heading"><div><span className="section-kicker"><Sparkles size={15} /> THE LIVE SCENE</span><h2>发现<span>正在直播</span>的精彩</h2><p>一个入口，解锁每一种正在发生的快乐。</p></div><span className="section-decoration">01 / DISCOVER</span></div>
      <div className="discover-toolbar"><div className="category-tabs"><button className={type === 1 ? 'selected' : ''} onClick={() => setParams({})}><Radio size={17} /> 推荐直播</button><button className={type === 2 ? 'selected' : ''} onClick={() => setParams({ type: '2' })}><Gamepad2 size={17} /> PK 对战</button></div><div className="toolbar-right"><span className="sort-chip"><span className="tiny-dot" /> {preview ? '界面预览' : `${rooms.length} 个直播间`}</span></div></div>
      {preview && <div className="preview-banner"><Bell size={18} /><span>直播列表请求失败，正在展示界面预览。原因：{error}。真实互动需连接后端。</span><button onClick={() => void fetchRooms(1)}>重新连接 <ArrowRight size={15} /></button></div>}
      {!preview && error && <div className="inline-error">{error} <button onClick={() => void fetchRooms(1)}>重试</button></div>}
      {loading && !rooms.length ? <div className="card-grid">{Array.from({ length: 8 }, (_, index) => <div className="skeleton-card" key={index}><span /><i /><i /></div>)}</div> : visible.length ? <div className="card-grid">{visible.map((room, index) => <RoomCard key={room.id} room={room} index={index} onClick={() => onOpenRoom(room, preview)} />)}</div> : <div className="empty-state"><div className="empty-orb"><Search size={30} /></div><h3>{search ? '没有找到匹配的直播间' : '此刻还没有直播'}</h3><p>{search ? '换个关键词，再看看新的精彩。' : '稍后再来，或由主播开启第一场直播。'}</p>{search && <button className="text-button" onClick={() => notify('清空顶部搜索框后可查看全部直播间')}>查看全部 <ArrowRight size={16} /></button>}</div>}
      {!preview && hasNext && !search && <div className="more-wrap"><button className="more-button" disabled={loading} onClick={() => void fetchRooms(page + 1, true)}>{loading ? '加载中…' : '加载更多直播'} <ArrowRight size={17} /></button></div>}
    </section>

    <section className="experience-strip"><div className="experience-icon"><Heart size={26} fill="currentColor" /></div><div><span>STAY IN THE MOMENT</span><h3>有趣的相遇，从走进直播间开始。</h3><p>发一条弹幕，送一份心意，和大家一起成为故事的一部分。</p></div><button onClick={() => roomSection.current?.scrollIntoView({ behavior: 'smooth' })}>开始探索 <ArrowUpRight size={19} /></button></section>
    <footer className="site-footer"><Brand /><span>让热爱，此刻发生。</span><small>© {new Date().getFullYear()} XY LIVE</small></footer>
  </main>
}

function LoginModal({ onClose, onSuccess, notify }: { onClose: () => void; onSuccess: () => void; notify: AppActions['notify'] }) {
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [seconds, setSeconds] = useState(0)
  const [busy, setBusy] = useState(false)
  useEffect(() => { if (seconds <= 0) return; const timer = window.setTimeout(() => setSeconds(seconds - 1), 1000); return () => clearTimeout(timer) }, [seconds])
  async function sendCode() {
    if (!/^1\d{10}$/.test(phone)) return notify('请输入有效的 11 位手机号', 'error')
    setBusy(true)
    try { await api.sendCode(phone); setSeconds(60); notify('验证码已发送', 'success') } catch (error) { notify(error instanceof Error ? error.message : '发送失败', 'error') } finally { setBusy(false) }
  }
  async function login(event: React.FormEvent) {
    event.preventDefault()
    if (!/^1\d{10}$/.test(phone) || !/^\d{4,6}$/.test(code)) return notify('请检查手机号和验证码', 'error')
    setBusy(true)
    try { await api.login(phone, Number(code)); notify('欢迎来到 XY LIVE', 'success'); onSuccess() } catch (error) { notify(error instanceof Error ? error.message : '登录失败', 'error') } finally { setBusy(false) }
  }
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}><section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="login-title"><button className="modal-close" onClick={onClose} aria-label="关闭"><X size={20} /></button><div className="auth-art"><div className="auth-orbit"><span>✦</span></div><span className="auth-art-label">WELCOME TO XY LIVE</span><strong>每一刻，都值得在场。</strong></div><div className="auth-form"><span className="modal-kicker">JOIN THE MOMENT</span><h2 id="login-title">登录 XY LIVE</h2><p>用手机号登录，和喜欢的人实时相遇。</p><form onSubmit={login}><label>手机号<input value={phone} onChange={event => setPhone(event.target.value.replace(/\D/g, '').slice(0, 11))} inputMode="tel" placeholder="请输入 11 位手机号" autoFocus /></label><label>验证码<div className="code-field"><input value={code} onChange={event => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" placeholder="请输入短信验证码" /><button type="button" disabled={busy || seconds > 0} onClick={() => void sendCode()}>{seconds ? `${seconds}s 后重试` : '获取验证码'}</button></div></label><button className="primary-button auth-submit" disabled={busy} type="submit">{busy ? '请稍候…' : '登录 / 注册'} <ArrowRight size={18} /></button></form><small>首次登录将自动注册账号</small></div></section></div>
}

function WalletModal({ onClose, notify }: { onClose: () => void; notify: AppActions['notify'] }) {
  const [wallet, setWallet] = useState<Wallet | null>(null)
  const [loading, setLoading] = useState(true)
  useEffect(() => { api.balance().then(setWallet).catch(error => notify(error instanceof Error ? error.message : '余额获取失败', 'error')).finally(() => setLoading(false)) }, [notify])
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}><section className="simple-modal" role="dialog" aria-modal="true" aria-labelledby="wallet-title"><button className="modal-close" onClick={onClose} aria-label="关闭"><X size={20} /></button><div className="wallet-symbol"><Coins size={30} /></div><span className="modal-kicker">MY WALLET</span><h2 id="wallet-title">我的星币</h2><p>为喜欢的直播和创作者送上一份心意。</p><div className="balance-card"><span>当前余额</span><strong>{loading ? '···' : (wallet?.currentBalance ?? 0).toLocaleString('zh-CN')} <small>星币</small></strong><span className="balance-spark">✦</span></div><div className="wallet-note"><Check size={17} /> 余额由后端账户实时提供</div></section></div>
}

function StartModal({ onClose, onStart, busy }: { onClose: () => void; onStart: (type: 1 | 2) => void; busy: boolean }) {
  const [type, setType] = useState<1 | 2>(1)
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}><section className="simple-modal start-modal" role="dialog" aria-modal="true" aria-labelledby="start-title"><button className="modal-close" onClick={onClose} aria-label="关闭"><X size={20} /></button><div className="wallet-symbol start-symbol"><Video size={29} /></div><span className="modal-kicker">GO LIVE</span><h2 id="start-title">开启你的直播间</h2><p>选择直播形式，和观众开启实时互动。</p><div className="start-options"><button className={type === 1 ? 'chosen' : ''} onClick={() => setType(1)}><Radio size={22} /><strong>普通直播</strong><small>自由分享你的此刻</small></button><button className={type === 2 ? 'chosen' : ''} onClick={() => setType(2)}><Flame size={22} /><strong>PK 对战</strong><small>开启双人热力挑战</small></button></div><div className="start-hint">开播会创建直播间；画面推流需要另行接入媒体服务。</div><button className="primary-button start-submit" disabled={busy} onClick={() => onStart(type)}>{busy ? '正在创建…' : '创建直播间'} <ArrowRight size={18} /></button></section></div>
}

export default function App() {
  const navigate = useNavigate()
  const [user, setUser] = useState<HomePage | null>(null)
  const [loginOpen, setLoginOpen] = useState(false)
  const [walletOpen, setWalletOpen] = useState(false)
  const [startOpen, setStartOpen] = useState(false)
  const [startBusy, setStartBusy] = useState(false)
  const [search, setSearch] = useState('')
  const [notices, setNotices] = useState<Notice[]>([])
  const pendingPath = useRef<string | null>(null)
  const pendingRoom = useRef<LiveRoom | null>(null)
  const notify = useCallback((message: string, kind: 'success' | 'error' = 'success') => {
    const id = Date.now() + Math.random()
    setNotices(items => [...items, { id, message, kind }])
    window.setTimeout(() => setNotices(items => items.filter(item => item.id !== id)), 4200)
  }, [])
  const refreshUser = useCallback(async () => {
    try { const result = await api.home(); setUser(result.loginStatus ? result : null) } catch { setUser(null) }
  }, [])
  useEffect(() => { if (localStorage.getItem('xy-token')) void refreshUser() }, [refreshUser])
  const openLogin = useCallback((next?: string) => { pendingPath.current = next || null; setLoginOpen(true) }, [])
  async function afterLogin() { await refreshUser(); setLoginOpen(false); if (pendingPath.current) navigate(pendingPath.current, { state: pendingRoom.current ? { room: pendingRoom.current } : undefined }); pendingPath.current = null; pendingRoom.current = null }
  function openRoom(room: LiveRoom, preview: boolean) { const path = `/${preview ? 'preview' : 'room'}/${room.id}${room.type === 2 ? '?type=2' : ''}`; if (preview || user?.loginStatus) navigate(path, { state: { room } }); else { pendingRoom.current = room; openLogin(path) } }
  function logout() { localStorage.removeItem('xy-token'); setUser(null); navigate('/'); notify('已退出登录') }
  function requestStart() { if (!user?.loginStatus) return openLogin(); if (!user.showStartLivingBtn) return notify('当前账号未开通主播权限', 'error'); setStartOpen(true) }
  async function startRoom(type: 1 | 2) {
    setStartBusy(true)
    try { const result = await api.startRoom(type); setStartOpen(false); navigate(`/room/${result.roomId}${type === 2 ? '?type=2' : ''}`); notify('直播间已创建', 'success') } catch (error) { notify(error instanceof Error ? error.message : '开播失败', 'error') } finally { setStartBusy(false) }
  }
  const actions: AppActions = { user, openLogin, notify, openWallet: () => setWalletOpen(true) }
  const roomView = <Suspense fallback={<main className="room-loading"><div className="loading-pulse">✦</div><p>正在进入直播间…</p></main>}><RoomPage {...actions} /></Suspense>
  return <><Header user={user} search={search} onSearch={setSearch} onLogin={() => openLogin()} onWallet={() => setWalletOpen(true)} onStart={requestStart} onLogout={logout} /><Routes><Route path="/" element={<Home search={search} onOpenRoom={openRoom} notify={notify} />} /><Route path="/room/:roomId" element={roomView} /><Route path="/preview/:roomId" element={roomView} /><Route path="*" element={<main className="not-found"><div className="empty-orb"><Compass size={30} /></div><h1>这里暂时没有直播</h1><p>回到发现页，看看正在发生的精彩。</p><Link className="primary-button" to="/">返回发现 <ArrowRight size={18} /></Link></main>} /></Routes>{loginOpen && <LoginModal onClose={() => setLoginOpen(false)} onSuccess={() => void afterLogin()} notify={notify} />}{walletOpen && <WalletModal onClose={() => setWalletOpen(false)} notify={notify} />}{startOpen && <StartModal onClose={() => setStartOpen(false)} onStart={type => void startRoom(type)} busy={startBusy} />}<div className="toast-stack" aria-live="polite">{notices.map(item => <div className={`toast ${item.kind}`} key={item.id}>{item.kind === 'success' ? <Check size={17} /> : <Bell size={17} />}{item.message}</div>)}</div></>
}
