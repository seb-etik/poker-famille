// Avatars drawn locally with DiceBear's Lorelei style (design CC0, by Lisa Wischofsky): no network call.
// An avatar id is "lorelei:<seed>"; the server only stores it.
import { createAvatar } from '@dicebear/core';
import * as lorelei from '@dicebear/lorelei';

const PREFIX = 'lorelei:';
const GALLERY_SIZE = 12;
const BACKGROUNDS = ['b6e3f4', 'c0aede', 'd1d4f9', 'ffd5dc', 'ffdfbf'];

// Emoji avatars of the first version, still found in saved profiles.
const LEGACY_EMOJIS = {
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

export function legacyEmoji(id) {
  return LEGACY_EMOJIS[id] ?? '🙂';
}

/** One page of avatars to choose from; another page gives other faces. */
export function gallery(page) {
  return Array.from({ length: GALLERY_SIZE }, (_, i) => `${PREFIX}famille-${page * GALLERY_SIZE + i}`);
}
