import React, { useState, useEffect, useRef } from 'react';
import type { MediaItem, LyricsLine } from '../types/media';
import { parseLrc, getCurrentLyricIndex } from '../services/lrcParser';
import { triggerHaptic } from '../services/haptic';
import { 
  Play, 
  Pause, 
  SkipBack, 
  SkipForward, 
  Shuffle, 
  Repeat, 
  SlidersHorizontal, 
  ListMusic, 
  Heart, 
  Disc, 
  Sparkles,
  Volume2,
  VolumeX,
  FastForward,
  Rewind
} from 'lucide-react';

interface MusicHubProps {
  currentTrack: MediaItem | null;
  isPlaying: boolean;
  onPlayPause: () => void;
  onPrev: () => void;
  onNext: () => void;
  currentTime: number;
  duration: number;
  onSeek: (time: number) => void;
  volume: number;
  onVolumeChange: (vol: number) => void;
  isMuted: boolean;
  onToggleMute: () => void;
  shuffle: boolean;
  onToggleShuffle: () => void;
  repeatMode: 'off' | 'all' | 'one';
  onCycleRepeat: () => void;
  playbackRate: number;
  onChangePlaybackRate: (rate: number) => void;
  onToggleFavorite: (id: string) => void;
  onOpenEqualizer: () => void;
  queue: MediaItem[];
  onSelectTrack: (item: MediaItem) => void;
}

