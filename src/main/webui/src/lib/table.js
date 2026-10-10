// Display helpers for the table screen. Pure functions: they never decide the rules,
// they only present the state received from the server.

const SEAT_COUNT = 8;

/** The 8 seats as displayed: my seat first (bottom), then the others clockwise. */
export function rotateSeats(seats, mySeat) {
  const start = mySeat ?? 0;
  return Array.from({ length: SEAT_COUNT }, (_, position) => {
    const number = (start + position) % SEAT_COUNT;
    return { position, number, seat: seats[number] ?? null };
  });
}

/** Position on the oval, in % of the table box: 0 at the bottom, then clockwise. */
export function seatPosition(position) {
  const angle = Math.PI / 2 + (position * 2 * Math.PI) / SEAT_COUNT;
  return {
    left: 50 + 44 * Math.cos(angle),
    top: 50 + 42 * Math.sin(angle),
  };
}

/** Everything on the table: previous rounds' pots plus the bets of the current round. */
export function totalPot(state) {
  if (!state.hand) {
    return 0;
  }
  const pots = state.hand.pots.reduce((sum, pot) => sum + pot.amount, 0);
  const bets = state.seats.reduce((sum, seat) => sum + (seat?.bet ?? 0), 0);
  return pots + bets;
}

/**
 * "Raise to" amounts for the shortcut buttons. A pot-sized raise first calls,
 * then raises by the size of the pot after that call.
 */
export function raiseShortcuts(state) {
  const { minAmount, maxAmount, callAmount } = state.allowedActions;
  const potAfterCall = totalPot(state) + callAmount;
  const clamp = (amount) => Math.min(maxAmount, Math.max(minAmount, Math.floor(amount)));
  return {
    halfPot: clamp(state.hand.currentBet + potAfterCall / 2),
    pot: clamp(state.hand.currentBet + potAfterCall),
    allIn: maxAmount,
  };
}

const SUITS = { S: '♠', H: '♥', D: '♦', C: '♣' };

export function cardLabel(code) {
  const rank = code[0] === 'T' ? '10' : code[0];
  const suit = code[1];
  return { rank, suit: SUITS[suit], red: suit === 'H' || suit === 'D' };
}

function cardText(code) {
  const { rank, suit } = cardLabel(code);
  return rank + suit;
}

const CATEGORIES = {
  highCard: 'Hauteur',
  onePair: 'Paire',
  twoPair: 'Double paire',
  threeOfAKind: 'Brelan',
  straight: 'Quinte',
  flush: 'Couleur',
  fullHouse: 'Full',
  fourOfAKind: 'Carré',
  straightFlush: 'Quinte flush',
};

export function categoryLabel(category) {
  return CATEGORIES[category] ?? category;
}

const STAGES = { flop: 'Flop', turn: 'Turn', river: 'River' };

/** One line for the table log, or null for events not worth a line. */
export function describeEvent(event, seats) {
  const name = (seat) => seats[seat]?.nickname ?? `Siège ${seat + 1}`;
  switch (event.name) {
    case 'handStarted':
      return `Main n° ${event.handNumber}`;
    case 'action':
      return `${name(event.seat)} ${describeAction(event)}`;
    case 'board':
      return `${STAGES[event.stage]} : ${event.cards.map(cardText).join(' ')}`;
    case 'win':
      return event.payouts
        .flatMap((payout) => Object.entries(payout.winners))
        .map(([seat, amount]) => `${name(Number(seat))} gagne ${amount}`)
        .join(', ');
    default:
      return null;
  }
}

function describeAction({ action, amount }) {
  switch (action) {
    case 'fold':
      return 'se couche';
    case 'check':
      return 'checke';
    case 'call':
      return 'suit';
    case 'bet':
      return `mise ${amount}`;
    case 'raise':
      return `relance à ${amount}`;
    default:
      return action;
  }
}

/** The five cards of the main pot winners' best hand, to highlight after a showdown. */
export function winningCards(state) {
  const result = state.lastResult;
  if (!result || result.payouts.length === 0) {
    return [];
  }
  const winners = Object.keys(result.payouts[0].winners);
  return [...new Set(winners.flatMap((seat) => result.shownHands[seat]?.cards ?? []))];
}

const STREET_PAUSE_MS = 1000;
const FLOP_CARD_GAP_MS = 150;

/**
 * Delay before showing board card `index`, when the cards from `firstNewIndex` on just arrived.
 * Flop cards come one after the other; when several streets arrive at once (all-in), each one
 * waits for the previous one.
 */
export function dealDelay(index, firstNewIndex) {
  const street = (i) => (i < 3 ? 0 : i - 2);
  const withinFlop = index < 3 ? index * FLOP_CARD_GAP_MS : 0;
  return (street(index) - street(firstNewIndex)) * STREET_PAUSE_MS + withinFlop;
}
