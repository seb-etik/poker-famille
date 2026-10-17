import { describe, expect, it } from 'vitest';
import { findMe, monthLabel, myLine, profileLine, rankLabel, victoryPhrase } from './leaderboard.js';

const entry = (nickname, wins, games, rank) => ({ nickname, avatar: 'fox', wins, games, rank });

describe('findMe', () => {
  it('finds my line whatever the case, accents or spaces of my nickname', () => {
    const entries = [entry('Zoé', 2, 3, 1), entry('Mamie', 1, 3, 2)];

    expect(findMe(entries, ' mamie ')).toBe(entries[1]);
    expect(findMe(entries, 'ZOE')).toBe(entries[0]);
    expect(findMe(entries, 'Papa')).toBeNull();
  });
});

describe('labels', () => {
  it('writes ranks in French', () => {
    expect(rankLabel(1)).toBe('1er');
    expect(rankLabel(2)).toBe('2ᵉ');
  });

  it('sums up my standing under my profile', () => {
    expect(profileLine(entry('Mamie', 5, 14, 2))).toBe('2ᵉ du classement · 5 victoires');
    expect(profileLine(entry('Mamie', 1, 1, 1))).toBe('1er du classement · 1 victoire');
    expect(profileLine(entry('Mamie', 0, 3, 4))).toBe('4ᵉ du classement · 0 victoire');
  });

  it('gives my line of the leaderboard with my win rate', () => {
    expect(myLine(entry('Mamie', 5, 14, 2))).toBe('Toi : 5 victoires sur 14 parties · 36 %');
    expect(myLine(entry('Mamie', 1, 1, 1))).toBe('Toi : 1 victoire sur 1 partie · 100 %');
  });

  it('names the current month', () => {
    expect(monthLabel(new Date('2026-10-17T12:00:00'))).toBe('Octobre 2026');
  });
});

describe('victoryPhrase', () => {
  const won = (wins, rank, top) => ({ name: 'gameWon', seat: 0, wins, rank, top });

  it('tells how far the winner is from the player just above', () => {
    const top = [entry('Mamie', 7, 9, 1), entry('Seb', 6, 10, 2), entry('Tessa', 2, 8, 3)];

    expect(victoryPhrase(won(6, 2, top), 'Seb')).toBe("6ᵉ victoire du mois · plus qu'une pour rattraper Mamie");
    expect(victoryPhrase(won(2, 3, top), 'Tessa')).toBe('2ᵉ victoire du mois · plus que 4 pour rattraper Seb');
  });

  it('says when the winner takes or keeps the lead', () => {
    expect(victoryPhrase(won(1, 1, [entry('Seb', 1, 1, 1)]), 'Seb')).toBe('1re victoire du mois · prend la tête du classement');
    expect(victoryPhrase(won(6, 1, [entry('Seb', 6, 8, 1), entry('Mamie', 5, 9, 2)]), 'Seb')).toBe(
      '6ᵉ victoire du mois · prend la tête du classement',
    );
    expect(victoryPhrase(won(6, 1, [entry('Seb', 6, 8, 1), entry('Mamie', 3, 9, 2)]), 'Seb')).toBe(
      '6ᵉ victoire du mois · reste en tête du classement',
    );
  });

  it('says when the winner shares the lead', () => {
    expect(victoryPhrase(won(1, 1, [entry('Mamie', 1, 2, 1), entry('Papa', 1, 2, 1)]), 'Mamie')).toBe(
      '1re victoire du mois · à égalité en tête avec Papa',
    );
  });

  it('says nothing when the game did not count', () => {
    expect(victoryPhrase(won(null, null, null), 'Seb')).toBeNull();
  });
});
