import type { MediaItem, RadioStation } from '../types/media';

export const DEMO_MEDIA_ITEMS: MediaItem[] = [
  {
    id: 'demo_video_1',
    title: 'Anbe Diana (2026) SLiv WEBRip',
    artist: 'Cinema Originals',
    album: 'Movies',
    folder: 'Movies',
    duration: 8392, // 2h 19m
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    type: 'video',
    format: 'MKV',
    resolution: '1080p FHD',
    decoder: 'HW',
    thumbnail: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80',
    lastPosition: 3005, // 50m watched (shows progress bar on thumbnail)
    size: 3242000000,
    subtitlesContent: `1
00:00:02,000 --> 00:00:08,000
NOTHING PLAYER - MX STYLE CINEMA DECODER

2
00:00:09,000 --> 00:00:15,000
Swipe Left: Brightness | Swipe Right: Volume (200% Boost)

3
00:00:16,000 --> 00:00:22,000
Horizontal Swipe: High Precision Seeking

4
00:00:25,000 --> 00:00:32,000
HW/SW Decoder & Screen Padlock Active`,
    subtitlesName: 'English (Default)',
    addedAt: Date.now() - 3600000 * 5,
    isFavorite: true
  },
  {
    id: 'demo_video_2',
    title: 'Toxic A Fairy Tale for Grown-ups',
    artist: 'Action Studios',
    album: 'Movies',
    folder: 'Movies',
    duration: 9420,
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
    type: 'video',
    format: 'MP4',
    resolution: '4K UHD',
    decoder: 'HW',
    thumbnail: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80',
    lastPosition: 1200,
    size: 7730000000,
    addedAt: Date.now() - 3600000 * 12,
    isFavorite: true
  },
  {
    id: 'demo_video_3',
    title: 'Awarapan 2 (Hindi Dubbed HD)',
    artist: 'Bollywood Action',
    album: 'Downloads',
    folder: 'Downloads',
    duration: 7200,
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
    type: 'video',
    format: 'MKV',
    resolution: '1080p',
    decoder: 'HW',
    thumbnail: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&auto=format&fit=crop&q=80',
    lastPosition: 0,
    size: 1513000000,
    addedAt: Date.now() - 3600000 * 24,
    isFavorite: false
  },
  {
    id: 'demo_video_4',
    title: 'Camera Roll - Night Drive 4K HDR',
    artist: 'Nothing Camera',
    album: 'Camera',
    folder: 'Camera',
    duration: 345,
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    type: 'video',
    format: 'MP4',
    resolution: '4K 60FPS',
    decoder: 'HW',
    thumbnail: 'https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80',
    lastPosition: 120,
    size: 640000000,
    addedAt: Date.now() - 3600000 * 48,
    isFavorite: true
  },
  {
    id: 'demo_video_5',
    title: 'VID_20260830_WA0012.mp4',
    artist: 'WhatsApp Media',
    album: 'WhatsApp Video',
    folder: 'WhatsApp Video',
    duration: 184,
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    type: 'video',
    format: 'MP4',
    resolution: '720p',
    decoder: 'SW',
    thumbnail: 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80',
    lastPosition: 0,
    size: 45000000,
    addedAt: Date.now() - 3600000 * 72,
    isFavorite: false
  },
  {
    id: 'demo_track_1',
    title: 'NOTHING GLYPH RHYTHM',
    artist: 'Carl Pei & The Glyphs',
    album: 'Nothing Phone (2a) Soundscapes',
    folder: 'Music / Nothing Audio',
    duration: 165,
    url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
    type: 'audio',
    format: 'MP3',
    thumbnail: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop&q=80',
    lyrics: `[00:01.00]Nothing Player (01) Initializing...
[00:06.00]Glyph Matrix reactive lighting engaged
[00:12.00]Feel the pure analog punch and digital precision
[00:20.00]Monochromatic dark aesthetics, red line indicator
[00:30.00]10-Band Equalizer active across all spectrums
[00:45.00]Bass boost vibrating through the chassis
[01:00.00]VLC Grade multi-format engine running silky smooth
[01:20.00]Nothing tech: Minimalist hardware, maximum soul
[01:40.00]Dual hub: Music deck and Video cinema suite
[02:00.00]Pure audio fidelity delivered to your device
[02:30.00]End of transmission - Nothing Player`,
    addedAt: Date.now() - 3600000 * 2,
    isFavorite: true
  },
  {
    id: 'demo_track_2',
    title: 'CYBERPUNK NEON DRIFT',
    artist: 'Kavinsky Resonance',
    album: 'Night City Overdrive',
    folder: 'Downloads / Audio',
    duration: 180,
    url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
    type: 'audio',
    format: 'FLAC',
    thumbnail: 'https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80',
    lyrics: `[00:02.00]Synthesizers booting up in the dark
[00:15.00]Speeding down the rain-slick highway
[00:32.00]Bass frequencies kicking through the matrix
[00:55.00]High notes ringing clear through the night
[01:25.00]Nothing Phone Glyph LED strips glowing bright
[01:55.00]Keep the rhythm pulsing in your ears`,
    addedAt: Date.now() - 3600000 * 5,
    isFavorite: true
  },
  {
    id: 'demo_track_3',
    title: 'LO-FI GLYPH AMBIENCE',
    artist: 'Tape Cassette Echoes',
    album: 'Late Night Chill',
    folder: 'Music / Lo-Fi',
    duration: 210,
    url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
    type: 'audio',
    format: 'WAV',
    thumbnail: 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80',
    addedAt: Date.now() - 3600000 * 10,
    isFavorite: false
  }
];

export const DEMO_RADIO_STATIONS: RadioStation[] = [
  {
    id: 'radio_synthwave',
    name: 'NIGHTWAVE PLAZA',
    genre: 'Synthwave / Vaporwave',
    streamUrl: 'https://radio.plaza.one/mp3',
    bitrate: '128 kbps',
    country: 'GLOBAL',
    coverArt: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop&q=80',
    description: 'Vaporwave, Future Funk & Cyberpunk aesthetic broadcast 24/7'
  },
  {
    id: 'radio_lofi',
    name: 'LOFI CHILLOUT LOUNGE',
    genre: 'Lo-Fi Hip Hop / Chill',
    streamUrl: 'https://stream.zeno.fm/f3wvbbqmdg8uv',
    bitrate: '192 kbps',
    country: 'GLOBAL',
    coverArt: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80',
    description: 'Relaxing beats to study, code, and chill'
  },
  {
    id: 'radio_techno',
    name: 'BERLIN UNDERGROUND TECHNO',
    genre: 'Industrial Techno',
    streamUrl: 'https://stream.sunshine-live.de/techno/mp3-192/',
    bitrate: '192 kbps',
    country: 'GERMANY',
    coverArt: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80',
    description: 'Raw deep bass and electronic beats tailored for Nothing Glyph lighting'
  },
  {
    id: 'radio_ambient',
    name: 'DEEP SPACE AMBIENT',
    genre: 'Space Ambient / Drone',
    streamUrl: 'https://ice1.somafm.com/dronezone-128-mp3',
    bitrate: '128 kbps',
    country: 'USA',
    coverArt: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&auto=format&fit=crop&q=80',
    description: 'Atmospheric sonic landscapes and cosmic textures'
  }
];
