import React from 'react';
import { Sun, Volume2, VolumeX, FastForward, Rewind } from 'lucide-react';

interface GestureOverlayProps {
  type: 'brightness' | 'volume' | 'seek' | 'doubletap_left' | 'doubletap_right' | null;
  value: number; // 0-100 or 0-200 for vol
  formattedText?: string;
  visible: boolean;
}

export const GestureOverlay: React.FC<GestureOverlayProps> = ({
  type,
  value,
  formattedText,
  visible
}) => {
  if (!visible || !type) return null;

  return (
    <div className="absolute inset-0 z-30 pointer-events-none flex items-center justify-center transition-opacity duration-200">
      
      {/* Brightness HUD (Left Swipe) */}
      {type === 'brightness' && (
        <div className="flex flex-col items-center gap-3 p-4 sm:p-5 rounded-2xl bg-black/80 backdrop-blur-md border border-white/20 shadow-2xl animate-fade-in">
          <Sun className="w-8 h-8 text-white animate-pulse" />
          <div className="flex flex-col items-center">
            <span className="font-ndot-title text-base text-white font-bold tracking-wider">
              BRIGHTNESS
            </span>
            <span className="font-ndot-num text-sm text-white/80 mt-0.5">
              {Math.round(value)}%
            </span>
          </div>

          {/* Dot Matrix Level Bar */}
          <div className="w-32 h-2 rounded-full bg-white/20 overflow-hidden">
            <div 
              className="h-full bg-white transition-all duration-75"
              style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
            />
          </div>
        </div>
      )}

      {/* Volume HUD (Right Swipe) with 200% Audio Boost */}
      {type === 'volume' && (
        <div className="flex flex-col items-center gap-3 p-4 sm:p-5 rounded-2xl bg-black/80 backdrop-blur-md border border-white/20 shadow-2xl animate-fade-in">
          {value === 0 ? (
            <VolumeX className="w-8 h-8 text-white/40" />
          ) : (
            <Volume2 className={`w-8 h-8 ${value > 100 ? 'text-[#D71921] animate-pulse' : 'text-white'}`} />
          )}

          <div className="flex flex-col items-center">
            <span className="font-ndot-title text-base text-white font-bold tracking-wider flex items-center gap-1.5">
              VOLUME {value > 100 && <span className="text-[#D71921] text-xs font-ndot font-normal">[BOOST]</span>}
            </span>
            <span className={`font-ndot-num text-sm font-bold mt-0.5 ${value > 100 ? 'text-[#D71921]' : 'text-white/80'}`}>
              {Math.round(value)}%
            </span>
          </div>

          {/* Level Bar with Red color when boosted above 100% */}
          <div className="w-32 h-2 rounded-full bg-white/20 overflow-hidden relative">
            <div 
              className={`h-full transition-all duration-75 ${value > 100 ? 'bg-[#D71921] glow-red' : 'bg-white'}`}
              style={{ width: `${Math.min(100, (value / 200) * 100)}%` }}
            />
          </div>
        </div>
      )}

      {/* Seek HUD (Horizontal Swipe) */}
      {type === 'seek' && (
        <div className="flex flex-col items-center gap-2 p-4 sm:p-6 rounded-2xl bg-black/85 backdrop-blur-md border border-white/20 shadow-2xl">
          <div className="font-ndot-num text-xl text-white font-bold tracking-wider">
            {formattedText || '00:00'}
          </div>
          <span className="font-ndot text-xs text-[#D71921] tracking-widest uppercase">
            // SEEK POSITION //
          </span>
        </div>
      )}

      {/* Double Tap Left (-10s) */}
      {type === 'doubletap_left' && (
        <div className="absolute left-8 flex flex-col items-center gap-1 p-4 rounded-full bg-black/70 border border-white/20 text-white animate-ping">
          <Rewind className="w-6 h-6 text-[#D71921]" />
          <span className="font-mono text-xs">-10s</span>
        </div>
      )}

      {/* Double Tap Right (+10s) */}
      {type === 'doubletap_right' && (
        <div className="absolute right-8 flex flex-col items-center gap-1 p-4 rounded-full bg-black/70 border border-white/20 text-white animate-ping">
          <FastForward className="w-6 h-6 text-[#D71921]" />
          <span className="font-mono text-xs">+10s</span>
        </div>
      )}

    </div>
  );
};
