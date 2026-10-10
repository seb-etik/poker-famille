import { describe, expect, it } from 'vitest';
import { avatarEmoji, avatarKind, avatarSrc, EMOJI_AVATARS, gallery } from './avatars.js';

describe('avatars', () => {
  it('draws a Lorelei avatar as an SVG data URI, the same for the same id', () => {
    const src = avatarSrc('lorelei:Mamie');

    expect(src).toMatch(/^data:image\/svg\+xml/);
    expect(avatarSrc('lorelei:Mamie')).toBe(src);
    expect(avatarSrc('lorelei:Papa')).not.toBe(src);
  });

  it('shows emoji avatars as emoji', () => {
    expect(avatarSrc('fox')).toBeNull();
    expect(avatarEmoji('fox')).toBe('🦊');
    expect(avatarEmoji('inconnu')).toBe('🙂');
  });

  it('tells faces from animals', () => {
    expect(avatarKind('lorelei:Mamie')).toBe('faces');
    expect(avatarKind('fox')).toBe('animals');
  });

  it('offers twelve animals', () => {
    expect(EMOJI_AVATARS).toHaveLength(12);
    expect(EMOJI_AVATARS.every((id) => avatarEmoji(id) !== '🙂')).toBe(true);
  });

  it('offers pages of distinct faces that fit the protocol', () => {
    const ids = gallery(0);

    expect(ids).toHaveLength(12);
    expect(new Set(ids).size).toBe(12);
    expect(ids.every((id) => id.startsWith('lorelei:') && id.length <= 64)).toBe(true);
    expect(gallery(1)).not.toEqual(ids);
  });
});
