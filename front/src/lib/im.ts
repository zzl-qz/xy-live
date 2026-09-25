export type ChatMessage = {
  id: string
  userId: number | string
  roomId: number
  senderName: string
  senderAvatar?: string
  content: string
  createdAt: number
  own?: boolean
}

export type ImStatus = 'connecting' | 'connected' | 'disconnected' | 'error'

export type ImEvent = {
  bizCode: number
  data: unknown
  msgId?: string
  userId?: number | string
}

export type RoomImOptions = {
  address: string
  token: string
  userId: number | string
  roomId: number
  senderName: string
  senderAvatar?: string
  onMessage: (message: ChatMessage) => void
  onStatus?: (status: ImStatus) => void
  onError?: (error: Error) => void
  onEvent?: (event: ImEvent) => void
}

const MAGIC = 18673
const APP_ID = 10001
const CHAT_BIZ_CODE = 5555

function asObject(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null
}

// Fastjson serializes Java byte[] as Base64; some deployments send an integer array.
function unpack(value: unknown): unknown {
  let current = value
  for (let depth = 0; depth < 4; depth += 1) {
    if (Array.isArray(current) && current.every((item) => typeof item === 'number')) {
      current = new TextDecoder().decode(new Uint8Array(current))
      continue
    }
    if (typeof current !== 'string') return current
    const raw = current
    try {
      current = JSON.parse(raw)
      continue
    } catch {
      if (!/^[A-Za-z0-9+/]+={0,2}$/.test(raw) || raw.length < 4) return current
      try {
        const bytes = Uint8Array.from(atob(raw), (char) => char.charCodeAt(0))
        current = new TextDecoder().decode(bytes)
      } catch {
        return current
      }
    }
  }
  return current
}

function messageId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function createRoomIm(options: RoomImOptions): { send(content: string): boolean; close(): void } {
  let socket: WebSocket | null = null
  let heartbeat: ReturnType<typeof setInterval> | null = null
  let status: ImStatus = 'disconnected'
  let closedByClient = false

  const setStatus = (next: ImStatus) => {
    if (status === next) return
    status = next
    options.onStatus?.(next)
  }

  const stopHeartbeat = () => {
    if (heartbeat) clearInterval(heartbeat)
    heartbeat = null
  }

  const sendFrame = (code: number, payload: object): boolean => {
    if (socket?.readyState !== WebSocket.OPEN) return false
    const body = JSON.stringify(payload)
    socket.send(JSON.stringify({ magic: MAGIC, code, len: new TextEncoder().encode(body).length, body }))
    return true
  }

  const fail = (message: string) => {
    setStatus('error')
    options.onError?.(new Error(message))
  }

  try {
    setStatus('connecting')
    const rawAddress = options.address.trim().replace(/\/+$/, '')
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws'
    const address = /^wss?:\/\//i.test(rawAddress) ? rawAddress : `${scheme}://${rawAddress}`
    // The IM token contains "%10001". Keep that literal: the server parses it from the raw path.
    socket = new WebSocket(`${address}/${options.token}/${options.userId}/1001/${options.roomId}`)

    socket.onmessage = (event: MessageEvent<string>) => {
      const frame = asObject(unpack(event.data))
      if (!frame) return
      const code = Number(frame.code)
      const body = asObject(unpack(frame.body))

      if (code === 1001) {
        if (body?.data === 'false') {
          fail('实时聊天登录失败')
          socket?.close()
          return
        }
        setStatus('connected')
        stopHeartbeat()
        heartbeat = setInterval(() => {
          sendFrame(1004, { appId: APP_ID, userId: options.userId })
        }, 30_000)
        return
      }
      if (code !== 1003 || !body) return

      const msgId = typeof body.msgId === 'string' ? body.msgId : undefined
      if (msgId) sendFrame(1005, { appId: APP_ID, userId: options.userId, msgId })

      const bizCode = Number(body.bizCode)
      const data = unpack(body.data)
      if (bizCode !== CHAT_BIZ_CODE) {
        options.onEvent?.({ bizCode, data, msgId, userId: body.userId as number | string | undefined })
        return
      }

      const chat = asObject(data)
      if (!chat || typeof chat.content !== 'string') return
      const userId = chat.userId as number | string | undefined
      options.onMessage({
        id: msgId ?? messageId(),
        userId: userId ?? '',
        roomId: Number(chat.roomId) || options.roomId,
        senderName: typeof chat.senderName === 'string' ? chat.senderName : '观众',
        senderAvatar: typeof chat.senderAvatar === 'string' ? chat.senderAvatar : undefined,
        content: chat.content,
        createdAt: Date.now(),
        own: userId !== undefined && String(userId) === String(options.userId),
      })
    }

    socket.onerror = () => fail('实时聊天连接失败')
    socket.onclose = () => {
      stopHeartbeat()
      if (closedByClient) {
        setStatus('disconnected')
      } else if (status === 'connecting') {
        fail('实时聊天握手失败')
      } else if (status !== 'error') {
        setStatus('disconnected')
        options.onError?.(new Error('实时聊天连接已断开'))
      }
    }
  } catch (error) {
    fail(error instanceof Error ? error.message : '实时聊天连接失败')
  }

  return {
    send(content: string) {
      const text = content.trim()
      if (!text || status !== 'connected') return false
      return sendFrame(1003, {
        appId: APP_ID,
        userId: options.userId,
        bizCode: CHAT_BIZ_CODE,
        msgId: messageId(),
        data: JSON.stringify({
          userId: options.userId,
          roomId: options.roomId,
          type: 0,
          senderName: options.senderName,
          senderAvatar: options.senderAvatar,
          content: text,
        }),
      })
    },
    close() {
      closedByClient = true
      stopHeartbeat()
      socket?.close()
      setStatus('disconnected')
    },
  }
}
