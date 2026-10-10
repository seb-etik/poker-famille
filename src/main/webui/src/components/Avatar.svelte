<script>
  import { avatarEmoji, avatarSrc } from '../lib/avatars.js';

  let { id, size = '2.8rem' } = $props();

  const src = $derived(avatarSrc(id));
</script>

{#if src}
  <img {src} alt="" style="width: {size}; height: {size}" />
{:else}
  <span class="emoji" style="width: {size}; height: {size}">{avatarEmoji(id)}</span>
{/if}

<style>
  img {
    display: block;
    border-radius: 50%;
  }
  .emoji {
    display: grid;
    place-items: center;
    font-size: 1.6rem;
  }
</style>
