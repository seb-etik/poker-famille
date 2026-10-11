// The name of a hand in French, from its category and its five best cards as the server orders them
// (main group first: the trips of a full house, then the pair; straights from their top card).

const RANKS = {
  A: { one: 'as', many: 'as', feminine: false },
  K: { one: 'roi', many: 'rois', feminine: false },
  Q: { one: 'dame', many: 'dames', feminine: true },
  J: { one: 'valet', many: 'valets', feminine: false },
  T: { one: '10', many: '10', feminine: false },
};

function rank(code) {
  return RANKS[code[0]] ?? { one: code[0], many: code[0], feminine: false };
}

/** "de rois", "d'as", "de 8". */
function of(code) {
  const name = rank(code).many;
  return name === 'as' ? "d'as" : `de ${name}`;
}

/** "au roi", "à la dame", "à l'as", "au 5". */
function toTop(code) {
  const { one, feminine } = rank(code);
  if (one === 'as') {
    return "à l'as";
  }
  return feminine ? `à la ${one}` : `au ${one}`;
}

export function handName(category, cards) {
  const [first, , third, fourth] = cards;
  switch (category) {
    case 'highCard':
      return `Hauteur ${rank(first).one}`;
    case 'onePair':
      return `Paire ${of(first)}`;
    case 'twoPair':
      return `Double paire ${rank(first).many} et ${rank(third).many}`;
    case 'threeOfAKind':
      return `Brelan ${of(first)}`;
    case 'straight':
      return `Quinte ${toTop(first)}`;
    case 'flush':
      return `Couleur ${toTop(first)}`;
    case 'fullHouse':
      return `Full aux ${rank(first).many} par les ${rank(fourth).many}`;
    case 'fourOfAKind':
      return `Carré ${of(first)}`;
    case 'straightFlush':
      return first[0] === 'A' ? 'Quinte flush royale' : `Quinte flush ${toTop(first)}`;
    default:
      return category;
  }
}
