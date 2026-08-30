import React, { useState } from 'react';
import type { MediaItem } from '../types/media';
import { Search, Plus, Heart } from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface MusicExplorerProps {
  tracks: MediaItem[];
  currentTrackId?: string;
  onPlayTrack: (item: MediaItem) => void;
  onImportFiles: (files: FileList | File[]) => void;
  onToggleFavorite: (id: string) => void;
}

export const MusicExplorer: React.FC<MusicExplorerProps> = ({
  tracks,
  currentTrackId,
  onPlayTrack,
  onImportFiles,
  onToggleFavorite
}) => {
  const [subTab, setSubTab] = useState<'TRACKS' | 'FOLDERS' | 'ARTISTS' | 'ALBUMS'>('TRACKS');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredTracks = tracks.filter(t => 
    t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (t.artist && t.artist.toLowerCase().includes(searchQuery.toLowerCase())) ||
    (t.album && t.album.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const formatDuration = (secs: number) => {
    if (!secs || isNaN(secs)) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col pb-36 pt-9 sm:pt-4 animate-fade-in px-3 sm:px-4">
      
      {/* Top Header */}
      <div className="flex items-center justify-between py-2 border-b border-white/10 mb-3">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <h1 className="font-dot text-base sm:text-lg font-bold tracking-wider text-white">
            NOTHING MUSIC
          </h1>
          <span className="text-[10px] font-mono text-white/40">({tracks.length})</span>
        </div>

        <label className="p-2 rounded-xl bg-white/5 border border-white/10 text-white active-press cursor-pointer">
          <Plus className="w-4 h-4 text-[#D71921]" />
          <input
            type="file"
            multiple
            accept="audio/*"
            className="hidden"
            onChange={(e) => {
              if (e.target.files && e.target.files.length > 0) {
                triggerHaptic('medium');
                onImportFiles(e.target.files);
              }
            }}
          />
        </label>
      </div>

      {/* Search Bar */}
      <div className="relative w-full mb-3">
        <Search className="w-4 h-4 text-white/40 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          placeholder="Search songs, artists, albums..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-[#111111] border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs font-sans text-white placeholder-white/40 focus:outline-none focus:border-white/30"
        />
      </div>

      {/* Sub Tabs */}
      <div className="flex items-center gap-2 mb-4 overflow-x-auto no-scrollbar">
        {(['TRACKS', 'FOLDERS', 'ARTISTS', 'ALBUMS'] as const).map((tab) => {
          const isSelected = subTab === tab;
          return (
            <button
              key={tab}
              onClick={() => {
                triggerHaptic('selection');
                setSubTab(tab);
              }}
              className={`px-4 py-1.5 rounded-full text-xs font-mono transition-all whitespace-nowrap active-press ${
                isSelected
                  ? 'bg-white text-black font-bold shadow-md shadow-white/10'
                  : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
              }`}
            >
              {tab}
            </button>
          );
        })}
      </div>

      {/* Track List */}
      <div className="flex flex-col gap-2">
        {filteredTracks.map((track) => {
          const isCurrent = currentTrackId === track.id;
          return (
            <div
              key={track.id}
              onClick={() => {
                triggerHaptic('medium');
                onPlayTrack(track);
              }}
              className={`flex items-center justify-between p-3 rounded-2xl border transition-all cursor-pointer group active-press ${
                isCurrent
                  ? 'bg-white/10 border-white text-white shadow-lg'
                  : 'bg-[#0e0e0e] border-white/10 text-white/80 hover:bg-white/5 hover:border-white/20'
              }`}
            >
              <div className="flex items-center gap-3.5 min-w-0 flex-1">
                {/* Artwork */}
                <div className="relative w-12 h-12 rounded-xl overflow-hidden bg-black/60 shrink-0 border border-white/10">
                  <img
                    src={track.thumbnail || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200&auto=format&fit=crop&q=80'}
                    alt={track.title}
                    className="w-full h-full object-cover"
                  />
                  {isCurrent && (
                    <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                      <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red animate-pulse" />
                    </div>
                  )}
                </div>

                <div className="flex flex-col min-w-0">
                  <h3 className={`font-mono text-xs font-bold line-clamp-1 group-hover:text-[#D71921] transition-colors ${
                    isCurrent ? 'text-[#D71921]' : 'text-white'
                  }`}>
                    {track.title}
                  </h3>
                  <div className="flex items-center gap-2 mt-0.5 text-[10px] font-mono text-white/40 line-clamp-1">
                    <span>{track.artist || 'Unknown Artist'}</span>
                    <span>•</span>
                    <span>{track.format}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <span className="font-mono text-xs text-white/50">{formatDuration(track.duration)}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    triggerHaptic('light');
                    onToggleFavorite(track.id);
                  }}
                  className="p-1.5 text-white/40 hover:text-[#D71921]"
                >
                  <Heart className={`w-4 h-4 ${track.isFavorite ? 'fill-[#D71921] text-[#D71921]' : ''}`} />
                </button>
              </div>
            </div>
          );
        })}
      </div>

    </div>
  );
};
