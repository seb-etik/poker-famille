// Table sounds, synthesised with the Web Audio API: no file to download.
// Browsers only allow sound after a user gesture: the context starts on the first touch.
import { readJson, writeJson } from './storage.js';

const MUTED_KEY = 'poker.muted';

let context = null;
let muted = readJson(MUTED_KEY) === true;

function audio() {
  if (!context) {
    const AudioContext = window.AudioContext ?? window.webkitAudioContext;
    if (!AudioContext) {
      return null;
    }
    context = new AudioContext();
  }
  if (context.state === 'suspended') {
    context.resume();
  }
  return context;
}

if (typeof document !== 'undefined') {
  document.addEventListener('pointerdown', () => audio(), { once: true });
}

function envelope(ctx, start, duration, volume) {
  const gain = ctx.createGain();
  gain.gain.setValueAtTime(volume, start);
  gain.gain.exponentialRampToValueAtTime(0.001, start + duration);
  gain.connect(ctx.destination);
  return gain;
}

function tone(ctx, frequency, delay, duration, volume, type = 'sine') {
  const start = ctx.currentTime + delay;
  const oscillator = ctx.createOscillator();
  oscillator.type = type;
  oscillator.frequency.value = frequency;
  oscillator.connect(envelope(ctx, start, duration, volume));
  oscillator.start(start);
  oscillator.stop(start + duration);
}

function noise(ctx, delay, duration, cutoff, volume) {
  const start = ctx.currentTime + delay;
  const buffer = ctx.createBuffer(1, Math.ceil(ctx.sampleRate * duration), ctx.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < data.length; i++) {
    data[i] = Math.random() * 2 - 1;
  }
  const source = ctx.createBufferSource();
  source.buffer = buffer;
  const filter = ctx.createBiquadFilter();
  filter.type = 'bandpass';
  filter.frequency.value = cutoff;
  source.connect(filter);
  filter.connect(envelope(ctx, start, duration, volume));
  source.start(start);
}

const SOUNDS = {
  // A few quick card slides.
  deal: (ctx) => [0, 0.09, 0.18, 0.27].forEach((delay) => noise(ctx, delay, 0.06, 3000, 0.5)),
  card: (ctx) => noise(ctx, 0, 0.08, 2500, 0.5),
  // Chips clicking together.
  chips: (ctx) => {
    tone(ctx, 2300, 0, 0.05, 0.12, 'triangle');
    tone(ctx, 2700, 0.06, 0.05, 0.1, 'triangle');
    tone(ctx, 2100, 0.11, 0.07, 0.08, 'triangle');
  },
  // Two knocks on the table.
  check: (ctx) => {
    tone(ctx, 170, 0, 0.09, 0.5);
    tone(ctx, 150, 0.13, 0.09, 0.45);
  },
  fold: (ctx) => noise(ctx, 0, 0.3, 700, 0.3),
  win: (ctx) => [523, 659, 784, 1047].forEach((frequency, i) => tone(ctx, frequency, i * 0.1, 0.3, 0.15, 'triangle')),
  // Two rising bells: the blinds go up.
  blindsUp: (ctx) => {
    tone(ctx, 660, 0, 0.25, 0.15, 'triangle');
    tone(ctx, 990, 0.18, 0.35, 0.15, 'triangle');
  },
  // A short fanfare: a rising arpeggio, then the final chord held.
  victory: (ctx) => {
    [523, 659, 784].forEach((frequency, i) => tone(ctx, frequency, i * 0.15, 0.25, 0.15, 'triangle'));
    [523, 659, 784, 1047].forEach((frequency) => tone(ctx, frequency, 0.5, 1.2, 0.1, 'triangle'));
  },
  turn: (ctx) => {
    tone(ctx, 880, 0, 0.12, 0.15);
    tone(ctx, 1320, 0.13, 0.2, 0.15);
  },
};

export function play(name) {
  if (muted || !SOUNDS[name]) {
    return;
  }
  try {
    const ctx = audio();
    if (ctx) {
      SOUNDS[name](ctx);
    }
  } catch {
    // No sound is better than a broken table.
  }
}

export function isMuted() {
  return muted;
}

export function setMuted(value) {
  muted = value;
  writeJson(MUTED_KEY, value);
}
