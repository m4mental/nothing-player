import React, { useState, useRef, useEffect } from 'react';
import type { MediaItem, VideoFilterState } from '../types/media';
import { parseSubtitles, getActiveSubtitle } from '../services/subtitleParser';
import { GestureOverlay } from './GestureOverlay';
import { triggerHaptic } from '../services/haptic';
import { 
  Play, 
  Pause, 
  ArrowLeft, 
  Lock, 
  Unlock, 
  Maximize, 
  PictureInPicture, 
  Subtitles, 
  Tv, 
  Volume2, 
  VolumeX, 
  FastForward, 
  Rewind, 
  Camera, 
  Cpu
} from 'lucide-react';

interface NativeVideoPlayerProps {
  video: MediaItem;
  videoList?: MediaItem[];
  onClose: () => void;
  onNextVideo: () => void;
  onPrevVideo?: () => void;
  onSaveProgress: (id: string, position: number) => void;
}

export const NativeVideoPlayer: React.FC<NativeVideoPlayerProps> = ({
  video,
  onClose,
  onNextVideo,
  onSaveProgress
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const playerContainerRef = useRef<HTMLDivElement | null>(null);

  const [isPlaying, setIsPlaying] = useState(true);
  const [currentTime, setCurrentTime] = useState(video.lastPosition || 0);
  const [duration, setDuration] = useState(video.duration || 0);
  const [audioBoostVolume, setAudioBoostVolume] = useState(100); // up to 200%
  const [isMuted, setIsMuted] = useState(false);
  const [isLocked, setIsLocked] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [aspectRatio, setAspectRatio] = useState<'fit' | 'stretch' | '16:9' | '21:9' | 'crop'>('fit');
  const [decoder, setDecoder] = useState<'HW' | 'SW'>('HW');
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1.0);
  const [showShaders, setShowShaders] = useState(false);
  const [showSubtitleSheet, setShowSubtitleSheet] = useState(false);

  // Shaders & Filters
  const [filters, setFilters] = useState<VideoFilterState>({
    brightness: 100,
    contrast: 100,
    saturation: 100,
    crtScanlines: false,
    duotoneRed: false,
    hueRotate: 0,
    invert: false
  });

  // Subtitles
  const [subtitleCues, setSubtitleCues] = useState<ReturnType<typeof parseSubtitles>>([]);
  const [subtitlesOffset, setSubtitlesOffset] = useState(0);
  const [subtitlesEnabled, setSubtitlesEnabled] = useState(true);

  // Gesture Feedback
  const [gesture, setGesture] = useState<{
    type: 'brightness' | 'volume' | 'seek' | 'doubletap_left' | 'doubletap_right' | null;
    value: number;
    formattedText?: string;
    visible: boolean;
  }>({
    type: null,
    value: 0,
    visible: false,
  });

  const controlsTimeoutRef = useRef<number | null>(null);
  const gestureTimeoutRef = useRef<number | null>(null);
  const touchStartRef = useRef<{ x: number; y: number; time: number } | null>(null);
  const lastTapRef = useRef<{ time: number; x: number } | null>(null);

  // Load Subtitles
  useEffect(() => {
    if (video.subtitlesContent) {
      setSubtitleCues(parseSubtitles(video.subtitlesContent, subtitlesOffset));
    } else {
      setSubtitleCues([]);
    }
  }, [video, subtitlesOffset]);

  // Resume last position on mount
  useEffect(() => {
    if (videoRef.current) {
      if (video.lastPosition && video.lastPosition > 0) {
        videoRef.current.currentTime = video.lastPosition;
      }
      videoRef.current.play().catch(e => console.warn('Auto play:', e));
      setIsPlaying(true);
    }
  }, [video]);

  // Auto-hide controls after 3.5s
  const resetControlsTimer = () => {
    if (isLocked) return;
    setShowControls(true);
    if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    controlsTimeoutRef.current = window.setTimeout(() => {
      setShowControls(false);
      setShowShaders(false);
      setShowSubtitleSheet(false);
    }, 3500);
  };

  useEffect(() => {
    resetControlsTimer();
    return () => {
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    };
  }, [isPlaying, isLocked]);

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      const pos = videoRef.current.currentTime;
      setCurrentTime(pos);
      // Periodically save resume position
      if (Math.floor(pos) % 5 === 0) {
        onSaveProgress(video.id, pos);
      }
    }
  };

  const handleLoadedMetadata = () => {
    if (videoRef.current) {
      setDuration(videoRef.current.duration);
    }
  };

  const togglePlayPause = () => {
    if (!videoRef.current) return;
    triggerHaptic('medium');
    if (videoRef.current.paused) {
      videoRef.current.play();
      setIsPlaying(true);
    } else {
      videoRef.current.pause();
      setIsPlaying(false);
    }
    resetControlsTimer();
  };

  const handleSeek = (time: number) => {
    if (videoRef.current) {
      videoRef.current.currentTime = time;
      setCurrentTime(time);
      onSaveProgress(video.id, time);
    }
  };

  // Capture Screenshot Frame
  const handleTakeScreenshot = () => {
    if (!videoRef.current) return;
    triggerHaptic('heavy');
    const canvas = document.createElement('canvas');
    canvas.width = videoRef.current.videoWidth || 1920;
    canvas.height = videoRef.current.videoHeight || 1080;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
      const a = document.createElement('a');
      a.href = canvas.toDataURL('image/png');
      a.download = `SCREEN_${video.title}_${Math.floor(currentTime)}s.png`;
      a.click();
    }
  };

  // Picture in Picture
  const handleTogglePiP = async () => {
    if (!videoRef.current) return;
    triggerHaptic('light');
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else {
        await videoRef.current.requestPictureInPicture();
      }
    } catch (e) {
      console.warn('PiP failed', e);
    }
  };

  // Orientation Rotate / Fullscreen
  const handleToggleFullscreen = () => {
    if (!playerContainerRef.current) return;
    triggerHaptic('light');
    if (!document.fullscreenElement) {
      playerContainerRef.current.requestFullscreen?.();
    } else {
      document.exitFullscreen?.();
    }
  };

  // Touch Gesture Handling
  const handleTouchStart = (e: React.TouchEvent) => {
    if (isLocked) return;
    const touch = e.touches[0];
    const now = Date.now();

    // Double tap detector
    if (lastTapRef.current && (now - lastTapRef.current.time < 300)) {
      const rect = playerContainerRef.current?.getBoundingClientRect();
      if (rect) {
        const isLeftHalf = touch.clientX - rect.left < rect.width / 2;
        if (isLeftHalf) {
          triggerHaptic('medium');
          handleSeek(Math.max(0, currentTime - 10));
          showGestureHUD('doubletap_left', 0);
        } else {
          triggerHaptic('medium');
          handleSeek(Math.min(duration, currentTime + 10));
          showGestureHUD('doubletap_right', 0);
        }
        lastTapRef.current = null;
        return;
      }
    }

    lastTapRef.current = { time: now, x: touch.clientX };
    touchStartRef.current = { x: touch.clientX, y: touch.clientY, time: now };
    resetControlsTimer();
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (isLocked || !touchStartRef.current || !playerContainerRef.current) return;
    const touch = e.touches[0];
    const deltaX = touch.clientX - touchStartRef.current.x;
    const deltaY = touch.clientY - touchStartRef.current.y;
    const rect = playerContainerRef.current.getBoundingClientRect();
    const isLeftHalf = touchStartRef.current.x - rect.left < rect.width / 2;

    if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > 15) {
      if (isLeftHalf) {
        // Brightness Swipe (Left side)
        const deltaPercent = -deltaY / 2;
        const newBrightness = Math.max(30, Math.min(150, filters.brightness + deltaPercent * 0.12));
        setFilters(prev => ({ ...prev, brightness: newBrightness }));
        showGestureHUD('brightness', newBrightness);
      } else {
        // Volume Swipe (Right side - up to 200% boost)
        const deltaPercent = -deltaY / 2;
        const newVolBoost = Math.max(0, Math.min(200, audioBoostVolume + deltaPercent * 0.25));
        setAudioBoostVolume(newVolBoost);
        if (videoRef.current) {
          videoRef.current.volume = Math.min(1.0, newVolBoost / 100);
        }
        showGestureHUD('volume', newVolBoost);
      }
    } else if (Math.abs(deltaX) > 20) {
      // Timeline Seek Swipe
      const seekOffset = (deltaX / rect.width) * 90;
      const targetTime = Math.max(0, Math.min(duration, currentTime + seekOffset));
      const formatted = `${formatTime(targetTime)} (${seekOffset > 0 ? `+${Math.round(seekOffset)}s` : `${Math.round(seekOffset)}s`})`;
      showGestureHUD('seek', targetTime, formatted);
    }
  };

  const handleTouchEnd = () => {
    touchStartRef.current = null;
  };

  const showGestureHUD = (
    type: 'brightness' | 'volume' | 'seek' | 'doubletap_left' | 'doubletap_right',
    val: number,
    text?: string
  ) => {
    setGesture({ type, value: val, formattedText: text, visible: true });
    if (gestureTimeoutRef.current) clearTimeout(gestureTimeoutRef.current);
    gestureTimeoutRef.current = window.setTimeout(() => {
      setGesture(prev => ({ ...prev, visible: false }));
    }, 1000);
  };

  const formatTime = (secs: number) => {
    if (isNaN(secs)) return '00:00';
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = Math.floor(secs % 60);
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // Aspect Ratio styles
  const getAspectRatioClasses = () => {
    switch (aspectRatio) {
      case 'stretch': return 'w-full h-full object-fill';
      case 'crop': return 'w-full h-full object-cover';
      case '16:9': return 'aspect-video object-contain';
      case '21:9': return 'aspect-[21/9] object-cover';
      case 'fit':
      default: return 'w-full h-full object-contain';
    }
  };

  const activeSubtitle = subtitlesEnabled ? getActiveSubtitle(subtitleCues, currentTime) : null;

  return (
    <div 
      ref={playerContainerRef}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onClick={resetControlsTimer}
      className={`fixed inset-0 z-50 bg-black flex items-center justify-center select-none overflow-hidden ${
        filters.crtScanlines ? 'crt-scanlines' : ''
      }`}
    >
      {/* Video Canvas Element */}
      <video
        ref={videoRef}
        src={video.url}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onEnded={onNextVideo}
        style={{
          filter: `brightness(${filters.brightness}%) contrast(${filters.contrast}%) saturate(${filters.saturation}%)`
        }}
        className={`${getAspectRatioClasses()} ${filters.duotoneRed ? 'filter-duotone-red' : ''}`}
        playsInline
      />

      {/* Gesture HUD */}
      <GestureOverlay
        type={gesture.type}
        value={gesture.value}
        formattedText={gesture.formattedText}
        visible={gesture.visible}
      />

      {/* Subtitles Overlay */}
      {activeSubtitle && (
        <div className="absolute bottom-20 left-4 right-4 z-25 flex justify-center pointer-events-none">
          <p className="bg-black/85 backdrop-blur-md text-white font-sans text-sm sm:text-base px-4 py-2 rounded-xl text-center shadow-2xl border border-white/10 font-medium">
            {activeSubtitle}
          </p>
        </div>
      )}

      {/* Screen Padlock Toggle Button (Floating on Left) */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          triggerHaptic('heavy');
          setIsLocked(!isLocked);
          setShowControls(true);
        }}
        className={`absolute left-4 top-1/2 -translate-y-1/2 z-35 p-3 rounded-full backdrop-blur-md border transition-all active-press ${
          isLocked
            ? 'bg-[#D71921] text-white border-[#D71921] glow-red'
            : showControls
              ? 'bg-black/60 text-white/80 border-white/20 hover:bg-black/80'
              : 'opacity-0 pointer-events-none'
        }`}
        title={isLocked ? 'Unlock Screen Controls' : 'Lock Screen Controls'}
      >
        {isLocked ? <Lock className="w-5 h-5" /> : <Unlock className="w-5 h-5" />}
      </button>

      {/* Top Controls App Bar */}
      {!isLocked && (
        <div className={`absolute top-0 left-0 right-0 pt-safe pt-4 px-4 pb-4 bg-gradient-to-b from-black/90 via-black/40 to-transparent z-30 flex items-center justify-between transition-opacity duration-300 ${
          showControls ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}>
          {/* Back & Title */}
          <div className="flex items-center gap-3 flex-1 min-w-0">
            <button
              onClick={(e) => {
                e.stopPropagation();
                triggerHaptic('light');
                onClose();
              }}
              className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white active-press"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>

            <div className="flex flex-col min-w-0">
              <span className="font-mono text-sm font-bold text-white line-clamp-1">
                {video.title}
              </span>
              <span className="text-[10px] font-mono text-white/40">
                {video.format} // {video.resolution || 'HD'}
              </span>
            </div>
          </div>

          {/* Top Right Quick Toggles */}
          <div className="flex items-center gap-1.5">
            {/* HW / SW Decoder Switcher */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                triggerHaptic('light');
                setDecoder(decoder === 'HW' ? 'SW' : 'HW');
              }}
              className="px-2.5 py-1 rounded-lg bg-black/60 border border-white/20 text-[10px] font-mono text-white font-bold active-press flex items-center gap-1"
            >
              <Cpu className="w-3 h-3 text-[#D71921]" />
              <span>{decoder}</span>
            </button>

            {/* Aspect Ratio Switcher */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                triggerHaptic('light');
                const ratios: ('fit' | 'stretch' | '16:9' | '21:9' | 'crop')[] = ['fit', 'stretch', '16:9', '21:9', 'crop'];
                const nextIdx = (ratios.indexOf(aspectRatio) + 1) % ratios.length;
                setAspectRatio(ratios[nextIdx]);
              }}
              className="px-2 py-1 rounded-lg bg-black/60 border border-white/20 text-[10px] font-mono text-white active-press"
            >
              {aspectRatio.toUpperCase()}
            </button>

            {/* Shaders Button */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                triggerHaptic('light');
                setShowShaders(!showShaders);
              }}
              className="p-2 rounded-lg bg-black/60 border border-white/20 text-white active-press"
              title="CRT & Video Shaders"
            >
              <Tv className="w-4 h-4" />
            </button>

            {/* Subtitles Button */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                triggerHaptic('light');
                setShowSubtitleSheet(!showSubtitleSheet);
              }}
              className="p-2 rounded-lg bg-black/60 border border-white/20 text-white active-press"
              title="Subtitles"
            >
              <Subtitles className="w-4 h-4" />
            </button>

            {/* Screenshot */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleTakeScreenshot();
              }}
              className="p-2 rounded-lg bg-black/60 border border-white/20 text-white active-press"
              title="Capture Frame"
            >
              <Camera className="w-4 h-4" />
            </button>

            {/* PiP */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleTogglePiP();
              }}
              className="p-2 rounded-lg bg-black/60 border border-white/20 text-white active-press"
              title="Floating PiP"
            >
              <PictureInPicture className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Bottom Timeline & Controls Bar */}
      {!isLocked && (
        <div className={`absolute bottom-0 left-0 right-0 pb-safe pb-5 px-4 pt-4 bg-gradient-to-t from-black/95 via-black/60 to-transparent z-30 flex flex-col gap-3 transition-opacity duration-300 ${
          showControls ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}>
          
          {/* Progress Seek Scrubber */}
          <div className="w-full flex items-center gap-3">
            <span className="font-mono text-xs text-white/80 min-w-[45px]">{formatTime(currentTime)}</span>
            
            <div 
              className="relative flex-1 h-3 bg-white/20 rounded-full cursor-pointer overflow-hidden group"
              onClick={(e) => {
                e.stopPropagation();
                const rect = e.currentTarget.getBoundingClientRect();
                const clickX = e.clientX - rect.left;
                const ratio = Math.max(0, Math.min(1, clickX / rect.width));
                triggerHaptic('light');
                handleSeek(ratio * duration);
              }}
            >
              <div 
                className="h-full bg-[#D71921] glow-red transition-all duration-75"
                style={{ width: `${duration > 0 ? (currentTime / duration) * 100 : 0}%` }}
              />
            </div>

            <span className="font-mono text-xs text-white/50 min-w-[45px] text-right">{formatTime(duration)}</span>
          </div>

          {/* Action Row */}
          <div className="flex items-center justify-between">
            {/* Speed & Lock */}
            <div className="flex items-center gap-2">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  triggerHaptic('light');
                  const speeds = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0];
                  const nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.length;
                  const newSpeed = speeds[nextIdx];
                  setPlaybackSpeed(newSpeed);
                  if (videoRef.current) videoRef.current.playbackRate = newSpeed;
                }}
                className="px-2.5 py-1 rounded-xl bg-white/10 text-white font-mono text-xs active-press"
              >
                {playbackSpeed}x
              </button>

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  triggerHaptic('light');
                  setIsMuted(!isMuted);
                  if (videoRef.current) videoRef.current.muted = !isMuted;
                }}
                className="p-2 text-white/70 hover:text-white"
              >
                {isMuted ? <VolumeX className="w-4 h-4 text-[#D71921]" /> : <Volume2 className="w-4 h-4" />}
              </button>
            </div>

            {/* Center Transport Buttons */}
            <div className="flex items-center gap-4">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  triggerHaptic('light');
                  handleSeek(Math.max(0, currentTime - 10));
                }}
                className="p-2 text-white/70 hover:text-white active-press"
                title="Rewind 10s"
              >
                <Rewind className="w-6 h-6" />
              </button>

              {/* Big Play / Pause */}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  togglePlayPause();
                }}
                className="p-4 rounded-full bg-white text-black hover:scale-105 active-press transition-transform shadow-2xl border-2 border-[#D71921]"
              >
                {isPlaying ? <Pause className="w-6 h-6 fill-black" /> : <Play className="w-6 h-6 fill-black ml-0.5" />}
              </button>

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  triggerHaptic('light');
                  handleSeek(Math.min(duration, currentTime + 10));
                }}
                className="p-2 text-white/70 hover:text-white active-press"
                title="Forward 10s"
              >
                <FastForward className="w-6 h-6" />
              </button>
            </div>

            {/* Next Video & Fullscreen */}
            <div className="flex items-center gap-2">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  triggerHaptic('medium');
                  onNextVideo();
                }}
                className="px-3 py-1 rounded-xl bg-white/10 text-white font-mono text-xs active-press"
              >
                NEXT
              </button>

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  handleToggleFullscreen();
                }}
                className="p-2 text-white/80 hover:text-white active-press"
              >
                <Maximize className="w-5 h-5" />
              </button>
            </div>
          </div>

        </div>
      )}

      {/* Shaders Drawer Modal */}
      {showShaders && (
        <div 
          onClick={(e) => e.stopPropagation()} 
          className="absolute bottom-24 right-4 z-40 w-72 p-4 rounded-3xl bg-[#111111]/95 backdrop-blur-xl border border-white/15 shadow-2xl flex flex-col gap-3 animate-fade-in"
        >
          <div className="flex justify-between items-center border-b border-white/10 pb-2">
            <span className="font-dot text-xs text-white font-bold">VIDEO SHADERS</span>
            <button onClick={() => setShowShaders(false)} className="text-white/40 hover:text-white text-xs">✕</button>
          </div>

          <button
            onClick={() => setFilters(prev => ({ ...prev, crtScanlines: !prev.crtScanlines }))}
            className={`p-2.5 rounded-xl border text-xs font-mono text-left transition-all ${
              filters.crtScanlines ? 'bg-white text-black font-bold border-white' : 'bg-white/5 border-white/10 text-white/70'
            }`}
          >
            RETRO CRT SCANLINES {filters.crtScanlines && '●'}
          </button>

          <button
            onClick={() => setFilters(prev => ({ ...prev, duotoneRed: !prev.duotoneRed }))}
            className={`p-2.5 rounded-xl border text-xs font-mono text-left transition-all ${
              filters.duotoneRed ? 'bg-[#D71921] text-white font-bold border-[#D71921] glow-red' : 'bg-white/5 border-white/10 text-white/70'
            }`}
          >
            NOTHING RED DUOTONE {filters.duotoneRed && '●'}
          </button>
        </div>
      )}

      {/* Subtitles Drawer Modal */}
      {showSubtitleSheet && (
        <div 
          onClick={(e) => e.stopPropagation()}
          className="absolute bottom-24 right-4 z-40 w-80 p-4 rounded-3xl bg-[#111111]/95 backdrop-blur-xl border border-white/15 shadow-2xl flex flex-col gap-3 animate-fade-in"
        >
          <div className="flex justify-between items-center border-b border-white/10 pb-2">
            <span className="font-dot text-xs text-white font-bold">SUBTITLE SYNC</span>
            <button onClick={() => setShowSubtitleSheet(false)} className="text-white/40 hover:text-white text-xs">✕</button>
          </div>

          <button
            onClick={() => setSubtitlesEnabled(!subtitlesEnabled)}
            className={`p-2 rounded-xl text-xs font-mono font-bold ${
              subtitlesEnabled ? 'bg-white text-black' : 'bg-white/10 text-white/60'
            }`}
          >
            {subtitlesEnabled ? 'SUBTITLES: ON' : 'SUBTITLES: OFF'}
          </button>

          <div className="flex items-center justify-between text-xs font-mono text-white/70">
            <span>SYNC OFFSET:</span>
            <span className="text-[#D71921] font-bold">{subtitlesOffset > 0 ? `+${subtitlesOffset}s` : `${subtitlesOffset}s`}</span>
          </div>

          <div className="flex gap-2">
            <button onClick={() => setSubtitlesOffset(prev => prev - 0.5)} className="flex-1 py-1.5 rounded-lg bg-white/10 text-xs font-mono text-white">-0.5s</button>
            <button onClick={() => setSubtitlesOffset(0)} className="px-3 py-1.5 rounded-lg bg-white/10 text-xs font-mono text-white/70">RESET</button>
            <button onClick={() => setSubtitlesOffset(prev => prev + 0.5)} className="flex-1 py-1.5 rounded-lg bg-white/10 text-xs font-mono text-white">+0.5s</button>
          </div>
        </div>
      )}

    </div>
  );
};
