/**
 * C端 WebSocket 连接管理服务
 * - 自动连接/重连（指数退避）
 * - 心跳保活（30s）
 * - 消息分发（回调注册）
 */
class WebSocketService {
  private ws: WebSocket | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null
  private reconnectAttempts = 0
  private readonly MAX_RECONNECT_ATTEMPTS = 10
  private readonly HEARTBEAT_INTERVAL = 30000
  private readonly WS_BASE_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/api/ws/client`
  private disposed = false

  /** 消息处理回调 */
  private handlers: Map<string, Set<(payload: any) => void>> = new Map()

  /**
   * 注册消息类型回调
   * 返回一个取消注册函数
   */
  on(type: string, handler: (payload: any) => void) {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, new Set())
    }
    this.handlers.get(type)!.add(handler)
    return () => this.handlers.get(type)?.delete(handler)
  }

  /**
   * 建立连接
   */
  connect() {
    const token = localStorage.getItem('token')
    if (!token) return

    // 幂等：已连接或正在握手中，直接复用
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    // 清理旧连接（CLOSING / CLOSED 状态）
    if (this.ws) {
      this.ws.onopen = null
      this.ws.onmessage = null
      this.ws.onclose = null
      this.ws.onerror = null
      this.ws = null
    }

    this.disposed = false
    this.stopHeartbeat()
    this.clearReconnect()

    const url = `${this.WS_BASE_URL}?token=${encodeURIComponent(token)}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.reconnectAttempts = 0
      this.startHeartbeat()
    }

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        const handlers = this.handlers.get(msg.type)
        if (handlers) {
          handlers.forEach(h => h(msg.payload))
        }
      } catch { /* ignore malformed messages */ }
    }

    this.ws.onclose = () => {
      this.stopHeartbeat()
      this.scheduleReconnect()
    }

    this.ws.onerror = () => {
      this.ws?.close()
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.disposed = true
    this.stopHeartbeat()
    this.clearReconnect()
    this.ws?.close()
    this.ws = null
  }

  /**
   * 当前是否已连接
   */
  get connected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'PING' }))
      }
    }, this.HEARTBEAT_INTERVAL)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private scheduleReconnect() {
    if (this.disposed) return
    if (this.reconnectAttempts >= this.MAX_RECONNECT_ATTEMPTS) return
    this.clearReconnect()
    // 指数退避: 1s, 2s, 4s, 8s ... 最大 30s
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000)
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => this.connect(), delay)
  }

  private clearReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }
}

export const wsService = new WebSocketService()
