export type TrackType = 'music' | 'recording';

export type BuiltInAudioKey =
  | 'homayoun'
  | 'homayoun_playback'
  | 'shur'
  | 'shur_playback';

export interface MediaTrack {
  trackId: string;
  songId: string;
  title: string;
  audioId: string;
  fileName?: string;
  trackType: TrackType;
  builtInAudioKey?: BuiltInAudioKey | null;
  durationSec?: number;
  displayTimestamp?: string;
  createdAt: number;
}

export interface Category {
  id: string;
  name: string;
  createdAt: number;
}

export interface Song {
  id: string;
  title: string;
  lyrics: string;
  categoryId?: string | null;
  tracks: MediaTrack[];
  fontSize: number;
  createdAt: number;
}

export type PersianFontId = 'vazirmatn' | 'nastaliq' | 'sahel';

export interface PersianFontOption {
  id: PersianFontId;
  shortLabel: string;
  fullLabel: string;
  cssClass: string;
  lineHeight: number;
}

export const PERSIAN_FONT_OPTIONS: PersianFontOption[] = [
  {
    id: 'vazirmatn',
    shortLabel: 'فونت وزیر',
    fullLabel: 'فونت وزیر (مدرن و خوانا)',
    cssClass: 'font-persian-vazirmatn',
    lineHeight: 1.95
  },
  {
    id: 'nastaliq',
    shortLabel: 'فونت نستعلیق',
    fullLabel: 'فونت نستعلیق (مناسب شعر سنتی و آواز)',
    cssClass: 'font-persian-nastaliq',
    lineHeight: 2.55
  },
  {
    id: 'sahel',
    shortLabel: 'فونت ساحل',
    fullLabel: 'فونت ساحل (نرم و خواندنی)',
    cssClass: 'font-persian-sahel',
    lineHeight: 2.0
  }
];

export const PLAYBACK_SPEEDS = [0.75, 0.9, 1.0, 1.5] as const;

export function formatPlaybackSpeedLabel(speed: number): string {
  if (Math.abs(speed - 0.75) < 0.01) return '0.75x';
  if (Math.abs(speed - 0.9) < 0.01) return '0.9x';
  if (Math.abs(speed - 1.5) < 0.01) return '1.5x';
  return '1.0x';
}

export const MIN_FONT_SIZE = 16;
export const MAX_FONT_SIZE = 40;
export const DEFAULT_FONT_SIZE = 20;
export const FONT_STEP = 2;

export function toPersianDigits(value: number | string): string {
  const digits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
  return String(value).replace(/\d/g, (d) => digits[Number(d)]);
}

export function getNextMusicTrackTitle(existingMusicCount: number): string {
  const nextIndex = Math.max(1, existingMusicCount + 1);
  return `آهنگ ${toPersianDigits(nextIndex)}`;
}

export function getNextRecordingTrackTitle(existingRecordingCount: number): string {
  const nextIndex = Math.max(1, existingRecordingCount + 1);
  return `ضبط صدا ${toPersianDigits(nextIndex)}`;
}

