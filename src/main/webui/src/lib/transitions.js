// Svelte transitions that move cards and chips from or to the centre of the table (the .felt element).
import { cubicOut } from 'svelte/easing';

function reducedMotion() {
  return matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function offsetToCenter(node) {
  const felt = document.querySelector('.felt');
  if (!felt) {
    return { dx: 0, dy: 0 };
  }
  const table = felt.getBoundingClientRect();
  const own = node.getBoundingClientRect();
  return {
    dx: table.left + table.width / 2 - (own.left + own.width / 2),
    dy: table.top + table.height / 2 - (own.top + own.height / 2),
  };
}

/** in: arrives from the centre of the table, like a dealt card. */
export function fromCenter(node, { delay = 0, duration = 450 } = {}) {
  if (reducedMotion()) {
    return { duration: 0 };
  }
  const { dx, dy } = offsetToCenter(node);
  return {
    delay,
    duration,
    easing: cubicOut,
    css: (t, u) => `transform: translate(${dx * u}px, ${dy * u}px) scale(${0.5 + 0.5 * t}); opacity: ${Math.min(1, t * 3)}`,
  };
}

/** out: leaves towards the centre of the table, like chips pushed into the pot. */
export function toCenter(node, { duration = 450 } = {}) {
  if (reducedMotion()) {
    return { duration: 0 };
  }
  const { dx, dy } = offsetToCenter(node);
  return {
    duration,
    easing: cubicOut,
    css: (t, u) => `transform: translate(${dx * u}px, ${dy * u}px); opacity: ${t}`,
  };
}

/** in: drops onto the table from above, for the board. */
export function dropIn(node, { delay = 0, duration = 350 } = {}) {
  if (reducedMotion()) {
    return { duration: 0 };
  }
  return {
    delay,
    duration,
    easing: cubicOut,
    css: (t, u) => `transform: translateY(${-40 * u}px) rotate(${-8 * u}deg); opacity: ${t}`,
  };
}
