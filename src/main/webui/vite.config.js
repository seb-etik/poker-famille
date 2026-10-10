import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    svelte(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'apple-touch-icon-180x180.png', 'icon.svg'],
      manifest: {
        name: 'Poker en famille',
        short_name: 'Poker',
        description: "Texas Hold'em entre nous, avec des jetons virtuels",
        lang: 'fr',
        start_url: '/',
        display: 'standalone',
        orientation: 'portrait',
        theme_color: '#0b1a12',
        background_color: '#0b1a12',
        icons: [
          { src: 'pwa-64x64.png', sizes: '64x64', type: 'image/png' },
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          { src: 'maskable-icon-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        // The app shell is precached; the cards (3 MB) are cached as they are shown.
        globPatterns: ['**/*.{js,css,html,png,ico,svg}'],
        globIgnores: ['themes/**'],
        navigateFallback: '/index.html',
        navigateFallbackDenylist: [/^\/api\//, /^\/ws\//, /^\/q\//],
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.pathname.startsWith('/themes/'),
            handler: 'CacheFirst',
            options: {
              cacheName: 'themes',
              expiration: { maxEntries: 200, maxAgeSeconds: 30 * 24 * 3600 },
            },
          },
        ],
      },
    }),
  ],
  server: {
    port: 5173,
    strictPort: true,
  },
});
