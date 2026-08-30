import React, { useState, useRef, useEffect } from 'react';
import type { MediaItem, VideoFilterState } from '../types/media';
import { parseSubtitles, getActiveSubtitle } from '../services/subtitleParser';
import { GestureOverlay } from './GestureOverlay';
import { triggerHaptic } from '../services/haptic';
import { 
  Play, 
  Pause, 
  Volume2, 
  VolumeX, 
  Maximize, 
  Minimize, 
  Camera, 
  Subtitles, 
  Tv, 
  ChevronLeft, 
  ChevronRight, 
  PictureInPicture,
  Film
} from 'lucide-react';

interface VideoHubProps {
  currentVideo: MediaItem | null;
  videoList: MediaItem[];
  onSelectVideo: (item: MediaItem) => void;
  onOpenEqualizer?: () => void;
}

export const VideoHub: React.FC<VideoHubProps> = ({
  currentVideo,
  videoList,
  onSelectVideo,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [audioBoostVolume, setAudioBoostVolume] = useState(100); // 0 to 200%
  const [isMuted, setIsMuted] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [aspectRatio, setAspectRatio] = useState<'fit' | '16:9' | '4:3' | '21:9' | 'stretch'>('fit');
  const [showControls, setShowControls] = useState(true);
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const [showSubtitleDrawer, setShowSubtitleDrawer] = useState(false);

  // Video Post-Processing Filters
  const [filters, setFilters] = useState<VideoFilterState>({
    brightness: 100,
    contrast: 100,
    saturation: 100,
    crtScanlines: false,
    duotoneRed: false,
    hueRotate: 0,
    invert: false,
  });

  // Subtitles State
  const [subtitleCues, setSubtitleCues] = useState<ReturnType<typeof parseSubtitles>>([]);
  const [subtitlesOffset, setSubtitlesOffset] = useState(0);
  const [subtitlesEnabled, setSubtitlesEnabled] = useState(true);
  const [subtitleFontSize, setSubtitleFontSize] = useState(16);

  // Gesture State
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

  const gestureTimeoutRef = useRef<number | null>(null);
  const touchStartRef = useRef<{ x: number; y: number; time: number } | null>(null);
  const lastTapRef = useRef<{ time: number; x: number } | null>(null);

  // Parse Subtitles whenever video or offset changes
  useEffect(() => {
    if (currentVideo?.subtitlesContent) {
      setSubtitleCues(parseSubtitles(currentVideo.subtitlesContent, subtitlesOffset));
    } else {
      setSubtitleCues([]);
    }
  }, [currentVideo, subtitlesOffset]);

  // Sync Video Time & Duration
  const handleTimeUpdate = () => {
    if (videoRef.current) {
      setCurrentTime(videoRef.current.currentTime);
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
  };

  const handleSeek = (time: number) => {
    if (videoRef.current) {
      videoRef.current.currentTime = time;
      setCurrentTime(time);
    }
  };

  // Step Frame forward or back (like VLC 'e')
  const handleFrameStep = (direction: 'forward' | 'backward') => {
    if (!videoRef.current) return;
    triggerHaptic('light');
    const frameDuration = 1 / 30; // approx 30fps
    const target = direction === 'forward' 
      ? Math.min(duration, videoRef.current.currentTime + frameDuration)
      : Math.max(0, videoRef.current.currentTime - frameDuration);
    videoRef.current.currentTime = target;
    setCurrentTime(target);
  };

  // Screenshot capture of current video frame
  const handleTakeScreenshot = () => {
    if (!videoRef.current) return;
    triggerHaptic('heavy');
    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 1920;
    canvas.height = video.videoHeight || 1080;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
      const dataUrl = canvas.toDataURL('image/png');
      const a = document.createElement('a');
      a.href = dataUrl;
      a.download = `NOTHING_FRAME_${currentVideo?.title || 'SCREEN'}_${Math.floor(currentTime)}s.png`;
      a.click();
    }
  };

  // Picture-in-Picture
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
      console.warn('PiP not supported or failed', e);
    }
  };

  // Fullscreen
  const handleToggleFullscreen = () => {
    if (!containerRef.current) return;
    triggerHaptic('light');
    if (!document.fullscreenElement) {
      containerRef.current.requestFullscreen?.();
      setIsFullscreen(true);
    } else {
      document.exitFullscreen?.();
      setIsFullscreen(false);
    }
  };

  // Mobile Swipe Gesture Handlers (VLC Mobile emulation)
  const handleTouchStart = (e: React.TouchEvent<HTMLDivElement>) => {
    const touch = e.touches[0];
    const now = Date.now();

    // Check for Double Tap (Left / Right)
    if (lastTapRef.current && (now - lastTapRef.current.time < 300)) {
      const rect = containerRef.current?.getBoundingClientRect();
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
  };

  const handleTouchMove = (e: React.TouchEvent<HTMLDivElement>) => {
    if (!touchStartRef.current || !containerRef.current) return;
    const touch = e.touches[0];
    const deltaX = touch.clientX - touchStartRef.current.x;
    const deltaY = touch.clientY - touchStartRef.current.y;
    const rect = containerRef.current.getBoundingClientRect();
    const isLeftHalf = touchStartRef.current.x - rect.left < rect.width / 2;

    // Detect Vertical vs Horizontal swipe
    if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > 15) {
      // Vertical Swipe
      if (isLeftHalf) {
        // Left Side: Brightness (0% to 150%)
        const deltaPercent = -deltaY / 2;
        const newBrightness = Math.max(30, Math.min(150, filters.brightness + deltaPercent * 0.15));
        setFilters(prev => ({ ...prev, brightness: newBrightness }));
        showGestureHUD('brightness', newBrightness);
      } else {
        // Right Side: Volume & Audio Boost (0% to 200%)
        const deltaPercent = -deltaY / 2;
        const newVolBoost = Math.max(0, Math.min(200, audioBoostVolume + deltaPercent * 0.25));
        setAudioBoostVolume(newVolBoost);
        if (videoRef.current) {
          videoRef.current.volume = Math.min(1.0, newVolBoost / 100);
        }
        showGestureHUD('volume', newVolBoost);
      }
    } else if (Math.abs(deltaX) > 25) {
      // Horizontal Swipe: Precision Seeking
      const seekOffset = (deltaX / rect.width) * 60; // swipe full screen = 60s
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

  // Active Subtitle Line lookup
  const activeSubtitle = subtitlesEnabled ? getActiveSubtitle(subtitleCues, currentTime) : null;

  const formatTime = (secs: number) => {
    if (isNaN(secs)) return '00:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // Video Aspect Ratio Styles
  const getAspectRatioClasses = () => {
    switch (aspectRatio) {
      case '16:9': return 'aspect-video object-contain';
      case '4:3': return 'aspect-[4/3] object-contain';
      case '21:9': return 'aspect-[21/9] object-cover';
      case 'stretch': return 'w-full h-full object-fill';
      case 'fit':
      default: return 'w-full h-full object-contain';
    }
  };

  // Video Filter CSS string
  const videoFilterStyle: React.CSSProperties = {
    filter: `
      brightness(${filters.brightness}%) 
      contrast(${filters.contrast}%) 
      saturate(${filters.saturation}%) 
      hue-rotate(${filters.hueRotate}deg) 
      ${filters.invert ? 'invert(100%)' : ''}
    `
  };

  return (
    <div className="w-full max-w-7xl mx-auto p-3 sm:p-6 flex flex-col gap-6 animate-fade-in">
      
      {/* Video Hub Top Bar */}
      <div className="flex items-center justify-between border-b border-white/10 pb-3">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <span className="font-dot text-sm sm:text-base tracking-widest text-white">
            CINEMA SUITE // 02 VIDEO
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              triggerHaptic('light');
              setShowFilterDrawer(!showFilterDrawer);
            }}
            className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-mono border transition-all active-press ${
              showFilterDrawer || filters.crtScanlines || filters.duotoneRed
                ? 'bg-white text-black font-bold border-white'
                : 'bg-white/5 text-white/70 border-white/10'
            }`}
          >
            <Tv className="w-3.5 h-3.5" />
            <span>SHADERS & CRT</span>
          </button>

          <button
            onClick={() => {
              triggerHaptic('light');
              setShowSubtitleDrawer(!showSubtitleDrawer);
            }}
            className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-mono border transition-all active-press ${
              showSubtitleDrawer ? 'bg-white text-black font-bold border-white' : 'bg-white/5 text-white/70 border-white/10'
            }`}
          >
            <Subtitles className="w-3.5 h-3.5" />
            <span>SUBTITLES</span>
          </button>
        </div>
      </div>

      {/* Main Video Cinema Container with VLC Mobile Gestures & HUD */}
      <div 
        ref={containerRef}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
        onMouseMove={() => setShowControls(true)}
        className={`relative w-full rounded-3xl bg-black border border-white/15 overflow-hidden shadow-2xl flex items-center justify-center ${
          isFullscreen ? 'fixed inset-0 z-50 rounded-none' : 'aspect-video min-h-[300px] max-h-[75vh]'
        } ${filters.crtScanlines ? 'crt-scanlines' : ''}`}
      >
        {/* Video Element */}
        <video
          ref={videoRef}
          src={currentVideo?.url}
          onTimeUpdate={handleTimeUpdate}
          onLoadedMetadata={handleLoadedMetadata}
          onClick={togglePlayPause}
          style={videoFilterStyle}
          className={`${getAspectRatioClasses()} ${filters.duotoneRed ? 'filter-duotone-red' : ''}`}
          playsInline
        />

        {/* Gesture Visual HUD Overlay */}
        <GestureOverlay
          type={gesture.type}
          value={gesture.value}
          formattedText={gesture.formattedText}
          visible={gesture.visible}
        />

        {/* Subtitles Overlay */}
        {activeSubtitle && (
          <div className="absolute bottom-16 sm:bottom-20 left-4 right-4 z-25 flex justify-center pointer-events-none">
            <p 
              className="bg-black/80 backdrop-blur-sm text-white px-4 py-2 rounded-xl text-center font-sans font-medium tracking-wide shadow-2xl border border-white/10"
              style={{ fontSize: `${subtitleFontSize}px` }}
            >
              {activeSubtitle}
            </p>
          </div>
        )}

        {/* Video Top Controls Overlay */}
        <div className={`absolute top-0 left-0 right-0 p-4 bg-gradient-to-b from-black/80 via-black/40 to-transparent flex items-center justify-between z-20 transition-opacity duration-300 ${
          showControls ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}>
          <div className="flex items-center gap-2">
            <span className="font-dot text-sm text-white font-bold tracking-wider line-clamp-1">
              {currentVideo?.title || 'VLC VIDEO PLAYER'}
            </span>
          </div>

          <div className="flex items-center gap-1.5">
            {/* Aspect Ratio Selector */}
            <button
              onClick={() => {
                triggerHaptic('light');
                const ratios: ('fit' | '16:9' | '4:3' | '21:9' | 'stretch')[] = ['fit', '16:9', '4:3', '21:9', 'stretch'];
                const nextIdx = (ratios.indexOf(aspectRatio) + 1) % ratios.length;
                setAspectRatio(ratios[nextIdx]);
              }}
              className="px-2 py-1 rounded bg-black/60 border border-white/20 text-[10px] font-mono text-white active-press"
              title="Change Aspect Ratio"
            >
              ASPECT: {aspectRatio.toUpperCase()}
            </button>

            {/* Screenshot Frame Grabber */}
            <button
              onClick={handleTakeScreenshot}
              className="p-1.5 rounded-lg bg-black/60 border border-white/20 text-white hover:text-[#D71921] active-press"
              title="Capture Frame Screenshot"
            >
              <Camera className="w-4 h-4" />
            </button>

            {/* PiP */}
            <button
              onClick={handleTogglePiP}
              className="p-1.5 rounded-lg bg-black/60 border border-white/20 text-white hover:text-white/80 active-press"
              title="Picture in Picture"
            >
              <PictureInPicture className="w-4 h-4" />
            </button>

            {/* Fullscreen */}
            <button
              onClick={handleToggleFullscreen}
              className="p-1.5 rounded-lg bg-black/60 border border-white/20 text-white hover:text-white/80 active-press"
              title="Fullscreen"
            >
              {isFullscreen ? <Minimize className="w-4 h-4" /> : <Maximize className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Video Bottom HUD & Timeline Bar */}
        <div className={`absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/90 via-black/50 to-transparent flex flex-col gap-2.5 z-20 transition-opacity duration-300 ${
          showControls ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}>
          
          {/* Progress Seek Scrubber */}
          <div className="w-full flex items-center gap-3">
            <span className="font-mono text-xs text-white/70 min-w-[42px]">{formatTime(currentTime)}</span>
            
            <div 
              className="relative flex-1 h-2 bg-white/20 rounded-full cursor-pointer overflow-hidden group"
              onClick={(e) => {
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

            <span className="font-mono text-xs text-white/50 min-w-[42px] text-right">{formatTime(duration)}</span>
          </div>

          {/* Action Row */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              {/* Play / Pause */}
              <button
                onClick={togglePlayPause}
                className="p-2.5 rounded-full bg-white text-black hover:scale-105 active-press transition-all"
              >
                {isPlaying ? <Pause className="w-5 h-5 fill-black" /> : <Play className="w-5 h-5 fill-black ml-0.5" />}
              </button>

              {/* Frame-by-frame navigation (VLC 'e') */}
              <div className="hidden sm:flex items-center gap-1">
                <button
                  onClick={() => handleFrameStep('backward')}
                  className="p-1.5 rounded-md bg-white/10 hover:bg-white/20 text-white text-xs font-mono active-press"
                  title="Previous Frame"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleFrameStep('forward')}
                  className="p-1.5 rounded-md bg-white/10 hover:bg-white/20 text-white text-xs font-mono active-press"
                  title="Next Frame (VLC 'E')"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>

              {/* Volume */}
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => {
                    triggerHaptic('light');
                    setIsMuted(!isMuted);
                    if (videoRef.current) videoRef.current.muted = !isMuted;
                  }}
                  className="p-1.5 text-white/70 hover:text-white active-press"
                >
                  {isMuted ? <VolumeX className="w-4 h-4 text-[#D71921]" /> : <Volume2 className="w-4 h-4" />}
                </button>
                <span className="font-mono text-xs text-white/70">{audioBoostVolume}%</span>
              </div>
            </div>

            {/* Quick Filter Status Indicator */}
            <div className="flex items-center gap-2 text-[10px] font-mono text-white/50">
              {filters.crtScanlines && <span className="text-[#D71921]">● CRT ACTIVE</span>}
              {filters.duotoneRed && <span className="text-[#D71921]">● RED DUOTONE</span>}
            </div>
          </div>

        </div>

      </div>

      {/* Shaders & Video Post-Processing Drawer */}
      {showFilterDrawer && (
        <div className="p-5 rounded-3xl bg-[#0e0e0e] border border-white/10 flex flex-col gap-4 animate-fade-in">
          <div className="flex items-center justify-between border-b border-white/10 pb-2">
            <span className="font-dot text-sm tracking-wider text-white">
              VIDEO POST-PROCESSING SHADERS
            </span>
            <button
              onClick={() => {
                triggerHaptic('light');
                setFilters({
                  brightness: 100,
                  contrast: 100,
                  saturation: 100,
                  crtScanlines: false,
                  duotoneRed: false,
                  hueRotate: 0,
                  invert: false
                });
              }}
              className="text-xs font-mono text-white/50 hover:text-white"
            >
              RESET
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            
            {/* CRT Scanline Toggle */}
            <button
              onClick={() => {
                triggerHaptic('medium');
                setFilters(prev => ({ ...prev, crtScanlines: !prev.crtScanlines }));
              }}
              className={`p-3 rounded-2xl border text-left flex flex-col justify-between transition-all active-press ${
                filters.crtScanlines
                  ? 'bg-white text-black border-white font-bold'
                  : 'bg-white/5 border-white/10 text-white/70'
              }`}
            >
              <span className="font-mono text-xs">RETRO CRT SCANLINES</span>
              <span className="text-[10px] opacity-70">Authentic 90s TV tube scanlines</span>
            </button>

            {/* Nothing Red Duotone Toggle */}
            <button
              onClick={() => {
                triggerHaptic('medium');
                setFilters(prev => ({ ...prev, duotoneRed: !prev.duotoneRed }));
              }}
              className={`p-3 rounded-2xl border text-left flex flex-col justify-between transition-all active-press ${
                filters.duotoneRed
                  ? 'bg-[#D71921] text-white border-[#D71921] font-bold glow-red'
                  : 'bg-white/5 border-white/10 text-white/70'
              }`}
            >
              <span className="font-mono text-xs">NOTHING RED DUOTONE</span>
              <span className="text-[10px] opacity-70">Monochrome red cinema grading</span>
            </button>

            {/* Contrast Slider */}
            <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col justify-between">
              <div className="flex justify-between text-xs font-mono text-white/70">
                <span>CONTRAST</span>
                <span>{filters.contrast}%</span>
              </div>
              <input
                type="range"
                min="50"
                max="200"
                value={filters.contrast}
                onChange={(e) => setFilters(prev => ({ ...prev, contrast: parseFloat(e.target.value) }))}
                className="accent-[#D71921] mt-2 cursor-pointer"
              />
            </div>

            {/* Saturation Slider */}
            <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col justify-between">
              <div className="flex justify-between text-xs font-mono text-white/70">
                <span>SATURATION</span>
                <span>{filters.saturation}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="200"
                value={filters.saturation}
                onChange={(e) => setFilters(prev => ({ ...prev, saturation: parseFloat(e.target.value) }))}
                className="accent-[#D71921] mt-2 cursor-pointer"
              />
            </div>

          </div>
        </div>
      )}

      {/* Subtitles Drawer */}
      {showSubtitleDrawer && (
        <div className="p-5 rounded-3xl bg-[#0e0e0e] border border-white/10 flex flex-col gap-4 animate-fade-in">
          <div className="flex items-center justify-between border-b border-white/10 pb-2">
            <span className="font-dot text-sm tracking-wider text-white">
              SUBTITLE CONTROLS & TIMING SYNC
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setSubtitlesEnabled(!subtitlesEnabled)}
                className={`px-3 py-1 rounded-md text-xs font-mono active-press ${
                  subtitlesEnabled ? 'bg-white text-black font-bold' : 'bg-white/10 text-white/60'
                }`}
              >
                {subtitlesEnabled ? 'SUBTITLES: ON' : 'SUBTITLES: OFF'}
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            
            {/* Sync Delay Offset */}
            <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col justify-between">
              <div className="flex justify-between text-xs font-mono text-white/70">
                <span>SYNC DELAY (SECONDS)</span>
                <span className="text-[#D71921]">{subtitlesOffset > 0 ? `+${subtitlesOffset}s` : `${subtitlesOffset}s`}</span>
              </div>
              <div className="flex items-center gap-2 mt-2">
                <button
                  onClick={() => setSubtitlesOffset(prev => prev - 0.5)}
                  className="flex-1 py-1 rounded bg-white/10 font-mono text-xs text-white"
                >
                  -0.5s
                </button>
                <button
                  onClick={() => setSubtitlesOffset(0)}
                  className="px-3 py-1 rounded bg-white/10 font-mono text-xs text-white/70"
                >
                  RESET
                </button>
                <button
                  onClick={() => setSubtitlesOffset(prev => prev + 0.5)}
                  className="flex-1 py-1 rounded bg-white/10 font-mono text-xs text-white"
                >
                  +0.5s
                </button>
              </div>
            </div>

            {/* Font Size */}
            <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col justify-between">
              <div className="flex justify-between text-xs font-mono text-white/70">
                <span>FONT SCALE</span>
                <span>{subtitleFontSize}px</span>
              </div>
              <input
                type="range"
                min="12"
                max="28"
                value={subtitleFontSize}
                onChange={(e) => setSubtitleFontSize(parseInt(e.target.value, 10))}
                className="accent-[#D71921] mt-2 cursor-pointer"
              />
            </div>

            {/* Upload Custom Subtitle File (.srt, .vtt) */}
            <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col justify-between">
              <span className="text-xs font-mono text-white/70">LOAD EXTERNAL SUBTITLE (.SRT / .VTT)</span>
              <label className="mt-2 py-1.5 px-3 rounded-xl bg-white text-black font-mono text-xs font-bold text-center cursor-pointer hover:bg-white/90 active-press">
                CHOOSE .SRT / .VTT FILE
                <input
                  type="file"
                  accept=".srt,.vtt,.sub"
                  className="hidden"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) {
                      const reader = new FileReader();
                      reader.onload = (event) => {
                        const content = event.target?.result as string;
                        if (content && currentVideo) {
                          currentVideo.subtitlesContent = content;
                          currentVideo.subtitlesName = file.name;
                          setSubtitleCues(parseSubtitles(content, subtitlesOffset));
                        }
                      };
                      reader.readAsText(file);
                    }
                  }}
                />
              </label>
            </div>

          </div>
        </div>
      )}

      {/* Video Playlist Scroller */}
      <div className="p-5 rounded-3xl bg-[#0c0c0c] border border-white/10 flex flex-col gap-4">
        <div className="flex items-center justify-between border-b border-white/10 pb-2">
          <div className="flex items-center gap-2">
            <Film className="w-4 h-4 text-[#D71921]" />
            <span className="font-dot text-sm tracking-wider text-white">
              VIDEO PLAYLIST & CLIPS ({videoList.length})
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {videoList.map((item) => {
            const isCurrent = currentVideo?.id === item.id;
            return (
              <div
                key={item.id}
                onClick={() => {
                  triggerHaptic('medium');
                  onSelectVideo(item);
                }}
                className={`flex gap-3 p-3 rounded-2xl border transition-all cursor-pointer active-press ${
                  isCurrent
                    ? 'bg-white text-black border-white font-bold shadow-lg shadow-white/10'
                    : 'bg-white/5 border-white/5 text-white/80 hover:bg-white/10'
                }`}
              >
                <div className="relative w-24 h-16 rounded-xl overflow-hidden bg-black/60 shrink-0 border border-white/10">
                  <img
                    src={item.thumbnail || 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&auto=format&fit=crop&q=80'}
                    alt={item.title}
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute bottom-1 right-1 px-1 rounded bg-black/80 text-[8px] font-mono text-white">
                    {formatTime(item.duration)}
                  </div>
                </div>

                <div className="flex flex-col justify-center">
                  <span className="font-mono text-xs font-bold line-clamp-1">{item.title}</span>
                  <span className={`text-[10px] font-mono ${isCurrent ? 'text-black/60' : 'text-white/40'}`}>
                    {item.format} // {item.artist || 'Video Studio'}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

    </div>
  );
};
