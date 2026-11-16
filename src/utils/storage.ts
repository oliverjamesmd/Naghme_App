import {
  Category,
  createRehearsalToneWavBytes,
  getNextMusicTrackTitle,
  getNextRecordingTrackTitle,
  INITIAL_SEED_CATEGORIES,
  INITIAL_SEED_SONGS,
  MediaTrack,
  PersianFontId,
  Song
} from './naghmehLogic';

const SONGS_STORAGE_KEY = 'naghmeh_songs_v3';
const PREV_STORAGE_KEY_V2 = 'naghmeh_songs_v2';
const LEGACY_STORAGE_KEY_V1 = 'naghmeh_songs_v1';
const CATEGORIES_STORAGE_KEY = 'naghmeh_categories_v1';
const FONT_STORAGE_KEY = 'naghmeh_font_v1';
const THEME_STORAGE_KEY = 'naghmeh_dark_theme_v1';
const AUDIO_DB_NAME = 'naghmeh_audio_db';
const AUDIO_STORE_NAME = 'audio_files';

export function loadCategoriesFromStorage(): Category[] {
  try {
    const raw = window.localStorage.getItem(CATEGORIES_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed;
    }
    window.localStorage.setItem(CATEGORIES_STORAGE_KEY, JSON.stringify(INITIAL_SEED_CATEGORIES));
    return INITIAL_SEED_CATEGORIES;
  } catch {
    return INITIAL_SEED_CATEGORIES;
  }
}

export function saveCategoriesToStorage(categories: Category[]): void {
  try {
    window.localStorage.setItem(CATEGORIES_STORAGE_KEY, JSON.stringify(categories));
  } catch {
    // Safe fallback
  }
}

export function loadThemeFromStorage(): boolean {
  try {
    return window.localStorage.getItem(THEME_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

export function saveThemeToStorage(isDark: boolean): void {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, String(isDark));
  } catch {
    // Safe fallback
  }
}

export function loadFontFromStorage(): PersianFontId {
  try {
    const val = window.localStorage.getItem(FONT_STORAGE_KEY);
    if (val === 'vazirmatn' || val === 'nastaliq' || val === 'sahel') {
      return val;
    }
    return 'vazirmatn';
  } catch {
    return 'vazirmatn';
  }
}

export function saveFontToStorage(fontId: PersianFontId): void {
  try {
    window.localStorage.setItem(FONT_STORAGE_KEY, fontId);
  } catch {
    // Safe fallback
  }
}

function normalizeTrackList(rawTracks: unknown[], songId: string): MediaTrack[] {
  let musicCount = 0;
  let recCount = 0;

  return rawTracks.map((item, idx) => {
    const raw = item as Record<string, unknown>;
    const isRecording = raw.trackType === 'recording';
    const nextTitle = isRecording
      ? getNextRecordingTrackTitle(recCount++)
      : getNextMusicTrackTitle(musicCount++);

    const existingTitle = typeof raw.title === 'string' ? raw.title.trim() : '';
    const useExistingTitle =
      existingTitle &&
      existingTitle !== 'آهنگ اصلی' &&
      existingTitle !== 'پلیبک بیکلام';

    return {
      trackId: typeof raw.trackId === 'string' ? raw.trackId : `migrated-${songId}-${idx}`,
      songId,
      title: useExistingTitle ? existingTitle : nextTitle,
      audioId: typeof raw.audioId === 'string' ? raw.audioId : '',
      fileName: typeof raw.fileName === 'string' ? raw.fileName : existingTitle || undefined,
      trackType: isRecording ? 'recording' : 'music',
      builtInAudioKey: (raw.builtInAudioKey as MediaTrack['builtInAudioKey']) ?? null,
      durationSec: typeof raw.durationSec === 'number' ? raw.durationSec : undefined,
      displayTimestamp: typeof raw.displayTimestamp === 'string' ? raw.displayTimestamp : undefined,
      createdAt: typeof raw.createdAt === 'number' ? raw.createdAt : Date.now()
    };
  });
}

export function loadSongsFromStorage(): Song[] {
  try {
    const raw = window.localStorage.getItem(SONGS_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed;
    }

    const v2Raw = window.localStorage.getItem(PREV_STORAGE_KEY_V2);
    if (v2Raw) {
      const v2Parsed = JSON.parse(v2Raw);
      if (Array.isArray(v2Parsed)) {
        const migrated: Song[] = v2Parsed.map((item) => {
          const id = String(item.id ?? `song-${Date.now()}`);
          const tracks = Array.isArray(item.tracks)
            ? normalizeTrackList(item.tracks, id)
            : [];
          return {
            id,
            title: String(item.title ?? ''),
            lyrics: String(item.lyrics ?? ''),
            categoryId: typeof item.categoryId === 'string' ? item.categoryId : null,
            tracks,
            fontSize: Number(item.fontSize ?? 20),
            createdAt: Number(item.createdAt ?? Date.now())
          };
        });
        window.localStorage.setItem(SONGS_STORAGE_KEY, JSON.stringify(migrated));
        return migrated;
      }
    }

    const legacyRaw = window.localStorage.getItem(LEGACY_STORAGE_KEY_V1);
    if (legacyRaw) {
      const legacyParsed = JSON.parse(legacyRaw);
      if (Array.isArray(legacyParsed)) {
        const migrated: Song[] = legacyParsed.map((item) => {
          const tracks: MediaTrack[] = item.audioId
            ? [
                {
                  trackId: `migrated-${item.id}`,
                  songId: item.id,
                  title: 'آهنگ ۱',
                  fileName: item.audioTitle || 'آهنگ ۱',
                  audioId: item.audioId,
                  trackType: 'music',
                  builtInAudioKey: item.builtInAudioKey ?? null,
                  createdAt: item.createdAt ?? Date.now()
                }
              ]
            : [];
          return {
            id: item.id,
            title: item.title,
            lyrics: item.lyrics,
            tracks,
            fontSize: item.fontSize ?? 20,
            createdAt: item.createdAt ?? Date.now()
          };
        });
        window.localStorage.setItem(SONGS_STORAGE_KEY, JSON.stringify(migrated));
        return migrated;
      }
    }

    window.localStorage.setItem(SONGS_STORAGE_KEY, JSON.stringify(INITIAL_SEED_SONGS));
    return INITIAL_SEED_SONGS;
  } catch {
    return INITIAL_SEED_SONGS;
  }
}

export function saveSongsToStorage(songs: Song[]): void {
  try {
    window.localStorage.setItem(SONGS_STORAGE_KEY, JSON.stringify(songs));
  } catch {
    // Ignore storage quota errors
  }
}

function openAudioDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB unavailable'));
      return;
    }
    const request = indexedDB.open(AUDIO_DB_NAME, 1);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(AUDIO_STORE_NAME)) {
        db.createObjectStore(AUDIO_STORE_NAME);
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error('Failed to open IndexedDB'));
  });
}

