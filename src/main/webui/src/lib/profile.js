import { readJson, writeJson } from './storage.js';

export const AVATARS = [
  { id: 'fox', emoji: '🦊' },
  { id: 'bear', emoji: '🐻' },
  { id: 'panda', emoji: '🐼' },
  { id: 'frog', emoji: '🐸' },
  { id: 'tiger', emoji: '🐯' },
  { id: 'lion', emoji: '🦁' },
  { id: 'monkey', emoji: '🐵' },
  { id: 'koala', emoji: '🐨' },
  { id: 'owl', emoji: '🦉' },
  { id: 'cat', emoji: '🐱' },
  { id: 'dog', emoji: '🐶' },
  { id: 'rabbit', emoji: '🐰' },
];

export function avatarEmoji(id) {
  return AVATARS.find((avatar) => avatar.id === id)?.emoji ?? '🙂';
}

// localStorage is shared by all the tabs of a site: "?as=name" gives a tab its own profile,
// so that several players can be tested from one browser.
function storageKey() {
  const slot = new URLSearchParams(location.search).get('as');
  return slot ? `poker.profile.${slot}` : 'poker.profile';
}

export function loadProfile() {
  const saved = readJson(storageKey());
  const profile = {
    playerId: saved?.playerId ?? newPlayerId(),
    nickname: saved?.nickname ?? '',
    avatar: saved?.avatar ?? AVATARS[0].id,
    theme: saved?.theme ?? 'classique',
  };
  saveProfile(profile);
  return profile;
}

export function saveProfile(profile) {
  writeJson(storageKey(), {
    playerId: profile.playerId,
    nickname: profile.nickname,
    avatar: profile.avatar,
    theme: profile.theme,
  });
}

// crypto.randomUUID only exists on HTTPS or localhost: a phone reaching the dev server
// over the local network has getRandomValues only.
function newPlayerId() {
  if (crypto.randomUUID) {
    return crypto.randomUUID();
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}
