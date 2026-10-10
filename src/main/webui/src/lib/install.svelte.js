// Android Chrome offers to install the app through `beforeinstallprompt`, fired early:
// it is caught here, at startup, and the home screen shows a button while it is available.
// (iOS has no such event: there, "Partager > Sur l'écran d'accueil".)

export const install = $state({ prompt: null });

if (typeof window !== 'undefined') {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    install.prompt = event;
  });
  window.addEventListener('appinstalled', () => {
    install.prompt = null;
  });
}

export async function promptInstall() {
  const prompt = install.prompt;
  if (!prompt) {
    return;
  }
  install.prompt = null;
  await prompt.prompt();
}
