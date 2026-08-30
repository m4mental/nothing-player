import React from 'react';
import { 
  Music, 
  Film, 
  FolderOpen, 
  Radio, 
  SlidersHorizontal, 
  HelpCircle, 
  PlusCircle,
  Sparkles
} from 'lucide-react';
import type { ActiveHub } from '../types/media';
import { triggerHaptic } from '../services/haptic';

interface HeaderProps {
  activeHub: ActiveHub;
  setActiveHub: (hub: ActiveHub) => void;
  onOpenEqualizer: () => void;
  onOpenShortcuts: () => void;
  onOpenImport: () => void;
  isPlaying: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  activeHub,
  setActiveHub,
  onOpenEqualizer,
  onOpenShortcuts,
  onOpenImport,
  isPlaying,
}) => {
  const hubs: { id: ActiveHub; label: string; number: string; icon: React.ReactNode }[] = [
    { id: 'MUSIC', label: 'MUSIC', number: '01', icon: <Music className="w-3.5 h-3.5" /> },
    { id: 'VIDEO', label: 'VIDEO', number: '02', icon: <Film className="w-3.5 h-3.5" /> },
    { id: 'LIBRARY', label: 'LIBRARY', number: '03', icon: <FolderOpen className="w-3.5 h-3.5" /> },
    { id: 'RADIO', label: 'RADIO', number: '04', icon: <Radio className="w-3.5 h-3.5" /> },
    { id: 'GLYPH', label: 'GLYPH', number: '05', icon: <Sparkles className="w-3.5 h-3.5" /> },
  ];

  return (
    <header className="sticky top-0 z-40 bg-[#050505]/95 backdrop-blur-md border-b border-white/10 px-3 sm:px-6 pt-9 sm:pt-4 pb-2.5">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-2.5">
        
        {/* Brand & Nothing LED indicator */}
        <div className="flex items-center justify-between w-full md:w-auto gap-4">
          <div className="flex items-center gap-2.5">
            {/* Blinking Nothing Red LED */}
            <div className={`w-2.5 h-2.5 rounded-full ${isPlaying ? 'bg-[#D71921] glow-red animate-pulse' : 'bg-white/30'}`} />
            
            <div className="flex flex-col">
              <span className="font-dot text-sm sm:text-base tracking-widest text-white font-bold flex items-center gap-1.5">
                NOTHING PLAYER <span className="text-[#D71921] text-xs font-mono font-normal">(01)</span>
              </span>
              <span className="font-mono-tech text-[9px] text-white/40 tracking-wider">
                VLC-GRADE DUAL HUB // TECH SUITE
              </span>
            </div>
          </div>

          {/* Quick Mobile Action Buttons */}
          <div className="flex md:hidden items-center gap-1">
            <button
              onClick={() => {
                triggerHaptic('light');
                onOpenImport();
              }}
              title="Add Media"
              className="p-1.5 rounded-lg border border-white/10 bg-white/5 active-press text-white/80"
            >
              <PlusCircle className="w-4 h-4 text-[#D71921]" />
            </button>
            <button
              onClick={() => {
                triggerHaptic('light');
                onOpenEqualizer();
              }}
              title="10-Band EQ"
              className="p-1.5 rounded-lg border border-white/10 bg-white/5 active-press text-white/80"
            >
              <SlidersHorizontal className="w-4 h-4" />
            </button>
            <button
              onClick={() => {
                triggerHaptic('light');
                onOpenShortcuts();
              }}
              title="Controls Guide"
              className="p-1.5 rounded-lg border border-white/10 bg-white/5 active-press text-white/80"
            >
              <HelpCircle className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Hub Tabs Navigation */}
        <nav className="flex items-center gap-1 sm:gap-1.5 overflow-x-auto w-full md:w-auto py-0.5 no-scrollbar">
          {hubs.map((hub) => {
            const isActive = activeHub === hub.id;
            return (
              <button
                key={hub.id}
                onClick={() => {
                  triggerHaptic('selection');
                  setActiveHub(hub.id);
                }}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono transition-all whitespace-nowrap active-press ${
                  isActive
                    ? 'bg-white text-black font-bold shadow-lg shadow-white/10'
                    : 'bg-white/5 text-white/60 hover:text-white hover:bg-white/10 border border-white/5'
                }`}
              >
                <span className={`text-[10px] ${isActive ? 'text-[#D71921] font-bold' : 'text-white/40'}`}>
                  {hub.number}
                </span>
                {hub.icon}
                <span className="tracking-wider">{hub.label}</span>
                {isActive && (
                  <span className="w-1.5 h-1.5 rounded-full bg-[#D71921] ml-0.5" />
                )}
              </button>
            );
          })}
        </nav>

        {/* Desktop Controls */}
        <div className="hidden md:flex items-center gap-2">
          <button
            onClick={() => {
              triggerHaptic('light');
              onOpenImport();
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md border border-[#D71921]/40 bg-[#D71921]/10 text-white hover:bg-[#D71921]/20 text-xs font-mono transition-all active-press"
          >
            <PlusCircle className="w-3.5 h-3.5 text-[#D71921]" />
            <span>ADD MEDIA</span>
          </button>
          <button
            onClick={() => {
              triggerHaptic('light');
              onOpenEqualizer();
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-md border border-white/10 bg-white/5 hover:bg-white/10 text-white/80 text-xs font-mono transition-all active-press"
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span>10-BAND EQ</span>
          </button>
          <button
            onClick={() => {
              triggerHaptic('light');
              onOpenShortcuts();
            }}
            className="p-1.5 rounded-md border border-white/10 bg-white/5 hover:bg-white/10 text-white/70 hover:text-white transition-all active-press"
            title="Shortcuts & Gestures"
          >
            <HelpCircle className="w-4 h-4" />
          </button>
        </div>

      </div>
    </header>
  );
};
