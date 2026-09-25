import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import {
  AlertCircle,
  Camera,
  CameraOff,
  Link2,
  Maximize2,
  Minimize2,
  Pause,
  Play,
  Radio,
  RotateCcw,
  Volume2,
  VolumeX,
} from 'lucide-react'
import './StreamPlayer.css'

type StreamPlayerProps = {
  roomId: number
  poster?: string
  isAnchor?: boolean
}

type RoomValue<T> = { roomId: number; value: T }

const sourceKey = (roomId: number) => `xy-live:stream-source:${roomId}`

function savedSource(roomId: number): string | null {
  try {
    return localStorage.getItem(sourceKey(roomId)) || null
  } catch {
    return null
  }
}

function templateSource(roomId: number): string {
  const template = (import.meta.env.VITE_STREAM_URL_TEMPLATE ?? '').trim()
  return template.replaceAll('{roomId}', String(roomId))
}

function isHls(url: string): boolean {
  return /\.m3u8(?:$|[?#])/i.test(url)
}

function normalizeUrl(value: string): string | null {
  if (!/^https?:\/\//i.test(value.trim())) return null
  try {
    const url = new URL(value.trim(), window.location.href)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : null
  } catch {
    return null
  }
}

export default function StreamPlayer({ roomId, poster, isAnchor = false }: StreamPlayerProps) {
  const stageRef = useRef<HTMLDivElement>(null)
  const videoRef = useRef<HTMLVideoElement>(null)
  const cameraVideoRef = useRef<HTMLVideoElement>(null)
  const cameraStreamRef = useRef<MediaStream | null>(null)
  const cameraRequestRef = useRef(0)

  const [override, setOverride] = useState<RoomValue<string | null>>(() => ({
    roomId,
    value: savedSource(roomId),
  }))
  const activeOverride = override.roomId === roomId ? override.value : savedSource(roomId)
  const sourceUrl = activeOverride ?? templateSource(roomId)
  const [draft, setDraft] = useState<RoomValue<string>>(() => ({
    roomId,
    value: savedSource(roomId) ?? templateSource(roomId),
  }))
  const draftUrl = draft.roomId === roomId ? draft.value : sourceUrl

  const [playbackError, setPlaybackError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [playing, setPlaying] = useState(false)
  const [muted, setMuted] = useState(true)
  const [volume, setVolume] = useState(0.8)
  const [fullscreen, setFullscreen] = useState(false)
  const [previewing, setPreviewing] = useState(false)
  const [cameraBusy, setCameraBusy] = useState(false)
  const [retryKey, setRetryKey] = useState(0)

  const releaseCamera = useCallback(() => {
    cameraRequestRef.current += 1
    cameraStreamRef.current?.getTracks().forEach((track) => track.stop())
    cameraStreamRef.current = null
    if (cameraVideoRef.current) cameraVideoRef.current.srcObject = null
  }, [])

  useEffect(() => {
    const next = savedSource(roomId)
    setOverride({ roomId, value: next })
    setDraft({ roomId, value: next ?? templateSource(roomId) })
    setNotice(null)
    setPreviewing(false)
    setCameraBusy(false)
    return releaseCamera
  }, [roomId, releaseCamera])

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    video.pause()
    video.removeAttribute('src')
    video.load()
    setPlaying(false)
    setPlaybackError(null)
    setLoading(Boolean(sourceUrl && !previewing))
    if (!sourceUrl || previewing) return

    let active = true
    let destroyHls: (() => void) | undefined
    const onReady = () => {
      if (!active) return
      setLoading(false)
      void video.play().catch(() => {
        // Browser autoplay policy can require a manual click.
      })
    }
    const onError = () => {
      if (!active) return
      setLoading(false)
      setPlaybackError('播放失败，请检查地址、视频格式或媒体服务的跨域配置。')
    }

    video.addEventListener('canplay', onReady)
    video.addEventListener('error', onError)

    if (isHls(sourceUrl) && !video.canPlayType('application/vnd.apple.mpegurl')) {
      void import('hls.js').then(({ default: Hls }) => {
        if (!active) return
        if (!Hls.isSupported()) {
          setLoading(false)
          setPlaybackError('当前浏览器不支持 HLS 播放。')
          return
        }
        const hls = new Hls({ enableWorker: true, lowLatencyMode: true })
        destroyHls = () => hls.destroy()
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (active && data.fatal) {
            setLoading(false)
            setPlaybackError('HLS 视频流连接失败，请检查播放地址和媒体服务。')
          }
        })
        hls.on(Hls.Events.MEDIA_ATTACHED, () => {
          if (active) hls.loadSource(sourceUrl)
        })
        hls.attachMedia(video)
      }).catch(() => {
        if (!active) return
        setLoading(false)
        setPlaybackError('HLS 播放器加载失败，请稍后重试。')
      })
    } else {
      video.src = sourceUrl
      video.load()
    }

    return () => {
      active = false
      video.removeEventListener('canplay', onReady)
      video.removeEventListener('error', onError)
      destroyHls?.()
      video.pause()
      video.removeAttribute('src')
      video.load()
    }
  }, [sourceUrl, previewing, retryKey])

  useEffect(() => {
    const video = videoRef.current
    if (video) {
      video.muted = muted
      video.volume = volume
    }
  }, [muted, volume])

  useEffect(() => {
    if (previewing && cameraVideoRef.current) {
      cameraVideoRef.current.srcObject = cameraStreamRef.current
      void cameraVideoRef.current.play().catch(() => {
        setNotice('摄像头预览无法自动播放，请检查浏览器权限。')
      })
    }
  }, [previewing])

  useEffect(() => {
    const onFullscreenChange = () => setFullscreen(document.fullscreenElement === stageRef.current)
    document.addEventListener('fullscreenchange', onFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange)
  }, [])

  function applySource(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const normalized = normalizeUrl(draftUrl)
    if (!normalized) {
      setNotice('请输入有效的 http:// 或 https:// 播放地址。')
      return
    }
    try {
      localStorage.setItem(sourceKey(roomId), normalized)
      setNotice('已保存此房间的播放地址。')
    } catch {
      setNotice('播放地址已应用，但浏览器未能保存，下次进入需重新填写。')
    }
    setOverride({ roomId, value: normalized })
    setDraft({ roomId, value: normalized })
  }

  function restoreSource() {
    try {
      localStorage.removeItem(sourceKey(roomId))
    } catch {
      // The current session can still return to the template.
    }
    setOverride({ roomId, value: null })
    setDraft({ roomId, value: templateSource(roomId) })
    setNotice('已恢复环境配置的默认播放地址。')
  }

  function togglePlayback() {
    const video = videoRef.current
    if (!video || !sourceUrl || previewing) return
    if (video.paused) {
      void video.play().catch(() => setPlaybackError('无法开始播放，请检查视频地址。'))
    } else {
      video.pause()
    }
  }

  function toggleMute() {
    if (muted && volume === 0) setVolume(0.8)
    setMuted((current) => !current)
  }

  async function toggleFullscreen() {
    try {
      if (document.fullscreenElement === stageRef.current) {
        await document.exitFullscreen()
      } else {
        await stageRef.current?.requestFullscreen()
      }
    } catch {
      setNotice('当前浏览器未能进入全屏模式。')
    }
  }

  async function toggleCamera() {
    if (previewing) {
      releaseCamera()
      setPreviewing(false)
      return
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      setNotice('当前浏览器无法访问摄像头；本地预览需要 HTTPS 或 localhost。')
      return
    }
    const requestId = ++cameraRequestRef.current
    setCameraBusy(true)
    setNotice(null)
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false })
      if (requestId !== cameraRequestRef.current) {
        stream.getTracks().forEach((track) => track.stop())
        return
      }
      cameraStreamRef.current = stream
      setPreviewing(true)
    } catch {
      if (requestId === cameraRequestRef.current) setNotice('无法打开摄像头，请检查浏览器权限和设备。')
    } finally {
      if (requestId === cameraRequestRef.current) setCameraBusy(false)
    }
  }

  const noSource = !sourceUrl && !previewing
  const showCenteredState = previewing || noSource || Boolean(playbackError) || loading || !playing

  return (
    <section className="stream-player" aria-label="直播画面播放器">
      <div className="stream-player__stage" ref={stageRef}>
        {poster && <img className="stream-player__poster" src={poster} alt="" />}
        <div className="stream-player__shade" />
        <video
          ref={videoRef}
          className={`stream-player__video${noSource || previewing ? ' stream-player__video--hidden' : ''}`}
          poster={poster}
          autoPlay
          muted={muted}
          playsInline
          onPlay={() => {
            setPlaying(true)
            setPlaybackError(null)
          }}
          onPause={() => setPlaying(false)}
        />
        {previewing && <video ref={cameraVideoRef} className="stream-player__video" autoPlay muted playsInline />}

        <div className="stream-player__topline">
          <span className={`stream-player__signal${playing && !previewing ? ' stream-player__signal--on' : ''}`}>
            <span />{previewing ? '本地预览' : playing ? '信号已连接' : '等待画面'}
          </span>
          <span className="stream-player__room-id">房间 #{roomId}</span>
        </div>

        {showCenteredState && (
          <div className="stream-player__center" aria-live="polite">
            {previewing ? (
              <div className="stream-player__state stream-player__state--preview">
                <Camera size={30} strokeWidth={1.7} />
                <strong>本机摄像头预览</strong>
                <span>仅在这台设备显示，尚未推流给观众</span>
              </div>
            ) : noSource ? (
              <div className="stream-player__state">
                <div className="stream-player__state-icon"><Radio size={34} strokeWidth={1.5} /></div>
                <span className="stream-player__eyebrow">NO VIDEO SIGNAL</span>
                <strong>画面未接入</strong>
                <span>当前房间没有播放地址，请在下方配置视频源。</span>
              </div>
            ) : playbackError ? (
              <div className="stream-player__state">
                <div className="stream-player__state-icon stream-player__state-icon--error"><AlertCircle size={32} strokeWidth={1.6} /></div>
                <strong>画面加载失败</strong>
                <span>{playbackError}</span>
                <button className="stream-player__retry" type="button" onClick={() => setRetryKey((value) => value + 1)}>重新连接</button>
              </div>
            ) : loading ? (
              <div className="stream-player__state">
                <div className="stream-player__loading" />
                <strong>正在连接视频信号</strong>
                <span>请稍候</span>
              </div>
            ) : (
              <button className="stream-player__big-play" type="button" onClick={togglePlayback} aria-label="播放视频">
                <Play size={28} fill="currentColor" />
              </button>
            )}
          </div>
        )}

        <div className="stream-player__controls">
          <div className="stream-player__controls-left">
            <button type="button" className="stream-player__icon-button" onClick={togglePlayback} disabled={!sourceUrl || previewing} aria-label={playing ? '暂停' : '播放'}>
              {playing && !previewing ? <Pause size={19} fill="currentColor" /> : <Play size={19} fill="currentColor" />}
            </button>
            <button type="button" className="stream-player__icon-button" onClick={toggleMute} disabled={!sourceUrl || previewing} aria-label={muted || volume === 0 ? '取消静音' : '静音'}>
              {muted || volume === 0 ? <VolumeX size={19} /> : <Volume2 size={19} />}
            </button>
            <input
              className="stream-player__volume"
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={muted ? 0 : volume}
              disabled={!sourceUrl || previewing}
              onChange={(event) => {
                const nextVolume = Number(event.target.value)
                setVolume(nextVolume)
                setMuted(nextVolume === 0)
              }}
              aria-label="播放音量"
            />
            <span className="stream-player__control-caption">{previewing ? '本地预览 · 未推流' : playing ? '直播画面' : '视频播放器'}</span>
          </div>
          <div className="stream-player__controls-right">
            {isAnchor && (
              <button type="button" className="stream-player__icon-button stream-player__camera-button" onClick={() => void toggleCamera()} disabled={cameraBusy} aria-label={previewing ? '关闭本地摄像头预览' : '打开本地摄像头预览'} title="仅本地预览，不会推流">
                {previewing ? <CameraOff size={19} /> : <Camera size={19} />}
              </button>
            )}
            <button type="button" className="stream-player__icon-button" onClick={() => void toggleFullscreen()} aria-label={fullscreen ? '退出全屏' : '全屏播放'}>
              {fullscreen ? <Minimize2 size={19} /> : <Maximize2 size={19} />}
            </button>
          </div>
        </div>
      </div>

      <div className="stream-player__source-panel">
        <div className="stream-player__source-heading">
          <div>
            <span className="stream-player__source-kicker"><Link2 size={14} /> 播放源</span>
            <h3>{activeOverride ? '当前使用房间专属地址' : sourceUrl ? '当前使用默认地址' : '尚未配置播放地址'}</h3>
          </div>
          <span className="stream-player__format">HLS / VIDEO</span>
        </div>
        <form className="stream-player__source-form" onSubmit={applySource}>
          <label className="stream-player__sr-only" htmlFor={`stream-source-${roomId}`}>视频播放地址</label>
          <input
            id={`stream-source-${roomId}`}
            type="url"
            value={draftUrl}
            onChange={(event) => setDraft({ roomId, value: event.target.value })}
            placeholder="https://media.example.com/live/room.m3u8"
            autoComplete="off"
            spellCheck={false}
          />
          <button type="submit">应用地址</button>
          {activeOverride && (
            <button type="button" className="stream-player__reset" onClick={restoreSource} aria-label="恢复默认播放地址" title="恢复默认播放地址">
              <RotateCcw size={17} />
            </button>
          )}
        </form>
        <p className="stream-player__source-help">支持 HLS (.m3u8) 及浏览器可播放的视频地址。视频播放需要单独的媒体服务，此处不会发起推流。</p>
        {notice && <p className="stream-player__notice" role="status">{notice}</p>}
      </div>
    </section>
  )
}
