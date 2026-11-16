import React, { useState } from 'react';
import { Check, Copy, X } from 'lucide-react';
import buildGradleRaw from '../../android/app/build.gradle.kts?raw';
import manifestRaw from '../../android/app/src/main/AndroidManifest.xml?raw';
import categoryEntityRaw from '../../android/app/src/main/java/com/naghmeh/app/data/CategoryEntity.kt?raw';
import songEntityRaw from '../../android/app/src/main/java/com/naghmeh/app/data/SongEntity.kt?raw';
import mediaTrackEntityRaw from '../../android/app/src/main/java/com/naghmeh/app/data/MediaTrackEntity.kt?raw';
import songWithTracksRaw from '../../android/app/src/main/java/com/naghmeh/app/data/SongWithTracks.kt?raw';
import songDaoRaw from '../../android/app/src/main/java/com/naghmeh/app/data/SongDao.kt?raw';
import appDatabaseRaw from '../../android/app/src/main/java/com/naghmeh/app/data/AppDatabase.kt?raw';
import audioPlayerManagerRaw from '../../android/app/src/main/java/com/naghmeh/app/audio/AudioPlayerManager.kt?raw';
import voiceRecorderManagerRaw from '../../android/app/src/main/java/com/naghmeh/app/audio/VoiceRecorderManager.kt?raw';
import songViewModelRaw from '../../android/app/src/main/java/com/naghmeh/app/ui/SongViewModel.kt?raw';
import appThemeRaw from '../../android/app/src/main/java/com/naghmeh/app/ui/theme/AppTheme.kt?raw';
import homeScreenRaw from '../../android/app/src/main/java/com/naghmeh/app/ui/screens/HomeScreen.kt?raw';
import practiceScreenRaw from '../../android/app/src/main/java/com/naghmeh/app/ui/screens/PracticeScreen.kt?raw';
import addEditScreenRaw from '../../android/app/src/main/java/com/naghmeh/app/ui/screens/AddEditScreen.kt?raw';
import mainActivityRaw from '../../android/app/src/main/java/com/naghmeh/app/MainActivity.kt?raw';

interface AndroidFileEntry {
  id: string;
  label: string;
  path: string;
  content: string;
}

const ANDROID_FILES: AndroidFileEntry[] = [
  {
    id: 'build-gradle',
    label: 'build.gradle.kts',
    path: 'app/build.gradle.kts',
    content: buildGradleRaw
  },
  {
    id: 'manifest',
    label: 'AndroidManifest.xml',
    path: 'app/src/main/AndroidManifest.xml',
    content: manifestRaw
  },
  {
    id: 'category-entity',
    label: 'CategoryEntity.kt',
    path: 'app/src/main/java/com/naghmeh/app/data/CategoryEntity.kt',
    content: categoryEntityRaw
  },
  {
    id: 'song-entity',
    label: 'SongEntity.kt',
    path: 'app/src/main/java/com/naghmeh/app/data/SongEntity.kt',
    content: songEntityRaw
  },
  {
    id: 'media-track-entity',
    label: 'MediaTrackEntity.kt',
    path: 'app/src/main/java/com/naghmeh/app/data/MediaTrackEntity.kt',
    content: mediaTrackEntityRaw
  },
  {
    id: 'song-with-tracks',
    label: 'SongWithTracks.kt',
    path: 'app/src/main/java/com/naghmeh/app/data/SongWithTracks.kt',
    content: songWithTracksRaw
  },
  {
    id: 'song-dao',
    label: 'SongDao.kt',
    path: 'app/src/main/java/com/naghmeh/app/data/SongDao.kt',
    content: songDaoRaw
  },
  {
    id: 'app-database',
    label: 'AppDatabase.kt',
    path: 'app/src/main/java/com/naghmeh/app/data/AppDatabase.kt',
    content: appDatabaseRaw
  },
  {
    id: 'audio-manager',
    label: 'AudioPlayerManager.kt',
    path: 'app/src/main/java/com/naghmeh/app/audio/AudioPlayerManager.kt',
    content: audioPlayerManagerRaw
  },
  {
    id: 'voice-recorder',
    label: 'VoiceRecorderManager.kt',
    path: 'app/src/main/java/com/naghmeh/app/audio/VoiceRecorderManager.kt',
    content: voiceRecorderManagerRaw
  },
  {
    id: 'song-viewmodel',
    label: 'SongViewModel.kt',
    path: 'app/src/main/java/com/naghmeh/app/ui/SongViewModel.kt',
    content: songViewModelRaw
  },
  {
    id: 'app-theme',
    label: 'AppTheme.kt',
    path: 'app/src/main/java/com/naghmeh/app/ui/theme/AppTheme.kt',
    content: appThemeRaw
  },
  {
    id: 'home-screen',
    label: 'HomeScreen.kt',
    path: 'app/src/main/java/com/naghmeh/app/ui/screens/HomeScreen.kt',
    content: homeScreenRaw
  },
  {
    id: 'practice-screen',
    label: 'PracticeScreen.kt',
    path: 'app/src/main/java/com/naghmeh/app/ui/screens/PracticeScreen.kt',
    content: practiceScreenRaw
  },
  {
    id: 'add-edit-screen',
    label: 'AddEditScreen.kt',
    path: 'app/src/main/java/com/naghmeh/app/ui/screens/AddEditScreen.kt',
    content: addEditScreenRaw
  },
  {
    id: 'main-activity',
    label: 'MainActivity.kt',
    path: 'app/src/main/java/com/naghmeh/app/MainActivity.kt',
    content: mainActivityRaw
  }
];

