import { registerPlugin, Capacitor } from '@capacitor/core';
import type { MediaItem } from '../types/media';

export interface MediaScannerPluginInterface {
  scanAllMedia(): Promise<{ videos: (MediaItem & { path?: string })[]; audios: (MediaItem & { path?: string })[]; videoCount: number; audioCount: number }>;
  scanVideos(): Promise<{ videos: (MediaItem & { path?: string })[]; count: number }>;
  scanAudios(): Promise<{ audios: (MediaItem & { path?: string })[]; count: number }>;
  openMediaFile(options: { path?: string; contentUri?: string }): Promise<void>;
  playInExoPlayer(options: { path?: string; contentUri?: string; title?: string; position?: number }): Promise<void>;
  playAudio(options: { path?: string; contentUri?: string; title?: string; artist?: string }): Promise<{ duration: number; isPlaying: boolean }>;
  pauseAudio(): Promise<void>;
  resumeAudio(): Promise<void>;
  seekAudio(options: { position: number }): Promise<void>;
  getAudioStatus(): Promise<{ isPlaying: boolean; currentTime: number; duration: number }>;
  stopAudio(): Promise<void>;
  addListener(eventName: string, listenerFunc: (data: any) => void): Promise<{ remove: () => void }>;
}

export const MediaScanner = registerPlugin<MediaScannerPluginInterface>('MediaScanner');

export async function nativePlayAudio(item: MediaItem): Promise<{ duration: number } | null> {
  if (Capacitor.isNativePlatform()) {
    try {
      const res = await MediaScanner.playAudio({
        path: item.path,
        contentUri: item.contentUri,
        title: item.title,
        artist: item.artist || 'Nothing Music'
      });
      return res;
    } catch (e) {
      console.warn('nativePlayAudio failed:', e);
    }
  }
  return null;
}

export async function nativePauseAudio() {
  if (Capacitor.isNativePlatform()) {
    try {
      await MediaScanner.pauseAudio();
    } catch (e) {
      console.warn('nativePauseAudio failed:', e);
    }
  }
}

export async function nativeResumeAudio() {
  if (Capacitor.isNativePlatform()) {
    try {
      await MediaScanner.resumeAudio();
    } catch (e) {
      console.warn('nativeResumeAudio failed:', e);
    }
  }
}

export async function nativeSeekAudio(position: number) {
  if (Capacitor.isNativePlatform()) {
    try {
      await MediaScanner.seekAudio({ position });
    } catch (e) {
      console.warn('nativeSeekAudio failed:', e);
    }
  }
}

export async function nativeGetAudioStatus() {
  if (Capacitor.isNativePlatform()) {
    try {
      return await MediaScanner.getAudioStatus();
    } catch (e) {
      console.warn('nativeGetAudioStatus failed:', e);
    }
  }
  return null;
}

export async function playNativeExo(video: MediaItem) {
  if (Capacitor.isNativePlatform()) {
    try {
      await MediaScanner.playInExoPlayer({
        path: video.path,
        contentUri: video.contentUri,
        title: video.title,
        position: video.lastPosition || 0
      });
      return true;
    } catch (e) {
      console.warn('playInExoPlayer error:', e);
    }
  }
  return false;
}

export async function openNativePlayer(path?: string, contentUri?: string) {
  if (Capacitor.isNativePlatform()) {
    try {
      await MediaScanner.openMediaFile({ path, contentUri });
    } catch (e) {
      console.warn('openNativePlayer failed:', e);
    }
  }
}

export async function scanDeviceStorage(): Promise<{ videos: MediaItem[]; audios: MediaItem[] }> {
  if (Capacitor.isNativePlatform()) {
    try {
      const res = await MediaScanner.scanAllMedia();
      
      const videos = (res.videos || []).map(v => {
        const playableUrl = v.path ? Capacitor.convertFileSrc(v.path) : v.url;
        return {
          ...v,
          url: playableUrl
        };
      });

      const audios = (res.audios || []).map(a => {
        const playableUrl = a.path ? Capacitor.convertFileSrc(a.path) : a.url;
        return {
          ...a,
          url: playableUrl
        };
      });

      return {
        videos,
        audios
      };
    } catch (e) {
      console.warn('Native MediaScanner error:', e);
      return { videos: [], audios: [] };
    }
  }

  // Web fallback
  return { videos: [], audios: [] };
}
