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
    case 'blindsUp':
      return `Les blindes montent : ${event.smallBlind} / ${event.bigBlind}`;
    case 'gameWon':
      return `${name(event.seat)} remporte la partie !`;
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

/** Name of the sound for an event (see sounds.js), or null for a silent one. */
export function soundFor(event) {
  switch (event.name) {
    case 'handStarted':
      return 'deal';
    case 'board':
      return 'card';
    case 'win':
      return 'win';
    case 'blindsUp':
      return 'blindsUp';
    case 'gameWon':
      return 'victory';
    case 'action':
      return event.action === 'fold' ? 'fold' : event.action === 'check' ? 'check' : 'chips';
    default:
      return null;
  }
}

/** 'small' or 'big' when this seat posted a blind in the hand, as the server tells it; otherwise null. */
export function blindOf(hand, seat) {
  if (!hand) {
    return null;
  }
  if (hand.smallBlindSeat === seat) {
    return 'small';
  }
  return hand.bigBlindSeat === seat ? 'big' : null;
}

/** In the header: "Blindes 10 / 20 · 15 / 30 dans 7 mains", or just the blinds when they are fixed. */
export function headerBlindsText(table) {
  const blinds = `Blindes ${table.smallBlind} / ${table.bigBlind}`;
  if (!table.nextBlinds) {
    return blinds;
  }
  const next = `${table.nextBlinds.smallBlind} / ${table.nextBlinds.bigBlind}`;
  const hands = table.handsUntilNextLevel;
  if (hands === 0) {
    return `${blinds} · ${next} à la prochaine main`;
  }
  return `${blinds} · ${next} dans ${hands} main${hands > 1 ? 's' : ''}`;
}

/**
 * How the shown cards of the opponents appear at the showdown (docs/UI-TABLE-A.md, V6), by number of opponents:
 * under the seat with the full hand name while there is room, in place of the avatar with the short name beyond.
 * labelWidth: the hand name stays on one line within it, so that it never reaches the board or a neighbour.
 * nudge: with three opponents, NO and NE lean outwards by this many pixels to leave room for the label of N.
 */
export function showdownStyle(opponents) {
  if (opponents <= 2) {
    return { placement: 'below', width: 52, fullNames: true, labelWidth: 186, nudge: 0 };
  }
  if (opponents === 3) {
    return { placement: 'below', width: 44, fullNames: true, labelWidth: 140, nudge: 14 };
  }
  return { placement: 'avatar', width: 44, fullNames: false, labelWidth: null, nudge: 0 };
}

/** The line under the board: "Léo gagne 480", or "Léo et Mamie se partagent 480". */
export function winnersLine(result, seats) {
  const won = new Map();
  for (const payout of result.payouts) {
    for (const [seat, amount] of Object.entries(payout.winners)) {
      won.set(seat, (won.get(seat) ?? 0) + amount);
    }
  }
  const names = [...won.keys()].map((seat) => seats[seat]?.nickname ?? `Siège ${Number(seat) + 1}`);
  const total = [...won.values()].reduce((sum, amount) => sum + amount, 0);
  if (names.length === 1) {
    return `${names[0]} gagne ${total}`;
  }
  return `${names.slice(0, -1).join(', ')} et ${names.at(-1)} se partagent ${total}`;
}
