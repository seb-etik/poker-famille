import { describe, expect, it } from 'vitest';
import { seatLayout } from './layout.js';

const slots = (layout) => layout.map((place) => `${place.number}:${place.slot}`);

describe('seatLayout', () => {
  it('places one opponent at the top', () => {
    expect(slots(seatLayout([3], 0))).toEqual(['3:N']);
  });

  it('fills the top first, in the order of play from my left', () => {
    expect(slots(seatLayout([3, 5], 0))).toEqual(['3:NO', '5:NE']);
    expect(slots(seatLayout([1, 3, 6], 5))).toEqual(['6:NO', '1:N', '3:NE']);
    expect(slots(seatLayout([1, 2, 3, 4], 0))).toEqual(['1:O1', '2:NO', '3:NE', '4:E1']);
    expect(slots(seatLayout([1, 2, 3, 4, 5], 0))).toEqual(['1:O1', '2:NO', '3:N', '4:NE', '5:E1']);
  });

  it('uses the low sides only from seven players on', () => {
    expect(slots(seatLayout([1, 2, 3, 4, 5, 6], 0)).map((s) => s.split(':')[1])).toEqual(['O2', 'O1', 'NO', 'NE', 'E1', 'E2']);
    expect(slots(seatLayout([1, 2, 3, 4, 5, 6, 7], 0)).map((s) => s.split(':')[1])).toEqual([
      'O2', 'O1', 'NO', 'N', 'NE', 'E1', 'E2',
    ]);
  });

  it('gives a spectator of a full table the bottom place too', () => {
    expect(slots(seatLayout([0, 1, 2, 3, 4, 5, 6, 7], null))[0]).toBe('0:S');
  });

  it('converts the 390 × 500 coordinates to percentages, with the bet between the seat and the centre', () => {
    const [north] = seatLayout([3], 0);

    expect(north.left).toBeCloseTo(50);
    expect(north.top).toBeCloseTo(11.6); // 58 px
    expect(north.betTop).toBeGreaterThan(north.top);
    expect(north.betTop).toBeLessThan(53.2); // above the board, which starts at 266 px
  });

  it('keeps the side seats above the board and the low ones beside it', () => {
    const places = seatLayout([1, 2, 3, 4, 5, 6, 7], 0);
    const bySlot = Object.fromEntries(places.map((place) => [place.slot, place]));

    expect(bySlot.O1.top).toBeCloseTo(37.6); // 188 px
    expect(bySlot.NO.top).toBeCloseTo(17.6); // 88 px
    // 420 px: the avatar and its fan of hidden cards (8 px above it) start under the board, which ends at 385 px.
    expect(bySlot.O2.top).toBeCloseTo(84);
    expect(bySlot.O1.betTop).toBeLessThan(53.2);
  });
});
