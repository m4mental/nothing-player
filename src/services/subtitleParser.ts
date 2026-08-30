import type { SubtitleCue } from '../types/media';

function timeToSeconds(timeStr: string): number {
  const parts = timeStr.trim().replace(',', '.').split(':');
  if (parts.length === 3) {
    const hours = parseFloat(parts[0]);
    const minutes = parseFloat(parts[1]);
    const seconds = parseFloat(parts[2]);
    return hours * 3600 + minutes * 60 + seconds;
  } else if (parts.length === 2) {
    const minutes = parseFloat(parts[0]);
    const seconds = parseFloat(parts[1]);
    return minutes * 60 + seconds;
  }
  return 0;
}

export function parseSubtitles(content: string, offsetSeconds = 0): SubtitleCue[] {
  if (!content) return [];
  
  // Normalize line endings
  const normalized = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
  const blocks = normalized.split(/\n\s*\n/);
  const cues: SubtitleCue[] = [];
  let counter = 1;

  for (const block of blocks) {
    const lines = block.trim().split('\n');
    if (lines.length < 2) continue;

    // Find line with timestamp arrow '-->'
    let timeLineIdx = -1;
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].includes('-->')) {
        timeLineIdx = i;
        break;
      }
    }

    if (timeLineIdx === -1) continue;

    const timeParts = lines[timeLineIdx].split('-->');
    if (timeParts.length === 2) {
      const startTime = timeToSeconds(timeParts[0]) + offsetSeconds;
      const endTime = timeToSeconds(timeParts[1].split(' ')[0]) + offsetSeconds;
      const textLines = lines.slice(timeLineIdx + 1);
      const text = textLines.join('\n').replace(/<[^>]*>/g, '').trim();

      if (text && startTime >= 0) {
        cues.push({
          id: counter++,
          startTime,
          endTime,
          text
        });
      }
    }
  }

  return cues.sort((a, b) => a.startTime - b.startTime);
}

export function getActiveSubtitle(cues: SubtitleCue[], currentTime: number): string | null {
  if (!cues || cues.length === 0) return null;
  const match = cues.find(cue => currentTime >= cue.startTime && currentTime <= cue.endTime);
  return match ? match.text : null;
}
