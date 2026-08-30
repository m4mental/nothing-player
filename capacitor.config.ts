import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.nothing.player',
  appName: 'NOTHING PLAYER',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  },
  android: {
    backgroundColor: '#050505',
    allowMixedContent: true
  }
};

export default config;
