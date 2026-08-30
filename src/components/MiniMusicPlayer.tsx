import React from 'react';
import type { MediaItem } from '../types/media';
import { Play, Pause, SkipForward, Heart } from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface MiniMusicPlayerProps {
  currentTrack: MediaItem | null;
  isPlaying: boolean;
  onPlayPause: () => void;
  onNext: () => void;
  onOpenFullPlayer: () => void;
  currentTime: number;
  duration: number;
  onToggleFavorite: (id: string) => void;
}

export const MiniMusicPlayer: React.FC<MiniMusicPlayerProps> = ({
  currentTrack,
  isPlaying,
  onPlayPause,
  onNext,
  onOpenFullPlayer,
  currentTime,
  duration,
  onToggleFavorite
}) => {
  if (!currentTrack || currentTrack.type === 'video') return null;

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div 
      onClick={() => {
        triggerHaptic('light');
        onOpenFullPlayer();
      }}
      className="fixed bottom-[60px] left-3 right-3 z-35 bg-black/95 backdrop-blur-xl border border-white/15 rounded-2xl p-2.5 flex items-center justify-between shadow-2xl cursor-pointer active-press group"
    >
      {/* Progress Bar Top Thin Line */}
      <div className="absolute top-0 left-3 right-3 h-[2px] bg-white/10 rounded-full overflow-hidden">
        <div 
          className="h-full bg-[#D71921] glow-red transition-all duration-100" 
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Track Art & Info */}
      <div className="flex items-center gap-3 min-w-0 flex-1">
        <div className="relative w-11 h-11 rounded-xl overflow-hidden bg-black shrink-0 border border-white/10">
          <img
            src={currentTrack.thumbnail || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200&auto=format&fit=crop&q=80'}
            alt={currentTrack.title}
            className={`w-full h-full object-cover ${isPlaying ? 'animate-spin-slow' : ''}`}
          />
          {isPlaying && (
            <div className="absolute inset-0 bg-black/20 flex items-center justify-center">
              <div className="w-2 h-2 rounded-full bg-[#D71921] glow-red animate-ping" />
            </div>
          )}
        </div>

        <div className="flex flex-col min-w-0">
          <span className="font-ndot-clean text-xs font-bold text-white line-clamp-1 group-hover:text-[#D71921] transition-colors tracking-wide">
            {currentTrack.title}
          </span>
          <span className="text-[10px] font-ndot text-white/50 line-clamp-1">
            {currentTrack.artist || 'Unknown Artist'}
          </span>
        </div>
      </div>

      {/* Controls */}
      <div className="flex items-center gap-1 shrink-0 ml-2" onClick={(e) => e.stopPropagation()}>
        <button
          onClick={() => {
            triggerHaptic('light');
            onToggleFavorite(currentTrack.id);
          }}
          className="p-2 text-white/40 hover:text-[#D71921] transition-colors"
        >
          <Heart className={`w-4 h-4 ${currentTrack.isFavorite ? 'fill-[#D71921] text-[#D71921]' : ''}`} />
        </button>

        <button
          onClick={() => {
            triggerHaptic('medium');
            onPlayPause();
          }}
          className="p-2.5 rounded-full bg-white text-black hover:scale-105 active-press transition-transform shadow-lg"
        >
          {isPlaying ? <Pause className="w-4 h-4 fill-black" /> : <Play className="w-4 h-4 fill-black ml-0.5" />}
        </button>

        <button
          onClick={() => {
            triggerHaptic('medium');
            onNext();
          }}
          className="p-2 text-white/70 hover:text-white active-press"
        >
          <SkipForward className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
