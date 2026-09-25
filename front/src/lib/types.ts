/** Java Long values are parsed as strings when they exceed JavaScript's safe integer range. */
export type Id = string | number
export type RoomType = 1 | 2

export interface HomePage {
  loginStatus: boolean
  userId: Id
  nickName: string | null
  avatar: string | null
  showStartLivingBtn: boolean
}

export interface LoginResult {
  userId: Id
  token: string
}

export interface LivingRoom {
  id: number
  roomName: string
  anchorId: Id
  watchNum: number
  goodNum: number
  /** The spelling matches the backend VO. */
  covertImg: string | null
  /** Added from the selected list category; backend list items omit this field. */
  type?: RoomType
}

export interface LivingRoomPage {
  list: LivingRoom[]
  hasNext: boolean
}

export interface LivingRoomInit {
  roomId: number
  anchorId: Id | null
  userId: Id | null
  anchorImg: string | null
  roomName: string | null
  isAnchor: boolean
  redPacketConfigCode: string | null
  avatar: string | null
  watcherNickName: string | null
  anchorNickName: string | null
  watcherAvatar: string | null
  defaultBgImg: string | null
  pkObjId: Id | null
  nickName: string | null
}

export interface Gift {
  giftId: number
  price: number
  giftName: string
  status: number
  coverImgUrl: string | null
  svgaUrl: string | null
  createTime?: string
  updateTime?: string
}

export interface SendGiftRequest {
  giftId: number
  roomId: number
  senderUserId: Id
  receiverId: Id
  /** 0 = ordinary gift; 1 = PK gift. */
  type: 0 | 1
}

export interface SkuInfo {
  skuId: Id
  skuPrice: number
  skuCode: string | null
  name: string
  iconUrl: string | null
  originalIconUrl: string | null
  remark: string | null
}

export interface CartItem {
  count: number
  selected: boolean
  roomId: number
  skuInfoDTO: SkuInfo
}

export interface Cart {
  userId: Id
  roomId: number
  shopCarItemRespDTOS: CartItem[]
}

export interface Balance {
  userId: Id
  currentBalance: number
  totalCharged: number
  status: number | null
  createTime?: string
  updateTime?: string
}

export interface RedPacketResult {
  price: number | null
  msg: string | null
}

export interface ImConfig {
  token: string
  wsImServerAddress: string
  tcpImServerAddress: string
}

// Short names used by the page components.
export type LiveRoom = LivingRoom
export type RoomPage = LivingRoomPage
export type RoomConfig = LivingRoomInit
export type Product = SkuInfo
export type Wallet = Balance
export type UserLogin = LoginResult
