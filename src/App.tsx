import React, { useState, useEffect, useRef } from 'react';
import type { MediaItem, ActiveHub, MediaType, RadioStation } from './types/media';
import { Header } from './components/Header';
import { MusicHub } from './components/MusicHub';
import { VideoHub } from './components/VideoHub';
import { LibraryHub } from './components/LibraryHub';
import { RadioHub } from './components/RadioHub';
import { GlyphMatrix } from './components/GlyphMatrix';
import { EqualizerModal } from './components/EqualizerModal';
import { VLCShortcutsModal } from './components/VLCShortcutsModal';
import { audioEngine } from './services/audioEngine';
import { 
  updateMediaSessionMetadata, 
  updateMediaSessionPosition, 
  setupMediaSessionActionHandlers 
} from './services/mediaSession';
import { saveMediaItem, getAllMediaItems, deleteMediaItem, getMediaBlob } from './services/db';
import { DEMO_MEDIA_ITEMS } from './services/demoData';
import { triggerHaptic } from './services/haptic';

export const App: React.FC = () => {
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Core State
  const [activeHub, setActiveHub] = useState<ActiveHub>('MUSIC');
  const [mediaList, setMediaList] = useState<MediaItem[]>([]);
  const [currentTrack, setCurrentTrack] = useState<MediaItem | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1.0);
  const [isMuted, setIsMuted] = useState(false);
  const [shuffle, setShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState<'off' | 'all' | 'one'>('off');
  const [playbackRate, setPlaybackRate] = useState(1.0);

  // Equalizer & Audio Boost State
  const [showEqualizer, setShowEqualizer] = useState(false);
  const [showShortcuts, setShowShortcuts] = useState(false);
  const [eqGains, setEqGains] = useState<number[]>([4, 5, 3, 1, -1, 0, 2, 4, 5, 5]);
  const [eqPreamp, setEqPreamp] = useState<number>(0);
  const [eqBassBoost, setEqBassBoost] = useState<number>(3);
  const [eqPreset, setEqPreset] = useState<string>('NOTHING PUNCH');
  const [audioBoost, setAudioBoost] = useState<number>(100); // 100% to 200%

  // Initialize DB and load media
  useEffect(() => {
    const initLibrary = async () => {
      try {
        const storedItems = await getAllMediaItems();
        if (storedItems.length === 0) {
          // Seed initial demo items
          for (const item of DEMO_MEDIA_ITEMS) {
            await saveMediaItem(item);
          }
          setMediaList(DEMO_MEDIA_ITEMS);
          setCurrentTrack(DEMO_MEDIA_ITEMS[0]);
        } else {
          setMediaList(storedItems);
          setCurrentTrack(storedItems[0]);
        }
      } catch (e) {
        console.warn('IndexedDB initial load error:', e);
        setMediaList(DEMO_MEDIA_ITEMS);
        setCurrentTrack(DEMO_MEDIA_ITEMS[0]);
      }
    };

    initLibrary();
  }, []);

  // Initialize Web Audio Engine with Audio element
  useEffect(() => {
    if (audioRef.current) {
      audioEngine.init(audioRef.current);
      audioEngine.setAllBands(eqGains);
      audioEngine.setPreamp(eqPreamp);
      audioEngine.setBassBoost(eqBassBoost);
      audioEngine.setVolumeBoost(audioBoost / 100);
    }
  }, [eqGains, eqPreamp, eqBassBoost, audioBoost]);

  // Setup Android MediaSession Action Handlers
  useEffect(() => {
    setupMediaSessionActionHandlers({
      onPlay: () => handlePlayPause(),
      onPause: () => handlePlayPause(),
      onPrev: () => handlePrev(),
      onNext: () => handleNext(),
      onSeek: (pos) => handleSeek(pos)
    });
  }, [currentTrack, isPlaying]);

  // Update MediaSession Metadata
  useEffect(() => {
    updateMediaSessionMetadata(currentTrack, isPlaying);
  }, [currentTrack, isPlaying]);

  // Sync Audio Time & Position State
  const handleTimeUpdate = () => {
    if (audioRef.current) {
      const pos = audioRef.current.currentTime;
      setCurrentTime(pos);
      updateMediaSessionPosition(pos, audioRef.current.duration || duration, playbackRate);
    }
  };

  const handleLoadedMetadata = () => {
    if (audioRef.current) {
      setDuration(audioRef.current.duration);
    }
  };

  const handleEnded = () => {
    if (repeatMode === 'one') {
      if (audioRef.current) {
        audioRef.current.currentTime = 0;
        audioRef.current.play();
      }
    } else {
      handleNext();
    }
  };

  // Play / Pause Toggle
  const handlePlayPause = async () => {
    if (!audioRef.current) return;
    audioEngine.resume();

    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      try {
        await audioRef.current.play();
        setIsPlaying(true);
      } catch (err) {
        console.warn('Audio playback error:', err);
      }
    }
  };

  // Play Specific Track
  const handleSelectTrack = async (item: MediaItem) => {
    setCurrentTrack(item);
    setCurrentTime(0);

    // If item has blobKey in IndexedDB, create object URL
    let playUrl = item.url;
    if (item.blobKey) {
      const blob = await getMediaBlob(item.blobKey);
      if (blob) {
        playUrl = URL.createObjectURL(blob);
      }
    }

    if (item.type === 'video') {
      setActiveHub('VIDEO');
      if (audioRef.current) {
        audioRef.current.pause();
        setIsPlaying(false);
      }
    } else {
      if (activeHub === 'VIDEO') {
        setActiveHub('MUSIC');
      }
      if (audioRef.current) {
        audioRef.current.src = playUrl;
        audioRef.current.load();
        audioEngine.resume();
        try {
          await audioRef.current.play();
          setIsPlaying(true);
        } catch (e) {
          console.warn('Playback error:', e);
        }
      }
    }
  };

  // Next Track
  const handleNext = () => {
    if (mediaList.length === 0) return;
    triggerHaptic('medium');
    let nextIndex = 0;
    const audioItems = mediaList.filter(m => m.type === 'audio' || m.type === 'stream');
    const currentIndex = audioItems.findIndex(m => m.id === currentTrack?.id);

    if (shuffle) {
      nextIndex = Math.floor(Math.random() * audioItems.length);
    } else {
      nextIndex = (currentIndex + 1) % audioItems.length;
    }

    handleSelectTrack(audioItems[nextIndex]);
  };

  // Previous Track
  const handlePrev = () => {
    if (mediaList.length === 0) return;
    triggerHaptic('medium');
    const audioItems = mediaList.filter(m => m.type === 'audio' || m.type === 'stream');
    const currentIndex = audioItems.findIndex(m => m.id === currentTrack?.id);
    const prevIndex = (currentIndex - 1 + audioItems.length) % audioItems.length;
    handleSelectTrack(audioItems[prevIndex]);
  };

  // Seek
  const handleSeek = (time: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = time;
      setCurrentTime(time);
    }
  };

  // Volume
  const handleVolumeChange = (newVol: number) => {
    setVolume(newVol);
    if (audioRef.current) {
      audioRef.current.volume = newVol;
    }
    setIsMuted(newVol === 0);
  };

  const handleToggleMute = () => {
    if (audioRef.current) {
      const nextMute = !isMuted;
      setIsMuted(nextMute);
      audioRef.current.muted = nextMute;
    }
  };

  // Playback Rate
  const handlePlaybackRateChange = (rate: number) => {
    setPlaybackRate(rate);
    if (audioRef.current) {
      audioRef.current.playbackRate = rate;
    }
  };

  // Favorite Toggle
  const handleToggleFavorite = async (id: string) => {
    triggerHaptic('light');
    const updated = mediaList.map(item => {
      if (item.id === id) {
        const toggled = { ...item, isFavorite: !item.isFavorite };
        saveMediaItem(toggled);
        return toggled;
      }
      return item;
    });
    setMediaList(updated);
    if (currentTrack?.id === id) {
      setCurrentTrack(prev => prev ? { ...prev, isFavorite: !prev.isFavorite } : null);
    }
  };

  // Delete Item
  const handleDeleteItem = async (id: string) => {
    await deleteMediaItem(id);
    setMediaList(prev => prev.filter(item => item.id !== id));
  };

  // Import Local Files
  const handleImportFiles = async (files: FileList | File[]) => {
    const newItems: MediaItem[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const isVideo = file.type.startsWith('video/');
      const ext = file.name.split('.').pop()?.toUpperCase() || (isVideo ? 'MP4' : 'MP3');
      
      const item: MediaItem = {
        id: `local_${Date.now()}_${i}`,
        title: file.name.replace(/\.[^/.]+$/, ''),
        artist: 'Local Device Media',
        album: isVideo ? 'Local Video Library' : 'Local Music Storage',
        duration: 0,
        url: URL.createObjectURL(file),
        type: isVideo ? 'video' : 'audio',
        format: ext,
        thumbnail: isVideo 
          ? 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&auto=format&fit=crop&q=80'
          : 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&auto=format&fit=crop&q=80',
        addedAt: Date.now(),
        size: file.size
      };

      await saveMediaItem(item, file);
      newItems.push(item);
    }

    setMediaList(prev => [...newItems, ...prev]);
    if (newItems.length > 0) {
      handleSelectTrack(newItems[0]);
    }
  };

  // Add Stream URL
  const handleAddStreamUrl = async (title: string, url: string, type: MediaType) => {
    const item: MediaItem = {
      id: `stream_${Date.now()}`,
      title: title,
      artist: 'Live Network Stream',
      album: 'Network Broadcast',
      duration: 0,
      url: url,
      type: type,
      format: url.includes('.m3u8') ? 'HLS' : type === 'video' ? 'MP4' : 'MP3',
      thumbnail: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400&auto=format&fit=crop&q=80',
      addedAt: Date.now()
    };

    await saveMediaItem(item);
    setMediaList(prev => [item, ...prev]);
    handleSelectTrack(item);
  };

  // Play Radio Station
  const handlePlayRadioStation = (station: RadioStation) => {
    const radioItem: MediaItem = {
      id: station.id,
      title: station.name,
      artist: station.genre,
      album: `${station.country} Broadcast`,
      duration: 0,
      url: station.streamUrl,
      type: 'stream',
      format: 'LIVE',
      thumbnail: station.coverArt,
      addedAt: Date.now()
    };
    handleSelectTrack(radioItem);
  };

  // Global VLC Keyboard Shortcuts Handler
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if user is typing in an input field
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;

      switch (e.code) {
        case 'Space':
          e.preventDefault();
          handlePlayPause();
          break;
        case 'ArrowLeft':
          e.preventDefault();
          handleSeek(Math.max(0, currentTime - 5));
          break;
        case 'ArrowRight':
          e.preventDefault();
          handleSeek(Math.min(duration, currentTime + 5));
          break;
        case 'ArrowUp':
          e.preventDefault();
          handleVolumeChange(Math.min(1.0, volume + 0.05));
          break;
        case 'ArrowDown':
          e.preventDefault();
          handleVolumeChange(Math.max(0, volume - 0.05));
          break;
        case 'KeyM':
          e.preventDefault();
          handleToggleMute();
          break;
        case 'KeyN':
          e.preventDefault();
          handleNext();
          break;
        case 'KeyB':
          e.preventDefault();
          handlePrev();
          break;
        case 'KeyS':
          // Open shortcuts guide
          setShowShortcuts(prev => !prev);
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentTime, duration, volume, isMuted, isPlaying]);

  const audioQueue = mediaList.filter(m => m.type === 'audio' || m.type === 'stream');
  const videoList = mediaList.filter(m => m.type === 'video');

  return (
    <div className="min-h-screen bg-[#050505] text-[#f5f5f5] flex flex-col selection:bg-[#D71921] selection:text-white pb-12">
      
      {/* Hidden Audio Engine Element */}
      <audio
        ref={audioRef}
        src={currentTrack?.type !== 'video' ? currentTrack?.url : undefined}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onEnded={handleEnded}
        preload="auto"
      />

      {/* Top Nothing OS Navigation Header */}
      <Header
        activeHub={activeHub}
        setActiveHub={setActiveHub}
        onOpenEqualizer={() => setShowEqualizer(true)}
        onOpenShortcuts={() => setShowShortcuts(true)}
        onOpenImport={() => {
          const input = document.createElement('input');
          input.type = 'file';
          input.multiple = true;
          input.accept = 'audio/*,video/*';
          input.onchange = (e) => {
            const files = (e.target as HTMLInputElement).files;
            if (files) handleImportFiles(files);
          };
          input.click();
        }}
        isPlaying={isPlaying}
      />

      {/* Main Hub Router View */}
      <main className="flex-1 flex flex-col">
        {activeHub === 'MUSIC' && (
          <MusicHub
            currentTrack={currentTrack}
            isPlaying={isPlaying}
            onPlayPause={handlePlayPause}
            onPrev={handlePrev}
            onNext={handleNext}
            currentTime={currentTime}
            duration={duration}
            onSeek={handleSeek}
            volume={volume}
            onVolumeChange={handleVolumeChange}
            isMuted={isMuted}
            onToggleMute={handleToggleMute}
            shuffle={shuffle}
            onToggleShuffle={() => setShuffle(!shuffle)}
            repeatMode={repeatMode}
            onCycleRepeat={() => {
              const modes: ('off' | 'all' | 'one')[] = ['off', 'all', 'one'];
              const next = modes[(modes.indexOf(repeatMode) + 1) % modes.length];
              setRepeatMode(next);
            }}
            playbackRate={playbackRate}
            onChangePlaybackRate={handlePlaybackRateChange}
            onToggleFavorite={handleToggleFavorite}
            onOpenEqualizer={() => setShowEqualizer(true)}
            queue={audioQueue}
            onSelectTrack={handleSelectTrack}
          />
        )}

        {activeHub === 'VIDEO' && (
          <VideoHub
            currentVideo={currentTrack?.type === 'video' ? currentTrack : videoList[0] || null}
            videoList={videoList}
            onSelectVideo={handleSelectTrack}
            onOpenEqualizer={() => setShowEqualizer(true)}
          />
        )}

        {activeHub === 'LIBRARY' && (
          <LibraryHub
            mediaItems={mediaList}
            currentTrackId={currentTrack?.id}
            onPlayItem={handleSelectTrack}
            onDeleteItem={handleDeleteItem}
            onToggleFavorite={handleToggleFavorite}
            onImportFiles={handleImportFiles}
            onAddStreamUrl={handleAddStreamUrl}
          />
        )}

        {activeHub === 'RADIO' && (
          <RadioHub
            currentStationUrl={currentTrack?.url}
            isPlaying={isPlaying}
            onPlayStation={handlePlayRadioStation}
          />
        )}

        {activeHub === 'GLYPH' && (
          <div className="w-full max-w-4xl mx-auto p-4 sm:p-8 flex flex-col items-center gap-6">
            <GlyphMatrix isPlaying={isPlaying} isExpanded={true} />
          </div>
        )}
      </main>

      {/* 10-Band Equalizer Modal */}
      <EqualizerModal
        isOpen={showEqualizer}
        onClose={() => setShowEqualizer(false)}
        gains={eqGains}
        setGains={setEqGains}
        preamp={eqPreamp}
        setPreamp={setEqPreamp}
        bassBoost={eqBassBoost}
        setBassBoost={setEqBassBoost}
        selectedPreset={eqPreset}
        setSelectedPreset={setEqPreset}
        audioBoost={audioBoost}
        setAudioBoost={setAudioBoost}
      />

      {/* VLC Shortcuts & Mobile Gestures Modal */}
      <VLCShortcutsModal
        isOpen={showShortcuts}
        onClose={() => setShowShortcuts(false)}
      />

    </div>
  );
};

export default App;