export async function saveAudioBlob(audioId: string, blob: Blob): Promise<void> {
  const db = await openAudioDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(AUDIO_STORE_NAME, 'readwrite');
    tx.objectStore(AUDIO_STORE_NAME).put(blob, audioId);
    tx.oncomplete = () => {
      db.close();
      resolve();
    };
    tx.onerror = () => {
      db.close();
      reject(tx.error ?? new Error('Failed to save audio'));
    };
  });
}

export async function getAudioBlob(audioId: string): Promise<Blob | null> {
  try {
    const db = await openAudioDatabase();
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(AUDIO_STORE_NAME, 'readonly');
      const req = tx.objectStore(AUDIO_STORE_NAME).get(audioId);
      req.onsuccess = () => {
        db.close();
        resolve((req.result as Blob | undefined) ?? null);
      };
      req.onerror = () => {
        db.close();
        reject(req.error ?? new Error('Failed to read audio'));
      };
    });
  } catch {
    return null;
  }
}

export async function deleteAudioBlob(audioId: string): Promise<void> {
  try {
    const db = await openAudioDatabase();
    await new Promise<void>((resolve) => {
      const tx = db.transaction(AUDIO_STORE_NAME, 'readwrite');
      tx.objectStore(AUDIO_STORE_NAME).delete(audioId);
      tx.oncomplete = () => {
        db.close();
        resolve();
      };
      tx.onerror = () => {
        db.close();
        resolve();
      };
    });
  } catch {
    // Safe fallback
  }
}

export async function resolveTrackAudioUrl(track: MediaTrack): Promise<string | null> {
  if (track.builtInAudioKey) {
    const wavBytes = createRehearsalToneWavBytes(track.builtInAudioKey, 24);
    const wavArrayBuffer = wavBytes.buffer.slice(
      wavBytes.byteOffset,
      wavBytes.byteOffset + wavBytes.byteLength
    ) as ArrayBuffer;
    const blob = new Blob([wavArrayBuffer], { type: 'audio/wav' });
    return URL.createObjectURL(blob);
  }

  const storedBlob = await getAudioBlob(track.audioId);
  if (!storedBlob) return null;
  return URL.createObjectURL(storedBlob);
}
