import React from 'react';
import { 
  SlidersHorizontal, 
  Cpu, 
  Smartphone, 
  HardDrive,
  Volume2
} from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface SettingsHubProps {
  onOpenEqualizer: () => void;
  onOpenShortcuts: () => void;
  audioBoost: number;
  setAudioBoost: (val: number) => void;
  totalMediaCount: number;
}

export const SettingsHub: React.FC<SettingsHubProps> = ({
  onOpenEqualizer,
  onOpenShortcuts,
  audioBoost,
  setAudioBoost,
  totalMediaCount
}) => {
  return (
    <div className="w-full max-w-3xl mx-auto flex flex-col gap-4 pb-32 pt-14 sm:pt-6 animate-fade-in px-4">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-white/10 pb-3 mb-2">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <h1 className="font-ndot-title text-sm sm:text-base font-bold tracking-wider text-white">
            SETTINGS & PREFERENCES
          </h1>
        </div>
        <span className="font-ndot text-[10px] text-white/40">NOTHING OS // 2.5</span>
      </div>

      {/* Settings Grid */}
      <div className="flex flex-col gap-3">
        
        {/* Equalizer Tile */}
        <div 
          onClick={() => {
            triggerHaptic('medium');
            onOpenEqualizer();
          }}
          className="flex items-center justify-between p-4 rounded-2xl bg-black border border-white/15 hover:border-white/25 transition-all cursor-pointer active-press group"
        >
          <div className="flex items-center gap-3.5">
            <div className="p-3 rounded-xl bg-white/5 border border-white/10 text-[#D71921]">
              <SlidersHorizontal className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-mono text-sm font-bold text-white group-hover:text-[#D71921] transition-colors">
                10-Band DSP Equalizer
              </h3>
              <p className="font-sans text-xs text-white/40 mt-0.5">
                Bass punch, Preamp, and Nothing acoustic presets
              </p>
            </div>
          </div>
          <span className="text-white/40 font-mono text-sm">›</span>
        </div>

        {/* Decoder Settings */}
        <div className="flex items-center justify-between p-4 rounded-2xl bg-black border border-white/15">
          <div className="flex items-center gap-3.5">
            <div className="p-3 rounded-xl bg-white/5 border border-white/10 text-white/80">
              <Cpu className="w-5 h-5 text-[#D71921]" />
            </div>
            <div>
              <h3 className="font-mono text-sm font-bold text-white">
                Hardware Acceleration (HW+)
              </h3>
              <p className="font-sans text-xs text-white/40 mt-0.5">
                Utilize GPU hardware decoding for 4K / 60FPS videos
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-lg bg-[#D71921]/20 text-[#D71921] font-mono text-xs font-bold border border-[#D71921]/40">
            ACTIVE (HW)
          </span>
        </div>

        {/* Audio Boost 200% */}
        <div className="flex flex-col p-4 rounded-2xl bg-black border border-white/15 gap-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3.5">
              <div className="p-3 rounded-xl bg-white/5 border border-white/10 text-white/80">
                <Volume2 className="w-5 h-5 text-[#D71921]" />
              </div>
              <div>
                <h3 className="font-mono text-sm font-bold text-white">
                  MX Audio Boost Limit
                </h3>
                <p className="font-sans text-xs text-white/40 mt-0.5">
                  Overdrive audio output up to 200% with clipping limiter
                </p>
              </div>
            </div>
            <span className={`font-mono text-xs font-bold ${audioBoost > 100 ? 'text-[#D71921]' : 'text-white'}`}>
              {audioBoost}%
            </span>
          </div>

          <input
            type="range"
            min="100"
            max="200"
            step="5"
            value={audioBoost}
            onChange={(e) => {
              triggerHaptic('light');
              setAudioBoost(parseFloat(e.target.value));
            }}
            className="accent-[#D71921] w-full cursor-pointer mt-1"
          />
        </div>

        {/* Gestures Guide */}
        <div 
          onClick={() => {
            triggerHaptic('medium');
            onOpenShortcuts();
          }}
          className="flex items-center justify-between p-4 rounded-2xl bg-black border border-white/15 hover:border-white/25 transition-all cursor-pointer active-press group"
        >
          <div className="flex items-center gap-3.5">
            <div className="p-3 rounded-xl bg-white/5 border border-white/10 text-white/80">
              <Smartphone className="w-5 h-5 text-[#D71921]" />
            </div>
            <div>
              <h3 className="font-mono text-sm font-bold text-white group-hover:text-[#D71921] transition-colors">
                Touch Gestures & Shortcuts Guide
              </h3>
              <p className="font-sans text-xs text-white/40 mt-0.5">
                Brightness, Volume, Seeking swipe tutorials
              </p>
            </div>
          </div>
          <span className="text-white/40 font-mono text-sm">›</span>
        </div>

        {/* Device Media Vault Info */}
        <div className="flex items-center justify-between p-4 rounded-2xl bg-black border border-white/15">
          <div className="flex items-center gap-3.5">
            <div className="p-3 rounded-xl bg-white/5 border border-white/10 text-white/80">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-mono text-sm font-bold text-white">
                IndexedDB Local Storage
              </h3>
              <p className="font-sans text-xs text-white/40 mt-0.5">
                {totalMediaCount} files indexed in offline database
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-lg bg-white/10 text-white font-mono text-xs">
            SAVED
          </span>
        </div>

      </div>

      {/* About Box */}
      <div className="p-5 rounded-3xl bg-black/60 border border-white/10 text-center flex flex-col items-center gap-2 mt-4">
        <span className="font-ndot-title text-sm text-white font-bold tracking-widest flex items-center gap-1.5">
          NOTHING PLAYER <span className="text-[#D71921]">(01)</span>
        </span>
        <span className="font-ndot text-[10px] text-white/40">
          VERSION 2.0.0 // NOTHING DUAL-ENGINE ARCHITECTURE
        </span>
        <span className="font-sans text-[11px] text-white/30">
          Engineered for Nothing Phone (2a) & all Android devices.
        </span>
      </div>

    </div>
  );
};
