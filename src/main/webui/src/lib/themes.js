// The card images live in public/themes/classique/: 52 faces named like the protocol codes
// (AS.svg, TD.svg), a back.svg and a theme.json. The folder layout leaves room for other themes.
const THEME = 'classique';

export function faceUrl(code) {
  return `/themes/${THEME}/${code}.svg`;
}

export function backUrl() {
  return `/themes/${THEME}/back.svg`;
}
