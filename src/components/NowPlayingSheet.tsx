import React, { useState, useEffect, useRef } from 'react';
import type { MediaItem, LyricsLine } from '../types/media';
import { parseLrc, getCurrentLyricIndex } from '../services/lrcParser';
import { triggerHaptic } from '../services/haptic';
import { 
  ChevronDown, 
  Play, 
  Pause, 
  SkipBack, 
  SkipForward, 
  Shuffle, 
  Repeat, 
  SlidersHorizontal, 
  Heart 
} from 'lucide-react';

interface NowPlayingSheetProps {
  isOpen: boolean;
  onClose: () => void;
  currentTrack: MediaItem | null;
  isPlaying: boolean;
  onPlayPause: () => void;
  onPrev: () => void;
  onNext: () => void;
  currentTime: number;
  duration: number;
  onSeek: (time: number) => void;
  volume?: number;
  onVolumeChange?: (vol: number) => void;
  isMuted?: boolean;
  onToggleMute?: () => void;
  shuffle: boolean;
  onToggleShuffle: () => void;
  repeatMode: 'off' | 'all' | 'one';
  onCycleRepeat: () => void;
  playbackRate?: number;
  onChangePlaybackRate?: (rate: number) => void;
  onToggleFavorite: (id: string) => void;
  onOpenEqualizer: () => void;
  queue: MediaItem[];
  onSelectTrack: (item: MediaItem) => void;
}

