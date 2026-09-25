import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Bell, ChevronRight, Coins, Copy, Gift as GiftIcon, Heart, MessageCircle, Minus, Plus, Radio, RefreshCw, Send, ShoppingBag, Sparkles, Users, X } from 'lucide-react'
import type { AppActions } from './App'
import { api } from './lib/api'
import { createRoomIm } from './lib/im'
import type { ChatMessage, ImStatus } from './lib/im'
import type { Cart, Gift, LiveRoom, Product, RoomConfig } from './lib/types'
import { previewRoom, previewRooms } from './demo'
import StreamPlayer from './components/StreamPlayer'

const giftSymbols = ['💜', '🌹', '✨', '🦋', '🎆', '🎁', '👑', '💫']
const previewChat: ChatMessage[] = [
  { id: 'preview-1', userId: '10', roomId: 0, senderName: '星星收藏家', content: '今晚的氛围也太好了 ✨', createdAt: Date.now() - 160000 },
  { id: 'preview-2', userId: '11', roomId: 0, senderName: '小鹿', content: '刚进来就不想走啦', createdAt: Date.now() - 120000 },
  { id: 'preview-3', userId: '12', roomId: 0, senderName: '余温', content: '主播今天准备了什么惊喜？', createdAt: Date.now() - 75000 },
]

function fmt(value?: number | null) {
  if (!value) return '0'
  return value >= 10000 ? `${(value / 10000).toFixed(1)}万` : value.toLocaleString('zh-CN')
}

function Avatar({ src, name }: { src?: string | null; name?: string | null }) {
  return <span className="room-avatar">{src ? <img src={src} alt="" onError={event => { event.currentTarget.style.display = 'none' }} /> : null}<span>{(name || 'X').slice(0, 1)}</span></span>
}

function RoomWelcome({ roomId, preview }: { roomId: number; preview: boolean }) {
  return <div className="room-welcome"><span className="welcome-spark">✦</span><strong>欢迎来到直播间</strong><p>{preview ? '这里展示聊天界面预览。连接后端后可实时互动。' : '文明交流，尊重主播与其他观众。聊天消息从进入房间后开始显示。'}</p><small>ROOM · {roomId}</small></div>
}

function ChatPanel({ roomId, preview, user, messages, status, onSend, onReconnect, redPacketCode, onGrab, grabbing }: {
  roomId: number; config: RoomConfig; preview: boolean; user: AppActions['user']; messages: ChatMessage[]; status: ImStatus; onSend: (text: string) => void; onReconnect: () => void; redPacketCode: string; onGrab: () => void; grabbing: boolean
}) {
  const [text, setText] = useState('')
  const bottom = useRef<HTMLDivElement>(null)
  useEffect(() => { bottom.current?.scrollIntoView({ behavior: 'smooth', block: 'end' }) }, [messages.length])
  function submit(event: React.FormEvent) { event.preventDefault(); if (!text.trim()) return; onSend(text.trim()); setText('') }
  return <aside className="chat-panel"><div className="chat-header"><div><span className="chat-header-icon"><MessageCircle size={20} /></span><div><strong>直播互动</strong><small>和大家一起聊聊</small></div></div><span className={`connection-indicator ${status}`}><i />{preview ? '预览' : status === 'connected' ? '实时连接' : status === 'connecting' ? '连接中' : '已断开'}</span></div><div className="chat-messages"><RoomWelcome roomId={roomId} preview={preview} />{redPacketCode && <button className="red-packet-card" onClick={onGrab} disabled={grabbing}><span>🧧</span><div><strong>红包雨来了！</strong><small>{grabbing ? '正在领取…' : '点击领取主播的惊喜'}</small></div><ChevronRight size={18} /></button>}{messages.map(message => <div className={`chat-line ${message.own ? 'own' : ''}`} key={message.id}><Avatar src={message.senderAvatar} name={message.senderName} /><div><span className="chat-name">{message.senderName}{message.own && <i>我</i>}</span><p>{message.content}</p></div></div>)}<div ref={bottom} /></div>{!preview && (status === 'error' || status === 'disconnected') && <button className="chat-reconnect" onClick={onReconnect}><RefreshCw size={14} /> 聊天未连接，点击重试</button>}<div className="chat-compose"><div className="chat-tips"><span><span className="tiny-dot" /> 友善发言，分享美好</span><span>{text.length}/140</span></div><form onSubmit={submit}><input value={text} maxLength={140} onChange={event => setText(event.target.value)} placeholder={preview ? '在预览中试发一条消息…' : user?.loginStatus ? '说点什么吧…' : '登录后参与互动'} disabled={!preview && (!user?.loginStatus || status !== 'connected')} aria-label="聊天内容" /><button type="submit" disabled={!text.trim() || (!preview && status !== 'connected')} aria-label="发送消息"><Send size={18} /></button></form></div></aside>
}

