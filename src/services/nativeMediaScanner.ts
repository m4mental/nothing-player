import { registerPlugin, Capacitor } from '@capacitor/core';
import type { MediaItem } from '../types/media';

export interface MediaScannerPluginInterface {
  scanAllMedia(): Promise<{ videos: (MediaItem & { path?: string })[]; audios: (MediaItem & { path?: string })[]; videoCount: number; audioCount: number }>;
  scanVideos(): Promise<{ videos: (MediaItem & { path?: string })[]; count: number }>;
  scanAudios(): Promise<{ audios: (MediaItem & { path?: string })[]; count: number }>;
  openMediaFile(options: { path?: string; contentUri?: string }): Promise<void>;
  playInExoPlayer(options: { path?: string; contentUri?: string; title?: string; position?: number }): Promise<void>;
}

const MediaScanner = registerPlugin<MediaScannerPluginInterface>('MediaScanner');

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
