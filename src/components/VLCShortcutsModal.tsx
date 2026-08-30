import React from 'react';
import { X, Smartphone, Keyboard, Sparkles } from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface VLCShortcutsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const VLCShortcutsModal: React.FC<VLCShortcutsModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  const gestures = [
    { action: 'Left Screen Swipe (Vertical)', result: 'Adjust Brightness smoothly (0% - 100%)' },
    { action: 'Right Screen Swipe (Vertical)', result: 'Adjust Volume up to 200% Audio Boost' },
    { action: 'Horizontal Swipe (Anywhere)', result: 'High-precision timeline seek' },
    { action: 'Double Tap Left Screen', result: 'Jump back 10 seconds (-10s)' },
    { action: 'Double Tap Right Screen', result: 'Jump forward 10 seconds (+10s)' },
    { action: 'Pinch on Video Cinema', result: 'Toggle between Fit / Stretch / 16:9 / 21:9' },
  ];

  const shortcuts = [
    { key: 'Space', desc: 'Play / Pause toggle' },
    { key: 'Left / Right', desc: 'Seek 5 seconds back / forward' },
    { key: 'Up / Down', desc: 'Increase / Decrease Volume (up to 200%)' },
    { key: 'F', desc: 'Toggle Fullscreen' },
    { key: 'M', desc: 'Mute / Unmute' },
    { key: 'P', desc: 'Picture-in-Picture mode (PiP)' },
    { key: 'S', desc: 'Take Screenshot of video frame' },
    { key: 'V', desc: 'Toggle / Cycle Subtitles' },
    { key: 'E', desc: 'Frame-by-frame step forward' },
    { key: 'N / B', desc: 'Next / Previous track in queue' },
    { key: '[ / ]', desc: 'Decrease / Increase Playback Speed' },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/80 backdrop-blur-xl animate-fade-in">
      <div className="relative w-full max-w-2xl bg-[#0e0e0e] border border-white/15 rounded-3xl p-5 sm:p-7 shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        
        {/* Background Grid Pattern */}
        <div className="absolute inset-0 bg-grid-pattern opacity-30 pointer-events-none" />

        {/* Modal Header */}
        <div className="relative z-10 flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2.5">
            <Sparkles className="w-4 h-4 text-[#D71921]" />
            <h2 className="font-dot text-base sm:text-lg tracking-widest text-white font-bold">
              VLC CONTROLS & MOBILE GESTURES
            </h2>
          </div>

          <button
            onClick={() => {
              triggerHaptic('light');
              onClose();
            }}
            className="p-1.5 rounded-lg border border-white/10 bg-white/5 hover:bg-white/15 text-white active-press"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Tabs */}
        <div className="relative z-10 overflow-y-auto space-y-6 my-4 pr-1">
          
          {/* Mobile Gestures */}
          <div>
            <div className="flex items-center gap-2 text-xs font-mono text-[#D71921] mb-3">
              <Smartphone className="w-4 h-4" />
              <span className="tracking-wider uppercase">// ANDROID & MOBILE TOUCH GESTURES //</span>
            </div>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {gestures.map((item, idx) => (
                <div key={idx} className="p-3 rounded-xl bg-white/5 border border-white/5 flex flex-col justify-between">
                  <span className="font-mono text-xs font-bold text-white mb-1">{item.action}</span>
                  <span className="text-[11px] text-white/60">{item.result}</span>
                </div>
              ))}
            </div>
          </div>

          {/* VLC Keyboard Shortcuts */}
          <div>
            <div className="flex items-center gap-2 text-xs font-mono text-white/70 mb-3">
              <Keyboard className="w-4 h-4 text-[#D71921]" />
              <span className="tracking-wider uppercase">// VLC KEYBOARD SHORTCUTS //</span>
            </div>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {shortcuts.map((item, idx) => (
                <div key={idx} className="p-2.5 rounded-xl bg-black/50 border border-white/10 flex items-center justify-between">
                  <kbd className="px-2 py-1 rounded bg-white/15 font-mono text-xs text-white border border-white/20 shadow-inner">
                    {item.key}
                  </kbd>
                  <span className="text-[11px] font-mono text-white/70 text-right">{item.desc}</span>
                </div>
              ))}
            </div>
          </div>

        </div>

      </div>
    </div>
  );
};
