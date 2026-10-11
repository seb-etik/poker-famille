import { describe, expect, it } from 'vitest';
import { handName } from './hands.js';

// Cards come from the server ordered by group (the trips first, then the pair…), high to low.
describe('handName', () => {
  it.each([
    ['highCard', ['AS', 'JD', '8C', '6H', '3S'], 'Hauteur as'],
    ['onePair', ['KS', 'KD', '9C', '6H', '3S'], 'Paire de rois'],
    ['onePair', ['AS', 'AD', '9C', '6H', '3S'], "Paire d'as"],
    ['twoPair', ['KS', 'KD', '9C', '9H', '3S'], 'Double paire rois et 9'],
    ['threeOfAKind', ['KS', 'KD', 'KC', '9H', '3S'], 'Brelan de rois'],
    ['threeOfAKind', ['8S', '8D', '8C', 'KH', '3S'], 'Brelan de 8'],
    ['straight', ['KS', 'QD', 'JC', 'TH', '9S'], 'Quinte au roi'],
    ['straight', ['5S', '4D', '3C', '2H', 'AS'], 'Quinte au 5'],
    ['flush', ['QH', 'JH', '8H', '4H', '2H'], 'Couleur à la dame'],
    ['flush', ['AH', 'JH', '8H', '4H', '2H'], "Couleur à l'as"],
    ['fullHouse', ['KS', 'KD', 'KC', '9H', '9S'], 'Full aux rois par les 9'],
    ['fourOfAKind', ['QS', 'QD', 'QC', 'QH', '3S'], 'Carré de dames'],
    ['straightFlush', ['9H', '8H', '7H', '6H', '5H'], 'Quinte flush au 9'],
    ['straightFlush', ['AS', 'KS', 'QS', 'JS', 'TS'], 'Quinte flush royale'],
  ])('%s %j → %s', (category, cards, expected) => {
    expect(handName(category, cards)).toBe(expected);
  });
});
