import React, { useState, useEffect, useRef } from 'react';
import type { MediaItem, MainTab, RadioStation } from './types/media';
import { BottomNavBar } from './components/BottomNavBar';
import { VideoExplorer } from './components/VideoExplorer';
import { NativeVideoPlayer } from './components/NativeVideoPlayer';
import { MusicExplorer } from './components/MusicExplorer';
import { MiniMusicPlayer } from './components/MiniMusicPlayer';
import { NowPlayingSheet } from './components/NowPlayingSheet';
import { RadioHub } from './components/RadioHub';
import { SettingsHub } from './components/SettingsHub';
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

import { scanDeviceStorage, playNativeExo } from './services/nativeMediaScanner';

export const App: React.FC = () => {
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Tabs & Player Views
  const [activeTab, setActiveTab] = useState<MainTab>('VIDEOS');
  const [activeVideo, setActiveVideo] = useState<MediaItem | null>(null);
  const [showNowPlayingSheet, setShowNowPlayingSheet] = useState(false);

  // Media Library State
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
  const [isScanning, setIsScanning] = useState(false);

  // Equalizer & Audio Boost State
  const [showEqualizer, setShowEqualizer] = useState(false);
  const [showShortcuts, setShowShortcuts] = useState(false);
  const [eqGains, setEqGains] = useState<number[]>([4, 5, 3, 1, -1, 0, 2, 4, 5, 5]);
  const [eqPreamp, setEqPreamp] = useState<number>(0);
  const [eqBassBoost, setEqBassBoost] = useState<number>(3);
  const [eqPreset, setEqPreset] = useState<string>('NOTHING PUNCH');
  const [audioBoost, setAudioBoost] = useState<number>(100);

  const handleScanDevice = async () => {
    setIsScanning(true);
    triggerHaptic('medium');
    try {
      const { videos: scannedVids, audios: scannedAuds } = await scanDeviceStorage();
      if (scannedVids.length > 0 || scannedAuds.length > 0) {
        const combined = [...scannedVids, ...scannedAuds];
        for (const item of combined) {
          await saveMediaItem(item);
        }
        setMediaList(prev => {
          const existingIds = new Set(prev.map(i => i.id));
          const newItems = combined.filter(i => !existingIds.has(i.id));
          return [...newItems, ...prev];
        });
      }
    } catch (err) {
      console.warn('Device scan failed:', err);
    } finally {
      setIsScanning(false);
    }
  };

  // Initialize DB and load media
  useEffect(() => {
    const initLibrary = async () => {
      try {
        const storedItems = await getAllMediaItems();
        if (storedItems.length === 0) {
          for (const item of DEMO_MEDIA_ITEMS) {
            await saveMediaItem(item);
          }
          setMediaList(DEMO_MEDIA_ITEMS);
          setCurrentTrack(DEMO_MEDIA_ITEMS.find(m => m.type === 'audio') || DEMO_MEDIA_ITEMS[0]);
        } else {
          setMediaList(storedItems);
          setCurrentTrack(storedItems.find(m => m.type === 'audio') || storedItems[0]);
        }
        // Auto-trigger device media scan
        handleScanDevice();
      } catch (e) {
        console.warn('IndexedDB load error:', e);
        setMediaList(DEMO_MEDIA_ITEMS);
        setCurrentTrack(DEMO_MEDIA_ITEMS[0]);
      }
    };

    initLibrary();
  }, []);

  // Web Audio DSP Engine
  useEffect(() => {
    if (audioRef.current) {
      audioEngine.init(audioRef.current);
      audioEngine.setAllBands(eqGains);
      audioEngine.setPreamp(eqPreamp);
      audioEngine.setBassBoost(eqBassBoost);
      audioEngine.setVolumeBoost(audioBoost / 100);
    }
  }, [eqGains, eqPreamp, eqBassBoost, audioBoost]);

  // MediaSession API setup
  useEffect(() => {
    setupMediaSessionActionHandlers({
      onPlay: () => handlePlayPause(),
      onPause: () => handlePlayPause(),
      onPrev: () => handlePrevTrack(),
      onNext: () => handleNextTrack(),
      onSeek: (pos) => handleSeek(pos)
    });
  }, [currentTrack, isPlaying]);

  useEffect(() => {
    updateMediaSessionMetadata(currentTrack, isPlaying);
  }, [currentTrack, isPlaying]);

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
      handleNextTrack();
    }
  };

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
        console.warn('Playback err:', err);
      }
    }
  };

  const handleSelectTrack = async (item: MediaItem) => {
    setCurrentTrack(item);
    setCurrentTime(0);

    let playUrl = item.url;
    if (item.blobKey) {
      const blob = await getMediaBlob(item.blobKey);
      if (blob) playUrl = URL.createObjectURL(blob);
    }

    if (audioRef.current) {
      audioRef.current.src = playUrl;
      audioRef.current.load();
      audioEngine.resume();
      try {
        await audioRef.current.play();
        setIsPlaying(true);
      } catch (e) {
        console.warn('Track play error:', e);
      }
    }
  };

  const handleNextTrack = () => {
    const audioItems = mediaList.filter(m => m.type === 'audio' || m.type === 'stream');
    if (audioItems.length === 0) return;
    triggerHaptic('medium');
    const currentIndex = audioItems.findIndex(m => m.id === currentTrack?.id);
    const nextIndex = shuffle 
      ? Math.floor(Math.random() * audioItems.length) 
      : (currentIndex + 1) % audioItems.length;
    handleSelectTrack(audioItems[nextIndex]);
  };

  const handlePrevTrack = () => {
    const audioItems = mediaList.filter(m => m.type === 'audio' || m.type === 'stream');
    if (audioItems.length === 0) return;
    triggerHaptic('medium');
    const currentIndex = audioItems.findIndex(m => m.id === currentTrack?.id);
    const prevIndex = (currentIndex - 1 + audioItems.length) % audioItems.length;
    handleSelectTrack(audioItems[prevIndex]);
  };

  const handleSeek = (time: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = time;
      setCurrentTime(time);
    }
  };

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

  const handleDeleteItem = async (id: string) => {
    await deleteMediaItem(id);
    setMediaList(prev => prev.filter(item => item.id !== id));
  };

  const handleSaveVideoProgress = (id: string, position: number) => {
    setMediaList(prev => prev.map(v => {
      if (v.id === id) {
        const updated = { ...v, lastPosition: position };
        saveMediaItem(updated);
        return updated;
      }
      return v;
    }));
  };

  const handleImportFiles = async (files: FileList | File[]) => {
    const newItems: MediaItem[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const isVideo = file.type.startsWith('video/');
      const ext = file.name.split('.').pop()?.toUpperCase() || (isVideo ? 'MP4' : 'MP3');
      
      const item: MediaItem = {
        id: `local_${Date.now()}_${i}`,
        title: file.name.replace(/\.[^/.]+$/, ''),
        artist: isVideo ? 'Device Video' : 'Device Audio',
        album: isVideo ? 'Downloads' : 'Music',
        folder: isVideo ? 'Downloads' : 'Internal Music',
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
      if (newItems[0].type === 'video') {
        setActiveVideo(newItems[0]);
      } else {
        handleSelectTrack(newItems[0]);
      }
    }
  };

  const handlePlayRadio = (station: RadioStation) => {
    const radioItem: MediaItem = {
      id: station.id,
      title: station.name,
      artist: station.genre,
      album: station.country,
      folder: 'Radio Streams',
      duration: 0,
      url: station.streamUrl,
      type: 'stream',
      format: 'LIVE',
      thumbnail: station.coverArt,
      addedAt: Date.now()
    };
    handleSelectTrack(radioItem);
  };

  const videos = mediaList.filter(m => m.type === 'video');
  const tracks = mediaList.filter(m => m.type === 'audio' || m.type === 'stream');

  return (
    <div className="min-h-screen bg-[#050505] text-[#f5f5f5] flex flex-col font-sans select-none overflow-x-hidden">
      
      {/* Hidden Audio Element */}
      <audio
        ref={audioRef}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onEnded={handleEnded}
        preload="auto"
      />

      {/* Main Tab Content */}
      <main className="flex-1 flex flex-col">
        {activeTab === 'VIDEOS' && (
          <VideoExplorer
            videos={videos}
            onPlayVideo={async (v) => {
              if (audioRef.current) {
                audioRef.current.pause();
                setIsPlaying(false);
              }
              const launched = await playNativeExo(v);
              if (!launched) {
                setActiveVideo(v);
              }
            }}
            onImportFiles={handleImportFiles}
            onDeleteVideo={handleDeleteItem}
            onScanDevice={handleScanDevice}
            isScanning={isScanning}
          />
        )}

        {activeTab === 'MUSIC' && (
          <MusicExplorer
            tracks={tracks}
            currentTrackId={currentTrack?.id}
            onPlayTrack={handleSelectTrack}
            onImportFiles={handleImportFiles}
            onToggleFavorite={handleToggleFavorite}
            onScanDevice={handleScanDevice}
            isScanning={isScanning}
          />
        )}

        {activeTab === 'RADIO' && (
          <div className="pt-9 sm:pt-4 pb-36">
            <RadioHub
              currentStationUrl={currentTrack?.url}
              isPlaying={isPlaying}
              onPlayStation={handlePlayRadio}
            />
          </div>
        )}

        {activeTab === 'ME' && (
          <SettingsHub
            onOpenEqualizer={() => setShowEqualizer(true)}
            onOpenShortcuts={() => setShowShortcuts(true)}
            audioBoost={audioBoost}
            setAudioBoost={setAudioBoost}
            totalMediaCount={mediaList.length}
          />
        )}
      </main>

      {/* MX Fullscreen Video Player */}
      {activeVideo && (
        <NativeVideoPlayer
          video={activeVideo}
          videoList={videos}
          onClose={() => setActiveVideo(null)}
          onNextVideo={() => {
            const idx = videos.findIndex(v => v.id === activeVideo.id);
            const next = videos[(idx + 1) % videos.length];
            setActiveVideo(next);
          }}
          onPrevVideo={() => {
            const idx = videos.findIndex(v => v.id === activeVideo.id);
            const prev = videos[(idx - 1 + videos.length) % videos.length];
            setActiveVideo(prev);
          }}
          onSaveProgress={handleSaveVideoProgress}
        />
      )}

      {/* Pinned Mini Music Player */}
      {!activeVideo && (
        <MiniMusicPlayer
          currentTrack={currentTrack}
          isPlaying={isPlaying}
          onPlayPause={handlePlayPause}
          onNext={handleNextTrack}
          onOpenFullPlayer={() => setShowNowPlayingSheet(true)}
          currentTime={currentTime}
          duration={duration}
          onToggleFavorite={handleToggleFavorite}
        />
      )}

      {/* Slide-Up Fullscreen Now Playing Sheet */}
      <NowPlayingSheet
        isOpen={showNowPlayingSheet}
        onClose={() => setShowNowPlayingSheet(false)}
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        onPlayPause={handlePlayPause}
        onPrev={handlePrevTrack}
        onNext={handleNextTrack}
        currentTime={currentTime}
        duration={duration}
        onSeek={handleSeek}
        volume={volume}
        onVolumeChange={(v) => {
          setVolume(v);
          if (audioRef.current) audioRef.current.volume = v;
        }}
        isMuted={isMuted}
        onToggleMute={() => {
          setIsMuted(!isMuted);
          if (audioRef.current) audioRef.current.muted = !isMuted;
        }}
        shuffle={shuffle}
        onToggleShuffle={() => setShuffle(!shuffle)}
        repeatMode={repeatMode}
        onCycleRepeat={() => {
          const modes: ('off' | 'all' | 'one')[] = ['off', 'all', 'one'];
          setRepeatMode(modes[(modes.indexOf(repeatMode) + 1) % modes.length]);
        }}
        playbackRate={playbackRate}
        onChangePlaybackRate={(r) => {
          setPlaybackRate(r);
          if (audioRef.current) audioRef.current.playbackRate = r;
        }}
        onToggleFavorite={handleToggleFavorite}
        onOpenEqualizer={() => setShowEqualizer(true)}
        queue={tracks}
        onSelectTrack={handleSelectTrack}
      />

      {/* Bottom Navigation Bar */}
      {!activeVideo && (
        <BottomNavBar
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          videoCount={videos.length}
          musicCount={tracks.length}
        />
      )}

      {/* 10-Band Graphic Equalizer */}
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

      {/* VLC Gestures Cheat Sheet */}
      <VLCShortcutsModal
        isOpen={showShortcuts}
        onClose={() => setShowShortcuts(false)}
      />

    </div>
  );
};

export default App;
