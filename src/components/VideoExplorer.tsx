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
  RefreshCw
} from 'lucide-react';
import { triggerHaptic } from '../services/haptic';
import { openNativePlayer } from '../services/nativeMediaScanner';

interface VideoExplorerProps {
  videos: MediaItem[];
  onPlayVideo: (item: MediaItem) => void;
  onImportFiles: (files: FileList | File[]) => void;
  onDeleteVideo: (id: string) => void;
  onScanDevice?: () => void;
  isScanning?: boolean;
}

export const VideoExplorer: React.FC<VideoExplorerProps> = ({
  videos,
  onPlayVideo,
  onImportFiles,
  onDeleteVideo,
  onScanDevice,
  isScanning
}) => {
  const [viewMode, setViewMode] = useState<'folders' | 'all'>('all');
  const [layoutMode, setLayoutMode] = useState<'grid' | 'list'>('grid');
  const [selectedFolder, setSelectedFolder] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'date' | 'name' | 'size' | 'duration'>('date');
  const [sortOrder] = useState<'asc' | 'desc'>('desc');
  const [selectedVideoIds, setSelectedVideoIds] = useState<string[]>([]);
  const [isMultiSelect] = useState(false);
  const [infoModalVideo, setInfoModalVideo] = useState<MediaItem | null>(null);

  // Group videos into folders
  const foldersMap = videos.reduce((acc, video) => {
    const folderName = video.folder || 'Internal Storage';
    if (!acc[folderName]) {
      acc[folderName] = [];
    }
    acc[folderName].push(video);
    return acc;
  }, {} as Record<string, MediaItem[]>);

  const folderNames = Object.keys(foldersMap);

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
    <div className="w-full max-w-5xl mx-auto flex flex-col pb-32 pt-9 sm:pt-4 animate-fade-in px-3 sm:px-4">
      
      {/* Top Native MX App Bar */}
      <div className="flex items-center justify-between py-2 border-b border-white/10 mb-3">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-[#D71921] glow-red" />
          <h1 className="font-dot text-base sm:text-lg font-bold tracking-wider text-white">
            NOTHING VIDEOS
          </h1>
          <span className="text-[10px] font-mono text-white/40">({videos.length})</span>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-1.5">
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

          {/* Grid / List Switcher */}
          <button
            onClick={() => {
              triggerHaptic('light');
              setLayoutMode(layoutMode === 'grid' ? 'list' : 'grid');
            }}
            className="p-2 rounded-xl bg-white/5 border border-white/10 hover:bg-white/15 text-white/70 hover:text-white active-press"
          >
            {layoutMode === 'grid' ? <List className="w-4 h-4" /> : <LayoutGrid className="w-4 h-4" />}
          </button>

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
          placeholder="Search videos, movies, series..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-[#111111] border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs font-sans text-white placeholder-white/40 focus:outline-none focus:border-white/30"
        />
      </div>

      {/* Tabs: Folders vs All Videos */}
      <div className="flex items-center gap-2 mb-4 overflow-x-auto no-scrollbar">
        <button
          onClick={() => {
            triggerHaptic('selection');
            setViewMode('all');
            setSelectedFolder(null);
          }}
          className={`px-4 py-1.5 rounded-full text-xs font-mono transition-all whitespace-nowrap active-press ${
            viewMode === 'all' && !selectedFolder
              ? 'bg-white text-black font-bold shadow-md shadow-white/10'
              : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
          }`}
        >
          All Videos ({videos.length})
        </button>

        <button
          onClick={() => {
            triggerHaptic('selection');
            setViewMode('folders');
            setSelectedFolder(null);
          }}
          className={`px-4 py-1.5 rounded-full text-xs font-mono transition-all whitespace-nowrap active-press ${
            viewMode === 'folders'
              ? 'bg-white text-black font-bold shadow-md shadow-white/10'
              : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
          }`}
        >
          Folders ({folderNames.length})
        </button>

        {folderNames.map((folder) => {
          const isSelected = selectedFolder === folder;
          return (
            <button
              key={folder}
              onClick={() => {
                triggerHaptic('selection');
                setSelectedFolder(folder);
                setViewMode('all');
              }}
              className={`px-3 py-1.5 rounded-full text-xs font-mono transition-all whitespace-nowrap active-press ${
                isSelected
                  ? 'bg-[#D71921] text-white font-bold glow-red'
                  : 'bg-white/5 text-white/60 hover:text-white border border-white/5'
              }`}
            >
              {folder} ({foldersMap[folder]?.length || 0})
            </button>
          );
        })}
      </div>

      {/* Folders Overview View */}
      {viewMode === 'folders' && !selectedFolder && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 mb-6">
          {folderNames.map((folderName) => {
            const folderVideos = foldersMap[folderName] || [];
            const previewVideo = folderVideos[0];
            return (
              <div
                key={folderName}
                onClick={() => {
                  triggerHaptic('medium');
                  setSelectedFolder(folderName);
                  setViewMode('all');
                }}
                className="flex items-center gap-3.5 p-3.5 rounded-2xl bg-[#0e0e0e] border border-white/10 hover:border-white/20 hover:bg-white/5 transition-all cursor-pointer group active-press"
              >
                {/* Folder Thumbnail Stack */}
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
                    <span className="px-1.5 py-0.5 rounded bg-white/10 text-white/80">
                      {folderVideos.length} Videos
                    </span>
                    <span>
                      {formatFileSize(folderVideos.reduce((sum, v) => sum + (v.size || 0), 0))}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Video Grid / List Cards */}
      {layoutMode === 'grid' ? (
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
                  <div className="absolute top-2 left-2 flex items-center gap-1.5">
                    {video.resolution && (
                      <span className="px-1.5 py-0.5 rounded bg-black/80 backdrop-blur-md text-[9px] font-mono text-white font-bold border border-white/10">
                        {video.resolution}
                      </span>
                    )}
                    <span className="px-1.5 py-0.5 rounded bg-[#D71921]/80 backdrop-blur-md text-[9px] font-mono text-white font-bold">
                      {video.format}
                    </span>
                  </div>

                  {/* Duration Badge Bottom Right */}
                  <div className="absolute bottom-2 right-2 px-2 py-0.5 rounded bg-black/85 backdrop-blur-md font-mono text-[10px] text-white font-bold border border-white/10">
                    {formatDuration(video.duration)}
                  </div>

                  {/* Red Playback Resume Progress Bar (Like MX Player) */}
                  {watchedPercent > 0 && (
                    <div className="absolute bottom-0 left-0 right-0 h-1 bg-white/20">
                      <div 
                        className="h-full bg-[#D71921] glow-red"
                        style={{ width: `${watchedPercent}%` }}
                      />
                    </div>
                  )}
                </div>

                {/* Video Info Body */}
                <div className="p-3 flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <h3 className="font-mono text-xs font-bold text-white line-clamp-2 group-hover:text-[#D71921] transition-colors">
                      {video.title}
                    </h3>
                    <div className="flex items-center gap-2 mt-1 text-[10px] font-mono text-white/50">
                      <span>{video.folder || 'Videos'}</span>
                      <span>•</span>
                      <span>{formatFileSize(video.size)}</span>
                      {watchedPercent > 0 && (
                        <>
                          <span>•</span>
                          <span className="text-[#D71921]">{Math.round(watchedPercent)}% watched</span>
                        </>
                      )}
                    </div>
                  </div>

                  {/* 3 Dots Menu Button */}
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
                  <div className="flex items-center gap-2 mt-1 text-[10px] font-mono text-white/50">
                    <span className="px-1.5 py-0.5 rounded bg-white/10 text-white/80">
                      {video.format}
                    </span>
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

      {/* Video Details Bottom Modal */}
      {infoModalVideo && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/80 backdrop-blur-md p-4 animate-fade-in">
          <div className="w-full max-w-md bg-[#111111] border border-white/15 rounded-3xl p-6 shadow-2xl flex flex-col gap-4">
            <div className="flex items-center justify-between border-b border-white/10 pb-3">
              <span className="font-dot text-sm text-white font-bold">VIDEO DETAILS & ACTIONS</span>
              <button onClick={() => setInfoModalVideo(null)} className="text-white/50 hover:text-white">✕</button>
            </div>

            <div className="flex flex-col gap-2 font-mono text-xs text-white/70">
              <div className="flex justify-between">
                <span>TITLE:</span>
                <span className="text-white font-bold line-clamp-1">{infoModalVideo.title}</span>
              </div>
              <div className="flex justify-between">
                <span>DURATION:</span>
                <span className="text-white">{formatDuration(infoModalVideo.duration)}</span>
              </div>
              <div className="flex justify-between">
                <span>SIZE:</span>
                <span className="text-white">{formatFileSize(infoModalVideo.size)}</span>
              </div>
              <div className="flex justify-between">
                <span>FORMAT / CODEC:</span>
                <span className="text-[#D71921]">{infoModalVideo.format} ({infoModalVideo.decoder || 'HW'})</span>
              </div>
              <div className="flex justify-between">
                <span>FOLDER:</span>
                <span className="text-white">{infoModalVideo.folder || 'Internal'}</span>
              </div>
            </div>

            <div className="flex flex-col gap-2 mt-2">
              <button
                onClick={() => {
                  triggerHaptic('heavy');
                  onPlayVideo(infoModalVideo);
                  setInfoModalVideo(null);
                }}
                className="py-2.5 rounded-xl bg-white text-black font-mono text-xs font-bold hover:bg-white/90 active-press flex items-center justify-center gap-2"
              >
                <Play className="w-4 h-4 fill-black" />
                PLAY IN NOTHING PLAYER
              </button>

              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => {
                    triggerHaptic('medium');
                    openNativePlayer(infoModalVideo.path, (infoModalVideo as any).contentUri);
                    setInfoModalVideo(null);
                  }}
                  className="py-2 rounded-xl bg-white/10 text-white font-mono text-xs font-bold border border-white/20 hover:bg-white/20 active-press"
                >
                  OPEN IN VLC / SYSTEM
                </button>
                <button
                  onClick={() => {
                    triggerHaptic('heavy');
                    onDeleteVideo(infoModalVideo.id);
                    setInfoModalVideo(null);
                  }}
                  className="py-2 rounded-xl bg-red-600/20 text-red-400 border border-red-500/30 font-mono text-xs font-bold hover:bg-red-600/30 active-press"
                >
                  DELETE
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
