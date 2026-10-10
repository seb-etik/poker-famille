// localStorage can be missing or throw (private mode, blocked storage): never let it break the app.

export function readJson(key) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function writeJson(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Not saved: the profile will be asked again next time.
  }
}
