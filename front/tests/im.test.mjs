import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'

const source = await readFile(new URL('../src/lib/im.ts', import.meta.url), 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 },
}).outputText
const { createRoomIm } = await import(`data:text/javascript;base64,${Buffer.from(compiled).toString('base64')}`)

test('room IM login, Base64 chat, ACK, and UTF-8 send length', () => {
  const previousWebSocket = globalThis.WebSocket
  const previousLocation = globalThis.location

  class FakeWebSocket {
    static OPEN = 1
    static instance

    constructor(url) {
      this.url = url
      this.readyState = 0
      this.sent = []
      FakeWebSocket.instance = this
    }

    send(text) {
      this.sent.push(JSON.parse(text))
    }

    close() {
      this.readyState = 3
      this.onclose?.()
    }

    receive(code, body) {
      const bytes = Buffer.from(JSON.stringify(body), 'utf8')
      this.onmessage?.({ data: JSON.stringify({ magic: 18673, code, len: bytes.length, body: bytes.toString('base64') }) })
    }
  }

  globalThis.WebSocket = FakeWebSocket
  globalThis.location = { protocol: 'http:' }

  let im
  try {
    const statuses = []
    const messages = []
    const errors = []
    im = createRoomIm({
      address: 'localhost:8086',
      token: 'abc%10001',
      userId: 42,
      roomId: 9,
      senderName: '我',
      onMessage: (message) => messages.push(message),
      onStatus: (status) => statuses.push(status),
      onError: (error) => errors.push(error),
    })

    const socket = FakeWebSocket.instance
    assert.equal(socket.url, 'ws://localhost:8086/abc%10001/42/1001/9')
    assert.deepEqual(statuses, ['connecting'])
    assert.equal(im.send('尚未登录'), false)

    socket.readyState = FakeWebSocket.OPEN
    socket.receive(1001, { appId: 10001, userId: 42, data: 'true' })
    assert.deepEqual(statuses, ['connecting', 'connected'])

    socket.receive(1003, {
      appId: 10001,
      userId: 42,
      bizCode: 5555,
      msgId: 'chat-1',
      data: JSON.stringify({ userId: 7, roomId: 9, senderName: '小明', content: '欢迎来到直播间' }),
    })
    assert.equal(messages.length, 1)
    assert.equal(messages[0].content, '欢迎来到直播间')
    assert.equal(messages[0].senderName, '小明')
    assert.equal(messages[0].id, 'chat-1')

    const ack = socket.sent.find((frame) => frame.code === 1005)
    assert.deepEqual(JSON.parse(ack.body), { appId: 10001, userId: 42, msgId: 'chat-1' })
    assert.equal(ack.len, Buffer.byteLength(ack.body, 'utf8'))

    assert.equal(im.send('你好，直播间'), true)
    const chat = socket.sent.find((frame) => frame.code === 1003)
    const body = JSON.parse(chat.body)
    assert.equal(chat.magic, 18673)
    assert.equal(body.bizCode, 5555)
    assert.equal(JSON.parse(body.data).type, 0)
    assert.equal(JSON.parse(body.data).content, '你好，直播间')
    assert.equal(chat.len, Buffer.byteLength(chat.body, 'utf8'))
    assert.ok(chat.len > chat.body.length)
    assert.deepEqual(errors, [])
  } finally {
    im?.close()
    globalThis.WebSocket = previousWebSocket
    globalThis.location = previousLocation
  }
})
