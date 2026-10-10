import { describe, expect, it } from 'vitest';
import {
  cardLabel,
  categoryLabel,
  dealDelay,
  describeEvent,
  rotateSeats,
  seatPosition,
  soundFor,
  totalPot,
  winningCards,
} from './table.js';

const seat = (n, extra = {}) => ({ seat: n, nickname: `J${n}`, stack: 1000, bet: 0, ...extra });

describe('rotateSeats', () => {
  it('puts my seat first, then the others clockwise', () => {
    const seats = [seat(0), null, seat(2), null, null, seat(5), null, null];

    const rotated = rotateSeats(seats, 5);

    expect(rotated.map((s) => s.number)).toEqual([5, 6, 7, 0, 1, 2, 3, 4]);
    expect(rotated[0].seat.nickname).toBe('J5');
    expect(rotated[1].seat).toBeNull();
  });

  it('keeps the table order for a spectator', () => {
    const rotated = rotateSeats(Array(8).fill(null), null);

    expect(rotated.map((s) => s.number)).toEqual([0, 1, 2, 3, 4, 5, 6, 7]);
  });
});

describe('seatPosition', () => {
  it('places the first position at the bottom centre', () => {
    const { left, top } = seatPosition(0);

    expect(left).toBeCloseTo(50);
    expect(top).toBeGreaterThan(80);
  });

  it('goes clockwise: the next seat is on the bottom left', () => {
    const { left, top } = seatPosition(1);

    expect(left).toBeLessThan(50);
    expect(top).toBeGreaterThan(50);
  });

  it('puts the opposite seat at the top', () => {
    expect(seatPosition(4).top).toBeLessThan(20);
  });
});

describe('totalPot', () => {
  it('adds the pots and the bets of the round', () => {
    const state = {
      hand: { pots: [{ amount: 100, seats: [0, 1] }, { amount: 40, seats: [1] }] },
      seats: [seat(0, { bet: 20 }), seat(1, { bet: 30 }), null],
    };

    expect(totalPot(state)).toBe(190);
  });

  it('is zero without a hand', () => {
    expect(totalPot({ hand: null, seats: [] })).toBe(0);
  });
});

describe('cardLabel', () => {
  it('splits a code into rank and suit symbol', () => {
    expect(cardLabel('AS')).toEqual({ rank: 'A', suit: '♠', red: false });
    expect(cardLabel('TD')).toEqual({ rank: '10', suit: '♦', red: true });
    expect(cardLabel('9H')).toEqual({ rank: '9', suit: '♥', red: true });
  });
});

describe('categoryLabel', () => {
  it('names hands in French', () => {
    expect(categoryLabel('threeOfAKind')).toBe('Brelan');
    expect(categoryLabel('straightFlush')).toBe('Quinte flush');
  });
});

describe('describeEvent', () => {
  const seats = [seat(0, { nickname: 'Mamie' }), seat(1, { nickname: 'Papa' })];

  it('describes actions', () => {
    expect(describeEvent({ name: 'action', seat: 0, action: 'raise', amount: 30 }, seats)).toBe('Mamie relance à 30');
    expect(describeEvent({ name: 'action', seat: 1, action: 'call', amount: 30 }, seats)).toBe('Papa suit');
    expect(describeEvent({ name: 'action', seat: 1, action: 'fold', amount: 0 }, seats)).toBe('Papa se couche');
  });

  it('describes the board and the winners', () => {
    expect(describeEvent({ name: 'board', stage: 'flop', cards: ['AS', 'KD', '7C'] }, seats)).toBe('Flop : A♠ K♦ 7♣');
    expect(describeEvent({ name: 'win', payouts: [{ amount: 450, winners: { 1: 450 } }] }, seats)).toBe('Papa gagne 450');
  });

  it('ignores events it has nothing to say about', () => {
    expect(describeEvent({ name: 'showdown' }, seats)).toBeNull();
  });
});

describe('winningCards', () => {
  it('returns the best five cards of the main pot winners', () => {
    const lastResult = {
      payouts: [{ amount: 450, winners: { 2: 450 } }, { amount: 500, winners: { 1: 500 } }],
      shownHands: {
        1: { category: 'onePair', cards: ['AH', 'AD', 'KS', '9H', '7D'] },
        2: { category: 'threeOfAKind', cards: ['KS', 'KD', 'KC', '9H', '7D'] },
      },
    };

    expect(winningCards({ lastResult })).toEqual(['KS', 'KD', 'KC', '9H', '7D']);
  });

  it('is empty without a showdown', () => {
    expect(winningCards({ lastResult: { payouts: [{ amount: 15, winners: { 0: 15 } }], shownHands: {} } })).toEqual([]);
    expect(winningCards({ lastResult: null })).toEqual([]);
  });
});

describe('dealDelay', () => {
  it('deals the flop one card after the other', () => {
    expect([0, 1, 2].map((i) => dealDelay(i, 0))).toEqual([0, 150, 300]);
  });

  it('shows the turn or the river at once when it comes alone', () => {
    expect(dealDelay(3, 3)).toBe(0);
    expect(dealDelay(4, 4)).toBe(0);
  });

  it('spreads the streets when they all come at once after an all-in', () => {
    expect([0, 1, 2, 3, 4].map((i) => dealDelay(i, 0))).toEqual([0, 150, 300, 1000, 2000]);
    expect([3, 4].map((i) => dealDelay(i, 3))).toEqual([0, 1000]);
  });
});

describe('soundFor', () => {
  it('gives each event its sound', () => {
    expect(soundFor({ name: 'handStarted' })).toBe('deal');
    expect(soundFor({ name: 'board', stage: 'flop', cards: ['AS', 'KD', '7C'] })).toBe('card');
    expect(soundFor({ name: 'action', action: 'fold' })).toBe('fold');
    expect(soundFor({ name: 'action', action: 'check' })).toBe('check');
    expect(soundFor({ name: 'action', action: 'raise' })).toBe('chips');
    expect(soundFor({ name: 'win', payouts: [] })).toBe('win');
  });

  it('stays silent for the showdown itself', () => {
    expect(soundFor({ name: 'showdown' })).toBeNull();
  });
});
