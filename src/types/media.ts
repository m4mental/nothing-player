export type MediaType = 'audio' | 'video' | 'stream';

export interface MediaItem {
  id: string;
  title: string;
  artist?: string;
  album?: string;
  duration: number; // in seconds
  url: string;
  blobKey?: string;
  type: MediaType;
  format: string; // MP4, MKV, AVI, MP3, FLAC, HLS, etc.
  folder?: string; // e.g. "Camera", "Download", "WhatsApp Video", "Movies"
  thumbnail?: string;
  subtitlesUrl?: string;
  subtitlesContent?: string;
  subtitlesName?: string;
  lyrics?: string;
  addedAt: number;
  isFavorite?: boolean;
  lastPosition?: number; // playback resume position in seconds
  size?: number; // in bytes
  resolution?: string; // e.g. "1080p", "4K UHD", "720p"
  decoder?: 'HW' | 'SW';
  path?: string;
  contentUri?: string;
}

export interface VideoFolder {
  name: string;
  count: number;
  totalDuration: number;
  previewThumbnail?: string;
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
  startTime: number;
  endTime: number;
  text: string;
}

export interface LyricsLine {
  time: number;
  text: string;
}

export interface VideoFilterState {
  brightness: number;
  contrast: number;
  saturation: number;
  crtScanlines: boolean;
  duotoneRed: boolean;
  hueRotate: number;
  invert: boolean;
}

export interface EqualizerPreset {
  name: string;
  gains: number[];
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

export type MainTab = 'VIDEOS' | 'MUSIC' | 'RADIO' | 'ME';
export type ActiveHub = 'MUSIC' | 'VIDEO' | 'LIBRARY' | 'RADIO' | 'GLYPH';
