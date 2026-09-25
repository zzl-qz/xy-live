import type { LiveRoom, RoomConfig } from './lib/types'

export const previewRooms: LiveRoom[] = [
  { id: 90001, roomName: '把夜晚唱给你听', anchorId: '10001', watchNum: 12840, goodNum: 9821, covertImg: '', type: 1 },
  { id: 90002, roomName: '峡谷开黑 · 今晚冲榜', anchorId: '10002', watchNum: 8960, goodNum: 7120, covertImg: '', type: 1 },
  { id: 90003, roomName: '周末好物开箱现场', anchorId: '10003', watchNum: 6720, goodNum: 4860, covertImg: '', type: 1 },
  { id: 90004, roomName: '深夜电台，故事交换局', anchorId: '10004', watchNum: 5420, goodNum: 3800, covertImg: '', type: 1 },
  { id: 90005, roomName: '星光舞台 · 即兴点歌', anchorId: '10005', watchNum: 4200, goodNum: 2980, covertImg: '', type: 1 },
  { id: 90006, roomName: '今晚一起画点什么', anchorId: '10006', watchNum: 3140, goodNum: 1810, covertImg: '', type: 1 },
  { id: 90007, roomName: '巅峰对决，谁是 MVP', anchorId: '10007', watchNum: 15500, goodNum: 12900, covertImg: '', type: 2 },
  { id: 90008, roomName: '双人 PK · 人气挑战赛', anchorId: '10008', watchNum: 11200, goodNum: 8790, covertImg: '', type: 2 },
  { id: 90009, roomName: '声音之间的默契挑战', anchorId: '10009', watchNum: 7300, goodNum: 5100, covertImg: '', type: 2 },
  { id: 90010, roomName: '今晚谁是全场焦点', anchorId: '10010', watchNum: 5980, goodNum: 4400, covertImg: '', type: 2 },
]

export const previewNames = ['鹿屿', '阿北同学', '小满', '晚风', '阿野', '一只橘子', 'Seven', '沐沐', '青木', '米亚']

export function previewRoom(id: number): RoomConfig | null {
  const item = previewRooms.find(room => room.id === id)
  if (!item) return null
  const name = previewNames[previewRooms.indexOf(item)]
  return {
    roomId: item.id,
    anchorId: item.anchorId,
    userId: '0',
    roomName: item.roomName,
    anchorNickName: name,
    watcherNickName: '预览访客',
    nickName: '预览访客',
    avatar: '',
    watcherAvatar: '',
    anchorImg: '',
    defaultBgImg: '',
    isAnchor: false,
    redPacketConfigCode: '',
    pkObjId: null,
  }
}