function GiftPanel({ gifts, selected, onSelect, onSend, sending, balance, preview, isAnchor, pkRoom, onWallet }: {
  gifts: Gift[]; selected: Gift | null; onSelect: (gift: Gift) => void; onSend: () => void; sending: boolean; balance: number | null; preview: boolean; isAnchor: boolean; pkRoom: boolean; onWallet: () => void
}) {
  return <div className="panel-content"><div className="panel-heading"><div><span className="section-kicker"><Sparkles size={14} /> SEND A LITTLE JOY</span><h3>把喜欢，变成看得见的心意</h3></div><button className="balance-pill" onClick={onWallet}><Coins size={16} /> {balance === null ? '我的星币' : `${fmt(balance)} 星币`} <ChevronRight size={14} /></button></div>{gifts.length ? <div className="gift-grid">{gifts.filter(gift => gift.status !== 0).map((gift, index) => <button key={gift.giftId} className={`gift-item ${selected?.giftId === gift.giftId ? 'selected' : ''}`} onClick={() => onSelect(gift)}><span className="gift-art">{gift.coverImgUrl ? <img src={gift.coverImgUrl} alt="" /> : giftSymbols[index % giftSymbols.length]}</span><strong>{gift.giftName}</strong><small><Coins size={12} /> {gift.price}</small></button>)}</div> : <div className="panel-empty"><GiftIcon size={26} /><strong>{preview ? '连接服务后展示真实礼物' : '暂无可用礼物'}</strong><span>礼物配置由后端实时提供</span></div>}<div className="panel-action-row"><p>{isAnchor ? '主播不能给自己的直播间送礼物。' : selected ? `已选 ${selected.giftName} · ${selected.price} 星币${pkRoom ? ' · 暂不计入 PK' : ''}` : pkRoom ? '礼物赠予主播，暂不计入 PK 进度。' : '选一份礼物，给此刻加点光。'}</p><button className="primary-button" onClick={onSend} disabled={!selected || sending || preview || isAnchor}>{sending ? '发送中…' : '送出礼物'} <ArrowRight size={17} /></button></div></div>
}

function ShopPanel({ products, onAdd, busyId, onCart, cartCount, preview }: { products: Product[]; onAdd: (product: Product) => void; busyId: string | null; onCart: () => void; cartCount: number; preview: boolean }) {
  return <div className="panel-content"><div className="panel-heading"><div><span className="section-kicker"><ShoppingBag size={14} /> LIVE PICKS</span><h3>主播正在分享的好物</h3></div><button className="balance-pill" onClick={onCart}><ShoppingBag size={16} /> 购物车 {cartCount > 0 && <span className="count-bubble">{cartCount}</span>} <ChevronRight size={14} /></button></div>{products.length ? <div className="product-grid">{products.map((product, index) => <div className="product-card" key={String(product.skuId)}><div className={`product-art product-art-${index % 4}`}>{product.iconUrl ? <img src={product.iconUrl} alt={product.name} /> : <ShoppingBag size={38} />}</div><div className="product-details"><strong>{product.name}</strong><p>{product.remark || '主播直播间精选好物'}</p><div><span>{product.skuPrice} 星币</span><button disabled={busyId === String(product.skuId) || preview} onClick={() => onAdd(product)}><Plus size={16} /> 加入购物车</button></div></div></div>)}</div> : <div className="panel-empty"><ShoppingBag size={26} /><strong>{preview ? '连接服务后展示真实商品' : '主播暂未上架商品'}</strong><span>好物上架后会出现在这里</span></div>}</div>
}

