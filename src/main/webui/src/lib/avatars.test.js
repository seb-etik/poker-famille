import { describe, expect, it } from 'vitest';
import { avatarSrc, gallery, legacyEmoji } from './avatars.js';

describe('avatars', () => {
  it('draws a Lorelei avatar as an SVG data URI, the same for the same id', () => {
    const src = avatarSrc('lorelei:Mamie');

    expect(src).toMatch(/^data:image\/svg\+xml/);
    expect(avatarSrc('lorelei:Mamie')).toBe(src);
    expect(avatarSrc('lorelei:Papa')).not.toBe(src);
  });

  it('keeps the emoji avatars of the first version', () => {
    expect(avatarSrc('fox')).toBeNull();
    expect(legacyEmoji('fox')).toBe('🦊');
    expect(legacyEmoji('inconnu')).toBe('🙂');
  });

  it('offers a gallery of distinct ids that fit the protocol', () => {
    const ids = gallery(0);

    expect(ids).toHaveLength(12);
    expect(new Set(ids).size).toBe(12);
    expect(ids.every((id) => id.startsWith('lorelei:') && id.length <= 64)).toBe(true);
    expect(gallery(1)).not.toEqual(ids);
  });
});
