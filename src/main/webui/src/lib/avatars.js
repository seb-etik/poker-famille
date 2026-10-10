// Two kinds of avatars, both drawn locally (no network call); the server only stores the id:
// - faces: DiceBear's Lorelei style (design CC0, by Lisa Wischofsky), id "lorelei:<seed>";
// - animals: emoji, id "fox", "bear"…
import { createAvatar } from '@dicebear/core';
import * as lorelei from '@dicebear/lorelei';

const PREFIX = 'lorelei:';
const GALLERY_SIZE = 12;
const BACKGROUNDS = ['b6e3f4', 'c0aede', 'd1d4f9', 'ffd5dc', 'ffdfbf'];

const EMOJIS = {
  fox: '🦊',
  bear: '🐻',
  panda: '🐼',
  frog: '🐸',
  tiger: '🐯',
  lion: '🦁',
  monkey: '🐵',
  koala: '🐨',
  owl: '🦉',
  cat: '🐱',
  dog: '🐶',
  rabbit: '🐰',
};

const cache = new Map();

/** Data URI of a Lorelei avatar, or null for an emoji avatar. */
export function avatarSrc(id) {
  if (!id?.startsWith(PREFIX)) {
    return null;
  }
  if (!cache.has(id)) {
    const avatar = createAvatar(lorelei, { seed: id.slice(PREFIX.length), backgroundColor: BACKGROUNDS });
    cache.set(id, avatar.toDataUri());
  }
  return cache.get(id);
}

export const EMOJI_AVATARS = Object.keys(EMOJIS);

export function avatarEmoji(id) {
  return EMOJIS[id] ?? '🙂';
}

/** 'faces' or 'animals': which tab of the gallery holds this avatar. */
export function avatarKind(id) {
  return id?.startsWith(PREFIX) ? 'faces' : 'animals';
}

/** One page of faces to choose from; another page gives other faces. */
export function gallery(page) {
  return Array.from({ length: GALLERY_SIZE }, (_, i) => `${PREFIX}famille-${page * GALLERY_SIZE + i}`);
}
