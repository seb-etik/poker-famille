import { describeEvent } from './table.js';

const PING_INTERVAL_MS = 25_000;
const MAX_RETRY_DELAY_MS = 10_000;
const LOG_SIZE = 6;
const ERROR_DISPLAY_MS = 4_000;

/**
 * The WebSocket of one table: sends intentions, keeps the last state received,
 * and reconnects on its own after a network cut.
 */
export class TableConnection {
  /** Last `state` message from the server, null before the first one. */
  state = $state(null);
  /** 'connecting', 'open', 'closed' (reconnecting) or 'notFound'. */
  status = $state('connecting');
  error = $state(null);
  log = $state([]);
  /** performance.now() when the last state arrived, to count down `remainingMs`. */
  receivedAt = $state(0);

  #code;
  #profile;
  #socket = null;
  #retries = 0;
  #pingTimer = null;
  #retryTimer = null;
  #errorTimer = null;
  #closed = false;

  constructor(code, profile) {
    this.#code = code;
    this.#profile = profile;
    document.addEventListener('visibilitychange', this.#onVisibilityChange);
    this.#connect();
  }

  sit(seat) {
    this.#send({ type: 'sit', seat });
  }

  leave() {
    this.#send({ type: 'leave' });
  }

  start() {
    this.#send({ type: 'start' });
  }

  act(action, amount) {
    this.#send({ type: 'action', action, amount });
  }

  close() {
    this.#closed = true;
    document.removeEventListener('visibilitychange', this.#onVisibilityChange);
    clearInterval(this.#pingTimer);
    clearTimeout(this.#retryTimer);
    clearTimeout(this.#errorTimer);
    this.#socket?.close();
  }

  #connect() {
    const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
    const socket = new WebSocket(`${scheme}://${location.host}/ws/table/${this.#code}`);
    this.#socket = socket;
    this.status = 'connecting';

    socket.onopen = () => {
      this.status = 'open';
      this.#retries = 0;
      const { playerId, nickname, avatar } = this.#profile;
      this.#send({ type: 'join', playerId, nickname, avatar });
      this.#pingTimer = setInterval(() => this.#send({ type: 'ping' }), PING_INTERVAL_MS);
    };
    socket.onmessage = (message) => this.#receive(JSON.parse(message.data));
    socket.onclose = () => {
      clearInterval(this.#pingTimer);
      if (this.#closed || this.status === 'notFound' || socket !== this.#socket) {
        return;
      }
      this.status = 'closed';
      this.#scheduleReconnect();
    };
  }

  #scheduleReconnect() {
    clearTimeout(this.#retryTimer);
    const delay = Math.min(MAX_RETRY_DELAY_MS, 1000 * 2 ** this.#retries);
    this.#retries++;
    this.#retryTimer = setTimeout(() => this.#connect(), delay);
  }

  // A phone that wakes up should not wait for the next retry.
  #onVisibilityChange = () => {
    if (document.visibilityState === 'visible' && this.status === 'closed') {
      clearTimeout(this.#retryTimer);
      this.#connect();
    }
  };

  #receive(message) {
    switch (message.type) {
      case 'state':
        this.state = message;
        this.receivedAt = performance.now();
        break;
      case 'event': {
        const line = describeEvent(message, this.state?.seats ?? []);
        if (line) {
          this.log = [line, ...this.log].slice(0, LOG_SIZE);
        }
        break;
      }
      case 'error':
        if (message.code === 'TABLE_NOT_FOUND') {
          this.status = 'notFound';
        } else {
          this.#showError(message.message);
        }
        break;
    }
  }

  #showError(text) {
    this.error = text;
    clearTimeout(this.#errorTimer);
    this.#errorTimer = setTimeout(() => (this.error = null), ERROR_DISPLAY_MS);
  }

  #send(message) {
    if (this.#socket?.readyState === WebSocket.OPEN) {
      this.#socket.send(JSON.stringify(message));
    }
  }
}
