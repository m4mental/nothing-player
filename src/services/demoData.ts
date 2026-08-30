import type { MediaItem, RadioStation } from '../types/media';

// No fake mock files - Pure local device media player
export const DEMO_MEDIA_ITEMS: MediaItem[] = [];

export const RADIO_STATIONS: RadioStation[] = [
  {
    id: 'radio_1',
    name: 'BBC Radio 1',
    genre: 'Top 40 / Dance',
    country: 'United Kingdom',
    streamUrl: 'https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one',
    coverArt: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=200&auto=format&fit=crop&q=80',
    bitrate: '128 kbps',
    description: 'The best new music and live events from the BBC.'
  },
  {
    id: 'radio_2',
    name: 'Radio Mirchi 98.3 FM',
    genre: 'Bollywood Hits',
    country: 'India',
    streamUrl: 'https://stream-160.zeno.fm/f3wvbbqmdg8uv',
    coverArt: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=200&auto=format&fit=crop&q=80',
    bitrate: '128 kbps',
    description: 'Hot Bollywood hits and Hindi chartbusters.'
  },
  {
    id: 'radio_3',
    name: 'Defected Radio House',
    genre: 'Deep & Soulful House',
    country: 'Global',
    streamUrl: 'https://icecast.defected.com/defected',
    coverArt: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=200&auto=format&fit=crop&q=80',
    bitrate: '192 kbps',
    description: 'The finest in house music curated weekly.'
  },
  {
    id: 'radio_4',
    name: 'Chillhop Lo-Fi Beats',
    genre: 'Lo-Fi / Instrumental',
    country: 'Netherlands',
    streamUrl: 'https://stream.zeno.fm/f3wvbbqmdg8uv',
    coverArt: 'https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=200&auto=format&fit=crop&q=80',
    bitrate: '320 kbps',
    description: 'Relaxing beats to study, relax and focus to.'
  }
];
