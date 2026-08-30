export const EQ_FREQUENCIES = [32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];

export const EQ_PRESETS = [
  { name: 'NOTHING PUNCH', gains: [4, 5, 3, 1, -1, 0, 2, 4, 5, 5] },
  { name: 'FLAT / STUDIO', gains: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { name: 'BASS BOOST', gains: [7, 6, 4, 2, 0, 0, 0, 0, 0, 0] },
  { name: 'ELECTRONIC', gains: [5, 4, 1, 0, -2, 2, 1, 3, 4, 5] },
  { name: 'VOCAL / PODCAST', gains: [-2, -2, -1, 1, 3, 4, 3, 1, 0, -1] },
  { name: 'ROCK / METAL', gains: [5, 3, 2, 0, -1, 1, 3, 4, 5, 5] },
  { name: 'ACOUSTIC', gains: [3, 2, 1, 1, 2, 2, 3, 3, 2, 1] },
  { name: 'NOTHING GLYPH', gains: [6, 4, 2, -1, -2, 1, 3, 5, 6, 6] }
];

class AudioEngine {
  private ctx: AudioContext | null = null;
  private sourceNode: MediaElementAudioSourceNode | null = null;
  private currentElement: HTMLMediaElement | null = null;
  private preampGain: GainNode | null = null;
  private filters: BiquadFilterNode[] = [];
  private bassBoostFilter: BiquadFilterNode | null = null;
  private compressor: DynamicsCompressorNode | null = null;
  private masterGain: GainNode | null = null;
  private analyser: AnalyserNode | null = null;
  private initialized = false;

  public init(mediaElement: HTMLMediaElement) {
    if (this.currentElement === mediaElement && this.initialized) return;

    try {
      const AudioCtxClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!this.ctx) {
        this.ctx = new AudioCtxClass();
      }

      if (this.ctx.state === 'suspended') {
        this.ctx.resume();
      }

      if (this.sourceNode) {
        try {
          this.sourceNode.disconnect();
        } catch {
          // ignore disconnect error
        }
      }

      this.currentElement = mediaElement;
      this.sourceNode = this.ctx.createMediaElementSource(mediaElement);

      // Preamp Gain
      this.preampGain = this.ctx.createGain();
      this.preampGain.gain.value = 1.0;

      // 10-Band EQ Filters
      this.filters = EQ_FREQUENCIES.map((freq, index) => {
        const filter = this.ctx!.createBiquadFilter();
        if (index === 0) {
          filter.type = 'lowshelf';
        } else if (index === EQ_FREQUENCIES.length - 1) {
          filter.type = 'highshelf';
        } else {
          filter.type = 'peaking';
          filter.Q.value = 1.4;
        }
        filter.frequency.value = freq;
        filter.gain.value = 0;
        return filter;
      });

      // Dedicated Bass Boost Sub-filter
      this.bassBoostFilter = this.ctx.createBiquadFilter();
      this.bassBoostFilter.type = 'lowshelf';
      this.bassBoostFilter.frequency.value = 80;
      this.bassBoostFilter.gain.value = 0;

      // Dynamics Compressor (Limiter against clipping during 200% boost)
      this.compressor = this.ctx.createDynamicsCompressor();
      this.compressor.threshold.setValueAtTime(-3, this.ctx.currentTime);
      this.compressor.knee.setValueAtTime(4, this.ctx.currentTime);
      this.compressor.ratio.setValueAtTime(12, this.ctx.currentTime);
      this.compressor.attack.setValueAtTime(0.003, this.ctx.currentTime);
      this.compressor.release.setValueAtTime(0.25, this.ctx.currentTime);

      // Master Gain for Audio Boost (0% to 200%)
      this.masterGain = this.ctx.createGain();
      this.masterGain.gain.value = 1.0;

      // FFT Analyser for Realtime Glyph Lighting & Waveforms
      this.analyser = this.ctx.createAnalyser();
      this.analyser.fftSize = 256;
      this.analyser.smoothingTimeConstant = 0.8;

      // Chain: Source -> Preamp -> Filters[0..9] -> BassBoost -> Compressor -> MasterGain -> Analyser -> Destination
      let lastNode: AudioNode = this.sourceNode;
      lastNode.connect(this.preampGain);
      lastNode = this.preampGain;

      for (const filter of this.filters) {
        lastNode.connect(filter);
        lastNode = filter;
      }

      lastNode.connect(this.bassBoostFilter);
      lastNode = this.bassBoostFilter;

      lastNode.connect(this.compressor);
      lastNode = this.compressor;

      lastNode.connect(this.masterGain);
      lastNode = this.masterGain;

      lastNode.connect(this.analyser);
      this.analyser.connect(this.ctx.destination);

      this.initialized = true;
    } catch (e) {
      console.warn('AudioEngine Web Audio initialization fallback:', e);
    }
  }

  public resume() {
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  public setPreamp(gainDb: number) {
    if (!this.preampGain || !this.ctx) return;
    const gainValue = Math.pow(10, gainDb / 20);
    this.preampGain.gain.setTargetAtTime(gainValue, this.ctx.currentTime, 0.05);
  }

  public setBandGain(index: number, gainDb: number) {
    if (!this.filters[index] || !this.ctx) return;
    this.filters[index].gain.setTargetAtTime(gainDb, this.ctx.currentTime, 0.05);
  }

  public setAllBands(gains: number[]) {
    gains.forEach((gain, i) => this.setBandGain(i, gain));
  }

  public setBassBoost(gainDb: number) {
    if (!this.bassBoostFilter || !this.ctx) return;
    this.bassBoostFilter.gain.setTargetAtTime(gainDb, this.ctx.currentTime, 0.05);
  }

  public setVolumeBoost(volumeMultiplier: number) {
    // 0.0 to 2.0 (representing 0% to 200%)
    if (!this.masterGain || !this.ctx) return;
    this.masterGain.gain.setTargetAtTime(volumeMultiplier, this.ctx.currentTime, 0.05);
  }

  public getFrequencyData(array: Uint8Array): void {
    if (this.analyser) {
      this.analyser.getByteFrequencyData(array as unknown as Uint8Array<ArrayBuffer>);
    } else {
      array.fill(0);
    }
  }

  public getTimeDomainData(array: Uint8Array): void {
    if (this.analyser) {
      this.analyser.getByteTimeDomainData(array as unknown as Uint8Array<ArrayBuffer>);
    } else {
      array.fill(128);
    }
  }
}

export const audioEngine = new AudioEngine();
