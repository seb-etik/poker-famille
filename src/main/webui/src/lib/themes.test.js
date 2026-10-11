import { describe, expect, it } from 'vitest';
import { backUrl, faceUrl } from './themes.js';

describe('card images', () => {
  it('serves faces named like the protocol codes', () => {
    expect(faceUrl('TD')).toBe('/themes/classique/TD.svg');
  });

  it('serves the back of the card', () => {
    expect(backUrl()).toBe('/themes/classique/back.svg');
  });
});
