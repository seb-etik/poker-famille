// The family leaderboard (docs/UI-TABLE-A.md, V5): the server computes it, these helpers present it.

/** Same rule as the server: a player is their nickname without case, accents or surrounding spaces. */
function nicknameKey(nickname) {
  return nickname.trim().normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

/** period: 'month' (the current month, in Paris) or 'all'. */
export async function fetchLeaderboard(period) {
  const response = await fetch(`/api/leaderboard?period=${period}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

export function findMe(entries, nickname) {
  const key = nicknameKey(nickname);
  return entries.find((entry) => nicknameKey(entry.nickname) === key) ?? null;
}

/** "1er", "2ᵉ"… */
export function rankLabel(rank) {
  return rank === 1 ? '1er' : `${rank}ᵉ`;
}

/** "1re", "2ᵉ"…, for a victory. */
function winOrdinal(wins) {
  return wins === 1 ? '1re' : `${wins}ᵉ`;
}

function plural(count, word) {
  return `${count} ${word}${count > 1 ? 's' : ''}`;
}

function percent(entry) {
  return entry.games === 0 ? 0 : Math.round((100 * entry.wins) / entry.games);
}

/** Under my profile on the home screen: "2ᵉ du classement · 5 victoires". */
export function profileLine(entry) {
  return `${rankLabel(entry.rank)} du classement · ${plural(entry.wins, 'victoire')}`;
}

/** At the bottom of the leaderboard: "Toi : 5 victoires sur 14 parties · 36 %". */
export function myLine(entry) {
  return `Toi : ${plural(entry.wins, 'victoire')} sur ${plural(entry.games, 'partie')} · ${percent(entry)} %`;
}

/** "Octobre 2026". */
export function monthLabel(date = new Date()) {
  const label = date.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric', timeZone: 'Europe/Paris' });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

/**
 * Under "Seb remporte la partie": "6ᵉ victoire du mois · plus qu'une pour rattraper Mamie", or
 * "… · prend la tête du classement". Null when the game did not count.
 */
export function victoryPhrase(event, winnerNickname) {
  if (event.wins == null) {
    return null;
  }
  const wins = `${winOrdinal(event.wins)} victoire du mois`;
  const top = event.top ?? [];
  const winner = findMe(top, winnerNickname);
  if (event.rank === 1) {
    const coLeader = top.find((entry) => entry.rank === 1 && entry !== winner);
    if (coLeader) {
      return `${wins} · à égalité en tête avec ${coLeader.nickname}`;
    }
    // Before this game the winner had one victory less: were they already ahead of the second?
    const second = top.find((entry) => entry !== winner);
    const wasAhead = second !== undefined && event.wins - 1 > second.wins;
    return `${wins} · ${wasAhead ? 'reste en tête' : 'prend la tête'} du classement`;
  }
  const above = top[event.rank - 2];
  if (!above) {
    return wins;
  }
  const gap = above.wins - event.wins;
  if (gap === 0) {
    return `${wins} · à égalité avec ${above.nickname}`;
  }
  return `${wins} · plus ${gap === 1 ? "qu'une" : `que ${gap}`} pour rattraper ${above.nickname}`;
}
