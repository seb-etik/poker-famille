// After a deploy, the service worker first serves the old version, installs the new one in the
// background, then takes control (autoUpdate). Reload then so that nobody stays on the old version,
// but never in the middle of a game: at a table, the new version comes with the next page load.

const serviceWorker = typeof navigator !== 'undefined' ? navigator.serviceWorker : undefined;

if (serviceWorker) {
  // Without a controller this is the very first install: nothing old is on screen.
  const hadController = Boolean(serviceWorker.controller);
  serviceWorker.addEventListener('controllerchange', () => {
    const atTable = location.pathname.startsWith('/table/');
    if (hadController && !atTable) {
      location.reload();
    }
  });
}
