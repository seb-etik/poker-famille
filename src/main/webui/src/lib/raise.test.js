import { describe, expect, it } from 'vitest';
import { raiseStep, raiseStops } from './raise.js';

describe('raiseStep', () => {
  it.each([
    [20, 10],
    [30, 10],
    [50, 25],
    [100, 50],
    [200, 100],
  ])('big blind %i → step %i', (bigBlind, step) => {
    expect(raiseStep(bigBlind)).toBe(step);
  });

  it('rounds half the big blind down to 1, 2, 5, 10, 25, 50, 100, 250, 500, 1000…', () => {
    expect(raiseStep(16)).toBe(5);
    expect(raiseStep(4)).toBe(2);
    expect(raiseStep(3000)).toBe(1000);
  });

  it('is at least 1', () => {
    expect(raiseStep(1)).toBe(1);
  });
});

describe('raiseStops', () => {
  it('goes from the minimum to the stack by multiples of the step', () => {
    expect(raiseStops(40, 100, 20)).toEqual([40, 60, 80, 100]);
  });

  it('keeps a minimum that is not round, then the round amounts', () => {
    expect(raiseStops(35, 70, 10)).toEqual([35, 40, 50, 60, 70]);
  });

  it('keeps the exact stack at the end', () => {
    expect(raiseStops(900, 957, 10).slice(-3)).toEqual([940, 950, 957]);
  });

  it('has a single stop when the minimum is the whole stack', () => {
    expect(raiseStops(300, 300, 10)).toEqual([300]);
  });
});
