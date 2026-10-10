// Card themes live in public/themes/<id>/: 52 faces named like the protocol codes (AS.svg, TD.svg),
// a back.svg and a theme.json. A theme may reuse the faces of another one ("faces").
// The choice is local to each player: the server never knows it.

export const THEMES = [
  { id: 'classique', name: 'Classique', faces: 'classique' },
  { id: 'famille', name: 'Famille', faces: 'classique' },
];

export function themeOrDefault(id) {
  return THEMES.find((theme) => theme.id === id) ?? THEMES[0];
}

export function faceUrl(themeId, code) {
  return `/themes/${themeOrDefault(themeId).faces}/${code}.svg`;
}

export function backUrl(themeId) {
  return `/themes/${themeOrDefault(themeId).id}/back.svg`;
}
