import React, { useEffect, useRef, useState } from 'react';
import { audioEngine } from '../services/audioEngine';
import { Sparkles, Zap, Flame, Volume2 } from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface GlyphMatrixProps {
  isPlaying: boolean;
  isExpanded?: boolean;
}

export const GlyphMatrix: React.FC<GlyphMatrixProps> = ({ isPlaying, isExpanded = false }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [glyphPattern, setGlyphPattern] = useState<'reactive' | 'strobe' | 'torch' | 'matrix'>('reactive');
  const [intensity, setIntensity] = useState<number>(0);
  const [bassLevel, setBassLevel] = useState<number>(0);

  useEffect(() => {
    let animationFrameId: number;
    const freqData = new Uint8Array(128);

    const render = () => {
      if (isPlaying) {
        audioEngine.getFrequencyData(freqData);

        // Calculate Bass Energy (first 8 bins)
        let bassSum = 0;
        for (let i = 0; i < 8; i++) {
          bassSum += freqData[i];
        }
        const currentBass = bassSum / (8 * 255);
        setBassLevel(currentBass);

        // Calculate Overall Energy
        let totalSum = 0;
        for (let i = 0; i < freqData.length; i++) {
          totalSum += freqData[i];
        }
        const currentIntensity = totalSum / (freqData.length * 255);
        setIntensity(currentIntensity);

        // Draw Canvas Dot Matrix Spectrum
        const canvas = canvasRef.current;
        if (canvas) {
          const ctx = canvas.getContext('2d');
          if (ctx) {
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            const numBars = 24;
            const barWidth = canvas.width / numBars;
            const step = Math.floor(freqData.length / numBars);

            for (let i = 0; i < numBars; i++) {
              const val = freqData[i * step] / 255;
              const barHeight = val * canvas.height;
              const numDots = Math.floor(barHeight / 6);

              for (let d = 0; d < numDots; d++) {
                const dotY = canvas.height - (d * 7) - 4;
                const dotX = i * barWidth + barWidth / 2;

                ctx.beginPath();
                ctx.arc(dotX, dotY, 2, 0, Math.PI * 2);

                if (d > 14) {
                  ctx.fillStyle = '#D71921'; // Nothing Red peak dots
                  ctx.shadowColor = '#D71921';
                  ctx.shadowBlur = 6;
                } else if (d > 8) {
                  ctx.fillStyle = '#ffffff';
                  ctx.shadowColor = '#ffffff';
                  ctx.shadowBlur = 4;
                } else {
                  ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
                  ctx.shadowBlur = 0;
                }
                ctx.fill();
              }
            }
          }
        }
      } else {
        setBassLevel(0);
        setIntensity(0);
        const canvas = canvasRef.current;
        if (canvas) {
          const ctx = canvas.getContext('2d');
          if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
      }

      animationFrameId = requestAnimationFrame(render);
    };

    render();
    return () => cancelAnimationFrame(animationFrameId);
  }, [isPlaying]);

  // Dynamic Opacity & Glow values calculated from audio FFT
  const glyphAlpha = isPlaying ? Math.max(0.15, Math.min(1.0, bassLevel * 1.6)) : 0.1;
  const redDotAlpha = isPlaying ? Math.max(0.4, Math.min(1.0, intensity * 2.0)) : 0.2;

  return (
    <div className={`relative flex flex-col items-center justify-center p-4 sm:p-6 rounded-2xl bg-[#0c0c0c] border border-white/10 shadow-2xl overflow-hidden ${isExpanded ? 'w-full max-w-3xl min-h-[500px]' : 'w-full'}`}>
      
      {/* Background Matrix Grid */}
      <div className="absolute inset-0 bg-grid-pattern opacity-40 pointer-events-none" />

      {/* Header Info */}
      <div className="relative z-10 w-full flex items-center justify-between border-b border-white/10 pb-3 mb-4">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-[#D71921]" />
          <span className="font-dot text-xs sm:text-sm tracking-widest text-white">
            GLYPH INTERFACE (02)
          </span>
        </div>
        <div className="flex items-center gap-2 font-mono text-[10px] text-white/50">
          <span>BASS: {Math.round(bassLevel * 100)}%</span>
          <span>//</span>
          <span>PEAK: {Math.round(intensity * 100)}%</span>
        </div>
      </div>

      {/* Center Nothing Glyph Hardware Layout */}
      <div className="relative z-10 w-full max-w-sm aspect-[4/5] flex items-center justify-center my-2">
        
        {/* Outer Phone Blueprint Frame */}
        <div className="relative w-full h-full rounded-[40px] border border-white/15 p-5 flex flex-col items-center justify-between bg-black/60 backdrop-blur-md shadow-inner">
          
          {/* Top Camera Halo & Diagonal Glyph Strip */}
          <div className="w-full flex items-start justify-between">
            {/* Camera Dual Ring */}
            <div 
              className="relative w-20 h-20 rounded-full border-4 border-white transition-all duration-75 flex items-center justify-center"
              style={{
                borderColor: `rgba(255, 255, 255, ${glyphAlpha})`,
                boxShadow: isPlaying ? `0 0 ${bassLevel * 25}px rgba(255, 255, 255, ${glyphAlpha * 0.8})` : 'none'
              }}
            >
              <div 
                className="w-12 h-12 rounded-full border-2 border-white/40 flex items-center justify-center"
                style={{ borderColor: `rgba(255, 255, 255, ${glyphAlpha * 0.7})` }}
              >
                <div className="w-4 h-4 rounded-full bg-white/20" />
              </div>
            </div>

            {/* Upper Right Diagonal Slash LED */}
            <div 
              className="w-16 h-2 rounded-full bg-white transition-all duration-75 rotate-[-25deg] mt-3"
              style={{
                backgroundColor: `rgba(255, 255, 255, ${glyphAlpha})`,
                boxShadow: isPlaying ? `0 0 ${intensity * 20}px rgba(255, 255, 255, ${glyphAlpha})` : 'none'
              }}
            />
          </div>

          {/* Central Segmented G-Glyph Ring */}
          <div className="relative flex items-center justify-center my-auto">
            <div 
              className="w-36 h-36 rounded-full border-4 border-dashed border-white transition-all duration-75 flex items-center justify-center"
              style={{
                borderColor: `rgba(255, 255, 255, ${glyphAlpha * 1.1})`,
                boxShadow: isPlaying ? `0 0 ${bassLevel * 35}px rgba(255, 255, 255, ${glyphAlpha})` : 'none',
                transform: `scale(${1 + bassLevel * 0.08})`
              }}
            >
              {/* Internal C-Arc LED */}
              <div 
                className="w-24 h-24 rounded-full border-4 border-t-white border-r-white border-b-transparent border-l-white transition-all duration-75 flex items-center justify-center"
                style={{
                  borderColor: `rgba(255, 255, 255, ${glyphAlpha * 1.2}) transparent rgba(255, 255, 255, ${glyphAlpha * 1.2}) rgba(255, 255, 255, ${glyphAlpha * 1.2})`
                }}
              >
                <Volume2 className={`w-6 h-6 transition-all duration-75 ${isPlaying ? 'text-white' : 'text-white/20'}`} />
              </div>
            </div>
          </div>

          {/* Bottom Section: Vertical Battery Bar & Signature Red Recording Dot */}
          <div className="w-full flex flex-col items-center gap-3">
            {/* Bottom Vertical Glyph Bar */}
            <div 
              className="w-32 h-2.5 rounded-full bg-white transition-all duration-75"
              style={{
                backgroundColor: `rgba(255, 255, 255, ${glyphAlpha * 0.9})`,
                boxShadow: isPlaying ? `0 0 15px rgba(255, 255, 255, ${glyphAlpha})` : 'none'
              }}
            />

            {/* Bottom Right Signature Red LED */}
            <div className="w-full flex items-center justify-between px-3">
              <span className="font-mono text-[9px] text-white/30 tracking-widest">
                NOTHING (R)
              </span>
              <div 
                className="w-3 h-3 rounded-full bg-[#D71921] transition-all duration-75"
                style={{
                  opacity: redDotAlpha,
                  boxShadow: `0 0 ${intensity * 20 + 5}px rgba(215, 25, 33, 0.9)`
                }}
              />
            </div>
          </div>

        </div>

      </div>

      {/* Realtime Dot-Matrix Spectrum Canvas */}
      <div className="relative z-10 w-full mt-3 bg-black/50 border border-white/10 rounded-xl p-2 flex flex-col items-center">
        <canvas 
          ref={canvasRef} 
          width={280} 
          height={60} 
          className="w-full h-14"
        />
        <div className="w-full flex items-center justify-between text-[9px] font-mono text-white/40 px-2 mt-1">
          <span>32Hz [SUB]</span>
          <span>1kHz [MID]</span>
          <span>16kHz [AIR]</span>
        </div>
      </div>

      {/* Glyph Interactive Controls */}
      <div className="relative z-10 flex items-center gap-2 mt-4">
        <button
          onClick={() => {
            triggerHaptic('medium');
            setGlyphPattern('reactive');
          }}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono border active-press ${
            glyphPattern === 'reactive'
              ? 'bg-white text-black border-white font-bold'
              : 'bg-white/5 text-white/70 border-white/10'
          }`}
        >
          <Zap className="w-3.5 h-3.5 text-[#D71921]" />
          <span>REACTIVE PULSE</span>
        </button>

        <button
          onClick={() => {
            triggerHaptic('medium');
            setGlyphPattern('strobe');
          }}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono border active-press ${
            glyphPattern === 'strobe'
              ? 'bg-white text-black border-white font-bold'
              : 'bg-white/5 text-white/70 border-white/10'
          }`}
        >
          <Flame className="w-3.5 h-3.5 text-[#D71921]" />
          <span>TORCH MODE</span>
        </button>
      </div>

    </div>
  );
};