export const MusicHub: React.FC<MusicHubProps> = ({
  currentTrack,
  isPlaying,
  onPlayPause,
  onPrev,
  onNext,
  currentTime,
  duration,
  onSeek,
  volume,
  onVolumeChange,
  isMuted,
  onToggleMute,
  shuffle,
  onToggleShuffle,
  repeatMode,
  onCycleRepeat,
  playbackRate,
  onChangePlaybackRate,
  onToggleFavorite,
  onOpenEqualizer,
  queue,
  onSelectTrack
}) => {
  const [showLyrics, setShowLyrics] = useState(true);
  const [showQueue, setShowQueue] = useState(false);
  const [lyricsLines, setLyricsLines] = useState<LyricsLine[]>([]);
  const lyricsContainerRef = useRef<HTMLDivElement | null>(null);

  // Parse lyrics when track changes
  useEffect(() => {
    if (currentTrack?.lyrics) {
      setLyricsLines(parseLrc(currentTrack.lyrics));
    } else {
      setLyricsLines([]);
    }
  }, [currentTrack]);

  const activeLyricIndex = getCurrentLyricIndex(lyricsLines, currentTime);

  // Auto-scroll lyrics smoothly to active line
  useEffect(() => {
    if (showLyrics && activeLyricIndex >= 0 && lyricsContainerRef.current) {
      const activeEl = lyricsContainerRef.current.children[activeLyricIndex] as HTMLElement;
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }, [activeLyricIndex, showLyrics]);

  const formatTime = (secs: number) => {
    if (isNaN(secs)) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div className="w-full max-w-7xl mx-auto p-3 sm:p-6 flex flex-col gap-6 animate-fade-in">
      
      {/* Top Deck Banner */}
      <div className="flex items-center justify-between border-b border-white/10 pb-3">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <span className="font-dot text-sm sm:text-base tracking-widest text-white">
            AUDIO DECK // 01 MUSIC
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              triggerHaptic('light');
              setShowLyrics(!showLyrics);
            }}
            className={`px-3 py-1 rounded-md text-xs font-mono border transition-all active-press ${
              showLyrics ? 'bg-white text-black font-bold border-white' : 'bg-white/5 text-white/70 border-white/10'
            }`}
          >
            LYRICS {lyricsLines.length > 0 && <span className="text-[#D71921] ml-1">●</span>}
          </button>

          <button
            onClick={() => {
              triggerHaptic('light');
              setShowQueue(!showQueue);
            }}
            className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-mono border transition-all active-press ${
              showQueue ? 'bg-white text-black font-bold border-white' : 'bg-white/5 text-white/70 border-white/10'
            }`}
          >
            <ListMusic className="w-3.5 h-3.5" />
            <span>QUEUE ({queue.length})</span>
          </button>
        </div>
      </div>

      {/* Main Dual Grid: Turntable / Vinyl + Synced Lyrics & Info */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* Left Column: Vinyl Turntable & Artwork (5 cols) */}
        <div className="lg:col-span-5 flex flex-col items-center justify-center p-6 sm:p-8 rounded-3xl bg-[#0c0c0c] border border-white/10 relative overflow-hidden shadow-2xl">
          <div className="absolute inset-0 bg-grid-pattern opacity-30 pointer-events-none" />

          {/* Vinyl Disc with Reactive LED Ring */}
          <div className="relative flex items-center justify-center my-4 group">
            
            {/* Spinning Outer Grooved Vinyl */}
            <div className={`relative w-56 h-56 sm:w-64 sm:h-64 rounded-full bg-[#111111] border-4 border-[#222222] shadow-2xl flex items-center justify-center transition-all ${
              isPlaying ? 'animate-spin-slow glow-glyph' : 'paused'
            }`}>
              
              {/* Vinyl Grooves concentric rings */}
              <div className="absolute inset-4 rounded-full border border-white/5" />
              <div className="absolute inset-8 rounded-full border border-white/5" />
              <div className="absolute inset-12 rounded-full border border-white/5" />
              <div className="absolute inset-16 rounded-full border border-white/10" />

              {/* Center Album Art Label */}
              <div className="relative w-24 h-24 sm:w-28 sm:h-28 rounded-full overflow-hidden border-2 border-white/30 shadow-inner">
                <img
                  src={currentTrack?.thumbnail || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80'}
                  alt={currentTrack?.title || 'Nothing Music'}
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-black/20" />
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-black border border-white/40" />
              </div>
            </div>

            {/* Tonearm Simulation */}
            <div className={`absolute top-2 right-4 w-1.5 h-20 bg-white/40 origin-top rounded-full transition-transform duration-500 pointer-events-none ${
              isPlaying ? 'rotate-[25deg]' : 'rotate-0'
            }`} />
          </div>

          {/* Hardware Specs Badge */}
          <div className="w-full flex items-center justify-between border-t border-white/10 pt-4 mt-2 text-[10px] font-mono text-white/50">
            <span className="flex items-center gap-1">
              <Disc className="w-3 h-3 text-[#D71921]" />
              FORMAT: {currentTrack?.format || 'AUDIO/FLAC'}
            </span>
            <span>SAMPLE: 48kHz / 24-BIT</span>
          </div>
        </div>

        {/* Right Column: Track Info, Lyrics Viewer or Queue Drawer (7 cols) */}
        <div className="lg:col-span-7 flex flex-col justify-between p-6 sm:p-8 rounded-3xl bg-[#0c0c0c] border border-white/10 relative overflow-hidden shadow-2xl min-h-[460px]">
          <div className="absolute inset-0 bg-grid-pattern opacity-20 pointer-events-none" />

          {showQueue ? (
            /* Queue View */
            <div className="relative z-10 flex flex-col h-full">
              <div className="flex items-center justify-between border-b border-white/10 pb-3 mb-4">
                <span className="font-dot text-sm tracking-wider text-white">UP NEXT IN QUEUE</span>
                <span className="font-mono text-xs text-white/50">{queue.length} Tracks</span>
              </div>

              <div className="flex-1 overflow-y-auto space-y-2 pr-1 max-h-[340px]">
                {queue.map((item, idx) => {
                  const isCurrent = currentTrack?.id === item.id;
                  return (
                    <div
                      key={item.id}
                      onClick={() => {
                        triggerHaptic('light');
                        onSelectTrack(item);
                      }}
                      className={`flex items-center justify-between p-3 rounded-2xl border transition-all cursor-pointer active-press ${
                        isCurrent
                          ? 'bg-white text-black border-white font-bold'
                          : 'bg-white/5 border-white/5 text-white/80 hover:bg-white/10'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <span className={`font-mono text-xs ${isCurrent ? 'text-[#D71921] font-bold' : 'text-white/40'}`}>
                          {(idx + 1).toString().padStart(2, '0')}
                        </span>
                        <div className="flex flex-col">
                          <span className="font-mono text-xs tracking-wide line-clamp-1">{item.title}</span>
                          <span className={`text-[10px] ${isCurrent ? 'text-black/70' : 'text-white/40'}`}>{item.artist || 'Unknown Artist'}</span>
                        </div>
                      </div>
                      <span className="font-mono text-[10px]">{formatTime(item.duration)}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : showLyrics && lyricsLines.length > 0 ? (
            /* Synced Lyrics Scroller View */
            <div className="relative z-10 flex flex-col h-full">
              <div className="flex items-center justify-between border-b border-white/10 pb-3 mb-4">
                <div className="flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-[#D71921]" />
                  <span className="font-dot text-xs tracking-wider text-white">SYNCED LYRICS (.LRC)</span>
                </div>
                <span className="font-mono text-[10px] text-white/40">TAP LINE TO JUMP</span>
              </div>

              <div 
                ref={lyricsContainerRef}
                className="flex-1 overflow-y-auto space-y-4 py-6 pr-2 max-h-[300px] text-center"
              >
                {lyricsLines.map((line, idx) => {
                  const isActive = idx === activeLyricIndex;
                  return (
                    <p
                      key={idx}
                      onClick={() => {
                        triggerHaptic('medium');
                        onSeek(line.time);
                      }}
                      className={`transition-all duration-300 cursor-pointer font-sans select-none ${
                        isActive
                          ? 'text-lg sm:text-xl font-bold text-white scale-105 text-glow-white'
                          : 'text-sm sm:text-base text-white/30 hover:text-white/60'
                      }`}
                    >
                      {line.text}
                    </p>
                  );
                })}
              </div>
            </div>
          ) : (
            /* Track Metadata Info View */
            <div className="relative z-10 flex flex-col justify-center flex-1 py-8">
              <div className="flex items-center gap-2 mb-2">
                <span className="px-2 py-0.5 rounded bg-[#D71921]/20 border border-[#D71921]/40 text-[#D71921] font-mono text-[10px]">
                  NOW PLAYING
                </span>
                <span className="font-mono text-[10px] text-white/40">
                  {currentTrack?.album || 'Nothing Album'}
                </span>
              </div>

              <h1 className="font-dot text-2xl sm:text-4xl text-white font-bold tracking-wide mb-2 line-clamp-2">
                {currentTrack?.title || 'SELECT TRACK'}
              </h1>
              
              <h2 className="font-sans text-base sm:text-lg text-white/70 tracking-wider mb-6">
                {currentTrack?.artist || 'Nothing Artist'}
              </h2>

              <div className="p-4 rounded-2xl bg-black/50 border border-white/10 font-mono text-xs text-white/60 space-y-1.5">
                <div className="flex justify-between">
                  <span>FILE TYPE:</span>
                  <span className="text-white">{currentTrack?.format || 'MPEG Audio'}</span>
                </div>
                <div className="flex justify-between">
                  <span>AUDIO ENGINE:</span>
                  <span className="text-[#D71921]">10-BAND REALTIME DSP</span>
                </div>
              </div>
            </div>
          )}

          {/* Bottom Mini Controls Bar (Within Card) */}
          <div className="relative z-10 flex items-center justify-between border-t border-white/10 pt-4 mt-4">
            <button
              onClick={() => currentTrack && onToggleFavorite(currentTrack.id)}
              className={`flex items-center gap-1.5 text-xs font-mono transition-colors active-press ${
                currentTrack?.isFavorite ? 'text-[#D71921]' : 'text-white/50 hover:text-white'
              }`}
            >
              <Heart className={`w-4 h-4 ${currentTrack?.isFavorite ? 'fill-[#D71921]' : ''}`} />
              <span>FAVORITE</span>
            </button>

            <button
              onClick={onOpenEqualizer}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-white/10 bg-white/5 hover:bg-white/10 text-white text-xs font-mono transition-all active-press"
            >
              <SlidersHorizontal className="w-3.5 h-3.5 text-[#D71921]" />
              <span>CUSTOM EQ</span>
            </button>
          </div>

        </div>

      </div>

      {/* Main Music Control Deck (Bottom Sticky / Master Bar) */}
      <div className="p-5 sm:p-7 rounded-3xl bg-[#0e0e0e] border border-white/15 shadow-2xl relative overflow-hidden flex flex-col gap-4">
        
        {/* Waveform Scrubber & Timestamps */}
        <div className="w-full flex flex-col gap-1.5">
          <div className="relative w-full h-3 bg-white/10 rounded-full cursor-pointer overflow-hidden group"
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

          <div className="flex items-center justify-between text-xs font-mono text-white/50">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Transport Actions Grid */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          
          {/* Secondary Controls (Shuffle, Repeat, Speed) */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                triggerHaptic('light');
                onToggleShuffle();
              }}
              title="Shuffle"
              className={`p-2.5 rounded-xl border transition-all active-press ${
                shuffle ? 'bg-white text-black border-white' : 'bg-white/5 text-white/60 border-white/10'
              }`}
            >
              <Shuffle className="w-4 h-4" />
            </button>

            <button
              onClick={() => {
                triggerHaptic('light');
                onCycleRepeat();
              }}
              title="Repeat"
              className={`p-2.5 rounded-xl border transition-all active-press ${
                repeatMode !== 'off' ? 'bg-white text-black border-white font-bold' : 'bg-white/5 text-white/60 border-white/10'
              }`}
            >
              <Repeat className="w-4 h-4" />
              {repeatMode === 'one' && <span className="text-[9px] absolute top-1 right-1 font-bold">1</span>}
            </button>

            <button
              onClick={() => {
                triggerHaptic('light');
                const rates = [0.75, 1.0, 1.25, 1.5, 2.0];
                const nextIdx = (rates.indexOf(playbackRate) + 1) % rates.length;
                onChangePlaybackRate(rates[nextIdx]);
              }}
              className="px-2.5 py-1.5 rounded-xl bg-white/5 border border-white/10 text-xs font-mono text-white/70 hover:text-white active-press"
            >
              {playbackRate}x
            </button>
          </div>

          {/* Primary Center Playback Controls */}
          <div className="flex items-center gap-3 sm:gap-4">
            <button
              onClick={() => {
                triggerHaptic('light');
                onSeek(Math.max(0, currentTime - 10));
              }}
              title="Rewind 10s"
              className="p-2 rounded-xl text-white/60 hover:text-white transition-all active-press"
            >
              <Rewind className="w-5 h-5" />
            </button>

            <button
              onClick={() => {
                triggerHaptic('medium');
                onPrev();
              }}
              title="Previous"
              className="p-3 rounded-2xl bg-white/5 border border-white/10 hover:bg-white/15 text-white transition-all active-press"
            >
              <SkipBack className="w-5 h-5" />
            </button>

            {/* Play/Pause Button with Nothing Red Active Ring */}
            <button
              onClick={() => {
                triggerHaptic('heavy');
                onPlayPause();
              }}
              title={isPlaying ? 'Pause' : 'Play'}
              className="p-5 rounded-full bg-white text-black hover:scale-105 active-press transition-all shadow-xl shadow-white/20 border-4 border-[#D71921]"
            >
              {isPlaying ? <Pause className="w-7 h-7 fill-black" /> : <Play className="w-7 h-7 fill-black ml-0.5" />}
            </button>

            <button
              onClick={() => {
                triggerHaptic('medium');
                onNext();
              }}
              title="Next"
              className="p-3 rounded-2xl bg-white/5 border border-white/10 hover:bg-white/15 text-white transition-all active-press"
            >
              <SkipForward className="w-5 h-5" />
            </button>

            <button
              onClick={() => {
                triggerHaptic('light');
                onSeek(Math.min(duration, currentTime + 10));
              }}
              title="Forward 10s"
              className="p-2 rounded-xl text-white/60 hover:text-white transition-all active-press"
            >
              <FastForward className="w-5 h-5" />
            </button>
          </div>

          {/* Volume Control */}
          <div className="flex items-center gap-2 w-full sm:w-44">
            <button
              onClick={() => {
                triggerHaptic('light');
                onToggleMute();
              }}
              className="p-2 text-white/70 hover:text-white active-press"
            >
              {isMuted || volume === 0 ? <VolumeX className="w-4 h-4 text-[#D71921]" /> : <Volume2 className="w-4 h-4" />}
            </button>
            
            <input
              type="range"
              min="0"
              max="1"
              step="0.01"
              value={isMuted ? 0 : volume}
              onChange={(e) => {
                triggerHaptic('light');
                onVolumeChange(parseFloat(e.target.value));
              }}
              className="accent-[#D71921] w-full cursor-pointer"
            />
          </div>

        </div>

      </div>

    </div>
  );
};
