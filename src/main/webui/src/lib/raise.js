// Positions of the raise slider (docs/UI-TABLE-A.md, V4): round amounts, plus the minimum and the stack.

/** 1, 2, 5, then 10, 25, 50, 100, 250, 500, 1 000… up to `limit`. */
function roundSteps(limit) {
  const steps = [1, 2, 5];
  for (let power = 10; power <= limit; power *= 10) {
    steps.push(power, 2.5 * power, 5 * power);
  }
  return steps;
}

/** Half the big blind, rounded down to a round step (at least 1). */
export function raiseStep(bigBlind) {
  const half = bigBlind / 2;
  return roundSteps(half).filter((step) => step <= half).at(-1) ?? 1;
}

/**
 * The minimum, then every multiple of the step strictly between the minimum and the stack, then the stack:
 * both ends stay reachable even when they are not round (35, 40, 50… 950, 957).
 */
export function raiseStops(min, max, step) {
  const stops = [min];
  for (let amount = (Math.floor(min / step) + 1) * step; amount < max; amount += step) {
    stops.push(amount);
  }
  if (max > min) {
    stops.push(max);
  }
  return stops;
}
