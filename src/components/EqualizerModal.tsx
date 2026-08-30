import React from 'react';
import { X, RotateCcw, Volume2, Flame, Sliders } from 'lucide-react';
import { EQ_FREQUENCIES, EQ_PRESETS, audioEngine } from '../services/audioEngine';
import { triggerHaptic } from '../services/haptic';

interface EqualizerModalProps {
  isOpen: boolean;
  onClose: () => void;
  gains: number[];
  setGains: (gains: number[]) => void;
  preamp: number;
  setPreamp: (val: number) => void;
  bassBoost: number;
  setBassBoost: (val: number) => void;
  selectedPreset: string;
  setSelectedPreset: (presetName: string) => void;
  audioBoost: number;
  setAudioBoost: (val: number) => void;
}

export const EqualizerModal: React.FC<EqualizerModalProps> = ({
  isOpen,
  onClose,
  gains,
  setGains,
  preamp,
  setPreamp,
  bassBoost,
  setBassBoost,
  selectedPreset,
  setSelectedPreset,
  audioBoost,
  setAudioBoost
}) => {
  if (!isOpen) return null;

  const handleBandChange = (index: number, value: number) => {
    triggerHaptic('light');
    const newGains = [...gains];
    newGains[index] = value;
    setGains(newGains);
    audioEngine.setBandGain(index, value);
    setSelectedPreset('CUSTOM');
  };

  const handlePresetSelect = (preset: { name: string; gains: number[] }) => {
    triggerHaptic('medium');
    setSelectedPreset(preset.name);
    setGains(preset.gains);
    audioEngine.setAllBands(preset.gains);
  };

  const handlePreampChange = (value: number) => {
    triggerHaptic('light');
    setPreamp(value);
    audioEngine.setPreamp(value);
  };

  const handleBassBoostChange = (value: number) => {
    triggerHaptic('light');
    setBassBoost(value);
    audioEngine.setBassBoost(value);
  };

  const handleAudioBoostChange = (value: number) => {
    triggerHaptic('medium');
    setAudioBoost(value);
    audioEngine.setVolumeBoost(value / 100);
  };

  const handleReset = () => {
    triggerHaptic('heavy');
    const flatGains = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    setGains(flatGains);
    setPreamp(0);
    setBassBoost(0);
    setSelectedPreset('FLAT / STUDIO');
    audioEngine.setAllBands(flatGains);
    audioEngine.setPreamp(0);
    audioEngine.setBassBoost(0);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/80 backdrop-blur-xl animate-fade-in">
      <div className="relative w-full max-w-2xl bg-[#0d0d0d] border border-white/15 rounded-3xl p-5 sm:p-7 shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        
        {/* Background Grid Accent */}
        <div className="absolute inset-0 bg-grid-pattern opacity-30 pointer-events-none" />

        {/* Modal Header */}
        <div className="relative z-10 flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-3 h-3 rounded-full bg-[#D71921] glow-red" />
            <h2 className="font-dot text-base sm:text-lg tracking-widest text-white font-bold">
              10-BAND GRAPHIC EQUALIZER
            </h2>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleReset}
              className="flex items-center gap-1 px-2.5 py-1 rounded-md text-[11px] font-mono text-white/60 hover:text-white border border-white/10 bg-white/5 active-press"
              title="Reset to Flat"
            >
              <RotateCcw className="w-3 h-3" />
              <span>RESET</span>
            </button>
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
        </div>

        {/* Presets Horizontal Scroller */}
        <div className="relative z-10 my-4">
          <div className="text-[10px] font-mono text-white/40 mb-2 uppercase tracking-wider">
            // ACOUSTIC PRESETS //
          </div>
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
            {EQ_PRESETS.map((preset) => {
              const isSelected = selectedPreset === preset.name;
              return (
                <button
                  key={preset.name}
                  onClick={() => handlePresetSelect(preset)}
                  className={`px-3 py-1.5 rounded-md text-xs font-mono whitespace-nowrap transition-all active-press ${
                    isSelected
                      ? 'bg-white text-black font-bold shadow-md shadow-white/20'
                      : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
                  }`}
                >
                  {preset.name}
                </button>
              );
            })}
          </div>
        </div>

        {/* 10 Faders Area */}
        <div className="relative z-10 flex-1 my-2 bg-black/60 border border-white/10 rounded-2xl p-4 sm:p-5 flex flex-col justify-between overflow-x-auto">
          
          <div className="flex items-center justify-between min-w-[340px] h-48 sm:h-52 px-1">
            {EQ_FREQUENCIES.map((freq, index) => {
              const gainVal = gains[index] || 0;
              return (
                <div key={freq} className="flex flex-col items-center justify-between h-full group">
                  {/* Gain Label */}
                  <span className={`text-[10px] font-mono ${gainVal > 0 ? 'text-[#D71921] font-bold' : gainVal < 0 ? 'text-white/40' : 'text-white/70'}`}>
                    {gainVal > 0 ? `+${gainVal}` : gainVal}
                  </span>

                  {/* Vertical Range Slider */}
                  <div className="relative flex items-center justify-center h-32 sm:h-36">
                    <input
                      type="range"
                      min="-12"
                      max="12"
                      step="1"
                      value={gainVal}
                      onChange={(e) => handleBandChange(index, parseFloat(e.target.value))}
                      className="accent-[#D71921] w-32 sm:w-36 -rotate-90 cursor-pointer"
                    />
                  </div>

                  {/* Frequency Label */}
                  <span className="text-[9px] font-mono text-white/50 group-hover:text-white transition-colors">
                    {freq >= 1000 ? `${freq / 1000}k` : `${freq}`}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Fader Scale Legend */}
          <div className="flex items-center justify-between border-t border-white/10 pt-2 text-[9px] font-mono text-white/30 px-2 mt-2">
            <span>SUB BASS</span>
            <span>MID FREQUENCIES</span>
            <span>HIGH TREBLE</span>
          </div>

        </div>

        {/* Rotary & Master Boost Section */}
        <div className="relative z-10 grid grid-cols-1 sm:grid-cols-3 gap-3 mt-3">
          
          {/* Preamp Gain */}
          <div className="p-3 rounded-xl bg-white/5 border border-white/10 flex flex-col justify-between">
            <div className="flex items-center justify-between text-xs font-mono text-white/70">
              <span className="flex items-center gap-1.5">
                <Sliders className="w-3.5 h-3.5 text-white/60" />
                PREAMP
              </span>
              <span className="font-bold text-white">{preamp > 0 ? `+${preamp}` : preamp} dB</span>
            </div>
            <input
              type="range"
              min="-12"
              max="12"
              step="1"
              value={preamp}
              onChange={(e) => handlePreampChange(parseFloat(e.target.value))}
              className="accent-[#D71921] mt-2 cursor-pointer"
            />
          </div>

          {/* Bass Boost */}
          <div className="p-3 rounded-xl bg-white/5 border border-white/10 flex flex-col justify-between">
            <div className="flex items-center justify-between text-xs font-mono text-white/70">
              <span className="flex items-center gap-1.5">
                <Flame className="w-3.5 h-3.5 text-[#D71921]" />
                BASS PUNCH
              </span>
              <span className="font-bold text-[#D71921]">+{bassBoost} dB</span>
            </div>
            <input
              type="range"
              min="0"
              max="12"
              step="1"
              value={bassBoost}
              onChange={(e) => handleBassBoostChange(parseFloat(e.target.value))}
              className="accent-[#D71921] mt-2 cursor-pointer"
            />
          </div>

          {/* 200% Audio Boost */}
          <div className="p-3 rounded-xl bg-white/5 border border-white/10 flex flex-col justify-between">
            <div className="flex items-center justify-between text-xs font-mono text-white/70">
              <span className="flex items-center gap-1.5">
                <Volume2 className={`w-3.5 h-3.5 ${audioBoost > 100 ? 'text-[#D71921]' : 'text-white/60'}`} />
                VLC 200% BOOST
              </span>
              <span className={`font-bold ${audioBoost > 100 ? 'text-[#D71921]' : 'text-white'}`}>{audioBoost}%</span>
            </div>
            <input
              type="range"
              min="100"
              max="200"
              step="5"
              value={audioBoost}
              onChange={(e) => handleAudioBoostChange(parseFloat(e.target.value))}
              className="accent-[#D71921] mt-2 cursor-pointer"
            />
          </div>

        </div>

      </div>
    </div>
  );
};