function CartDrawer({ cart, roomId, busy, onClose, onChange, onRemove, onPrepare, prepared }: {
  cart: Cart | null; roomId: number; busy: boolean; onClose: () => void; onChange: (skuId: string | number, changeType: 1 | 2) => void; onRemove: (skuId: string | number) => void; onPrepare: () => void; prepared: boolean
}) {
  const items = cart?.shopCarItemRespDTOS || []
  const total = items.reduce((sum, item) => sum + item.count * item.skuInfoDTO.skuPrice, 0)
  return <div className="drawer-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}><aside className="cart-drawer" role="dialog" aria-modal="true" aria-label="购物车"><div className="drawer-head"><div><span className="section-kicker">YOUR SELECTION</span><h2>我的购物车</h2><p>来自直播间 #{roomId} 的好物</p></div><button className="icon-button" onClick={onClose} aria-label="关闭购物车"><X size={20} /></button></div><div className="cart-list">{items.length ? items.map(item => <div className="cart-item" key={String(item.skuInfoDTO.skuId)}><div className="cart-product-image">{item.skuInfoDTO.iconUrl ? <img src={item.skuInfoDTO.iconUrl} alt="" /> : <ShoppingBag size={24} />}</div><div className="cart-product-main"><strong>{item.skuInfoDTO.name}</strong><span>{item.skuInfoDTO.skuPrice} 星币</span><div className="cart-item-actions"><div className="quantity-control"><button disabled={busy} onClick={() => item.count <= 1 ? onRemove(item.skuInfoDTO.skuId) : onChange(item.skuInfoDTO.skuId, 2)} aria-label="减少数量"><Minus size={14} /></button><span>{item.count}</span><button disabled={busy} onClick={() => onChange(item.skuInfoDTO.skuId, 1)} aria-label="增加数量"><Plus size={14} /></button></div><button className="remove-link" disabled={busy} onClick={() => onRemove(item.skuInfoDTO.skuId)}>移除</button></div></div></div>) : <div className="cart-empty"><ShoppingBag size={30} /><strong>购物车还是空的</strong><span>去看看主播正在分享什么吧</span></div>}</div><div className={`cart-checkout ${prepared ? 'prepared' : ''}`}><div className="cart-total"><span>合计 <small>共 {items.reduce((sum, item) => sum + item.count, 0)} 件</small></span><strong>{total} 星币</strong></div>{prepared ? <div className="order-notice">预下单请求已提交。后端未返回可核对的订单金额，页面暂不发起支付。</div> : <button className="primary-button" disabled={busy || !items.length} onClick={onPrepare}>{busy ? '处理中…' : '提交订单'} <ArrowRight size={18} /></button>}<small>提交后将根据购物车尝试生成待支付订单。</small></div></aside></div>
}

