import Dexie, { type Table } from 'dexie';
import type { MediaItem, Playlist } from '../types/media';

export interface StoredBlob {
  id: string; // key
  blob: Blob;
  name: string;
  type: string;
  size: number;
}

export interface UserSettings {
  id: string;
  volume: number;
  isMuted: boolean;
  eqPreset: string;
  eqGains: number[];
  audioBoost: number; // 100 to 200
  activeHub: string;
  repeatMode: 'off' | 'all' | 'one';
  shuffle: boolean;
  playbackRate: number;
}

export class NothingDatabase extends Dexie {
  mediaItems!: Table<MediaItem, string>;
  playlists!: Table<Playlist, string>;
  blobs!: Table<StoredBlob, string>;
  settings!: Table<UserSettings, string>;

  constructor() {
    super('NothingPlayerDB');
    this.version(1).stores({
      mediaItems: 'id, title, artist, type, format, isFavorite, addedAt',
      playlists: 'id, name, createdAt',
      blobs: 'id',
      settings: 'id'
    });
  }
}

export const db = new NothingDatabase();

export async function saveMediaItem(item: MediaItem, fileBlob?: Blob): Promise<void> {
  if (fileBlob) {
    const blobKey = `blob_${item.id}`;
    await db.blobs.put({
      id: blobKey,
      blob: fileBlob,
      name: item.title,
      type: fileBlob.type,
      size: fileBlob.size
    });
    item.blobKey = blobKey;
  }
  await db.mediaItems.put(item);
}

export async function getMediaBlob(blobKey: string): Promise<Blob | undefined> {
  const record = await db.blobs.get(blobKey);
  return record?.blob;
}

export async function getAllMediaItems(): Promise<MediaItem[]> {
  return await db.mediaItems.toArray();
}

export async function deleteMediaItem(id: string): Promise<void> {
  const item = await db.mediaItems.get(id);
  if (item?.blobKey) {
    await db.blobs.delete(item.blobKey);
  }
  await db.mediaItems.delete(id);
}