export const NowPlayingSheet: React.FC<NowPlayingSheetProps> = ({
  isOpen,
  onClose,
  currentTrack,
  isPlaying,
  onPlayPause,
  onPrev,
  onNext,
  currentTime,
  duration,
  onSeek,
  shuffle,
  onToggleShuffle,
  repeatMode,
  onCycleRepeat,
  onToggleFavorite,
  onOpenEqualizer,
  queue,
  onSelectTrack
}) => {
  if (!isOpen || !currentTrack) return null;

  const [activeTab, setActiveTab] = useState<'art' | 'lyrics' | 'queue'>('art');
  const [lyricsLines, setLyricsLines] = useState<LyricsLine[]>([]);
  const lyricsContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (currentTrack?.lyrics) {
      setLyricsLines(parseLrc(currentTrack.lyrics));
    } else {
      setLyricsLines([]);
    }
  }, [currentTrack]);

  const activeLyricIndex = getCurrentLyricIndex(lyricsLines, currentTime);

  useEffect(() => {
    if (activeTab === 'lyrics' && activeLyricIndex >= 0 && lyricsContainerRef.current) {
      const activeEl = lyricsContainerRef.current.children[activeLyricIndex] as HTMLElement;
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }, [activeLyricIndex, activeTab]);

  const formatTime = (secs: number) => {
    if (isNaN(secs)) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="fixed inset-0 z-50 bg-[#050505] flex flex-col justify-between animate-fade-in overflow-hidden select-none pb-safe pb-6">
      
      {/* Top Slide Handle & App Bar */}
      <div className="pt-safe pt-4 px-4 flex items-center justify-between border-b border-white/10 pb-3">
        <button
          onClick={() => {
            triggerHaptic('light');
            onClose();
          }}
          className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white active-press"
        >
          <ChevronDown className="w-6 h-6" />
        </button>

        <div className="flex flex-col items-center">
          <span className="font-dot text-xs tracking-widest text-[#D71921] font-bold">
            PLAYING FROM // {currentTrack.folder || 'NOTHING MUSIC'}
          </span>
          <span className="font-mono text-[10px] text-white/40">
            {currentTrack.format} • 24-BIT 48kHz
          </span>
        </div>

        <button
          onClick={() => {
            triggerHaptic('light');
            onOpenEqualizer();
          }}
          className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white active-press"
          title="10-Band EQ"
        >
          <SlidersHorizontal className="w-5 h-5 text-[#D71921]" />
        </button>
      </div>

      {/* Mode Switcher Tabs (Artwork / Synced Lyrics / Queue) */}
      <div className="flex items-center justify-center gap-2 my-2 px-4">
        <button
          onClick={() => {
            triggerHaptic('selection');
            setActiveTab('art');
          }}
          className={`px-3 py-1 rounded-full text-[11px] font-mono transition-all ${
            activeTab === 'art' ? 'bg-white text-black font-bold' : 'bg-white/5 text-white/50'
          }`}
        >
          ARTWORK
        </button>
        <button
          onClick={() => {
            triggerHaptic('selection');
            setActiveTab('lyrics');
          }}
          className={`px-3 py-1 rounded-full text-[11px] font-mono transition-all ${
            activeTab === 'lyrics' ? 'bg-white text-black font-bold' : 'bg-white/5 text-white/50'
          }`}
        >
          LYRICS {lyricsLines.length > 0 && '●'}
        </button>
        <button
          onClick={() => {
            triggerHaptic('selection');
            setActiveTab('queue');
          }}
          className={`px-3 py-1 rounded-full text-[11px] font-mono transition-all ${
            activeTab === 'queue' ? 'bg-white text-black font-bold' : 'bg-white/5 text-white/50'
          }`}
        >
          QUEUE ({queue.length})
        </button>
      </div>

      {/* Main Center Content */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 overflow-y-auto">
        
        {/* Artwork / Spinning Vinyl View */}
        {activeTab === 'art' && (
          <div className="relative flex flex-col items-center justify-center my-auto">
            {/* Spinning Grooved Vinyl */}
            <div className={`relative w-64 h-64 sm:w-72 sm:h-72 rounded-full bg-[#111111] border-4 border-[#222222] shadow-2xl flex items-center justify-center transition-all ${
              isPlaying ? 'animate-spin-slow glow-glyph' : 'paused'
            }`}>
              <div className="absolute inset-4 rounded-full border border-white/5" />
              <div className="absolute inset-10 rounded-full border border-white/5" />
              <div className="absolute inset-16 rounded-full border border-white/10" />

              <div className="relative w-28 h-28 rounded-full overflow-hidden border-2 border-white/30 shadow-inner">
                <img
                  src={currentTrack.thumbnail || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80'}
                  alt={currentTrack.title}
                  className="w-full h-full object-cover"
                />
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-black border border-white/40" />
              </div>
            </div>
          </div>
        )}

        {/* Synced Lyrics View */}
        {activeTab === 'lyrics' && (
          <div className="w-full h-full flex flex-col max-h-[340px]">
            {lyricsLines.length > 0 ? (
              <div ref={lyricsContainerRef} className="flex-1 overflow-y-auto space-y-4 py-8 text-center no-scrollbar">
                {lyricsLines.map((line, idx) => (
                  <p
                    key={idx}
                    onClick={() => {
                      triggerHaptic('medium');
                      onSeek(line.time);
                    }}
                    className={`transition-all duration-300 cursor-pointer ${
                      idx === activeLyricIndex
                        ? 'text-xl font-bold text-white scale-105 text-glow-white font-sans'
                        : 'text-sm text-white/30 hover:text-white/60 font-sans'
                    }`}
                  >
                    {line.text}
                  </p>
                ))}
              </div>
            ) : (
              <div className="flex-1 flex items-center justify-center text-white/40 font-mono text-xs">
                NO SYNCHRONIZED LYRICS AVAILABLE
              </div>
            )}
          </div>
        )}

        {/* Queue View */}
        {activeTab === 'queue' && (
          <div className="w-full h-full flex flex-col max-h-[340px] overflow-y-auto space-y-2 pr-1 no-scrollbar">
            {queue.map((item, idx) => {
              const isCurrent = currentTrack.id === item.id;
              return (
                <div
                  key={item.id}
                  onClick={() => {
                    triggerHaptic('light');
                    onSelectTrack(item);
                  }}
                  className={`flex items-center justify-between p-3 rounded-2xl border transition-all cursor-pointer ${
                    isCurrent ? 'bg-white text-black font-bold' : 'bg-white/5 border-white/5 text-white/80'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className={`font-mono text-xs ${isCurrent ? 'text-[#D71921]' : 'text-white/40'}`}>
                      {(idx + 1).toString().padStart(2, '0')}
                    </span>
                    <div className="flex flex-col">
                      <span className="font-mono text-xs line-clamp-1">{item.title}</span>
                      <span className={`text-[10px] ${isCurrent ? 'text-black/60' : 'text-white/40'}`}>{item.artist}</span>
                    </div>
                  </div>
                  <span className="font-mono text-[10px]">{formatTime(item.duration)}</span>
                </div>
              );
            })}
          </div>
        )}

      </div>

      {/* Track Info (Title & Artist) */}
      <div className="px-6 mb-2 flex items-center justify-between">
        <div className="flex-1 min-w-0 pr-4">
          <h2 className="font-dot text-xl sm:text-2xl font-bold text-white line-clamp-1">
            {currentTrack.title}
          </h2>
          <p className="font-sans text-sm text-white/60 line-clamp-1 mt-0.5">
            {currentTrack.artist || 'Unknown Artist'}
          </p>
        </div>

        <button
          onClick={() => onToggleFavorite(currentTrack.id)}
          className="p-2 text-white/50 hover:text-[#D71921] active-press"
        >
          <Heart className={`w-6 h-6 ${currentTrack.isFavorite ? 'fill-[#D71921] text-[#D71921]' : ''}`} />
        </button>
      </div>

      {/* Progress Scrubber Bar */}
      <div className="px-6 flex flex-col gap-1.5 mb-3">
        <div 
          className="relative w-full h-3 bg-white/10 rounded-full cursor-pointer overflow-hidden group"
          onClick={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const ratio = Math.max(0, Math.min(1, clickX / rect.width));
            triggerHaptic('light');
            onSeek(ratio * duration);
          }}
        >
          <div 
            className="h-full bg-white transition-all duration-75 group-hover:bg-[#D71921]"
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        <div className="flex justify-between font-mono text-[11px] text-white/50">
          <span>{formatTime(currentTime)}</span>
          <span>{formatTime(duration)}</span>
        </div>
      </div>

      {/* Main Transport Controls Row */}
      <div className="px-6 flex items-center justify-between gap-2">
        <button
          onClick={() => {
            triggerHaptic('light');
            onToggleShuffle();
          }}
          className={`p-2.5 rounded-xl border transition-all ${
            shuffle ? 'bg-white text-black border-white' : 'bg-white/5 text-white/50 border-white/10'
          }`}
        >
          <Shuffle className="w-4 h-4" />
        </button>

        <button
          onClick={() => {
            triggerHaptic('medium');
            onPrev();
          }}
          className="p-3 rounded-2xl bg-white/5 text-white hover:bg-white/15 active-press"
        >
          <SkipBack className="w-6 h-6" />
        </button>

        {/* Big Play / Pause */}
        <button
          onClick={() => {
            triggerHaptic('heavy');
            onPlayPause();
          }}
          className="p-5 rounded-full bg-white text-black hover:scale-105 active-press transition-transform shadow-2xl border-4 border-[#D71921]"
        >
          {isPlaying ? <Pause className="w-7 h-7 fill-black" /> : <Play className="w-7 h-7 fill-black ml-0.5" />}
        </button>

        <button
          onClick={() => {
            triggerHaptic('medium');
            onNext();
          }}
          className="p-3 rounded-2xl bg-white/5 text-white hover:bg-white/15 active-press"
        >
          <SkipForward className="w-6 h-6" />
        </button>

        <button
          onClick={() => {
            triggerHaptic('light');
            onCycleRepeat();
          }}
          className={`p-2.5 rounded-xl border transition-all ${
            repeatMode !== 'off' ? 'bg-white text-black border-white font-bold' : 'bg-white/5 text-white/50 border-white/10'
          }`}
        >
          <Repeat className="w-4 h-4" />
        </button>
      </div>

    </div>
  );
};
