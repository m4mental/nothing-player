export type MediaType = 'audio' | 'video' | 'stream';

export interface MediaItem {
  id: string;
  title: string;
  artist?: string;
  album?: string;
  duration: number; // in seconds
  url: string;
  blobKey?: string; // key in IndexedDB for offline blobs
  type: MediaType;
  format: string; // mp3, flac, mp4, mkv, hls, etc.
  thumbnail?: string;
  subtitlesUrl?: string;
  subtitlesContent?: string;
  subtitlesName?: string;
  lyrics?: string;
  addedAt: number;
  isFavorite?: boolean;
  lastPosition?: number;
  size?: number;
}

export interface Playlist {
  id: string;
  name: string;
  description?: string;
  mediaIds: string[];
  createdAt: number;
  coverColor?: string;
}

export interface SubtitleCue {
  id: number;
  startTime: number; // in seconds
  endTime: number; // in seconds
  text: string;
}

export interface LyricsLine {
  time: number; // in seconds
  text: string;
}

export interface VideoFilterState {
  brightness: number; // 50 to 150 (default 100)
  contrast: number; // 50 to 150 (default 100)
  saturation: number; // 0 to 200 (default 100)
  crtScanlines: boolean;
  duotoneRed: boolean;
  hueRotate: number; // 0 to 360
  invert: boolean;
}

export interface EqualizerPreset {
  name: string;
  gains: number[]; // 10 bands from -12dB to +12dB
}

export interface RadioStation {
  id: string;
  name: string;
  genre: string;
  streamUrl: string;
  bitrate: string;
  country: string;
  coverArt: string;
  description: string;
}

export type ActiveHub = 'MUSIC' | 'VIDEO' | 'LIBRARY' | 'RADIO' | 'GLYPH';
