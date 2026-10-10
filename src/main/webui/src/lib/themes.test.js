import { describe, expect, it } from 'vitest';
import { backUrl, faceUrl, THEMES, themeOrDefault } from './themes.js';

describe('themes', () => {
  it('serves faces from the theme folder', () => {
    expect(faceUrl('classique', 'TD')).toBe('/themes/classique/TD.svg');
  });

  it('a theme may borrow the faces of another one and keep its own back', () => {
    expect(faceUrl('famille', 'AS')).toBe('/themes/classique/AS.svg');
    expect(backUrl('famille')).toBe('/themes/famille/back.svg');
  });

  it('falls back to the first theme for an unknown id', () => {
    expect(themeOrDefault('disparu')).toBe(THEMES[0]);
    expect(backUrl('disparu')).toBe('/themes/classique/back.svg');
  });
});
