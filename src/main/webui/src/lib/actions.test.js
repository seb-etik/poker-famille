import { describe, expect, it } from 'vitest';
import { actionButtons, myTurnText } from './actions.js';

const allowed = (types, extra = {}) => ({ types, callAmount: 0, minAmount: 0, maxAmount: 0, ...extra });
const byKey = (buttons) => Object.fromEntries(buttons.map((b) => [b.key, b]));

describe('actionButtons', () => {
  it('always gives four buttons in the same order', () => {
    const buttons = actionButtons(allowed(['check', 'bet'], { minAmount: 20, maxAmount: 960 }), 960, 20);

    expect(buttons.map((b) => b.key)).toEqual(['fold', 'call', 'raise', 'allIn']);
  });

  it('when checking is free: Check, Relancer at the slider amount, Tapis; Coucher greyed', () => {
    const b = byKey(actionButtons(allowed(['check', 'bet'], { minAmount: 20, maxAmount: 960 }), 960, 60));

    expect(b.fold.enabled).toBe(false);
    expect(b.call).toMatchObject({ label: 'Check', amount: null, action: 'check', enabled: true });
    expect(b.raise).toMatchObject({ label: 'Relancer', amount: 60, action: 'bet', actionAmount: 60, enabled: true });
    expect(b.allIn).toMatchObject({ label: 'Tapis', amount: 960, action: 'bet', actionAmount: 960, enabled: true });
  });

  it('facing a bet: Suivre with the amount to call', () => {
    const b = byKey(actionButtons(allowed(['fold', 'call', 'raise'], { callAmount: 40, minAmount: 80, maxAmount: 1000 }), 960, 80));

    expect(b.fold).toMatchObject({ label: 'Coucher', action: 'fold', enabled: true });
    expect(b.call).toMatchObject({ label: 'Suivre', amount: 40, action: 'call', enabled: true });
    expect(b.raise).toMatchObject({ label: 'Relancer', action: 'raise', actionAmount: 80 });
    // The total for the round is 1000, but the button shows the chips left in front of the player.
    expect(b.allIn).toMatchObject({ amount: 960, action: 'raise', actionAmount: 1000 });
  });

  it('when calling takes the whole stack, Tapis is a call', () => {
    const b = byKey(actionButtons(allowed(['fold', 'call'], { callAmount: 300 }), 300, null));

    expect(b.raise.enabled).toBe(false);
    expect(b.allIn).toMatchObject({ amount: 300, action: 'call', enabled: true });
  });

  it('greys Relancer and Tapis when raising is not allowed', () => {
    const b = byKey(actionButtons(allowed(['fold', 'call'], { callAmount: 40 }), 960, null));

    expect(b.raise.enabled).toBe(false);
    expect(b.allIn.enabled).toBe(false);
    expect(b.allIn.amount).toBe(960);
  });
});

describe('actionButtons when it is not my turn', () => {
  it('greys every button and shows Check rather than an empty Suivre', () => {
    const buttons = actionButtons(allowed([]), 990, 0);

    expect(buttons.every((b) => !b.enabled)).toBe(true);
    expect(byKey(buttons).call).toMatchObject({ label: 'Check', amount: null });
  });
});

describe('myTurnText', () => {
  it('tells me it is my turn, what to call and the seconds left', () => {
    expect(myTurnText(allowed(['check', 'bet']), 28)).toBe('À toi · 28 s');
    expect(myTurnText(allowed(['fold', 'call', 'raise'], { callAmount: 20 }), 28)).toBe('À toi · 20 pour suivre · 28 s');
  });

  it('says nothing when it is not my turn', () => {
    expect(myTurnText(null, null)).toBeNull();
  });
});
