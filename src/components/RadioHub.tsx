import React from 'react';
import type { RadioStation } from '../types/media';
import { RADIO_STATIONS } from '../services/demoData';
import { Radio, Play, Pause, Signal, Globe } from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface RadioHubProps {
  currentStationUrl?: string;
  isPlaying: boolean;
  onPlayStation: (station: RadioStation) => void;
}

export const RadioHub: React.FC<RadioHubProps> = ({
  currentStationUrl,
  isPlaying,
  onPlayStation
}) => {
  return (
    <div className="w-full max-w-7xl mx-auto p-3 sm:p-6 flex flex-col gap-6 animate-fade-in">
      
      {/* Radio Header */}
      <div className="flex items-center justify-between border-b border-white/10 pb-3">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <span className="font-dot text-sm sm:text-base tracking-widest text-white">
            WORLD RADIO // 04 BROADCAST
          </span>
        </div>

        <div className="flex items-center gap-1.5 font-mono text-[10px] text-white/50">
          <Signal className="w-3.5 h-3.5 text-[#D71921] animate-pulse" />
          <span>LIVE BROADCAST 24/7</span>
        </div>
      </div>

      {/* Hero Radio Banner */}
      <div className="relative p-6 sm:p-8 rounded-3xl bg-[#0c0c0c] border border-white/10 overflow-hidden shadow-2xl flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="absolute inset-0 bg-grid-pattern opacity-30 pointer-events-none" />

        <div className="relative z-10 flex items-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-white/5 border border-white/15 flex items-center justify-center text-white">
            <Radio className="w-8 h-8 text-[#D71921]" />
          </div>
          <div>
            <h2 className="font-dot text-xl sm:text-2xl text-white font-bold tracking-wider">
              INTERNET LIVE STREAM TUNER
            </h2>
            <p className="font-mono text-xs text-white/60 mt-1">
              Low-latency continuous audio broadcasts formatted for Nothing DSP Audio Engine.
            </p>
          </div>
        </div>

        <div className="relative z-10 flex items-center gap-2">
          <span className="px-3 py-1.5 rounded-xl bg-white/10 border border-white/10 font-mono text-xs text-white/80">
            AUDIO CODEC: MP3 / AAC
          </span>
        </div>
      </div>

      {/* Stations Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {RADIO_STATIONS.map((station: RadioStation) => {
          const isCurrent = currentStationUrl === station.streamUrl;
          return (
            <div
              key={station.id}
              onClick={() => {
                triggerHaptic('medium');
                onPlayStation(station);
              }}
              className={`p-4 rounded-3xl border transition-all flex flex-col justify-between cursor-pointer group active-press ${
                isCurrent && isPlaying
                  ? 'bg-white text-black border-white shadow-xl shadow-white/10 font-bold'
                  : 'bg-[#0d0d0d] border-white/10 text-white/80 hover:bg-white/5 hover:border-white/20'
              }`}
            >
              <div>
                {/* Station Cover Artwork */}
                <div className="relative w-full aspect-video rounded-2xl overflow-hidden bg-black/60 mb-3 border border-white/10">
                  <img
                    src={station.coverArt}
                    alt={station.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                  />
                  <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
                    <div className={`p-3 rounded-full ${isCurrent && isPlaying ? 'bg-black text-white' : 'bg-white text-black'} shadow-lg`}>
                      {isCurrent && isPlaying ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
                    </div>
                  </div>
                  <div className="absolute top-2 left-2 px-2 py-0.5 rounded bg-black/80 font-mono text-[9px] text-white">
                    {station.genre}
                  </div>
                </div>

                <h3 className="font-mono text-xs sm:text-sm font-bold line-clamp-1">
                  {station.name}
                </h3>
                
                <p className={`text-[10px] font-sans mt-1 line-clamp-2 ${isCurrent && isPlaying ? 'text-black/70' : 'text-white/40'}`}>
                  {station.description}
                </p>
              </div>

              {/* Bottom Specs */}
              <div className={`flex items-center justify-between border-t pt-3 mt-4 text-[9px] font-mono ${
                isCurrent && isPlaying ? 'border-black/10 text-black/60' : 'border-white/5 text-white/40'
              }`}>
                <span className="flex items-center gap-1">
                  <Globe className="w-3 h-3" />
                  {station.country}
                </span>
                <span>{station.bitrate}</span>
              </div>
            </div>
          );
        })}
      </div>

    </div>
  );
};
