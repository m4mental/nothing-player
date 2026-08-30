import type { MediaItem, RadioStation } from '../types/media';

export const DEMO_MEDIA_ITEMS: MediaItem[] = [
  {
    id: 'demo_track_1',
    title: 'NOTHING GLYPH RHYTHM',
    artist: 'Carl Pei & The Glyphs',
    album: 'Nothing Phone (3) Soundscapes',
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
    duration: 210,
    url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
    type: 'audio',
    format: 'WAV',
    thumbnail: 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80',
    addedAt: Date.now() - 3600000 * 10,
    isFavorite: false
  },
  {
    id: 'demo_video_1',
    title: 'BIG BUCK BUNNY (CINEMA 4K HDR)',
    artist: 'Blender Open Movie Studio',
    album: 'Open Source Cinema Showcase',
    duration: 596,
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
    type: 'video',
    format: 'MP4 (H.264)',
    thumbnail: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80',
    subtitlesContent: `1
00:00:02,000 --> 00:00:08,000
NOTHING PLAYER (01) CINEMA SUITE

2
00:00:09,000 --> 00:00:15,000
Swipe Left: Brightness | Swipe Right: Volume (200%)

3
00:00:16,000 --> 00:00:22,000
Horizontal Swipe: High Precision Seeking

4
00:00:25,000 --> 00:00:32,000
Try Retro CRT Scanline filter or Nothing Red Duotone mode!`,
    subtitlesName: 'English (Demo Subtitles)',
    addedAt: Date.now() - 3600000 * 24,
    isFavorite: true
  },
  {
    id: 'demo_video_2',
    title: 'TEARS OF STEEL (SCI-FI 4K)',
    artist: 'Blender VFX Studio',
    album: 'Future Cybernetics',
    duration: 734,
    url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4',
    type: 'video',
    format: 'MKV / MP4',
    thumbnail: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80',
    addedAt: Date.now() - 3600000 * 48,
    isFavorite: true
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
