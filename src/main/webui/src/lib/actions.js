// The four action buttons of the table, always in the same order and at the same place
// (docs/UI-TABLE-A.md). A button that is not allowed stays where it is, greyed out.

/**
 * @param allowed `allowedActions` from the server
 * @param stack chips in front of the player, shown on the Tapis button
 * @param amount the raise chosen with the slider, as a total for the round
 * @returns [Coucher, Check|Suivre, Relancer, Tapis]: { key, label, amount, action, actionAmount, enabled }
 */
export function actionButtons(allowed, stack, amount) {
  const can = (type) => allowed.types.includes(type);
  const raiseType = can('bet') ? 'bet' : can('raise') ? 'raise' : null;

  const fold = { key: 'fold', label: 'Coucher', amount: null, action: 'fold', actionAmount: null, enabled: can('fold') };

  // "Suivre" only when there is something to call; otherwise "Check" (greyed when it is not my turn).
  const call = can('call')
    ? { key: 'call', label: 'Suivre', amount: allowed.callAmount, action: 'call', actionAmount: null, enabled: true }
    : { key: 'call', label: 'Check', amount: null, action: 'check', actionAmount: null, enabled: can('check') };

  const raise = {
    key: 'raise',
    label: 'Relancer',
    amount: raiseType ? amount : null,
    action: raiseType,
    actionAmount: amount,
    enabled: raiseType !== null,
  };

  // Going all in: a raise to the maximum, or a call when the call already takes every chip.
  let allIn = { key: 'allIn', label: 'Tapis', amount: stack, action: null, actionAmount: null, enabled: false };
  if (raiseType) {
    allIn = { ...allIn, action: raiseType, actionAmount: allowed.maxAmount, enabled: true };
  } else if (can('call') && allowed.callAmount === stack) {
    allIn = { ...allIn, action: 'call', enabled: true };
  }

  return [fold, call, raise, allIn];
}

/** In my strip when it is my turn: "À toi · 20 pour suivre · 28 s", or null. */
export function myTurnText(allowed, seconds) {
  if (!allowed) {
    return null;
  }
  const call = allowed.types.includes('call') ? ` · ${allowed.callAmount} pour suivre` : '';
  return `À toi${call} · ${seconds} s`;
}

/**
 * Whether "Relancer" opens the slider (a range of amounts to choose from) rather than raising at once.
 * The slider takes the place of the buttons, so that the bar, and the table above it, never change height.
 */
export function opensSlider(allowed) {
  if (!allowed) {
    return false;
  }
  const canRaise = allowed.types.includes('bet') || allowed.types.includes('raise');
  return canRaise && allowed.maxAmount > allowed.minAmount;
}