export default function RoomPage({ user, openLogin, notify, openWallet }: AppActions) {
  const { roomId: rawId } = useParams()
  const roomId = Number(rawId)
  const navigate = useNavigate()
  const location = useLocation()
  const listRoom = (location.state as { room?: LiveRoom } | null)?.room || previewRooms.find(room => room.id === roomId)
  const previewConfig = useMemo(() => location.pathname.startsWith('/preview/') ? previewRoom(roomId) : null, [location.pathname, roomId])
  const preview = Boolean(previewConfig)
  const [config, setConfig] = useState<RoomConfig | null>(previewConfig)
  const [loading, setLoading] = useState(!previewConfig)
  const [error, setError] = useState('')
  const [tab, setTab] = useState<'gifts' | 'shop'>('gifts')
  const [gifts, setGifts] = useState<Gift[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [cart, setCart] = useState<Cart | null>(null)
  const [cartOpen, setCartOpen] = useState(false)
  const [cartBusy, setCartBusy] = useState(false)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [prepared, setPrepared] = useState(false)
  const [selectedGift, setSelectedGift] = useState<Gift | null>(null)
  const [giftSending, setGiftSending] = useState(false)
  const [balance, setBalance] = useState<number | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>(preview ? previewChat : [])
  const [imStatus, setImStatus] = useState<ImStatus>('disconnected')
  const [imRetry, setImRetry] = useState(0)
  const [redPacketCode, setRedPacketCode] = useState('')
  const [anchorPacketCode, setAnchorPacketCode] = useState('')
  const [packetReady, setPacketReady] = useState(false)
  const [grabbing, setGrabbing] = useState(false)
  const [redBusy, setRedBusy] = useState(false)
  const im = useRef<ReturnType<typeof createRoomIm> | null>(null)

  useEffect(() => {
    setMessages(preview ? previewChat.map(message => ({ ...message, roomId })) : [])
    setRedPacketCode('')
    setSelectedGift(null)
    setCart(null)
    setPrepared(false)
    try {
      setAnchorPacketCode(sessionStorage.getItem(`xy-live:packet:${roomId}`) || '')
      setPacketReady(sessionStorage.getItem(`xy-live:packet:${roomId}:ready`) === '1')
    } catch { setAnchorPacketCode(''); setPacketReady(false) }
  }, [roomId, preview])

  useEffect(() => {
    if (!config?.isAnchor || config.roomId !== roomId || !config.redPacketConfigCode) return
    setAnchorPacketCode(config.redPacketConfigCode)
    setPacketReady(false)
    try {
      sessionStorage.setItem(`xy-live:packet:${roomId}`, config.redPacketConfigCode)
      sessionStorage.removeItem(`xy-live:packet:${roomId}:ready`)
    } catch { /* Session storage may be disabled. */ }
  }, [config?.redPacketConfigCode, config?.isAnchor, roomId])

  const loadRoom = useCallback(async () => {
    if (previewConfig) { setConfig(previewConfig); setLoading(false); return }
    setConfig(null)
    if (!Number.isInteger(roomId) || roomId <= 0) { setError('直播间编号无效'); setLoading(false); return }
    if (!localStorage.getItem('xy-token')) { setError('登录后即可进入直播间'); setLoading(false); openLogin(`/room/${roomId}`); return }
    setLoading(true); setError('')
    try { const result = await api.room(roomId); if (result.roomId === -1) throw new Error('直播间已结束'); setConfig(result) } catch (cause) { setError(cause instanceof Error ? cause.message : '直播间加载失败') } finally { setLoading(false) }
  }, [roomId, previewConfig, openLogin, user?.loginStatus])

  useEffect(() => { void loadRoom() }, [loadRoom])
  useEffect(() => {
    if (!config || config.roomId !== roomId || preview) return
    api.gifts().then(setGifts).catch(() => setGifts([]))
    if (config.anchorId) api.products(config.anchorId).then(setProducts).catch(() => setProducts([]))
    api.balance().then(wallet => setBalance(wallet.currentBalance)).catch(() => setBalance(null))
  }, [config?.roomId, config?.anchorId, preview])

  useEffect(() => {
    if (!config || config.roomId !== roomId || preview || !config.userId) return
    let cancelled = false
    setImStatus('connecting')
    api.imConfig().then(details => {
      if (cancelled) return
      im.current = createRoomIm({
        address: details.wsImServerAddress, token: details.token, userId: config.userId!, roomId,
        senderName: config.watcherNickName || config.nickName || user?.nickName || '我', senderAvatar: config.watcherAvatar || user?.avatar || undefined,
        onMessage: message => setMessages(items => [...items, message].slice(-100)),
        onStatus: setImStatus,
        onError: error => notify(error.message, 'error'),
        onEvent: event => {
          if (event.bizCode === 5560 && event.data && typeof event.data === 'object') {
            const packet = (event.data as { redPacketConfigPO?: { configCode?: string } }).redPacketConfigPO
            if (packet?.configCode) setRedPacketCode(packet.configCode)
          }
          if (event.bizCode === 5557 && event.data && typeof event.data === 'object') notify(String((event.data as { msg?: string }).msg || '礼物发送失败'), 'error')
        },
      })
    }).catch(cause => { if (!cancelled) { setImStatus('error'); notify(cause instanceof Error ? cause.message : '聊天连接失败', 'error') } })
    return () => { cancelled = true; im.current?.close(); im.current = null }
  }, [config?.roomId, config?.userId, roomId, preview, imRetry])

  async function refreshCart() { if (!preview) { const result = await api.cart(roomId); setCart(result) } }
  async function openCart() { if (preview) return notify('预览模式无法使用购物车', 'error'); setCartOpen(true); try { await refreshCart() } catch (cause) { notify(cause instanceof Error ? cause.message : '购物车加载失败', 'error') } }
  async function addProduct(product: Product) {
    if (!user?.loginStatus) return openLogin(`/room/${roomId}`)
    setBusyId(String(product.skuId))
    try { const current = await api.cart(roomId); const exists = current?.shopCarItemRespDTOS?.some(item => String(item.skuInfoDTO.skuId) === String(product.skuId)); const ok = exists ? await api.changeCart(product.skuId, roomId, 1) : await api.addCart(product.skuId, roomId); if (!ok) throw new Error('添加购物车失败'); await refreshCart(); setPrepared(false); notify('已加入购物车') } catch (cause) { notify(cause instanceof Error ? cause.message : '添加失败', 'error') } finally { setBusyId(null) }
  }
  async function changeCart(skuId: string | number, changeType: 1 | 2) { setCartBusy(true); try { await api.changeCart(skuId, roomId, changeType); await refreshCart(); setPrepared(false) } catch (cause) { notify(cause instanceof Error ? cause.message : '修改数量失败', 'error') } finally { setCartBusy(false) } }
  async function removeCart(skuId: string | number) { setCartBusy(true); try { await api.removeCart(skuId, roomId); await refreshCart(); setPrepared(false) } catch (cause) { notify(cause instanceof Error ? cause.message : '移除失败', 'error') } finally { setCartBusy(false) } }
  async function prepareOrder() { setCartBusy(true); try { const ok = await api.prepareOrder(roomId); if (!ok) throw new Error('预下单请求失败'); setPrepared(true); notify('预下单请求已提交，请核对订单状态'); void refreshCart().catch(() => {}) } catch (cause) { notify(cause instanceof Error ? cause.message : '下单失败', 'error') } finally { setCartBusy(false) } }
  function sendChat(content: string) {
    if (preview) { setMessages(items => [...items, { id: `preview-${Date.now()}`, userId: '0', roomId, senderName: '预览访客', content, createdAt: Date.now(), own: true }]); return }
    if (!config) return
    if (!im.current?.send(content)) return notify('聊天暂未连接，请稍后重试', 'error')
    setMessages(items => [...items, { id: `own-${Date.now()}`, userId: config.userId || '', roomId, senderName: config.watcherNickName || '我', senderAvatar: config.watcherAvatar || undefined, content, createdAt: Date.now(), own: true }].slice(-100))
  }
  async function sendGift() {
    if (!config || !selectedGift || !config.anchorId || !config.userId) return
    setGiftSending(true)
    try { const ok = await api.sendGift({ giftId: selectedGift.giftId, roomId, senderUserId: config.userId, receiverId: config.anchorId, type: 0 }); if (!ok) throw new Error('礼物发送失败'); notify('礼物已提交，正在送达'); setSelectedGift(null); api.balance().then(wallet => setBalance(wallet.currentBalance)).catch(() => {}) } catch (cause) { notify(cause instanceof Error ? cause.message : '送礼失败', 'error') } finally { setGiftSending(false) }
  }
  async function prepareRedPacket() { if (!anchorPacketCode) return notify('当前没有可准备的红包配置，请先在后端配置', 'error'); setRedBusy(true); try { const ok = await api.prepareRedPacket(roomId); if (!ok) throw new Error('准备红包失败'); setPacketReady(true); try { sessionStorage.setItem(`xy-live:packet:${roomId}:ready`, '1') } catch { /* Session storage may be disabled. */ } notify('红包已准备，可以开始红包雨') } catch (cause) { notify(cause instanceof Error ? cause.message : '准备失败', 'error') } finally { setRedBusy(false) } }
  async function startRedPacket() { if (!anchorPacketCode) return; setRedBusy(true); try { const ok = await api.startRedPacket(anchorPacketCode); if (!ok) throw new Error('开启红包雨失败'); notify('红包雨已开启'); setAnchorPacketCode(''); setPacketReady(false); try { sessionStorage.removeItem(`xy-live:packet:${roomId}`); sessionStorage.removeItem(`xy-live:packet:${roomId}:ready`) } catch { /* Session storage may be disabled. */ } } catch (cause) { notify(cause instanceof Error ? cause.message : '开启失败', 'error') } finally { setRedBusy(false) } }
  async function grabRedPacket() { if (!redPacketCode) return; setGrabbing(true); try { const result = await api.getRedPacket(redPacketCode); notify(result.msg || (result.price ? `抢到 ${result.price} 星币` : '红包已领取')); setRedPacketCode('') } catch (cause) { notify(cause instanceof Error ? cause.message : '领取失败', 'error') } finally { setGrabbing(false) } }
  async function closeRoom() { if (!window.confirm('确定结束这场直播吗？直播间会立即关闭。')) return; try { await api.closeRoom(roomId); notify('直播已结束'); navigate('/') } catch (cause) { notify(cause instanceof Error ? cause.message : '关播失败', 'error') } }
  async function shareRoom() { try { await navigator.clipboard.writeText(window.location.href); notify('直播间链接已复制') } catch { notify('复制失败，请手动复制浏览器地址', 'error') } }

  if (loading || (config && config.roomId !== roomId)) return <main className="room-loading"><div className="loading-pulse">✦</div><p>正在进入直播间…</p></main>
  if (error || !config) return <main className="room-error"><div className="empty-orb"><Radio size={31} /></div><h1>{error || '直播间暂不可用'}</h1><p>这场直播可能已结束，也可能还未开始。</p><div><button className="primary-button" onClick={() => void loadRoom()}>重新加载 <RefreshCw size={17} /></button><Link to="/">返回发现 <ArrowRight size={17} /></Link></div></main>
  const title = config.roomName || listRoom?.roomName || `主播 ${config.anchorNickName || String(config.anchorId).slice(-4)} 的直播间`
  const pkRoom = listRoom?.type === 2 || new URLSearchParams(location.search).get('type') === '2'
  const isAnchor = config.isAnchor && !preview
  return <main className="room-page"><div className="room-topline"><Link to="/" className="back-link"><ArrowLeft size={17} /> 返回发现</Link><span className="room-breadcrumb">直播广场 <ChevronRight size={14} /> {title}</span><div className="room-id">ROOM ID&nbsp; {roomId}</div></div>{preview && <div className="preview-banner room-preview"><Bell size={18} /><span>界面预览模式 · 当前内容为示例，真实互动需要连接后端服务。</span></div>}<div className="room-layout"><div className="room-main"><div className="stage-frame"><StreamPlayer roomId={roomId} poster={config.defaultBgImg || listRoom?.covertImg || undefined} isAnchor={isAnchor} /></div><div className="room-info-row"><div className="anchor-info"><Avatar src={config.avatar || config.anchorImg} name={config.anchorNickName} /><div><span className="room-live-label"><i /> 正在直播 {pkRoom && <b>PK</b>}</span><h1>{title}</h1><p>{config.anchorNickName || `主播 ${String(config.anchorId).slice(-4)}`} <span>·</span> 和你分享此刻</p></div></div><div className="room-info-actions"><span className="room-view-count"><Users size={17} /> {fmt(listRoom?.watchNum)} 人在看</span><button className="outline-button" onClick={() => void shareRoom()}><Copy size={16} /> 分享</button>{isAnchor && <button className="end-live-button" onClick={() => void closeRoom()}>结束直播</button>}</div></div><div className="room-content-panel"><div className="room-tabs"><button className={tab === 'gifts' ? 'selected' : ''} onClick={() => setTab('gifts')}><GiftIcon size={17} /> 礼物星球</button><button className={tab === 'shop' ? 'selected' : ''} onClick={() => setTab('shop')}><ShoppingBag size={17} /> 直播好物 {products.length > 0 && <span>{products.length}</span>}</button><span className="tab-end">陪伴每个闪闪发光的时刻 <Heart size={15} /></span></div>{tab === 'gifts' ? <GiftPanel gifts={gifts} selected={selectedGift} onSelect={setSelectedGift} onSend={() => void sendGift()} sending={giftSending} balance={balance} preview={preview} isAnchor={isAnchor} pkRoom={pkRoom} onWallet={openWallet} /> : <ShopPanel products={products} onAdd={product => void addProduct(product)} busyId={busyId} onCart={() => void openCart()} cartCount={(cart?.shopCarItemRespDTOS || []).reduce((sum, item) => sum + item.count, 0)} preview={preview} />}</div>{isAnchor && <div className="anchor-tools"><div><span className="anchor-tools-icon">🧧</span><span><strong>主播控制台</strong><small>为直播间制造一些惊喜</small></span></div><button onClick={() => void (packetReady ? startRedPacket() : prepareRedPacket())} disabled={redBusy || !anchorPacketCode}>{redBusy ? '处理中…' : !anchorPacketCode ? '暂无红包配置' : packetReady ? '开启红包雨' : '准备红包雨'} <ArrowRight size={16} /></button></div>}</div><ChatPanel roomId={roomId} config={config} preview={preview} user={user} messages={messages} status={imStatus} onSend={sendChat} onReconnect={() => setImRetry(value => value + 1)} redPacketCode={redPacketCode} onGrab={() => void grabRedPacket()} grabbing={grabbing} /></div><div className="room-bottom-note"><span>XY <b>LIVE</b></span> 不止看见，更要在场。 <span>✦</span></div>{cartOpen && <CartDrawer cart={cart} roomId={roomId} busy={cartBusy} onClose={() => setCartOpen(false)} onChange={(skuId, changeType) => void changeCart(skuId, changeType)} onRemove={skuId => void removeCart(skuId)} onPrepare={() => void prepareOrder()} prepared={prepared} />}</main>
}
