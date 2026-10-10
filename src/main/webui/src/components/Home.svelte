<script>
  import { untrack } from 'svelte';
  import { AVATARS, saveProfile } from '../lib/profile.js';

  let { profile = $bindable(), presetCode = '', onEnter } = $props();

  let code = $state(untrack(() => presetCode));
  let error = $state(null);
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
      {#each AVATARS as avatar (avatar.id)}
        <button
          type="button"
          class:selected={profile.avatar === avatar.id}
          onclick={() => (profile.avatar = avatar.id)}
          aria-label={avatar.id}>{avatar.emoji}</button
        >
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
    font-size: 1.6rem;
    padding: 0.3rem;
    background: #1d3a2b;
    border: 2px solid transparent;
  }
  .avatars button.selected {
    border-color: #ffd23f;
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
