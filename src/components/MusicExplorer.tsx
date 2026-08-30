import React, { useState } from 'react';
import type { MediaItem } from '../types/media';
import { 
  Search, 
  Plus, 
  Heart, 
  RefreshCw, 
  MoreVertical, 
  Copy, 
  Check, 
  HardDrive, 
  Music, 
  Play,
  Volume2
} from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface MusicExplorerProps {
  tracks: MediaItem[];
  currentTrackId?: string;
  onPlayTrack: (item: MediaItem) => void;
  onImportFiles: (files: FileList | File[]) => void;
  onToggleFavorite: (id: string) => void;
  onDeleteTrack?: (id: string) => void;
  onScanDevice?: () => void;
  isScanning?: boolean;
}

export const MusicExplorer: React.FC<MusicExplorerProps> = ({
  tracks,
  currentTrackId,
  onPlayTrack,
  onImportFiles,
  onToggleFavorite,
  onDeleteTrack,
  onScanDevice,
  isScanning
}) => {
  const [subTab, setSubTab] = useState<'TRACKS' | 'FOLDERS' | 'ARTISTS' | 'ALBUMS'>('TRACKS');
  const [searchQuery, setSearchQuery] = useState('');
  const [infoModalTrack, setInfoModalTrack] = useState<MediaItem | null>(null);
  const [copiedPath, setCopiedPath] = useState(false);

  const filteredTracks = tracks.filter(t => 
    t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (t.artist && t.artist.toLowerCase().includes(searchQuery.toLowerCase())) ||
    (t.album && t.album.toLowerCase().includes(searchQuery.toLowerCase())) ||
    (t.folder && t.folder.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const formatDuration = (secs: number) => {
    if (!secs || isNaN(secs)) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'N/A';
    if (bytes >= 1073741824) return `${(bytes / 1073741824).toFixed(2)} GB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  };

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col pb-36 pt-14 sm:pt-6 animate-fade-in px-3 sm:px-4">
      
      {/* Top Header */}
      <div className="flex items-center justify-between py-2 border-b border-white/10 mb-3">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <h1 className="font-ndot-title text-sm sm:text-base font-bold tracking-wider text-white">
            NOTHING MUSIC
          </h1>
          <span className="text-[10px] font-ndot-num text-white/40">({tracks.length})</span>
        </div>

        <div className="flex items-center gap-1.5">
          {/* Scan Device Button */}
          {onScanDevice && (
            <button
              onClick={() => {
                triggerHaptic('medium');
                onScanDevice();
              }}
              disabled={isScanning}
              className={`p-2 rounded-xl border text-xs font-mono active-press flex items-center gap-1 ${
                isScanning 
                  ? 'bg-[#D71921]/20 border-[#D71921] text-[#D71921]' 
                  : 'bg-white/5 border-white/10 text-white/80 hover:bg-white/15'
              }`}
              title="Scan Phone Storage For Audio"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? 'animate-spin text-[#D71921]' : ''}`} />
              <span className="text-[10px] hidden sm:inline">{isScanning ? 'SCANNING...' : 'SCAN'}</span>
            </button>
          )}

          {/* Import Music Button */}
          <label className="p-2 rounded-xl bg-white/5 border border-white/10 hover:bg-white/15 text-white active-press cursor-pointer">
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
      </div>

      {/* Search Input Bar */}
      <div className="relative w-full mb-3">
        <Search className="w-4 h-4 text-white/40 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          placeholder="Search tracks, artists, albums, folders..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-[#111111] border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs font-sans text-white placeholder-white/40 focus:outline-none focus:border-white/30"
        />
      </div>

      {/* Sub Category Tabs */}
      <div className="flex items-center gap-2 mb-4 overflow-x-auto no-scrollbar">
        {(['TRACKS', 'FOLDERS', 'ARTISTS', 'ALBUMS'] as const).map((tab) => {
          const isActive = subTab === tab;
          return (
            <button
              key={tab}
              onClick={() => {
                triggerHaptic('selection');
                setSubTab(tab);
              }}
              className={`px-3.5 py-1.5 rounded-full text-xs font-ndot transition-all whitespace-nowrap active-press ${
                isActive
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
        {filteredTracks.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-12 text-center rounded-3xl bg-[#0e0e0e] border border-white/10 my-4 gap-4">
            <div className="w-16 h-16 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-white/40">
              <Music className="w-8 h-8" />
            </div>
            <div className="flex flex-col gap-1">
              <h3 className="font-dot text-base text-white font-bold">NO MUSIC FOUND</h3>
              <p className="font-mono text-xs text-white/50 max-w-xs">
                Tap below to scan your device storage for audio tracks and songs.
              </p>
            </div>
            {onScanDevice && (
              <button
                onClick={onScanDevice}
                disabled={isScanning}
                className="px-6 py-3 rounded-2xl bg-white text-black font-mono text-xs font-bold hover:bg-white/90 active-press shadow-xl flex items-center gap-2"
              >
                <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
                {isScanning ? 'SCANNING STORAGE...' : 'SCAN DEVICE AUDIO'}
              </button>
            )}
          </div>
        ) : (
          filteredTracks.map((track) => {
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
                    <h3 className={`font-ndot-clean text-xs font-bold line-clamp-1 group-hover:text-[#D71921] transition-colors tracking-wide ${
                      isCurrent ? 'text-[#D71921]' : 'text-white'
                    }`}>
                      {track.title}
                    </h3>
                    <div className="flex items-center gap-2 mt-0.5 text-[10px] font-ndot text-white/40 line-clamp-1">
                      <span>{track.artist || 'Unknown Artist'}</span>
                      <span>•</span>
                      <span>{track.folder || 'Music'}</span>
                      <span>•</span>
                      <span>{track.format}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span className="font-ndot-num text-xs text-white/50">{formatDuration(track.duration)}</span>
                  
                  {/* Favorite Button */}
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

                  {/* Three-Dot Info Menu */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      triggerHaptic('light');
                      setInfoModalTrack(track);
                    }}
                    className="p-1.5 text-white/40 hover:text-white"
                  >
                    <MoreVertical className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Track Details & Technical Specs Bottom Modal */}
      {infoModalTrack && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/90 backdrop-blur-xl p-3 sm:p-4 animate-fade-in">
          <div className="w-full max-w-lg bg-black border border-white/20 rounded-3xl p-5 sm:p-6 shadow-2xl flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
                <span className="font-ndot-title text-xs sm:text-sm text-white font-bold tracking-wider">AUDIO FILE SPECIFICATIONS</span>
              </div>
              <button 
                onClick={() => setInfoModalTrack(null)} 
                className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-white/60 hover:text-white active-press"
              >
                ✕
              </button>
            </div>

            {/* Audio Title & Artist */}
            <div className="p-3.5 rounded-2xl bg-black border border-white/15 flex items-start gap-3">
              <div className="p-2.5 rounded-xl bg-[#D71921]/20 text-[#D71921] shrink-0 mt-0.5">
                <Music className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-[10px] font-ndot text-white/40 uppercase">TITLE & ARTIST</div>
                <div className="font-ndot-clean text-xs text-white font-bold break-all leading-relaxed mt-0.5 tracking-wide">
                  {infoModalTrack.title}
                </div>
                <div className="font-ndot text-[10px] text-white/60 mt-1">
                  {infoModalTrack.artist || 'Unknown Artist'} • {infoModalTrack.album || 'Music'}
                </div>
              </div>
            </div>

            {/* Exact Storage Location & Path (With Copy Button) */}
            <div className="p-3.5 rounded-2xl bg-black border border-white/15 flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-ndot text-white/40 uppercase flex items-center gap-1.5">
                  <HardDrive className="w-3.5 h-3.5 text-[#D71921]" />
                  EXACT STORAGE LOCATION
                </span>
                <button
                  onClick={() => {
                    const fullPath = infoModalTrack.path || infoModalTrack.url || infoModalTrack.contentUri || '';
                    if (fullPath) {
                      navigator.clipboard.writeText(fullPath);
                      triggerHaptic('medium');
                      setCopiedPath(true);
                      setTimeout(() => setCopiedPath(false), 2000);
                    }
                  }}
                  className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 text-[10px] font-ndot text-white font-bold flex items-center gap-1 active-press"
                >
                  {copiedPath ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                  {copiedPath ? 'COPIED ✓' : 'COPY PATH'}
                </button>
              </div>

              <div className="font-mono text-[11px] text-white/90 bg-black p-2.5 rounded-xl border border-white/10 break-all select-all">
                {infoModalTrack.path || (infoModalTrack.contentUri ? `Content URI: ${infoModalTrack.contentUri}` : 'Internal Device Storage')}
              </div>
            </div>

            {/* Technical Metadata Grid */}
            <div className="grid grid-cols-2 gap-2.5 font-ndot text-xs">
              <div className="p-3 rounded-2xl bg-black border border-white/15 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">FORMAT</span>
                <span className="text-white font-bold tracking-wider">{infoModalTrack.format}</span>
              </div>

              <div className="p-3 rounded-2xl bg-black border border-white/15 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">DURATION</span>
                <span className="text-white font-bold font-ndot-num">{formatDuration(infoModalTrack.duration)}</span>
              </div>

              <div className="p-3 rounded-2xl bg-black border border-white/15 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">FILE SIZE</span>
                <span className="text-white font-bold font-ndot-num">{formatFileSize(infoModalTrack.size)}</span>
              </div>

              <div className="p-3 rounded-2xl bg-black border border-white/15 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">FOLDER</span>
                <span className="text-white font-bold line-clamp-1 tracking-wider">{infoModalTrack.folder || 'MUSIC'}</span>
              </div>

              <div className="p-3 rounded-2xl bg-black border border-white/15 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">DATE ADDED</span>
                <span className="text-white font-bold font-ndot-num">
                  {infoModalTrack.addedAt ? new Date(infoModalTrack.addedAt).toLocaleDateString() : 'Recent'}
                </span>
              </div>

              <div className="p-3 rounded-2xl bg-black border border-white/15 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">SAMPLE RATE</span>
                <span className="text-white font-bold font-ndot-num">44.1 kHz • 16/24-bit</span>
              </div>
            </div>

            {/* Audio Codec & Sound Architecture */}
            <div className="p-3.5 rounded-2xl bg-black border border-white/15 flex flex-col gap-1.5 font-ndot">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-white/40 uppercase flex items-center gap-1.5">
                  <Volume2 className="w-3.5 h-3.5 text-[#D71921]" />
                  AUDIO CODEC & SOUND FORMAT
                </span>
                <span className="px-2 py-0.5 rounded-full bg-white/10 border border-white/15 text-white/90 text-[9px] font-bold">
                  {infoModalTrack.format}
                </span>
              </div>
              <div className="text-white font-bold text-xs tracking-wide">
                {infoModalTrack.audioCodec || (infoModalTrack.format === 'FLAC' ? 'FLAC Lossless Audio' : (infoModalTrack.format === 'M4A' || infoModalTrack.format === 'AAC' ? 'AAC LC (MPEG-4 Audio)' : 'MPEG-1 Audio Layer III (MP3)'))}
              </div>
              <div className="text-[11px] text-white/60">
                Configuration: <span className="text-white/90 font-medium font-ndot">{infoModalTrack.audioChannels || 'Stereo (2 Channels, 44.1 kHz, 16/24-bit)'}</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-2.5 pt-2">
              <button
                onClick={() => {
                  const target = infoModalTrack;
                  setInfoModalTrack(null);
                  onPlayTrack(target);
                }}
                className="flex-1 py-3 rounded-2xl bg-white text-black font-ndot text-xs font-bold hover:bg-white/90 active-press flex items-center justify-center gap-2 shadow-xl"
              >
                <Play className="w-4 h-4 fill-black" />
                PLAY AUDIO
              </button>

              {onDeleteTrack && (
                <button
                  onClick={() => {
                    onDeleteTrack(infoModalTrack.id);
                    setInfoModalTrack(null);
                  }}
                  className="px-5 py-3 rounded-2xl bg-red-500/15 border border-red-500/30 text-red-500 font-mono text-xs font-bold hover:bg-red-500/25 active-press"
                >
                  DELETE
                </button>
              )}
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
