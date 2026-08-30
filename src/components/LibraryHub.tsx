import React, { useState } from 'react';
import type { MediaItem, MediaType } from '../types/media';
import { 
  Search, 
  Plus, 
  Trash2, 
  Heart, 
  Play, 
  Music, 
  Film, 
  Radio, 
  HardDrive, 
  Link
} from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface LibraryHubProps {
  mediaItems: MediaItem[];
  currentTrackId?: string;
  onPlayItem: (item: MediaItem) => void;
  onDeleteItem: (id: string) => void;
  onToggleFavorite: (id: string) => void;
  onImportFiles: (files: FileList | File[]) => void;
  onAddStreamUrl: (title: string, url: string, type: MediaType) => void;
}

export const LibraryHub: React.FC<LibraryHubProps> = ({
  mediaItems,
  currentTrackId,
  onPlayItem,
  onDeleteItem,
  onToggleFavorite,
  onImportFiles,
  onAddStreamUrl
}) => {
  const [filterType, setFilterType] = useState<'ALL' | 'AUDIO' | 'VIDEO' | 'FAVORITES' | 'STREAMS'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showStreamModal, setShowStreamModal] = useState(false);
  const [streamTitle, setStreamTitle] = useState('');
  const [streamUrl, setStreamUrl] = useState('');
  const [streamType, setStreamType] = useState<MediaType>('audio');
  const [isDragging, setIsDragging] = useState(false);

  // Filter items
  const filteredItems = mediaItems.filter((item) => {
    const matchesSearch = item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.artist && item.artist.toLowerCase().includes(searchQuery.toLowerCase()));

    if (!matchesSearch) return false;

    if (filterType === 'AUDIO') return item.type === 'audio';
    if (filterType === 'VIDEO') return item.type === 'video';
    if (filterType === 'FAVORITES') return !!item.isFavorite;
    if (filterType === 'STREAMS') return item.type === 'stream';
    return true;
  });

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      triggerHaptic('medium');
      onImportFiles(e.dataTransfer.files);
    }
  };

  const handleAddStreamSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!streamUrl.trim()) return;
    triggerHaptic('medium');
    onAddStreamUrl(streamTitle.trim() || 'Network Stream', streamUrl.trim(), streamType);
    setStreamTitle('');
    setStreamUrl('');
    setShowStreamModal(false);
  };

  const formatDuration = (secs: number) => {
    if (!secs || isNaN(secs)) return '--:--';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div 
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      className={`w-full max-w-7xl mx-auto p-3 sm:p-6 flex flex-col gap-6 animate-fade-in transition-all ${
        isDragging ? 'ring-4 ring-[#D71921] bg-[#D71921]/5 rounded-3xl' : ''
      }`}
    >
      {/* Library Top Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/10 pb-4">
        <div className="flex items-center gap-2.5">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <span className="font-dot text-sm sm:text-base tracking-widest text-white">
            MEDIA VAULT // 03 LIBRARY ({mediaItems.length} ITEMS)
          </span>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          {/* Add Local File / Folder */}
          <label className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white text-black font-mono text-xs font-bold cursor-pointer hover:bg-white/90 active-press transition-all">
            <Plus className="w-4 h-4" />
            <span>IMPORT FILES</span>
            <input
              type="file"
              multiple
              accept="audio/*,video/*"
              className="hidden"
              onChange={(e) => {
                if (e.target.files && e.target.files.length > 0) {
                  triggerHaptic('medium');
                  onImportFiles(e.target.files);
                }
              }}
            />
          </label>

          {/* Add Stream URL */}
          <button
            onClick={() => {
              triggerHaptic('light');
              setShowStreamModal(true);
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/5 border border-white/15 text-white font-mono text-xs hover:bg-white/10 active-press transition-all"
          >
            <Link className="w-3.5 h-3.5 text-[#D71921]" />
            <span>ADD STREAM URL</span>
          </button>
        </div>
      </div>

      {/* Drag & Drop Hero Box */}
      <div className="relative p-6 sm:p-8 rounded-3xl bg-[#0d0d0d] border border-dashed border-white/20 text-center flex flex-col items-center justify-center gap-3 overflow-hidden group hover:border-[#D71921]/60 transition-colors">
        <div className="absolute inset-0 bg-grid-pattern opacity-20 pointer-events-none" />
        
        <div className="relative z-10 w-12 h-12 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-white group-hover:scale-110 transition-transform">
          <HardDrive className="w-6 h-6 text-[#D71921]" />
        </div>

        <div className="relative z-10 flex flex-col">
          <span className="font-dot text-base sm:text-lg text-white font-bold tracking-wider">
            DRAG & DROP MEDIA FILES OR FOLDERS HERE
          </span>
          <span className="font-mono text-xs text-white/50 mt-1">
            SUPPORTS: MP3, FLAC, WAV, AAC, MP4, MKV, WEBM, MOV, HLS (.M3U8)
          </span>
        </div>
      </div>

      {/* Search & Category Filter Tabs */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-3">
        
        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 no-scrollbar">
          {(['ALL', 'AUDIO', 'VIDEO', 'FAVORITES', 'STREAMS'] as const).map((cat) => {
            const isSelected = filterType === cat;
            return (
              <button
                key={cat}
                onClick={() => {
                  triggerHaptic('selection');
                  setFilterType(cat);
                }}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-mono whitespace-nowrap transition-all active-press ${
                  isSelected
                    ? 'bg-white text-black font-bold shadow-md shadow-white/10'
                    : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
                }`}
              >
                {cat}
              </button>
            );
          })}
        </div>

        {/* Search Bar */}
        <div className="relative w-full md:w-72">
          <Search className="w-4 h-4 text-white/40 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search titles, artists, formats..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-[#111111] border border-white/10 rounded-xl pl-9 pr-4 py-2 text-xs font-mono text-white placeholder-white/40 focus:outline-none focus:border-white/30"
          />
        </div>
      </div>

      {/* Media Vault Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
        {filteredItems.map((item) => {
          const isCurrent = currentTrackId === item.id;
          return (
            <div
              key={item.id}
              className={`p-3.5 rounded-2xl border transition-all flex flex-col justify-between group ${
                isCurrent
                  ? 'bg-white/10 border-white text-white shadow-xl shadow-white/5'
                  : 'bg-[#0e0e0e] border-white/10 text-white/80 hover:bg-white/5 hover:border-white/20'
              }`}
            >
              <div className="flex items-start gap-3">
                {/* Thumbnail / Type Badge */}
                <div 
                  onClick={() => {
                    triggerHaptic('medium');
                    onPlayItem(item);
                  }}
                  className="relative w-16 h-16 rounded-xl overflow-hidden bg-black/60 shrink-0 border border-white/10 cursor-pointer"
                >
                  <img
                    src={item.thumbnail || (item.type === 'video' 
                      ? 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=300&auto=format&fit=crop&q=80'
                      : 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=300&auto=format&fit=crop&q=80')}
                    alt={item.title}
                    className="w-full h-full object-cover group-hover:scale-110 transition-transform"
                  />
                  <div className="absolute inset-0 bg-black/30 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <Play className="w-5 h-5 fill-white text-white" />
                  </div>
                  {isCurrent && (
                    <div className="absolute bottom-1 right-1 w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red animate-pulse" />
                  )}
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5 mb-1">
                    <span className="px-1.5 py-0.5 rounded bg-white/10 text-[9px] font-mono text-white/70">
                      {item.format}
                    </span>
                    {item.type === 'video' ? (
                      <Film className="w-3 h-3 text-[#D71921]" />
                    ) : item.type === 'audio' ? (
                      <Music className="w-3 h-3 text-white/60" />
                    ) : (
                      <Radio className="w-3 h-3 text-[#D71921]" />
                    )}
                  </div>

                  <h3 
                    onClick={() => {
                      triggerHaptic('medium');
                      onPlayItem(item);
                    }}
                    className="font-mono text-xs font-bold text-white line-clamp-1 cursor-pointer hover:text-[#D71921]"
                  >
                    {item.title}
                  </h3>
                  
                  <p className="text-[10px] font-mono text-white/40 line-clamp-1 mt-0.5">
                    {item.artist || 'Unknown Artist'}
                  </p>
                </div>
              </div>

              {/* Bottom Actions */}
              <div className="flex items-center justify-between border-t border-white/5 pt-2.5 mt-3 text-[10px] font-mono text-white/40">
                <span>{formatDuration(item.duration)}</span>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => {
                      triggerHaptic('light');
                      onToggleFavorite(item.id);
                    }}
                    className={`p-1.5 rounded-md hover:bg-white/10 active-press transition-colors ${
                      item.isFavorite ? 'text-[#D71921]' : 'text-white/40'
                    }`}
                  >
                    <Heart className={`w-3.5 h-3.5 ${item.isFavorite ? 'fill-[#D71921]' : ''}`} />
                  </button>

                  <button
                    onClick={() => {
                      triggerHaptic('heavy');
                      onDeleteItem(item.id);
                    }}
                    className="p-1.5 rounded-md text-white/40 hover:text-red-400 hover:bg-white/10 active-press"
                    title="Delete item"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {filteredItems.length === 0 && (
        <div className="p-12 text-center text-white/40 font-mono text-xs">
          NO MEDIA FOUND. DRAG & DROP AUDIO/VIDEO FILES TO IMPORT.
        </div>
      )}

      {/* Stream URL Modal */}
      {showStreamModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fade-in">
          <div className="w-full max-w-md bg-[#111111] border border-white/15 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <span className="font-dot text-sm text-white font-bold">ADD NETWORK STREAM URL</span>
              <button onClick={() => setShowStreamModal(false)} className="text-white/50 hover:text-white">✕</button>
            </div>

            <form onSubmit={handleAddStreamSubmit} className="flex flex-col gap-3">
              <div>
                <label className="text-[10px] font-mono text-white/60">STREAM TITLE</label>
                <input
                  type="text"
                  placeholder="e.g. Cyberpunk Live Stream"
                  value={streamTitle}
                  onChange={(e) => setStreamTitle(e.target.value)}
                  className="w-full mt-1 bg-black/60 border border-white/10 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-white/30"
                />
              </div>

              <div>
                <label className="text-[10px] font-mono text-white/60">STREAM URL (HLS / MP4 / MP3 / RADIO)</label>
                <input
                  type="url"
                  required
                  placeholder="https://example.com/live/stream.m3u8"
                  value={streamUrl}
                  onChange={(e) => setStreamUrl(e.target.value)}
                  className="w-full mt-1 bg-black/60 border border-white/10 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none focus:border-white/30"
                />
              </div>

              <div>
                <label className="text-[10px] font-mono text-white/60">TYPE</label>
                <div className="flex gap-2 mt-1">
                  <button
                    type="button"
                    onClick={() => setStreamType('audio')}
                    className={`flex-1 py-1.5 rounded-lg text-xs font-mono border ${
                      streamType === 'audio' ? 'bg-white text-black border-white font-bold' : 'bg-white/5 text-white/60 border-white/10'
                    }`}
                  >
                    AUDIO
                  </button>
                  <button
                    type="button"
                    onClick={() => setStreamType('video')}
                    className={`flex-1 py-1.5 rounded-lg text-xs font-mono border ${
                      streamType === 'video' ? 'bg-white text-black border-white font-bold' : 'bg-white/5 text-white/60 border-white/10'
                    }`}
                  >
                    VIDEO
                  </button>
                </div>
              </div>

              <button
                type="submit"
                className="mt-2 w-full py-2.5 rounded-xl bg-white text-black font-mono text-xs font-bold hover:bg-white/90 active-press"
              >
                ADD TO MEDIA VAULT
              </button>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
