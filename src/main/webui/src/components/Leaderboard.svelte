<script>
  import Avatar from './Avatar.svelte';
  import { fetchLeaderboard, findMe, monthLabel, myLine, rankLabel } from '../lib/leaderboard.js';

  let { profile, onBack } = $props();

  let period = $state('month');
  let entries = $state(null);
  let error = $state(null);

  $effect(() => {
    const selected = period;
    entries = null;
    error = null;
    fetchLeaderboard(selected)
      .then((result) => {
        if (selected === period) {
          entries = result;
        }
      })
      .catch(() => (error = 'Impossible de charger le classement.'));
  });

  // The podium shows the 2nd, the 1st (in the middle, higher) and the 3rd.
  const podium = $derived(entries ? [entries[1], entries[0], entries[2]].filter(Boolean) : []);
  const others = $derived(entries ? entries.slice(3) : []);
  const me = $derived(entries ? findMe(entries, profile.nickname) : null);
  const percent = (entry) => (entry.games === 0 ? 0 : Math.round((100 * entry.wins) / entry.games));
</script>

<div class="screen">
  <header>
    <button class="icon" onclick={onBack} aria-label="Retour à l'accueil">‹</button>
    <h1>Classement de la famille</h1>
    <span class="icon"></span>
  </header>

  <div class="tabs" role="tablist">
    <button role="tab" aria-selected={period === 'month'} class:active={period === 'month'} onclick={() => (period = 'month')}>
      {monthLabel()}
    </button>
    <button role="tab" aria-selected={period === 'all'} class:active={period === 'all'} onclick={() => (period = 'all')}>
      Depuis le début
    </button>
  </div>

  <main>
    {#if error}
      <p class="empty">{error}</p>
    {:else if entries === null}
      <p class="empty">Chargement…</p>
    {:else if entries.length === 0}
      <p class="empty">Pas encore de partie gagnée {period === 'month' ? 'ce mois-ci' : ''}.</p>
    {:else}
      <div class="podium">
        {#each podium as entry (entry.nickname)}
          <div class="step" class:first={entry.rank === 1}>
            <div class="avatar"><Avatar id={entry.avatar} size="100%" /></div>
            <div class="name">{entry.nickname}</div>
            <div class="wins">{entry.wins} victoire{entry.wins > 1 ? 's' : ''}</div>
            <div class="block">{rankLabel(entry.rank)}</div>
          </div>
        {/each}
      </div>

      {#if others.length > 0}
        <ol class="list">
          {#each others as entry (entry.nickname)}
            <li class:me={me === entry}>
              <span class="rank">{rankLabel(entry.rank)}</span>
              <span class="small-avatar"><Avatar id={entry.avatar} size="100%" /></span>
              <span class="nickname">{entry.nickname}</span>
              <span class="numbers">{entry.wins} / {entry.games} · {percent(entry)} %</span>
            </li>
          {/each}
        </ol>
      {/if}
    {/if}

    <p class="rule">
      Une victoire, c'est une partie gagnée jusqu'au bout : il ne reste que toi avec des jetons. Le classement repart à
      zéro quand le serveur redémarre.
    </p>
  </main>

  {#if me}
    <footer>{myLine(me)}</footer>
  {/if}
</div>

<style>
  .screen {
    display: flex;
    flex-direction: column;
    min-height: 100dvh;
    background: #0b1a12;
  }
  header {
    display: flex;
    align-items: center;
    padding: calc(6px + env(safe-area-inset-top)) 8px 6px;
  }
  h1 {
    flex: 1;
    margin: 0;
    font-size: 18px;
    text-align: center;
  }
  .icon {
    width: 44px;
    height: 44px;
    padding: 0;
    font-size: 26px;
    background: none;
  }
  .tabs {
    display: flex;
    gap: 6px;
    padding: 0 16px;
  }
  .tabs button {
    flex: 1;
    min-height: 44px;
    background: #10261b;
    border: 1px solid #2b5a41;
  }
  .tabs button.active {
    background: #2b5a41;
    border-color: #ffd23f;
  }
  main {
    flex: 1;
    padding: 16px;
  }
  .empty {
    text-align: center;
    opacity: 0.8;
  }

  .podium {
    display: flex;
    justify-content: center;
    align-items: flex-end;
    gap: 8px;
    margin: 8px 0 20px;
  }
  .step {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    width: 104px;
  }
  .avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    border: 3px solid #e8d8a8;
    background: #0d3b26;
    overflow: hidden;
    box-sizing: border-box;
  }
  .first .avatar {
    width: 72px;
    height: 72px;
    border-color: #ffd23f;
    box-shadow: 0 0 18px rgb(255 210 63 / 0.5);
  }
  .avatar :global(.emoji) {
    font-size: 32px;
  }
  .name {
    max-width: 100%;
    font-weight: 700;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .wins {
    font-size: 13px;
    opacity: 0.85;
  }
  .block {
    display: grid;
    place-items: center;
    width: 100%;
    height: 44px;
    border-radius: 10px 10px 0 0;
    background: #2b5a41;
    font-weight: 700;
  }
  .first .block {
    height: 68px;
    background: #b8860b;
    color: #fff;
  }

  .list {
    list-style: none;
    margin: 0;
    padding: 0;
  }
  .list li {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 4px;
    border-bottom: 1px solid #2b5a41;
  }
  .list li.me {
    color: #ffd23f;
  }
  .rank {
    width: 2.5em;
    font-weight: 700;
  }
  .small-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: #0d3b26;
    overflow: hidden;
    flex: none;
  }
  .small-avatar :global(.emoji) {
    font-size: 20px;
  }
  .nickname {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .numbers {
    font-size: 14px;
    opacity: 0.85;
  }
  .rule {
    margin-top: 24px;
    font-size: 13px;
    opacity: 0.7;
    text-align: center;
  }
  footer {
    padding: 14px 16px calc(14px + env(safe-area-inset-bottom));
    background: #10261b;
    border-top: 1px solid #2b5a41;
    color: #ffd23f;
    font-weight: 600;
    text-align: center;
  }
</style>
