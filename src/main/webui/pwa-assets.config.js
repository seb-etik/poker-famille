import { defineConfig, minimal2023Preset } from '@vite-pwa/assets-generator/config';

// The drawing already sits in the safe zone of maskable icons: no padding, felt colour as background.
const felt = { background: '#0b3d24' };

export default defineConfig({
  preset: {
    ...minimal2023Preset,
    maskable: { ...minimal2023Preset.maskable, padding: 0, resizeOptions: felt },
    apple: { ...minimal2023Preset.apple, padding: 0, resizeOptions: felt },
  },
  images: ['public/icon.svg'],
});
