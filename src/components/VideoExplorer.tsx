import React, { useState } from 'react';
import type { MediaItem } from '../types/media';
import { 
  Folder, 
  Search, 
  LayoutGrid, 
  List, 
  ArrowUpDown, 
  MoreVertical, 
  Play, 
  Plus, 
  RefreshCw, 
  ChevronLeft, 
  ChevronRight,
  Copy,
  Check,
  HardDrive,
  Film,
  Globe,
  ExternalLink,
  History,
  Volume2
} from 'lucide-react';
import { triggerHaptic } from '../services/haptic';

interface VideoExplorerProps {
  videos: MediaItem[];
  onPlayVideo: (item: MediaItem) => void;
  onImportFiles: (files: FileList | File[]) => void;
  onDeleteVideo: (id: string) => void;
  onScanDevice?: () => void;
  isScanning?: boolean;
  selectedFolder: string | null;
  setSelectedFolder: (folder: string | null) => void;
}

export const VideoExplorer: React.FC<VideoExplorerProps> = ({
  videos,
  onPlayVideo,
  onImportFiles,
  onDeleteVideo,
  onScanDevice,
  isScanning,
  selectedFolder,
  setSelectedFolder
}) => {
  const [viewMode, setViewMode] = useState<'folders' | 'all'>('folders');
  const [layoutMode, setLayoutMode] = useState<'grid' | 'list'>('grid');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'date' | 'name' | 'size' | 'duration'>('date');
  const [sortOrder] = useState<'asc' | 'desc'>('desc');
  const [selectedVideoIds, setSelectedVideoIds] = useState<string[]>([]);
  const [isMultiSelect] = useState(false);
  const [infoModalVideo, setInfoModalVideo] = useState<MediaItem | null>(null);
  const [copiedPath, setCopiedPath] = useState(false);

  // Network Stream State
  const [showStreamModal, setShowStreamModal] = useState(false);
  const [streamUrl, setStreamUrl] = useState('');
  const [streamTitle, setStreamTitle] = useState('');
  const [recentStreams, setRecentStreams] = useState<{ url: string; title: string; date: number }[]>(() => {
    try {
      const saved = localStorage.getItem('nothing_recent_streams');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const saveRecentStream = (url: string, title: string) => {
    const updated = [
      { url, title, date: Date.now() },
      ...recentStreams.filter(s => s.url !== url)
    ].slice(0, 8);
    setRecentStreams(updated);
    try {
      localStorage.setItem('nothing_recent_streams', JSON.stringify(updated));
    } catch {}
  };

  const handleStartStream = (url: string, customTitle?: string) => {
    if (!url || !url.trim()) return;
    const cleanUrl = url.trim();
    const resolvedTitle = customTitle?.trim() || cleanUrl.split('/').pop()?.split('?')[0] || 'Network Stream';
    
    saveRecentStream(cleanUrl, resolvedTitle);
    setShowStreamModal(false);
    setStreamUrl('');
    setStreamTitle('');
    triggerHaptic('heavy');

    const streamItem: MediaItem = {
      id: `stream_${Date.now()}`,
      title: resolvedTitle,
      url: cleanUrl,
      path: cleanUrl,
      format: cleanUrl.includes('.m3u8') ? 'HLS' : cleanUrl.includes('.mpd') ? 'DASH' : 'STREAM',
      size: 0,
      duration: 0,
      type: 'video',
      folder: 'Network Stream',
      addedAt: Date.now(),
      thumbnail: 'https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=600&auto=format&fit=crop&q=80'
    };

    onPlayVideo(streamItem);
  };

  // Group videos into folders
  const foldersMap = videos.reduce((acc, video) => {
    const folderName = video.folder || 'Internal Storage';
    if (!acc[folderName]) {
      acc[folderName] = [];
    }
    acc[folderName].push(video);
    return acc;
  }, {} as Record<string, MediaItem[]>);

  const folderNames = Object.keys(foldersMap).sort();

  // Filter and Sort videos
  const displayedVideos = (selectedFolder ? (foldersMap[selectedFolder] || []) : videos)
    .filter(v => 
      v.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (v.folder && v.folder.toLowerCase().includes(searchQuery.toLowerCase()))
    )
    .sort((a, b) => {
      let comparison = 0;
      if (sortBy === 'date') comparison = b.addedAt - a.addedAt;
      else if (sortBy === 'name') comparison = a.title.localeCompare(b.title);
      else if (sortBy === 'size') comparison = (b.size || 0) - (a.size || 0);
      else if (sortBy === 'duration') comparison = b.duration - a.duration;
      return sortOrder === 'desc' ? comparison : -comparison;
    });

  const formatDuration = (secs: number) => {
    if (!secs || isNaN(secs)) return '00:00';
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = Math.floor(secs % 60);
    if (h > 0) {
      return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'N/A';
    if (bytes >= 1073741824) return `${(bytes / 1073741824).toFixed(2)} GB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  };

  const toggleSelectVideo = (id: string) => {
    triggerHaptic('light');
    if (selectedVideoIds.includes(id)) {
      setSelectedVideoIds(prev => prev.filter(item => item !== id));
    } else {
      setSelectedVideoIds(prev => [...prev, id]);
    }
  };

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col pb-32 pt-14 sm:pt-6 animate-fade-in px-3 sm:px-4">
      
      {/* Top Native MX App Bar */}
      <div className="flex items-center justify-between py-2 border-b border-white/10 mb-3">
        <div className="flex items-center gap-2">
          {selectedFolder ? (
            <button
              onClick={() => {
                triggerHaptic('light');
                setSelectedFolder(null);
              }}
              className="flex items-center gap-1.5 p-1 -ml-1 text-white hover:text-[#D71921] active-press"
            >
              <ChevronLeft className="w-5 h-5" />
              <span className="font-dot text-base sm:text-lg font-bold tracking-wider uppercase">
                {selectedFolder}
              </span>
              <span className="text-[10px] font-mono text-white/40">
                ({foldersMap[selectedFolder]?.length || 0})
              </span>
            </button>
          ) : (
            <>
              <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
              <h1 className="font-dot text-base sm:text-lg font-bold tracking-wider text-white">
                NOTHING VIDEOS
              </h1>
              <span className="text-[10px] font-mono text-white/40">
                ({viewMode === 'folders' ? `${folderNames.length} Folders` : `${videos.length} Videos`})
              </span>
            </>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-1.5">
          {/* Network Stream URL Button */}
          <button
            onClick={() => {
              triggerHaptic('medium');
              setShowStreamModal(true);
            }}
            className="p-2 rounded-xl bg-[#D71921]/15 border border-[#D71921]/40 text-[#D71921] hover:bg-[#D71921]/25 active-press flex items-center gap-1"
            title="Stream Direct Video URL / HLS / DASH"
          >
            <Globe className="w-4 h-4" />
            <span className="text-[10px] font-mono font-bold hidden sm:inline">STREAM</span>
          </button>

          {/* Scan Device Storage Button */}
          {onScanDevice && (
            <button
              onClick={() => {
                triggerHaptic('medium');
                onScanDevice();
              }}
              disabled={isScanning}
              className={`p-2 rounded-xl border text-xs font-mono active-press flex items-center gap-1 ${
                isScanning 
                  ? 'bg-[#D71921]/20 border-[#D71921] text-[#D71921]' 
                  : 'bg-white/5 border-white/10 text-white/80 hover:bg-white/15'
              }`}
              title="Scan Phone Storage For Videos"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? 'animate-spin text-[#D71921]' : ''}`} />
              <span className="text-[10px] hidden sm:inline">{isScanning ? 'SCANNING...' : 'SCAN'}</span>
            </button>
          )}

          {/* File Picker Import */}
          <label className="p-2 rounded-xl bg-white/5 border border-white/10 hover:bg-white/15 text-white active-press cursor-pointer">
            <Plus className="w-4 h-4 text-[#D71921]" />
            <input
              type="file"
              multiple
              accept="video/*"
              className="hidden"
              onChange={(e) => {
                if (e.target.files && e.target.files.length > 0) {
                  triggerHaptic('medium');
                  onImportFiles(e.target.files);
                }
              }}
            />
          </label>

          {/* Grid / List Switcher (when viewing videos) */}
          {(selectedFolder || viewMode === 'all') && (
            <button
              onClick={() => {
                triggerHaptic('light');
                setLayoutMode(layoutMode === 'grid' ? 'list' : 'grid');
              }}
              className="p-2 rounded-xl bg-white/5 border border-white/10 hover:bg-white/15 text-white/70 hover:text-white active-press"
            >
              {layoutMode === 'grid' ? <List className="w-4 h-4" /> : <LayoutGrid className="w-4 h-4" />}
            </button>
          )}

          {/* Sort By Toggle */}
          <button
            onClick={() => {
              triggerHaptic('light');
              const sorts: ('date' | 'name' | 'size' | 'duration')[] = ['date', 'name', 'size', 'duration'];
              const nextIdx = (sorts.indexOf(sortBy) + 1) % sorts.length;
              setSortBy(sorts[nextIdx]);
            }}
            className="px-2.5 py-1.5 rounded-xl bg-white/5 border border-white/10 text-[10px] font-mono text-white/70 hover:text-white active-press flex items-center gap-1"
          >
            <ArrowUpDown className="w-3 h-3 text-[#D71921]" />
            <span className="uppercase">{sortBy}</span>
          </button>
        </div>
      </div>

      {/* Search Input Bar */}
      <div className="relative w-full mb-3">
        <Search className="w-4 h-4 text-white/40 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          placeholder={selectedFolder ? `Search inside ${selectedFolder}...` : "Search videos, folders, movies..."}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-[#111111] border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs font-sans text-white placeholder-white/40 focus:outline-none focus:border-white/30"
        />
      </div>

      {/* View Switcher Chips (Folders vs All Videos) */}
      {!selectedFolder && (
        <div className="flex items-center gap-2 mb-4 overflow-x-auto no-scrollbar">
          <button
            onClick={() => {
              triggerHaptic('selection');
              setViewMode('folders');
            }}
            className={`px-4 py-1.5 rounded-full text-xs font-mono transition-all whitespace-nowrap active-press ${
              viewMode === 'folders'
                ? 'bg-white text-black font-bold shadow-md shadow-white/10'
                : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
            }`}
          >
            📁 Folders ({folderNames.length})
          </button>

          <button
            onClick={() => {
              triggerHaptic('selection');
              setViewMode('all');
            }}
            className={`px-4 py-1.5 rounded-full text-xs font-mono transition-all whitespace-nowrap active-press ${
              viewMode === 'all'
                ? 'bg-white text-black font-bold shadow-md shadow-white/10'
                : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
            }`}
          >
            🎬 All Videos ({videos.length})
          </button>
        </div>
      )}

      {/* 1. Pure Folders View (No videos underneath) */}
      {viewMode === 'folders' && !selectedFolder && (
        <>
          {folderNames.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center rounded-3xl bg-[#0e0e0e] border border-white/10 my-6 gap-4">
              <div className="w-16 h-16 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-white/40">
                <Folder className="w-8 h-8" />
              </div>
              <div className="flex flex-col gap-1">
                <h3 className="font-dot text-base text-white font-bold">NO FOLDERS FOUND</h3>
                <p className="font-mono text-xs text-white/50 max-w-xs">
                  Tap below to scan your device storage for Camera, WhatsApp, Movies and Download folders.
                </p>
              </div>
              {onScanDevice && (
                <button
                  onClick={onScanDevice}
                  disabled={isScanning}
                  className="px-6 py-3 rounded-2xl bg-white text-black font-mono text-xs font-bold hover:bg-white/90 active-press shadow-xl flex items-center gap-2"
                >
                  <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
                  {isScanning ? 'SCANNING STORAGE...' : 'SCAN DEVICE VIDEOS'}
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {folderNames.map((folderName) => {
                const folderVideos = foldersMap[folderName] || [];
                const previewVideo = folderVideos[0];
                const totalSize = folderVideos.reduce((sum, v) => sum + (v.size || 0), 0);

                return (
                  <div
                    key={folderName}
                    onClick={() => {
                      triggerHaptic('medium');
                      setSelectedFolder(folderName);
                    }}
                    className="flex items-center gap-3.5 p-3.5 rounded-2xl bg-[#0e0e0e] border border-white/10 hover:border-white/30 hover:bg-white/5 transition-all cursor-pointer group active-press"
                  >
                    {/* Folder Thumbnail Preview */}
                    <div className="relative w-16 h-16 rounded-2xl bg-black/60 overflow-hidden shrink-0 border border-white/10 flex items-center justify-center">
                      {previewVideo?.thumbnail ? (
                        <img 
                          src={previewVideo.thumbnail} 
                          alt={folderName} 
                          className="w-full h-full object-cover group-hover:scale-110 transition-transform"
                        />
                      ) : (
                        <Folder className="w-7 h-7 text-[#D71921]" />
                      )}
                      <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                        <Folder className="w-6 h-6 text-white drop-shadow" />
                      </div>
                    </div>

                    <div className="flex-1 min-w-0">
                      <h3 className="font-mono text-sm font-bold text-white line-clamp-1 group-hover:text-[#D71921] transition-colors">
                        {folderName}
                      </h3>
                      <div className="flex items-center gap-2 mt-1 text-[10px] font-mono text-white/50">
                        <span className="px-1.5 py-0.5 rounded bg-white/10 text-white/80 font-bold">
                          {folderVideos.length} Videos
                        </span>
                        <span>•</span>
                        <span>{formatFileSize(totalSize)}</span>
                      </div>
                    </div>

                    <ChevronRight className="w-5 h-5 text-white/30 group-hover:text-white transition-colors" />
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}

      {/* 2. Video Grid / List View (When inside a folder OR in "All Videos" mode) */}
      {(selectedFolder || viewMode === 'all') && (
        <>
          {displayedVideos.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center rounded-3xl bg-[#0e0e0e] border border-white/10 my-6 gap-4">
              <div className="w-16 h-16 rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-white/40">
                <Folder className="w-8 h-8" />
              </div>
              <div className="flex flex-col gap-1">
                <h3 className="font-dot text-base text-white font-bold">NO VIDEOS FOUND</h3>
                <p className="font-mono text-xs text-white/50 max-w-xs">
                  {selectedFolder ? `No videos found in ${selectedFolder}` : 'Tap below to scan your device storage for videos.'}
                </p>
              </div>
              {selectedFolder ? (
                <button
                  onClick={() => setSelectedFolder(null)}
                  className="px-6 py-2.5 rounded-2xl bg-white text-black font-mono text-xs font-bold active-press"
                >
                  BACK TO FOLDERS
                </button>
              ) : onScanDevice && (
                <button
                  onClick={onScanDevice}
                  disabled={isScanning}
                  className="px-6 py-3 rounded-2xl bg-white text-black font-mono text-xs font-bold hover:bg-white/90 active-press shadow-xl flex items-center gap-2"
                >
                  <RefreshCw className={`w-4 h-4 ${isScanning ? 'animate-spin' : ''}`} />
                  {isScanning ? 'SCANNING STORAGE...' : 'SCAN DEVICE VIDEOS'}
                </button>
              )}
            </div>
          ) : layoutMode === 'grid' ? (
            /* MX Player Grid View */
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {displayedVideos.map((video) => {
                const watchedPercent = video.duration > 0 && video.lastPosition 
                  ? Math.min(100, (video.lastPosition / video.duration) * 100) 
                  : 0;

                return (
                  <div
                    key={video.id}
                    onClick={() => {
                      if (isMultiSelect) {
                        toggleSelectVideo(video.id);
                      } else {
                        triggerHaptic('heavy');
                        onPlayVideo(video);
                      }
                    }}
                    className="group relative flex flex-col rounded-2xl bg-[#0e0e0e] border border-white/10 overflow-hidden hover:border-white/30 transition-all cursor-pointer active-press shadow-lg"
                  >
                    {/* Big Thumbnail with Progress Bar & Duration Pill */}
                    <div className="relative w-full aspect-video bg-black/80 overflow-hidden">
                      <img
                        src={video.thumbnail || 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80'}
                        alt={video.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />

                      {/* Play Hover Overlay */}
                      <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                        <div className="p-3 rounded-full bg-white text-black shadow-xl">
                          <Play className="w-6 h-6 fill-black ml-0.5" />
                        </div>
                      </div>

                      {/* Top Badges (Resolution & Codec) */}
                      <div className="absolute top-2 left-2 flex items-center gap-1.5 flex-wrap">
                        {video.resolution && (
                          <span className="px-1.5 py-0.5 rounded bg-black/80 backdrop-blur-md text-[9px] font-mono text-white font-bold border border-white/10">
                            {video.resolution}
                          </span>
                        )}
                        <span className="px-1.5 py-0.5 rounded bg-[#D71921]/80 backdrop-blur-md text-[9px] font-mono text-white font-bold">
                          {video.format}
                        </span>
                        {(video.audioCodec?.includes('Dolby') || video.audioCodec?.includes('E-AC-3') || video.audioCodec?.includes('5.1') || video.title?.toLowerCase().includes('dd5.1') || video.title?.toLowerCase().includes('5.1')) && (
                          <span className="px-1.5 py-0.5 rounded bg-emerald-500/80 backdrop-blur-md text-[9px] font-mono text-white font-bold">
                            5.1 SURROUND
                          </span>
                        )}
                      </div>

                      {/* Duration Tag */}
                      <div className="absolute bottom-2 right-2 px-2 py-0.5 rounded-lg bg-black/80 backdrop-blur-md text-[10px] font-mono text-white font-bold border border-white/10">
                        {formatDuration(video.duration)}
                      </div>

                      {/* Resume Progress Bar */}
                      {watchedPercent > 0 && (
                        <div className="absolute bottom-0 left-0 right-0 h-1 bg-white/20">
                          <div 
                            className="h-full bg-[#D71921] glow-red transition-all" 
                            style={{ width: `${watchedPercent}%` }} 
                          />
                        </div>
                      )}
                    </div>

                    {/* Metadata Footer */}
                    <div className="p-3.5 flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <h3 className="font-mono text-xs font-bold text-white line-clamp-1 group-hover:text-[#D71921] transition-colors">
                          {video.title}
                        </h3>
                        <div className="flex items-center gap-2 mt-1 text-[10px] font-mono text-white/50">
                          <span>{video.folder || 'Storage'}</span>
                          <span>•</span>
                          <span>{formatFileSize(video.size)}</span>
                        </div>
                      </div>

                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          triggerHaptic('light');
                          setInfoModalVideo(video);
                        }}
                        className="p-1 rounded-lg text-white/40 hover:text-white hover:bg-white/10 transition-colors"
                      >
                        <MoreVertical className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            /* MX Player List View */
            <div className="flex flex-col gap-2">
              {displayedVideos.map((video) => {
                const watchedPercent = video.duration > 0 && video.lastPosition 
                  ? Math.min(100, (video.lastPosition / video.duration) * 100) 
                  : 0;

                return (
                  <div
                    key={video.id}
                    onClick={() => {
                      triggerHaptic('heavy');
                      onPlayVideo(video);
                    }}
                    className="flex items-center gap-3 p-3 rounded-2xl bg-[#0e0e0e] border border-white/10 hover:border-white/20 hover:bg-white/5 transition-all cursor-pointer group active-press"
                  >
                    {/* Thumbnail */}
                    <div className="relative w-28 h-18 rounded-xl bg-black/80 overflow-hidden shrink-0 border border-white/10">
                      <img
                        src={video.thumbnail || 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&auto=format&fit=crop&q=80'}
                        alt={video.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                      />
                      <div className="absolute bottom-1 right-1 px-1 rounded bg-black/80 text-[8px] font-mono text-white">
                        {formatDuration(video.duration)}
                      </div>
                      {watchedPercent > 0 && (
                        <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-white/20">
                          <div className="h-full bg-[#D71921]" style={{ width: `${watchedPercent}%` }} />
                        </div>
                      )}
                    </div>

                    {/* Info */}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-mono text-xs font-bold text-white line-clamp-1 group-hover:text-[#D71921] transition-colors">
                        {video.title}
                      </h3>
                      <div className="flex items-center gap-2 mt-1 text-[10px] font-mono text-white/50 flex-wrap">
                        <span className="px-1.5 py-0.5 rounded bg-white/10 text-white/80">
                          {video.format}
                        </span>
                        {(video.audioCodec?.includes('Dolby') || video.audioCodec?.includes('E-AC-3') || video.audioCodec?.includes('5.1') || video.title?.toLowerCase().includes('dd5.1') || video.title?.toLowerCase().includes('5.1')) && (
                          <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 font-bold border border-emerald-500/30">
                            5.1 DD
                          </span>
                        )}
                        <span>{video.folder}</span>
                        <span>•</span>
                        <span>{formatFileSize(video.size)}</span>
                      </div>
                    </div>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        triggerHaptic('light');
                        setInfoModalVideo(video);
                      }}
                      className="p-2 text-white/40 hover:text-white"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}

      {/* Video Details & Technical Specs Bottom Modal */}
      {infoModalVideo && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/80 backdrop-blur-md p-3 sm:p-4 animate-fade-in">
          <div className="w-full max-w-lg bg-[#0e0e0e] border border-white/20 rounded-3xl p-5 sm:p-6 shadow-2xl flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
                <span className="font-dot text-sm text-white font-bold tracking-wider">FILE SPECIFICATIONS & INFO</span>
              </div>
              <button 
                onClick={() => setInfoModalVideo(null)} 
                className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-white/60 hover:text-white active-press"
              >
                ✕
              </button>
            </div>

            {/* Video File Name */}
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 flex items-start gap-3">
              <div className="p-2.5 rounded-xl bg-[#D71921]/20 text-[#D71921] shrink-0 mt-0.5">
                <Film className="w-5 h-5" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-[10px] font-mono text-white/40 uppercase">FILE NAME</div>
                <div className="font-mono text-xs text-white font-bold break-all leading-tight mt-0.5">
                  {infoModalVideo.title}
                </div>
              </div>
            </div>

            {/* Exact Storage Location & Path (With Copy Button) */}
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono text-white/40 uppercase flex items-center gap-1.5">
                  <HardDrive className="w-3.5 h-3.5 text-[#D71921]" />
                  EXACT STORAGE LOCATION
                </span>
                <button
                  onClick={() => {
                    const fullPath = infoModalVideo.path || infoModalVideo.url || infoModalVideo.contentUri || '';
                    if (fullPath) {
                      navigator.clipboard.writeText(fullPath);
                      triggerHaptic('medium');
                      setCopiedPath(true);
                      setTimeout(() => setCopiedPath(false), 2000);
                    }
                  }}
                  className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/20 text-[10px] font-mono text-white font-bold flex items-center gap-1 active-press"
                >
                  {copiedPath ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                  {copiedPath ? 'COPIED ✓' : 'COPY PATH'}
                </button>
              </div>

              <div className="font-mono text-[11px] text-white/90 bg-black/60 p-2.5 rounded-xl border border-white/10 break-all select-all">
                {infoModalVideo.path || (infoModalVideo.contentUri ? `Content URI: ${infoModalVideo.contentUri}` : 'Internal Device Storage')}
              </div>
            </div>

            {/* Technical Metadata Grid */}
            <div className="grid grid-cols-2 gap-2.5 font-mono text-xs">
              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">FORMAT / CONTAINER</span>
                <span className="text-white font-bold">{infoModalVideo.format}</span>
              </div>

              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">RESOLUTION</span>
                <span className="text-white font-bold">{infoModalVideo.resolution || 'Auto / Full HD'}</span>
              </div>

              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">DURATION</span>
                <span className="text-white font-bold">{formatDuration(infoModalVideo.duration)}</span>
              </div>

              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">FILE SIZE</span>
                <span className="text-white font-bold">{formatFileSize(infoModalVideo.size)}</span>
              </div>

              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">PARENT FOLDER</span>
                <span className="text-white font-bold line-clamp-1">{infoModalVideo.folder || 'Storage'}</span>
              </div>

              <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-1">
                <span className="text-[10px] text-white/40 uppercase">DATE ADDED</span>
                <span className="text-white font-bold">
                  {infoModalVideo.addedAt ? new Date(infoModalVideo.addedAt).toLocaleDateString() : 'Recent'}
                </span>
              </div>
            </div>

            {/* Audio Codec & Sound Architecture */}
            <div className="p-3.5 rounded-2xl bg-white/5 border border-white/10 flex flex-col gap-1.5 font-mono">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-white/40 uppercase flex items-center gap-1.5">
                  <Volume2 className="w-3.5 h-3.5 text-[#D71921]" />
                  AUDIO CODEC & SOUND FORMAT
                </span>
                <span className="px-2 py-0.5 rounded-full bg-[#D71921]/20 border border-[#D71921]/40 text-[#D71921] text-[9px] font-bold">
                  {infoModalVideo.audioCodec?.includes('Dolby') || infoModalVideo.audioCodec?.includes('E-AC-3') || infoModalVideo.audioCodec?.includes('5.1') || infoModalVideo.title?.toLowerCase().includes('dd5.1') || infoModalVideo.title?.toLowerCase().includes('5.1') ? 'DOLBY 5.1' : 'STEREO'}
                </span>
              </div>
              <div className="text-white font-bold text-xs">
                {infoModalVideo.audioCodec || (infoModalVideo.title?.toLowerCase().includes('dd5.1') || infoModalVideo.title?.toLowerCase().includes('eac3') ? 'Dolby Digital Plus (E-AC-3 5.1 Surround)' : 'AAC LC (Stereo)')}
              </div>
              <div className="text-[11px] text-white/60">
                Configuration: <span className="text-white/90 font-medium">{infoModalVideo.audioChannels || (infoModalVideo.title?.toLowerCase().includes('dd5.1') || infoModalVideo.title?.toLowerCase().includes('5.1') ? '6 Channels (5.1 Surround, 48 kHz)' : '2 Channels (Stereo, 48 kHz)')}</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-2.5 pt-2">
              <button
                onClick={() => {
                  const target = infoModalVideo;
                  setInfoModalVideo(null);
                  onPlayVideo(target);
                }}
                className="flex-1 py-3 rounded-2xl bg-white text-black font-mono text-xs font-bold hover:bg-white/90 active-press flex items-center justify-center gap-2 shadow-xl"
              >
                <Play className="w-4 h-4 fill-black" />
                PLAY VIDEO
              </button>

              <button
                onClick={() => {
                  onDeleteVideo(infoModalVideo.id);
                  setInfoModalVideo(null);
                }}
                className="px-5 py-3 rounded-2xl bg-red-500/15 border border-red-500/30 text-red-500 font-mono text-xs font-bold hover:bg-red-500/25 active-press"
              >
                DELETE
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Network Stream Modal */}
      {showStreamModal && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/80 backdrop-blur-md p-3 sm:p-4 animate-fade-in">
          <div className="w-full max-w-lg bg-[#0e0e0e] border border-white/20 rounded-3xl p-5 sm:p-6 shadow-2xl flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            
            {/* Header */}
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
                <span className="font-dot text-sm text-white font-bold tracking-wider">NETWORK STREAM / DIRECT URL</span>
              </div>
              <button 
                onClick={() => setShowStreamModal(false)} 
                className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-white/60 hover:text-white active-press"
              >
                ✕
              </button>
            </div>

            {/* URL Input */}
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <label className="text-[10px] font-mono text-white/50 uppercase">
                  DIRECT VIDEO URL / HLS / DASH / MKV
                </label>
                <button
                  type="button"
                  onClick={async () => {
                    try {
                      const text = await navigator.clipboard.readText();
                      if (text) {
                        setStreamUrl(text.trim());
                        triggerHaptic('light');
                      }
                    } catch (e) {
                      console.warn('Clipboard read failed:', e);
                    }
                  }}
                  className="text-[10px] font-mono text-[#D71921] hover:underline flex items-center gap-1 active-press"
                >
                  <Copy className="w-3 h-3" />
                  PASTE LINK
                </button>
              </div>

              <div className="relative">
                <input
                  type="url"
                  placeholder="https://example.com/video.mp4 or .m3u8 or .mkv"
                  value={streamUrl}
                  onChange={(e) => setStreamUrl(e.target.value)}
                  className="w-full bg-black/70 border border-white/15 rounded-2xl px-4 py-3 text-xs font-mono text-white placeholder-white/30 focus:outline-none focus:border-[#D71921]"
                  autoFocus
                />
                {streamUrl && (
                  <button
                    onClick={() => setStreamUrl('')}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white text-xs"
                  >
                    ✕
                  </button>
                )}
              </div>
            </div>

            {/* Optional Title */}
            <div className="flex flex-col gap-1.5">
              <label className="text-[10px] font-mono text-white/50 uppercase">
                STREAM TITLE (OPTIONAL)
              </label>
              <input
                type="text"
                placeholder="e.g. Live Stream / Movie Title"
                value={streamTitle}
                onChange={(e) => setStreamTitle(e.target.value)}
                className="w-full bg-black/70 border border-white/15 rounded-2xl px-4 py-2.5 text-xs font-sans text-white placeholder-white/30 focus:outline-none focus:border-white/40"
              />
            </div>

            {/* Quick Test Samples */}
            <div className="flex flex-col gap-1.5">
              <span className="text-[10px] font-mono text-white/40 uppercase">QUICK DEMO SAMPLES</span>
              <div className="flex flex-wrap gap-1.5">
                <button
                  onClick={() => {
                    triggerHaptic('light');
                    setStreamUrl('https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4');
                    setStreamTitle('Big Buck Bunny (1080p MP4)');
                  }}
                  className="px-2.5 py-1 rounded-xl bg-white/5 hover:bg-white/15 border border-white/10 text-[10px] font-mono text-white/80 active-press"
                >
                  🐰 Big Buck Bunny (MP4)
                </button>
                <button
                  onClick={() => {
                    triggerHaptic('light');
                    setStreamUrl('https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4');
                    setStreamTitle('Tears of Steel (Sci-Fi)');
                  }}
                  className="px-2.5 py-1 rounded-xl bg-white/5 hover:bg-white/15 border border-white/10 text-[10px] font-mono text-white/80 active-press"
                >
                  🚀 Tears of Steel (FHD)
                </button>
                <button
                  onClick={() => {
                    triggerHaptic('light');
                    setStreamUrl('https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4');
                    setStreamTitle('Elephants Dream');
                  }}
                  className="px-2.5 py-1 rounded-xl bg-white/5 hover:bg-white/15 border border-white/10 text-[10px] font-mono text-white/80 active-press"
                >
                  🐘 Elephants Dream
                </button>
              </div>
            </div>

            {/* Recent Streams History */}
            {recentStreams.length > 0 && (
              <div className="flex flex-col gap-2 pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-mono text-white/40 uppercase flex items-center gap-1">
                    <History className="w-3 h-3 text-[#D71921]" />
                    RECENT STREAMS
                  </span>
                  <button
                    onClick={() => {
                      triggerHaptic('light');
                      setRecentStreams([]);
                      try { localStorage.removeItem('nothing_recent_streams'); } catch {}
                    }}
                    className="text-[9px] font-mono text-white/30 hover:text-red-400"
                  >
                    CLEAR HISTORY
                  </button>
                </div>

                <div className="flex flex-col gap-1 max-h-32 overflow-y-auto no-scrollbar">
                  {recentStreams.map((s, idx) => (
                    <div
                      key={idx}
                      onClick={() => handleStartStream(s.url, s.title)}
                      className="flex items-center justify-between p-2.5 rounded-xl bg-white/5 border border-white/5 hover:border-white/20 hover:bg-white/10 cursor-pointer active-press group"
                    >
                      <div className="flex-1 min-w-0 pr-2">
                        <div className="font-mono text-xs text-white font-bold truncate group-hover:text-[#D71921]">
                          {s.title}
                        </div>
                        <div className="font-mono text-[10px] text-white/40 truncate">
                          {s.url}
                        </div>
                      </div>
                      <ExternalLink className="w-3.5 h-3.5 text-white/40 group-hover:text-white shrink-0" />
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Start Stream Button */}
            <button
              onClick={() => handleStartStream(streamUrl, streamTitle)}
              disabled={!streamUrl.trim()}
              className={`w-full py-3.5 rounded-2xl font-mono text-xs font-bold active-press flex items-center justify-center gap-2 shadow-2xl transition-all ${
                streamUrl.trim()
                  ? 'bg-white text-black hover:bg-white/90 shadow-white/10'
                  : 'bg-white/10 text-white/30 cursor-not-allowed'
              }`}
            >
              <Play className="w-4 h-4 fill-current" />
              START STREAMING
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
