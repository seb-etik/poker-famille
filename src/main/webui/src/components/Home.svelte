<script>
  import { untrack } from 'svelte';
  import Avatar from './Avatar.svelte';
  import { avatarKind, EMOJI_AVATARS, gallery } from '../lib/avatars.js';
  import { install, promptInstall } from '../lib/install.svelte.js';
  import { saveProfile } from '../lib/profile.js';

  let { profile = $bindable(), presetCode = '', onEnter } = $props();

  let code = $state(untrack(() => presetCode));
  let error = $state(null);
  let page = $state(0);
  // Opens on the tab of the current avatar.
  let kind = $state(untrack(() => avatarKind(profile.avatar)));
  // On the faces tab, the chosen face stays first, even on another page.
  const avatars = $derived.by(() => {
    if (kind === 'animals') {
      return EMOJI_AVATARS;
    }
    const faces = gallery(page);
    return avatarKind(profile.avatar) === 'faces'
      ? [profile.avatar, ...faces.filter((id) => id !== profile.avatar)].slice(0, 12)
      : faces;
  });
  let creating = $state(false);
  // Settings of a new table, shown when the host taps "Créer une table".
  let showSettings = $state(false);
  const settings = $state({
    smallBlind: 10,
    bigBlind: 20,
    startingStack: 1000,
    rising: true,
    everyHands: 10,
    percent: 50,
  });
  const ready = $derived(profile.nickname.trim().length > 0 && profile.nickname.trim().length <= 20);

  $effect(() => saveProfile(profile));

  async function createTable() {
    creating = true;
    error = null;
    try {
      const response = await fetch('/api/tables', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          smallBlind: settings.smallBlind,
          bigBlind: settings.bigBlind,
          startingStack: settings.startingStack,
          blindIncreaseEveryHands: settings.rising ? settings.everyHands : 0,
          blindIncreasePercent: settings.percent,
        }),
      });
      if (response.status === 400) {
        error = 'Réglages impossibles : la grosse blinde doit être au moins la petite, le tapis au moins la grosse blinde, la hausse entre 10 et 200 %.';
        return;
      }
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      onEnter((await response.json()).code);
    } catch {
      error = 'Impossible de créer la table, réessaie.';
    } finally {
      creating = false;
    }
  }

  function join(event) {
    event.preventDefault();
    onEnter(code.trim().toUpperCase());
  }
</script>

<main>
  <h1>Poker en famille</h1>

  <label>
    Ton surnom
    <input bind:value={profile.nickname} maxlength="20" placeholder="Mamie, Léo…" />
  </label>

  <fieldset>
    <legend>Ton avatar</legend>
    <div class="kinds" role="tablist">
      <button type="button" role="tab" aria-selected={kind === 'faces'} class:active={kind === 'faces'}
        onclick={() => (kind = 'faces')}>Visages</button>
      <button type="button" role="tab" aria-selected={kind === 'animals'} class:active={kind === 'animals'}
        onclick={() => (kind = 'animals')}>Animaux</button>
    </div>
    <div class="avatars">
      {#each avatars as id (id)}
        <button type="button" class:selected={profile.avatar === id} onclick={() => (profile.avatar = id)} aria-label="avatar">
          <Avatar {id} size="100%" />
        </button>
      {/each}
    </div>
    {#if kind === 'faces'}
      <button type="button" class="more" onclick={() => (page += 1)}>Autres têtes</button>
    {/if}
  </fieldset>

  {#if !presetCode}
    {#if !showSettings}
      <button class="primary" disabled={!ready} onclick={() => (showSettings = true)}>Créer une table</button>
    {:else}
      <fieldset class="settings">
        <legend>Nouvelle table</legend>
        <div class="row">
          <label>Petite blinde <input type="number" inputmode="numeric" min="1" bind:value={settings.smallBlind} /></label>
          <label>Grosse blinde <input type="number" inputmode="numeric" min="1" bind:value={settings.bigBlind} /></label>
        </div>
        <label>Tapis de départ <input type="number" inputmode="numeric" min="1" bind:value={settings.startingStack} /></label>
        <label class="check">
          <input type="checkbox" bind:checked={settings.rising} /> Faire monter les blindes
        </label>
        {#if settings.rising}
          <div class="row">
            <label>Mains par niveau <input type="number" inputmode="numeric" min="1" max="100" bind:value={settings.everyHands} /></label>
            <label>Hausse (%) <input type="number" inputmode="numeric" min="10" max="200" bind:value={settings.percent} /></label>
          </div>
        {/if}
        <div class="row">
          <button type="button" onclick={() => (showSettings = false)}>Annuler</button>
          <button type="button" class="primary" disabled={!ready || creating} onclick={createTable}>Créer la table</button>
        </div>
      </fieldset>
    {/if}
  {/if}

  <form onsubmit={join}>
    <input bind:value={code} maxlength="4" placeholder="CODE" autocapitalize="characters" />
    <button disabled={!ready || code.trim().length !== 4}>Rejoindre</button>
  </form>

  {#if error}<p class="error">{error}</p>{/if}

  {#if install.prompt}
    <button class="install" onclick={promptInstall}>📲 Installer l'appli sur ce téléphone</button>
  {/if}
</main>

<style>
  main {
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    max-width: 26rem;
    margin: 0 auto;
    padding: 2rem 1rem;
  }
  h1 {
    text-align: center;
    margin: 0;
  }
  label {
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
  }
  fieldset {
    border: none;
    padding: 0;
    margin: 0;
  }
  legend {
    margin-bottom: 0.4rem;
  }
  .kinds {
    display: flex;
    gap: 0.25rem;
    margin-bottom: 0.5rem;
  }
  .kinds button {
    flex: 1;
    background: #10261b;
    border: 1px solid #2b5a41;
  }
  .kinds button.active {
    background: #2b5a41;
    border-color: #ffd23f;
  }
  .avatars {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 0.4rem;
  }
  .avatars button {
    display: grid;
    place-items: center;
    aspect-ratio: 1;
    padding: 0.2rem;
    border-radius: 50%;
    background: #1d3a2b;
    border: 2px solid transparent;
  }
  .avatars button.selected {
    border-color: #ffd23f;
  }
  .more {
    margin-top: 0.5rem;
    width: 100%;
    background: none;
    border: 1px dashed #2b5a41;
  }
  .settings {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
    padding: 0.75rem;
    border: 1px solid #2b5a41;
    border-radius: 0.5rem;
  }
  .settings legend {
    padding: 0 0.3rem;
    font-weight: 600;
  }
  .settings .row {
    display: flex;
    gap: 0.5rem;
  }
  .settings .row > * {
    flex: 1;
  }
  .settings input[type='number'] {
    width: 100%;
    box-sizing: border-box;
  }
  .settings .check {
    flex-direction: row;
    align-items: center;
    gap: 0.5rem;
  }
  .primary {
    min-height: 3rem;
    font-size: 1.1rem;
    background: #b8860b;
  }
  form {
    display: flex;
    gap: 0.5rem;
  }
  form input {
    flex: 1;
    /* Without it, the field keeps its intrinsic width and pushes "Rejoindre" off a narrow screen. */
    min-width: 0;
    text-transform: uppercase;
    letter-spacing: 0.3em;
    text-align: center;
  }
  form button {
    flex: none;
  }
  .install {
    background: none;
    border: 1px solid #2b5a41;
  }
  .error {
    color: #ff8a8a;
  }
</style>
