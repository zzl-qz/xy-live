// Keep the backend's Java Long IDs exact.
import JSONbig from 'json-bigint'
import type {
  Balance,
  Cart,
  Gift,
  HomePage,
  Id,
  ImConfig,
  LivingRoomInit,
  LivingRoomPage,
  LoginResult,
  RedPacketResult,
  RoomType,
  SendGiftRequest,
  SkuInfo,
} from './types'

const json = JSONbig({ storeAsString: true })
const base = (import.meta.env.VITE_API_ORIGIN || '/api').replace(/\/$/, '')

export type ApiErrorKind = 'network' | 'http' | 'business' | 'invalid'

export class ApiError extends Error {
  constructor(
    public readonly kind: ApiErrorKind,
    message: string,
    public readonly status?: number,
    public readonly code?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface Envelope<T> {
  code: number
  msg: string | null
  data: T
}

type FormValue = string | number

async function post<T>(path: string, body?: object, form = false): Promise<T> {
  const token = localStorage.getItem('xy-token')
  const headers: Record<string, string> = {}
  if (token) headers.Xytk = token
  let requestBody: BodyInit | undefined
  if (body !== undefined) {
    if (form) {
      headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8'
      requestBody = new URLSearchParams(
        Object.entries(body).map(([key, value]) => [key, String(value as FormValue)]),
      )
    } else {
      headers['Content-Type'] = 'application/json'
      requestBody = JSON.stringify(body)
    }
  }

  let response: Response
  try {
    response = await fetch(`${base}${path}`, { method: 'POST', headers, body: requestBody })
  } catch {
    throw new ApiError('network', '连接后端失败，请检查服务是否运行')
  }
  if (!response.ok) {
    throw new ApiError('http', `请求失败（HTTP ${response.status}）`, response.status)
  }
  let envelope: Envelope<T>
  try {
    envelope = json.parse(await response.text()) as Envelope<T>
  } catch {
    throw new ApiError('invalid', '后端返回了无法解析的数据')
  }
  if (!envelope || typeof envelope !== 'object' || typeof envelope.code !== 'number') {
    throw new ApiError('invalid', '后端返回的数据格式不正确')
  }
  if (envelope.code !== 200) {
    throw new ApiError('business', envelope.msg || `业务请求失败（${envelope.code}）`, undefined, envelope.code)
  }
  return envelope.data
}

export const api = {
  home: () => post<HomePage>('/home/initPage'),
  rooms: (type: RoomType, page: number, pageSize: number) =>
    post<LivingRoomPage>('/living/list', { type, page, pageSize }),
  room: (roomId: number) => post<LivingRoomInit>('/living/anchorConfig', { roomId }, true),
  sendCode: (phone: string) => post<unknown>('/userLogin/sendLoginCode', { phone }, true),
  login: async (phone: string, code: number) => {
    const result = await post<LoginResult>('/userLogin/login', { phone, code }, true)
    localStorage.setItem('xy-token', result.token)
    return result
  },
  startRoom: (type: RoomType) => post<Pick<LivingRoomInit, 'roomId'>>('/living/startingLiving', { type }, true),
  closeRoom: (roomId: number) => post<unknown>('/living/closeLiving', { roomId }, true),
  gifts: () => post<Gift[]>('/gift/listGift'),
  sendGift: (body: SendGiftRequest) => post<boolean>('/gift/send', body),
  products: (anchorId: Id) => post<SkuInfo[]>('/shop/listSkuInfo', { anchorId }, true),
  cart: (roomId: number) => post<Cart>('/shop/getCarInfo_v2', { roomId }),
  addCart: (skuId: Id, roomId: number) => post<boolean>('/shop/addCar_v2', { skuId, roomId }),
  removeCart: (skuId: Id, roomId: number) => post<boolean>('/shop/removeFromCar_v2', { skuId, roomId }),
  changeCart: (skuId: Id, roomId: number, changeType: 1 | 2) =>
    post<boolean>(`/shop/changeCarSkuNum_v2?changeType=${changeType}`, { skuId, roomId }),
  prepareOrder: (roomId: number) => post<boolean>('/shop/preparerOrder', { roomId }),
  payNow: (roomId: number) => post<boolean>('/shop/payNow', { roomId }),
  balance: () => post<Balance>('/bank/getByUserId'),
  prepareRedPacket: (roomId: number) => post<boolean>('/living/prepareRedPacket', { roomId }),
  startRedPacket: (redPacketConfigCode: string) =>
    post<boolean>('/living/startRedPacket', { redPacketConfigCode }),
  getRedPacket: (redPacketConfigCode: string) =>
    post<RedPacketResult>('/living/getRedPacket', { redPacketConfigCode }),
  imConfig: () => post<ImConfig>('/im/getImConfig'),
}
