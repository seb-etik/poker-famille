<script>
  import { setContext } from 'svelte';
  import Home from './components/Home.svelte';
  import Table from './components/Table.svelte';
  import { loadProfile } from './lib/profile.js';

  let profile = $state(loadProfile());
  // Cards read the theme through the context: a getter, so that a change reaches them.
  setContext('theme', () => profile.theme);
  let path = $state(location.pathname);

  const tableCode = $derived(path.match(/^\/table\/([A-Za-z]{4})\/?$/)?.[1]?.toUpperCase() ?? null);
  const hasNickname = $derived(profile.nickname.trim().length > 0);

  // Keeps "?as=…" so that a tab keeps its own profile.
  function navigate(to) {
    history.pushState({}, '', to + location.search);
    path = to;
  }
</script>

<svelte:window onpopstate={() => (path = location.pathname)} />

{#if tableCode && hasNickname}
  {#key tableCode}
    <Table code={tableCode} {profile} onExit={() => navigate('/')} />
  {/key}
{:else}
  <Home bind:profile presetCode={tableCode ?? ''} onEnter={(code) => navigate(`/table/${code}`)} />
{/if}
