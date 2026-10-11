<script>
  import { untrack } from 'svelte';
  import Home from './components/Home.svelte';
  import Table from './components/Table.svelte';
  import { loadProfile } from './lib/profile.js';

  let profile = $state(loadProfile());
  let path = $state(location.pathname);

  const tableCode = $derived(path.match(/^\/table\/([A-Za-z]{4})\/?$/)?.[1]?.toUpperCase() ?? null);
  // Shows the table only once the player has entered: a returning player with a saved nickname enters at once,
  // a new one arriving by a table link only when tapping "Rejoindre" (not while typing the nickname).
  let entered = $state(untrack(() => profile.nickname.trim().length > 0));

  // Keeps "?as=…" so that a tab keeps its own profile.
  function navigate(to) {
    history.pushState({}, '', to + location.search);
    path = to;
  }

  function enter(code) {
    entered = true;
    navigate(`/table/${code}`);
  }
</script>

<svelte:window onpopstate={() => (path = location.pathname)} />

{#if tableCode && entered}
  {#key tableCode}
    <Table code={tableCode} {profile} onExit={() => navigate('/')} />
  {/key}
{:else}
  <Home bind:profile presetCode={tableCode ?? ''} onEnter={enter} />
{/if}
