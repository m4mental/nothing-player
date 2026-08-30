import type { MediaItem } from '../types/media';

export interface MediaSessionHandlers {
  onPlay: () => void;
  onPause: () => void;
  onPrev: () => void;
  onNext: () => void;
  onSeek: (position: number) => void;
}

export function updateMediaSessionMetadata(item: MediaItem | null, isPlaying: boolean) {
  if (!('mediaSession' in navigator) || !item) return;

  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: item.title || 'Nothing Track',
      artist: item.artist || 'NOTHING PLAYER',
      album: item.album || 'Nothing OS Audio Hub',
      artwork: [
        {
          src: item.thumbnail || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80',
          sizes: '512x512',
          type: 'image/jpeg'
        }
      ]
    });

    navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused';
  } catch (e) {
    console.debug('MediaSession metadata update failed:', e);
  }
}

export function updateMediaSessionPosition(position: number, duration: number, playbackRate = 1.0) {
  if (!('mediaSession' in navigator) || !('setPositionState' in navigator.mediaSession)) return;
  try {
    if (duration > 0 && !isNaN(position) && !isNaN(duration)) {
      navigator.mediaSession.setPositionState({
        duration: duration,
        playbackRate: playbackRate,
        position: Math.min(position, duration)
      });
    }
  } catch (e) {
    console.debug('MediaSession position update error:', e);
  }
}

export function setupMediaSessionActionHandlers(handlers: MediaSessionHandlers) {
  if (!('mediaSession' in navigator)) return;

  const actions: [MediaSessionAction, MediaSessionActionHandler][] = [
    ['play', () => handlers.onPlay()],
    ['pause', () => handlers.onPause()],
    ['previoustrack', () => handlers.onPrev()],
    ['nexttrack', () => handlers.onNext()],
    ['seekto', (details) => {
      if (details.seekTime !== undefined) {
        handlers.onSeek(details.seekTime);
      }
    }],
    ['seekbackward', (details) => {
      const skipTime = details.seekOffset || 10;
      handlers.onSeek(Math.max(0, (navigator.mediaSession.playbackState === 'playing' ? 0 : 0) - skipTime));
    }],
    ['seekforward', (details) => {
      const skipTime = details.seekOffset || 10;
      handlers.onSeek(skipTime);
    }]
  ];

  for (const [action, handler] of actions) {
    try {
      navigator.mediaSession.setActionHandler(action, handler);
    } catch {
      // action not supported on browser
    }
  }
}
