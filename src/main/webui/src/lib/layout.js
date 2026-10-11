// Where the opponents sit on screen (docs/UI-TABLE-A.md, V2): the place depends on how many opponents
// there are, not on their seat numbers, so that nobody lands on the board or the pot.

// Coordinates in the 390 × 500 box of the table: avatar centre, then the bet between the seat and the centre
// (always above the board band, 280 to 373 px). S is the bottom place, only for a spectator of a full table.
const BOX = { width: 390, height: 500 };
const SLOTS = {
  N: { x: 195, y: 62, betX: 195, betY: 150 },
  NO: { x: 100, y: 92, betX: 158, betY: 172 },
  NE: { x: 290, y: 92, betX: 232, betY: 172 },
  O1: { x: 44, y: 196, betX: 108, betY: 200 },
  E1: { x: 346, y: 196, betX: 282, betY: 200 },
  O2: { x: 44, y: 410, betX: 108, betY: 410 },
  E2: { x: 346, y: 410, betX: 282, betY: 410 },
  S: { x: 195, y: 455, betX: 130, betY: 455 },
};

// From my left to my right, for 1 to 7 opponents: the top first, the low sides from seven players on.
const ARRANGEMENTS = [
  [],
  ['N'],
  ['NO', 'NE'],
  ['NO', 'N', 'NE'],
  ['O1', 'NO', 'NE', 'E1'],
  ['O1', 'NO', 'N', 'NE', 'E1'],
  ['O2', 'O1', 'NO', 'NE', 'E1', 'E2'],
  ['O2', 'O1', 'NO', 'N', 'NE', 'E1', 'E2'],
  ['S', 'O2', 'O1', 'NO', 'N', 'NE', 'E1', 'E2'],
];

const SEAT_COUNT = 8;

/**
 * @param numbers seat numbers to show (opponents, or every place while waiting)
 * @param mySeat my seat, or null for a spectator
 * @returns in order of play from my left: { number, slot, left, top, betLeft, betTop } in % of the box
 */
export function seatLayout(numbers, mySeat) {
  const start = mySeat === null ? 0 : mySeat + 1;
  const ordered = [...numbers].sort(
    (a, b) => ((a - start + SEAT_COUNT) % SEAT_COUNT) - ((b - start + SEAT_COUNT) % SEAT_COUNT),
  );
  const arrangement = ARRANGEMENTS[ordered.length];
  return ordered.map((number, i) => {
    const slot = SLOTS[arrangement[i]];
    return {
      number,
      slot: arrangement[i],
      left: (100 * slot.x) / BOX.width,
      top: (100 * slot.y) / BOX.height,
      betLeft: (100 * slot.betX) / BOX.width,
      betTop: (100 * slot.betY) / BOX.height,
    };
  });
}
