import { Haptics, ImpactStyle } from '@capacitor/haptics';

export async function triggerHaptic(style: 'light' | 'medium' | 'heavy' | 'selection' = 'light') {
  try {
    if (style === 'selection') {
      await Haptics.selectionStart();
      await Haptics.selectionChanged();
    } else {
      const impact = style === 'heavy' 
        ? ImpactStyle.Heavy 
        : style === 'medium' 
          ? ImpactStyle.Medium 
          : ImpactStyle.Light;
      await Haptics.impact({ style: impact });
    }
  } catch {
    // Fallback to Web Vibration API if on mobile browser
    if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      const duration = style === 'heavy' ? 40 : style === 'medium' ? 25 : 12;
      navigator.vibrate(duration);
    }
  }
}
