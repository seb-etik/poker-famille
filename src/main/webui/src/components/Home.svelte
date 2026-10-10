<script>
  import { untrack } from 'svelte';
  import Avatar from './Avatar.svelte';
  import { gallery } from '../lib/avatars.js';
  import { saveProfile } from '../lib/profile.js';
  import { backUrl, faceUrl, THEMES } from '../lib/themes.js';

  let { profile = $bindable(), presetCode = '', onEnter } = $props();

  let code = $state(untrack(() => presetCode));
  let error = $state(null);
  let page = $state(0);
  // The chosen avatar stays first, even on another page of the gallery.
  const avatars = $derived([profile.avatar, ...gallery(page).filter((id) => id !== profile.avatar)].slice(0, 12));
  let creating = $state(false);
  const ready = $derived(profile.nickname.trim().length > 0 && profile.nickname.trim().length <= 20);

  $effect(() => saveProfile(profile));

  async function createTable() {
    creating = true;
    error = null;
    try {
      const response = await fetch('/api/tables', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: '{}',
      });
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
    <div class="avatars">
      {#each avatars as id (id)}
        <button type="button" class:selected={profile.avatar === id} onclick={() => (profile.avatar = id)} aria-label="avatar">
          <Avatar {id} size="100%" />
        </button>
      {/each}
    </div>
    <button type="button" class="more" onclick={() => (page += 1)}>Autres têtes</button>
  </fieldset>

  <fieldset>
    <legend>Tes cartes</legend>
    <div class="themes">
      {#each THEMES as theme (theme.id)}
        <button type="button" class:selected={profile.theme === theme.id} onclick={() => (profile.theme = theme.id)}>
          <span class="preview">
            <img src={backUrl(theme.id)} alt="" />
            <img src={faceUrl(theme.id, 'AS')} alt="" />
            <img src={faceUrl(theme.id, 'KH')} alt="" />
          </span>
          {theme.name}
        </button>
      {/each}
    </div>
  </fieldset>

  {#if !presetCode}
    <button class="primary" disabled={!ready || creating} onclick={createTable}>Créer une table</button>
  {/if}

  <form onsubmit={join}>
    <input bind:value={code} maxlength="4" placeholder="CODE" autocapitalize="characters" />
    <button disabled={!ready || code.trim().length !== 4}>Rejoindre</button>
  </form>

  {#if error}<p class="error">{error}</p>{/if}
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
  .avatars {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 0.4rem;
  }
  .avatars button {
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
  .themes {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 0.5rem;
  }
  .themes button {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.4rem;
    background: #1d3a2b;
    border: 2px solid transparent;
  }
  .themes button.selected {
    border-color: #ffd23f;
  }
  .preview {
    display: flex;
  }
  .preview img {
    width: 2.6rem;
    aspect-ratio: 167 / 243;
    margin-left: -0.9rem;
    border-radius: 6%;
    background: #fff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 0.5);
  }
  .preview img:first-child {
    margin-left: 0;
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
    text-transform: uppercase;
    letter-spacing: 0.3em;
    text-align: center;
  }
  .error {
    color: #ff8a8a;
  }
</style>
