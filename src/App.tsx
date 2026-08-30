import React, { useState, useEffect, useRef } from 'react';
import type { MediaItem, MainTab } from './types/media';
import { BottomNavBar } from './components/BottomNavBar';
import { VideoExplorer } from './components/VideoExplorer';
import { NativeVideoPlayer } from './components/NativeVideoPlayer';
import { MusicExplorer } from './components/MusicExplorer';
import { MiniMusicPlayer } from './components/MiniMusicPlayer';
import { NowPlayingSheet } from './components/NowPlayingSheet';
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
import { triggerHaptic } from './services/haptic';

import { 
  scanDeviceStorage, 
  playNativeExo,
  nativePlayAudio,
  nativePauseAudio,
  nativeResumeAudio,
  nativeSeekAudio,
  nativeGetAudioStatus,
  MediaScanner
} from './services/nativeMediaScanner';
import { Capacitor } from '@capacitor/core';
import { App as CapApp } from '@capacitor/app';

export const App: React.FC = () => {
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Tabs & Player Views (Videos, Music, Settings)
  const [activeTab, setActiveTab] = useState<MainTab>('VIDEOS');
  const [selectedFolder, setSelectedFolder] = useState<string | null>(null);
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

  // Android Native Hardware Back Button & Gesture Navigation with double-tap protection
  const lastBackPressRef = useRef<number>(0);
  useEffect(() => {
    let removeListener: (() => void) | undefined;

    if (Capacitor.isNativePlatform()) {
      CapApp.addListener('backButton', () => {
        if (showNowPlayingSheet) {
          setShowNowPlayingSheet(false);
          return;
        }
        if (showEqualizer) {
          setShowEqualizer(false);
          return;
        }
        if (showShortcuts) {
          setShowShortcuts(false);
          return;
        }
        if (selectedFolder !== null) {
          setSelectedFolder(null);
          return;
        }
        if (activeTab !== 'VIDEOS') {
          setActiveTab('VIDEOS');
          return;
        }

        const now = Date.now();
        if (now - lastBackPressRef.current < 2000) {
          CapApp.exitApp();
        } else {
          lastBackPressRef.current = now;
          triggerHaptic('light');
        }
      }).then(handle => {
        removeListener = () => handle.remove();
      });
    }

    return () => {
      if (removeListener) removeListener();
    };
  }, [showNowPlayingSheet, showEqualizer, showShortcuts, selectedFolder, activeTab]);

  // Handle Remote Notification and Lockscreen Media Controls
  useEffect(() => {
    const unsubs: (() => void)[] = [];

    if (Capacitor.isNativePlatform()) {
      MediaScanner.addListener('audioNextRequested', () => {
        handleNextTrack();
      }).then(h => unsubs.push(() => h.remove()));

      MediaScanner.addListener('audioPrevRequested', () => {
        handlePrevTrack();
      }).then(h => unsubs.push(() => h.remove()));

      MediaScanner.addListener('audioTrackEnded', () => {
        handleNextTrack();
      }).then(h => unsubs.push(() => h.remove()));

      MediaScanner.addListener('audioPlayStateChanged', (data: { isPlaying: boolean }) => {
        setIsPlaying(data.isPlaying);
      }).then(h => unsubs.push(() => h.remove()));
    }

    return () => {
      unsubs.forEach(fn => fn());
    };
  }, [mediaList, currentTrack, shuffle]);

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
        setMediaList(combined);
        if (!currentTrack && scannedAuds.length > 0) {
          setCurrentTrack(scannedAuds[0]);
        }
      }
    } catch (err) {
      console.warn('Device scan failed:', err);
    } finally {
      setIsScanning(false);
    }
  };

  // Initialize DB and purge fake demo items
  useEffect(() => {
    const initLibrary = async () => {
      try {
        const storedItems = await getAllMediaItems();
        const realItems: MediaItem[] = [];
        for (const item of storedItems) {
          if (item.id.startsWith('demo_')) {
            await deleteMediaItem(item.id);
          } else {
            realItems.push(item);
          }
        }

        setMediaList(realItems);
        if (realItems.length > 0) {
          const firstAudio = realItems.find(m => m.type === 'audio');
          if (firstAudio) setCurrentTrack(firstAudio);
        }

        // Auto-trigger device media scan
        handleScanDevice();
      } catch (e) {
        console.warn('DB load error:', e);
        setMediaList([]);
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

  // Native Android Audio Status Poll
  useEffect(() => {
    if (!Capacitor.isNativePlatform() || !isPlaying) return;

    const interval = setInterval(async () => {
      const status = await nativeGetAudioStatus();
      if (status) {
        if (status.duration > 0) {
          setDuration(status.duration);
        }
        setCurrentTime(status.currentTime);
        setIsPlaying(status.isPlaying);

        if (status.duration > 0 && status.currentTime >= status.duration - 0.5) {
          handleEnded();
        }
      }
    }, 500);

    return () => clearInterval(interval);
  }, [isPlaying, duration, repeatMode]);

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
      if (Capacitor.isNativePlatform() && currentTrack) {
        nativeSeekAudio(0);
        nativeResumeAudio();
        setIsPlaying(true);
      } else if (audioRef.current) {
        audioRef.current.currentTime = 0;
        audioRef.current.play();
      }
    } else {
      handleNextTrack();
    }
  };

  const handlePlayPause = async () => {
    if (Capacitor.isNativePlatform()) {
      if (isPlaying) {
        await nativePauseAudio();
        setIsPlaying(false);
      } else {
        await nativeResumeAudio();
        setIsPlaying(true);
      }
      return;
    }

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

    if (Capacitor.isNativePlatform()) {
      const res = await nativePlayAudio(item);
      if (res) {
        setDuration(res.duration || item.duration || 0);
        setIsPlaying(true);
        return;
      }
    }

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
    if (Capacitor.isNativePlatform()) {
      nativeSeekAudio(time);
      setCurrentTime(time);
      return;
    }

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
    triggerHaptic('medium');
    await deleteMediaItem(id);
    setMediaList(prev => prev.filter(m => m.id !== id));
    if (currentTrack?.id === id) {
      setCurrentTrack(null);
      setIsPlaying(false);
    }
    if (activeVideo?.id === id) {
      setActiveVideo(null);
    }
  };

  const handleSaveVideoProgress = async (id: string, pos: number) => {
    const item = mediaList.find(m => m.id === id);
    if (item) {
      const updated = { ...item, lastPosition: pos };
      await saveMediaItem(updated);
      setMediaList(prev => prev.map(m => m.id === id ? updated : m));
    }
  };

  const handleImportFiles = async (files: FileList | File[]) => {
    const fileArray = Array.from(files);
    const newItems: MediaItem[] = [];

    for (const file of fileArray) {
      const isVideo = file.type.startsWith('video/') || /\.(mp4|mkv|avi|mov|webm|flv|ts)$/i.test(file.name);
      const ext = file.name.split('.').pop()?.toUpperCase() || (isVideo ? 'MP4' : 'MP3');

      const item: MediaItem = {
        id: `user_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
        title: file.name.replace(/\.[^/.]+$/, ''),
        artist: isVideo ? 'Local Video' : 'Local Audio',
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
        const launched = await playNativeExo(newItems[0]);
        if (!launched) {
          setActiveVideo(newItems[0]);
        }
      } else {
        handleSelectTrack(newItems[0]);
      }
    }
  };

  const videos = mediaList.filter(m => m.type === 'video');
  const tracks = mediaList.filter(m => m.type === 'audio' || m.type === 'stream');

  return (
    <div className="min-h-screen bg-[#050505] text-[#f5f5f5] flex flex-col font-sans select-none overflow-x-hidden">
      
      {/* Hidden Audio Element for Web */}
      <audio
        ref={audioRef}
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onEnded={handleEnded}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
      />

      {/* Main Tab Router View */}
      <main className="flex-1 w-full flex flex-col">
        {activeTab === 'VIDEOS' && (
          <VideoExplorer
            videos={videos}
            onPlayVideo={async (v) => {
              const launched = await playNativeExo(v);
              if (!launched) {
                setActiveVideo(v);
              }
            }}
            onImportFiles={handleImportFiles}
            onDeleteVideo={handleDeleteItem}
            onScanDevice={handleScanDevice}
            isScanning={isScanning}
            selectedFolder={selectedFolder}
            setSelectedFolder={setSelectedFolder}
          />
        )}

        {activeTab === 'MUSIC' && (
          <MusicExplorer
            tracks={tracks}
            currentTrackId={currentTrack?.id}
            onPlayTrack={handleSelectTrack}
            onImportFiles={handleImportFiles}
            onToggleFavorite={handleToggleFavorite}
            onDeleteTrack={handleDeleteItem}
            onScanDevice={handleScanDevice}
            isScanning={isScanning}
          />
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

      {/* Fullscreen Video Player (Web Fallback) */}
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
          const next = modes[(modes.indexOf(repeatMode) + 1) % modes.length];
          setRepeatMode(next);
        }}
        playbackRate={playbackRate}
        onChangePlaybackRate={(rate) => {
          setPlaybackRate(rate);
          if (audioRef.current) audioRef.current.playbackRate = rate;
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

      {/* DSP Equalizer Modal */}
      {showEqualizer && (
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
      )}

      {/* VLC Gestures & Shortcuts Guide */}
      {showShortcuts && (
        <VLCShortcutsModal
          isOpen={showShortcuts}
          onClose={() => setShowShortcuts(false)}
        />
      )}
    </div>
  );
};

export default App;