export function formatRecordingTimestamp(date: Date = new Date()): string {
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${toPersianDigits(hours)}:${toPersianDigits(minutes)} · ${toPersianDigits(month)}/${toPersianDigits(day)}`;
}

export function getFontOption(fontId: string | null | undefined): PersianFontOption {
  return PERSIAN_FONT_OPTIONS.find((opt) => opt.id === fontId) ?? PERSIAN_FONT_OPTIONS[0];
}

export function clampFontSize(size: number): number {
  if (!Number.isFinite(size)) return DEFAULT_FONT_SIZE;
  return Math.min(MAX_FONT_SIZE, Math.max(MIN_FONT_SIZE, Math.round(size)));
}

export function formatTimeSeconds(totalSeconds: number): string {
  if (!Number.isFinite(totalSeconds) || totalSeconds < 0) {
    return '00:00';
  }
  const rounded = Math.floor(totalSeconds);
  const minutes = Math.floor(rounded / 60);
  const seconds = rounded % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export function computeSeekTime(
  currentSec: number,
  deltaSec: number,
  durationSec: number
): number {
  const safeDuration = Number.isFinite(durationSec) && durationSec > 0 ? durationSec : 0;
  const target = (Number.isFinite(currentSec) ? currentSec : 0) + deltaSec;
  if (safeDuration === 0) {
    return Math.max(0, target);
  }
  return Math.min(safeDuration, Math.max(0, target));
}

export function filterSongs(
  songs: Song[],
  query: string,
  categoryId: string | null = null
): Song[] {
  const trimmed = query.trim().toLowerCase();
  return songs.filter((song) => {
    const matchesCategory = !categoryId || song.categoryId === categoryId;
    const matchesQuery =
      !trimmed ||
      song.title.toLowerCase().includes(trimmed) ||
      song.lyrics.toLowerCase().includes(trimmed);
    return matchesCategory && matchesQuery;
  });
}

export function validateCategoryName(
  rawName: string,
  existingCategories: Category[]
): string | null {
  const trimmed = rawName.trim();
  if (trimmed.length === 0) {
    return 'لطفاً نام دسته‌بندی را وارد کنید.';
  }
  const duplicate = existingCategories.some(
    (c) => c.name.trim().toLowerCase() === trimmed.toLowerCase()
  );
  if (duplicate) {
    return 'این دسته‌بندی قبلاً ساخته شده است.';
  }
  return null;
}

export function getLyricsPreview(lyrics: string, maxLines = 2): string {
  return lyrics
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .slice(0, maxLines)
    .join('\n');
}

export function validateSongForm(title: string, lyrics: string): string | null {
  if (title.trim().length === 0) {
    return 'لطفاً عنوان شعر را وارد کنید.';
  }
  if (lyrics.trim().length === 0) {
    return 'لطفاً متن شعر را وارد کنید.';
  }
  return null;
}

/**
 * Generates a gentle, plucked-string Persian classical rehearsal melody as a standard
 * 16-bit mono PCM WAV byte buffer so seeded poems have real, playable, seekable tracks.
 */
export function createRehearsalToneWavBytes(
  mode: BuiltInAudioKey,
  durationSec = 24
): Uint8Array {
  const sampleRate = 22050;
  const numSamples = Math.floor(sampleRate * durationSec);
  const dataSize = numSamples * 2;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  const writeAscii = (offset: number, str: string) => {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i));
    }
  };

  writeAscii(0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeAscii(8, 'WAVE');
  writeAscii(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeAscii(36, 'data');
  view.setUint32(40, dataSize, true);

  const isHomayoun = mode === 'homayoun' || mode === 'homayoun_playback';
  const isPlayback = mode === 'homayoun_playback' || mode === 'shur_playback';

  const scale = isHomayoun
    ? [293.66, 311.13, 369.99, 392.0, 440.0, 466.16, 523.25, 440.0, 392.0, 369.99, 311.13, 293.66]
    : [293.66, 320.24, 349.23, 392.0, 440.0, 466.16, 523.25, 466.16, 440.0, 392.0, 349.23, 293.66];

  const noteDurationSec = isPlayback ? 2.5 : 2.0;
  for (let i = 0; i < numSamples; i++) {
    const t = i / sampleRate;
    const noteIndex = Math.floor(t / noteDurationSec) % scale.length;
    const noteTime = t % noteDurationSec;
    const baseFreq = scale[noteIndex];
    const freq = isPlayback ? baseFreq * 0.5 : baseFreq;

    const attack = Math.min(1, noteTime / 0.03);
    const decay = Math.exp(-noteTime * (isPlayback ? 1.5 : 2.2));
    const envelope = attack * decay;

    const sample =
      (Math.sin(2 * Math.PI * freq * t) * 0.65 +
        Math.sin(2 * Math.PI * freq * 2 * t) * 0.25 +
        Math.sin(2 * Math.PI * freq * 3 * t) * 0.1) *
      envelope *
      0.32;

    const int16 = Math.max(-32768, Math.min(32767, Math.round(sample * 32767)));
    view.setInt16(44 + i * 2, int16, true);
  }

  return new Uint8Array(buffer);
}

export const INITIAL_SEED_CATEGORIES: Category[] = [
  {
    id: 'cat-hafez',
    name: 'شعر حافظ',
    createdAt: 1731600000000
  },
  {
    id: 'cat-classic',
    name: 'اشعار کلاسیک',
    createdAt: 1731600001000
  }
];

export const INITIAL_SEED_SONGS: Song[] = [
  {
    id: 'seed-1',
    title: 'الا یا ایها الساقی (حافظ)',
    categoryId: 'cat-hafez',
    lyrics: [
      'الا یا ایها الساقی ادر کاساً و ناولها',
      'که عشق آسان نمود اول ولی افتاد مشکل‌ها',
      '',
      'به بوی نافه‌ای کاخر صبا زان طره بگشاید',
      'ز تاب جعد مشکینش چه خون افتاد در دل‌ها',
      '',
      'مرا در منزل جانان چه امن عیش چون هر دم',
      'جرس فریاد می‌دارد که بربندید محمل‌ها',
      '',
      'شب تاریک و بیم موج و گردابی چنین هایل',
      'کجا دانند حال ما سبکباران ساحل‌ها'
    ].join('\n'),
    tracks: [
      {
        trackId: 'track-1a',
        songId: 'seed-1',
        title: 'آهنگ ۱',
        fileName: 'مایه همایون (آهنگ مرجع)',
        audioId: 'builtin-homayoun',
        trackType: 'music',
        builtInAudioKey: 'homayoun',
        durationSec: 24,
        createdAt: 1731600000000
      },
      {
        trackId: 'track-1b',
        songId: 'seed-1',
        title: 'آهنگ ۲',
        fileName: 'پلی‌بک بی‌کلام همایون',
        audioId: 'builtin-homayoun-pb',
        trackType: 'music',
        builtInAudioKey: 'homayoun_playback',
        durationSec: 24,
        createdAt: 1731600001000
      }
    ],
    fontSize: 22,
    createdAt: 1731600000000
  },
  {
    id: 'seed-2',
    title: 'بوی جوی مولیان (رودکی)',
    categoryId: 'cat-classic',
    lyrics: [
      'بوی جوی مولیان آید همی',
      'یاد یار مهربان آید همی',
      '',
      'ریگ آموی و درشتی راه او',
      'زیر پایم پرنیان آید همی',
      '',
      'آب جیحون از نشاط روی دوست',
      'خنگ ما را تا میان آید همی',
      '',
      'ای بخارا شاد باش و دیر زی',
      'میر زی تو شادمان آید همی'
    ].join('\n'),
    tracks: [
      {
        trackId: 'track-2a',
        songId: 'seed-2',
        title: 'آهنگ ۱',
        fileName: 'مایه شور (آهنگ مرجع)',
        audioId: 'builtin-shur',
        trackType: 'music',
        builtInAudioKey: 'shur',
        durationSec: 24,
        createdAt: 1731599000000
      },
      {
        trackId: 'track-2b',
        songId: 'seed-2',
        title: 'آهنگ ۲',
        fileName: 'پلی‌بک بی‌کلام شور',
        audioId: 'builtin-shur-pb',
        trackType: 'music',
        builtInAudioKey: 'shur_playback',
        durationSec: 24,
        createdAt: 1731599001000
      }
    ],
    fontSize: 22,
    createdAt: 1731599000000
  },
  {
    id: 'seed-3',
    title: 'بشنو از نی چون حکایت می‌کند (مولانا)',
    categoryId: 'cat-classic',
    lyrics: [
      'بشنو از نی چون حکایت می‌کند',
      'از جدایی‌ها شکایت می‌کند',
      '',
      'کز نیستان تا مرا ببریده‌اند',
      'در نفیرم مرد و زن نالیده‌اند',
      '',
      'سینه خواهم شرحه شرحه از فراق',
      'تا بگویم شرح درد اشتیاق'
    ].join('\n'),
    tracks: [],
    fontSize: 20,
    createdAt: 1731598000000
  }
];