interface AndroidSourceModalProps {
  onClose: () => void;
}

export const AndroidSourceModal: React.FC<AndroidSourceModalProps> = ({ onClose }) => {
  const [selectedId, setSelectedId] = useState(ANDROID_FILES[0].id);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const selectedFile = ANDROID_FILES.find((f) => f.id === selectedId) ?? ANDROID_FILES[0];

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(selectedFile.content);
      setCopiedId(selectedFile.id);
      setTimeout(() => setCopiedId(null), 2000);
    } catch {
      // Clipboard blocked in restricted frames
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-3 sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="android-source-title"
    >
      <div className="flex h-[88dvh] w-full max-w-6xl flex-col overflow-hidden rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] shadow-2xl dark:border-[#2A394A] dark:bg-[#161F2A]">
        <header className="flex shrink-0 items-center justify-between gap-4 border-b border-[#DDD6C8] bg-[#F8F6F1] px-4 py-3 dark:border-[#2A394A] dark:bg-[#0E141B] sm:px-6">
          <div>
            <h2 id="android-source-title" className="text-lg font-bold text-[#1B1E24] dark:text-[#F1EDE4]">
              سورس‌کد کامل پروژه اندروید (Kotlin · Jetpack Compose · Media3 · Room)
            </h2>
            <p className="text-xs text-[#525866] dark:text-[#9FB0C2]" dir="ltr">
              {selectedFile.path}
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleCopy}
              className="inline-flex min-h-[42px] items-center gap-2 rounded-xl bg-[#0E6B64] px-4 py-2 text-sm font-bold text-white hover:bg-[#0A554F] dark:bg-[#3CB8AC] dark:text-[#072623] dark:hover:bg-[#52C9BD]"
            >
              {copiedId === selectedFile.id ? (
                <>
                  <Check className="h-4 w-4" aria-hidden="true" />
                  <span>کپی شد</span>
                </>
              ) : (
                <>
                  <Copy className="h-4 w-4" aria-hidden="true" />
                  <span>کپی کد</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={onClose}
              aria-label="بستن پنجره کد اندروید"
              className="flex h-11 w-11 items-center justify-center rounded-xl text-[#525866] hover:bg-[#EFECE4] hover:text-[#1B1E24] dark:text-[#9FB0C2] dark:hover:bg-[#1F2B38] dark:hover:text-[#F1EDE4]"
            >
              <X className="h-5 w-5" aria-hidden="true" />
            </button>
          </div>
        </header>

        <div className="flex flex-1 flex-col overflow-hidden md:flex-row">
          <nav
            aria-label="فایل‌های پروژه اندروید"
            dir="ltr"
            className="flex shrink-0 gap-1 overflow-x-auto border-b border-[#DDD6C8] bg-[#F8F6F1] p-2 dark:border-[#2A394A] dark:bg-[#0E141B] md:w-64 md:flex-col md:overflow-y-auto md:border-b-0 md:border-l"
          >
            {ANDROID_FILES.map((file) => {
              const active = file.id === selectedFile.id;
              return (
                <button
                  key={file.id}
                  type="button"
                  onClick={() => setSelectedId(file.id)}
                  className={`rounded-xl px-3 py-2.5 text-left font-mono text-xs whitespace-nowrap ${
                    active
                      ? 'bg-[#0E6B64] font-semibold text-white dark:bg-[#3CB8AC] dark:text-[#072623]'
                      : 'text-[#1B1E24] hover:bg-[#EFECE4] dark:text-[#F1EDE4] dark:hover:bg-[#1F2B38]'
                  }`}
                >
                  {file.label}
                </button>
              );
            })}
          </nav>

          <div
            dir="ltr"
            className="flex-1 overflow-auto bg-[#181B20] p-4 font-mono text-xs leading-relaxed text-[#E9ECEF] sm:p-6 sm:text-sm"
          >
            <pre className="whitespace-pre">{selectedFile.content}</pre>
          </div>
        </div>
      </div>
    </div>
  );
};
