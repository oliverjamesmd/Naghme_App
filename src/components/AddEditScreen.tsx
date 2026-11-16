import React, { useEffect, useRef, useState } from 'react';
import {
  ArrowRight,
  Check,
  ClipboardPaste,
  FileAudio,
  Mic,
  Moon,
  Music,
  Pause,
  Play,
  Plus,
  Square,
  Sun,
  Trash2
} from 'lucide-react';
import {
  Category,
  DEFAULT_FONT_SIZE,
  formatRecordingTimestamp,
  formatTimeSeconds,
  getNextMusicTrackTitle,
  getNextRecordingTrackTitle,
  MediaTrack,
  Song,
  validateCategoryName,
  validateSongForm
} from '../utils/naghmehLogic';
import { resolveTrackAudioUrl, saveAudioBlob } from '../utils/storage';

interface DraftMusicSlot extends MediaTrack {
  pendingFile?: File;
}

interface DraftRecordingItem extends MediaTrack {
  pendingBlob?: Blob;
  previewUrl?: string;
}

interface AddEditScreenProps {
  existingSong: Song | null;
  categories: Category[];
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onCreateCategory: (name: string) => Category | null;
  onSave: (song: Song) => void;
  onCancel: () => void;
}

function createEmptyMusicSlot(songId: string, indexNumber: number): DraftMusicSlot {
  return {
    trackId: `music-${Date.now()}-${indexNumber}-${Math.random().toString(36).slice(2, 6)}`,
    songId,
    title: getNextMusicTrackTitle(indexNumber - 1),
    audioId: '',
    fileName: '',
    trackType: 'music',
    createdAt: Date.now() + indexNumber
  };
}

export const AddEditScreen: React.FC<AddEditScreenProps> = ({
  existingSong,
  categories,
  isDarkTheme,
  onToggleTheme,
  onCreateCategory,
  onSave,
  onCancel
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const lyricsInputRef = useRef<HTMLTextAreaElement | null>(null);
  const activePickerSlotIdRef = useRef<string | null>(null);
  const songIdRef = useRef(existingSong?.id ?? `song-${Date.now()}`);

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);
  const recordingTimerRef = useRef<number | null>(null);
  const recordingSecRef = useRef(0);

  const inlineAudioRef = useRef<HTMLAudioElement | null>(null);
  const previewUrlsRef = useRef<string[]>([]);

  const [title, setTitle] = useState(existingSong?.title ?? '');
  const [lyrics, setLyrics] = useState(existingSong?.lyrics ?? '');
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(
    existingSong?.categoryId ?? null
  );
  const [showInlineCategory, setShowInlineCategory] = useState(false);
  const [newCategoryDraft, setNewCategoryDraft] = useState('');
  const [newCategoryError, setNewCategoryError] = useState<string | null>(null);

  const [musicSlots, setMusicSlots] = useState<DraftMusicSlot[]>(() => {
    const existingMusic = (existingSong?.tracks ?? []).filter(
      (t) => t.trackType !== 'recording'
    );
    return existingMusic.length > 0
      ? existingMusic
      : [createEmptyMusicSlot(songIdRef.current, 1)];
  });

  const [recordings, setRecordings] = useState<DraftRecordingItem[]>(() =>
    (existingSong?.tracks ?? []).filter((t) => t.trackType === 'recording')
  );

  const [isRecording, setIsRecording] = useState(false);
  const [recordingSec, setRecordingSec] = useState(0);
  const [recorderError, setRecorderError] = useState<string | null>(null);

  const [playingRecId, setPlayingRecId] = useState<string | null>(null);
  const [inlineAudioUrl, setInlineAudioUrl] = useState<string | null>(null);
  const [inlinePlaying, setInlinePlaying] = useState(false);
  const [inlineCurrentSec, setInlineCurrentSec] = useState(0);
  const [inlineDurationSec, setInlineDurationSec] = useState(0);

  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [pasteHint, setPasteHint] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [pendingDeleteItem, setPendingDeleteItem] = useState<{
    kind: 'music' | 'recording';
    trackId: string;
    title: string;
  } | null>(null);

  useEffect(() => {
    return () => {
      if (recordingTimerRef.current !== null) {
        window.clearInterval(recordingTimerRef.current);
      }
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
      mediaStreamRef.current?.getTracks().forEach((t) => t.stop());
      previewUrlsRef.current.forEach((url) => URL.revokeObjectURL(url));
    };
  }, []);

  const handlePasteClipboard = async () => {
    setPasteHint(null);
    try {
      const text = await navigator.clipboard.readText();
      if (!text.trim()) {
        setPasteHint('حافظه موقت (کلیپ‌بورد) خالی است.');
        return;
      }
      setLyrics((prev) => (prev.trim().length === 0 ? text : `${prev.trimEnd()}\n${text}`));
      setErrorMsg(null);
    } catch {
      lyricsInputRef.current?.focus();
      setPasteHint('اجازه خواندن خودکار کلیپ‌بورد داده نشد؛ داخل کادر زیر لمس طولانی کرده و «الصاق» را بزنید (یا Ctrl+V).');
    }
  };

  const handleOpenPickerForSlot = (slotId: string) => {
    activePickerSlotIdRef.current = slotId;
    fileInputRef.current?.click();
  };

  const handleFileSelected = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    const targetSlotId = activePickerSlotIdRef.current;
    activePickerSlotIdRef.current = null;

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    if (!file || !targetSlotId) return;

    const audioId = `media-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
    setMusicSlots((prev) =>
      prev.map((slot) =>
        slot.trackId === targetSlotId
          ? {
              ...slot,
              audioId,
              fileName: file.name,
              builtInAudioKey: null,
              pendingFile: file
            }
          : slot
      )
    );
    setErrorMsg(null);
  };

  const handleAddMusicSlot = () => {
    setMusicSlots((prev) => [...prev, createEmptyMusicSlot(songIdRef.current, prev.length + 1)]);
  };

  const handleUpdateMusicTitle = (trackId: string, nextTitle: string) => {
    setMusicSlots((prev) =>
      prev.map((slot) => (slot.trackId === trackId ? { ...slot, title: nextTitle } : slot))
    );
  };

  const handleRemoveMusicSlot = (trackId: string) => {
    setMusicSlots((prev) => {
      const remaining = prev.filter((slot) => slot.trackId !== trackId);
      return remaining.length > 0 ? remaining : [createEmptyMusicSlot(songIdRef.current, 1)];
    });
  };

  const handleStartOrStopRecording = async () => {
    setRecorderError(null);

    if (isRecording) {
      if (recordingTimerRef.current !== null) {
        window.clearInterval(recordingTimerRef.current);
        recordingTimerRef.current = null;
      }
      mediaRecorderRef.current?.stop();
      setIsRecording(false);
      return;
    }

    if (inlinePlaying && inlineAudioRef.current) {
      inlineAudioRef.current.pause();
      setInlinePlaying(false);
    }

    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setRecorderError('ضبط صدا در این مرورگر پشتیبانی نمی‌شود.');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      mediaStreamRef.current = stream;
      recordedChunksRef.current = [];

      const recorder = new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          recordedChunksRef.current.push(event.data);
        }
      };

      recorder.onstop = () => {
        stream.getTracks().forEach((track) => track.stop());
        mediaStreamRef.current = null;
        const duration = recordingSecRef.current;
        recordingSecRef.current = 0;
        setRecordingSec(0);

        const blob = new Blob(recordedChunksRef.current, {
          type: recorder.mimeType || 'audio/webm'
        });
        if (blob.size === 0) {
          setRecorderError('فایل ضبط‌شده خالی بود. دوباره تلاش کنید.');
          return;
        }

        const previewUrl = URL.createObjectURL(blob);
        previewUrlsRef.current.push(previewUrl);
        const now = new Date();

        setRecordings((prev) => [
          ...prev,
          {
            trackId: `rec-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
            songId: songIdRef.current,
            title: getNextRecordingTrackTitle(prev.length),
            audioId: `rec-${Date.now()}`,
            trackType: 'recording',
            durationSec: duration,
            displayTimestamp: formatRecordingTimestamp(now),
            createdAt: now.getTime(),
            pendingBlob: blob,
            previewUrl
          }
        ]);
      };

      recorder.start();
      setIsRecording(true);
      recordingSecRef.current = 0;
      setRecordingSec(0);
      recordingTimerRef.current = window.setInterval(() => {
        recordingSecRef.current += 1;
        setRecordingSec(recordingSecRef.current);
      }, 1000);
    } catch {
      setRecorderError('دسترسی به میکروفون داده نشد یا میکروفونی متصل نیست.');
    }
  };

  const handleToggleInlinePlayback = async (rec: DraftRecordingItem) => {
    const audio = inlineAudioRef.current;

    if (playingRecId === rec.trackId && audio) {
      if (inlinePlaying) {
        audio.pause();
        setInlinePlaying(false);
      } else {
        try {
          await audio.play();
          setInlinePlaying(true);
        } catch {
          setRecorderError('پخش این فایل ضبط‌شده ممکن نشد.');
        }
      }
      return;
    }

    let url = rec.previewUrl ?? null;
    if (!url) {
      url = await resolveTrackAudioUrl(rec);
      if (url) {
        previewUrlsRef.current.push(url);
      }
    }
    if (!url) {
      setRecorderError('فایل ضبط‌شده در دسترس نیست.');
      return;
    }

    setPlayingRecId(rec.trackId);
    setInlineCurrentSec(0);
    setInlineDurationSec(rec.durationSec ?? 0);
    setInlineAudioUrl(url);
    setInlinePlaying(true);
  };

  const handleRemoveRecording = (trackId: string) => {
    if (playingRecId === trackId) {
      inlineAudioRef.current?.pause();
      setPlayingRecId(null);
      setInlinePlaying(false);
      setInlineAudioUrl(null);
    }
    setRecordings((prev) => prev.filter((item) => item.trackId !== trackId));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const validationError = validateSongForm(title, lyrics);
    if (validationError) {
      setErrorMsg(validationError);
      return;
    }

    setIsSaving(true);
    try {
      const validMusicSlots = musicSlots.filter(
        (slot) => Boolean(slot.audioId || slot.builtInAudioKey || slot.pendingFile)
      );

      for (const slot of validMusicSlots) {
        if (slot.pendingFile) {
          await saveAudioBlob(slot.audioId, slot.pendingFile);
        }
      }

      for (const rec of recordings) {
        if (rec.pendingBlob) {
          await saveAudioBlob(rec.audioId, rec.pendingBlob);
        }
      }

      const persistedMusic: MediaTrack[] = validMusicSlots.map(
        ({ pendingFile: _ignored, ...rest }, idx) => ({
          ...rest,
          title: rest.title.trim() || getNextMusicTrackTitle(idx)
        })
      );

      const persistedRecordings: MediaTrack[] = recordings.map(
        ({ pendingBlob: _ignoredBlob, previewUrl: _ignoredUrl, ...rest }, idx) => ({
          ...rest,
          title: rest.title.trim() || getNextRecordingTrackTitle(idx)
        })
      );

      const savedSong: Song = {
        id: songIdRef.current,
        title: title.trim(),
        lyrics: lyrics.trim(),
        categoryId: selectedCategoryId,
        tracks: [...persistedMusic, ...persistedRecordings],
        fontSize: existingSong?.fontSize ?? DEFAULT_FONT_SIZE,
        createdAt: existingSong?.createdAt ?? Date.now()
      };
      onSave(savedSong);
    } catch {
      setErrorMsg('ذخیره فایل‌های صوتی یا ضبط‌شده با خطا مواجه شد. لطفاً دوباره تلاش کنید.');
      setIsSaving(false);
    }
  };

  const handleAddInlineCategory = () => {
    const err = validateCategoryName(newCategoryDraft, categories);
    if (err) {
      setNewCategoryError(err);
      return;
    }
    const created = onCreateCategory(newCategoryDraft);
    if (created) {
      setSelectedCategoryId(created.id);
    }
    setNewCategoryDraft('');
    setNewCategoryError(null);
    setShowInlineCategory(false);
  };

  return (
    <div className="min-h-dvh bg-[#F8F6F1] text-[#1B1E24] dark:bg-[#0E141B] dark:text-[#F1EDE4]">
      <header className="border-b border-[#DDD6C8] bg-[#FFFFFF] dark:border-[#2A394A] dark:bg-[#161F2A]">
        <div className="mx-auto flex max-w-3xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
          <div className="flex items-center gap-2 min-w-0">
            <button
              type="button"
              onClick={onCancel}
              aria-label="بازگشت"
              className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-[#1B1E24] transition-colors hover:bg-[#EFECE4] dark:text-[#F1EDE4] dark:hover:bg-[#1F2B38] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
            >
              <ArrowRight className="h-6 w-6" aria-hidden="true" />
            </button>
            <h1 className="truncate text-lg font-bold text-[#1B1E24] dark:text-[#F1EDE4] sm:text-xl">
              {existingSong ? 'ویرایش شعر' : 'افزودن شعر جدید'}
            </h1>
          </div>

          <button
            type="button"
            onClick={onToggleTheme}
            data-testid="theme-toggle"
            aria-label={isDarkTheme ? 'تغییر به حالت روز' : 'تغییر به حالت شب'}
            title={isDarkTheme ? 'حالت روز' : 'حالت شب'}
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] text-[#A86416] transition-colors hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#E5A952] dark:hover:bg-[#273646] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
          >
            {isDarkTheme ? (
              <Sun className="h-5 w-5" aria-hidden="true" />
            ) : (
              <Moon className="h-5 w-5" aria-hidden="true" />
            )}
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-4 py-6 sm:px-6">
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          {errorMsg && (
            <div
              role="alert"
              className="rounded-2xl border border-[#B3261E] bg-[#B3261E]/10 px-4 py-3 text-base font-medium text-[#B3261E] dark:border-[#F28B82] dark:bg-[#F28B82]/15 dark:text-[#F28B82]"
            >
              {errorMsg}
            </div>
          )}

          <div>
            <label
              htmlFor="song-title-input"
              className="mb-2 block text-lg font-semibold text-[#1B1E24] dark:text-[#F1EDE4]"
            >
              عنوان شعر
            </label>
            <input
              id="song-title-input"
              type="text"
              value={title}
              onChange={(e) => {
                setTitle(e.target.value);
                setErrorMsg(null);
              }}
              placeholder="مثلاً: غزل شماره ۱ حافظ یا تصنیف مرغ سحر"
              className="min-h-[54px] w-full rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] px-4 py-3 text-lg text-[#1B1E24] placeholder:text-[#6B7280] focus:border-[#0E6B64] focus:outline-none focus:ring-2 focus:ring-[#0E6B64]/20 dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:placeholder:text-[#8496AA] dark:focus:border-[#3CB8AC]"
            />
          </div>

          {/* Category Selector + Quick Add Category */}
          <div className="space-y-2.5">
            <span className="block text-lg font-semibold text-[#1B1E24] dark:text-[#F1EDE4]">
              دسته‌بندی (اختیاری)
            </span>
            <div
              role="group"
              aria-label="انتخاب دسته‌بندی شعر"
              className="flex flex-wrap items-center gap-2"
            >
              <button
                type="button"
                aria-pressed={selectedCategoryId === null}
                onClick={() => setSelectedCategoryId(null)}
                className={`min-h-[42px] rounded-xl border px-3.5 py-1.5 text-sm transition-colors ${
                  selectedCategoryId === null
                    ? 'border-[#0E6B64] bg-[#D6ECE9] font-bold text-[#063A36] dark:border-[#3CB8AC] dark:bg-[#163D3A] dark:text-[#BFECE6]'
                    : 'border-[#DDD6C8] bg-[#FFFFFF] font-medium text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:hover:bg-[#1F2B38]'
                }`}
              >
                بدون دسته
              </button>

              {categories.map((cat) => {
                const selected = selectedCategoryId === cat.id;
                return (
                  <button
                    key={cat.id}
                    type="button"
                    aria-pressed={selected}
                    onClick={() => setSelectedCategoryId(cat.id)}
                    className={`min-h-[42px] rounded-xl border px-3.5 py-1.5 text-sm transition-colors ${
                      selected
                        ? 'border-[#0E6B64] bg-[#D6ECE9] font-bold text-[#063A36] dark:border-[#3CB8AC] dark:bg-[#163D3A] dark:text-[#BFECE6]'
                        : 'border-[#DDD6C8] bg-[#FFFFFF] font-medium text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:hover:bg-[#1F2B38]'
                    }`}
                  >
                    {cat.name}
                  </button>
                );
              })}

              <button
                type="button"
                onClick={() => {
                  setShowInlineCategory((prev) => !prev);
                  setNewCategoryError(null);
                }}
                className="inline-flex min-h-[42px] items-center gap-1.5 rounded-xl border border-dashed border-[#0E6B64]/60 bg-[#FAF8F3] px-3.5 py-1.5 text-sm font-bold text-[#0E6B64] hover:bg-[#D6ECE9]/40 dark:border-[#3CB8AC]/60 dark:bg-[#121A24] dark:text-[#3CB8AC] dark:hover:bg-[#163D3A]/45"
              >
                <Plus className="h-4 w-4" aria-hidden="true" />
                <span>دسته جدید</span>
              </button>
            </div>

            {showInlineCategory && (
              <div className="space-y-2 rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-3 dark:border-[#2A394A] dark:bg-[#161F2A]">
                <div className="flex min-w-0 items-center gap-2">
                  <input
                    type="text"
                    value={newCategoryDraft}
                    onChange={(e) => {
                      setNewCategoryDraft(e.target.value);
                      setNewCategoryError(null);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddInlineCategory();
                      }
                    }}
                    aria-label="نام دسته جدید"
                    placeholder="نام دسته (مثلاً: شعر حافظ)"
                    className="min-h-[44px] min-w-0 flex-1 rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-3 text-base text-[#1B1E24] focus:border-[#0E6B64] focus:outline-none dark:border-[#2A394A] dark:bg-[#0E141B] dark:text-[#F1EDE4] dark:focus:border-[#3CB8AC]"
                  />
                  <button
                    type="button"
                    onClick={handleAddInlineCategory}
                    className="min-h-[44px] shrink-0 rounded-xl bg-[#0E6B64] px-4 text-sm font-bold text-white hover:bg-[#0A554F] dark:bg-[#3CB8AC] dark:text-[#072623] dark:hover:bg-[#52C9BD]"
                  >
                    افزودن
                  </button>
                </div>
                {newCategoryError && (
                  <p role="alert" className="text-sm font-medium text-[#B3261E] dark:text-[#F28B82]">
                    {newCategoryError}
                  </p>
                )}
              </div>
            )}
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between gap-2">
              <label
                htmlFor="song-lyrics-input"
                className="text-lg font-semibold text-[#1B1E24] dark:text-[#F1EDE4]"
              >
                متن شعر
              </label>

              <button
                type="button"
                onClick={handlePasteClipboard}
                className="inline-flex min-h-[44px] items-center gap-2 rounded-xl border border-[#DDD6C8] bg-[#EFECE4] px-4 py-2 text-sm font-bold text-[#1B1E24] transition-colors hover:bg-[#E2DDD1] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
              >
                <ClipboardPaste className="h-4 w-4 text-[#0E6B64] dark:text-[#3CB8AC]" aria-hidden="true" />
                <span>الصاق متن</span>
              </button>
            </div>

            {pasteHint && (
              <p className="mb-2 text-sm text-[#A86416] dark:text-[#E5A952]">{pasteHint}</p>
            )}

            <textarea
              ref={lyricsInputRef}
              id="song-lyrics-input"
              rows={8}
              value={lyrics}
              onChange={(e) => {
                setLyrics(e.target.value);
                setErrorMsg(null);
              }}
              placeholder="متن کامل شعر یا ترانه را اینجا بنویسید یا الصاق کنید..."
              className="w-full rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] px-4 py-3 text-lg leading-relaxed text-[#1B1E24] placeholder:text-[#6B7280] focus:border-[#0E6B64] focus:outline-none focus:ring-2 focus:ring-[#0E6B64]/20 dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:placeholder:text-[#8496AA] dark:focus:border-[#3CB8AC]"
            />
          </div>

          {/* Dedicated Section 1: Music Tracks ("فایل‌های آهنگ و همراهی") */}
          <section
            aria-labelledby="music-tracks-heading"
            className="rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-4 sm:p-5 space-y-4 dark:border-[#2A394A] dark:bg-[#161F2A]"
          >
            <div className="flex items-center gap-2">
              <Music className="h-5 w-5 text-[#0E6B64] dark:text-[#3CB8AC]" aria-hidden="true" />
              <h2
                id="music-tracks-heading"
                className="text-lg font-bold text-[#1B1E24] dark:text-[#F1EDE4]"
              >
                فایل‌های آهنگ و همراهی
              </h2>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="audio/*,video/*,.mp3,.wav,.mp4,.mkv,.m4a,.aac,.ogg,.webm"
              onChange={handleFileSelected}
              className="hidden"
              id="song-media-files-input"
            />

            <ul className="space-y-3" aria-label="لیست فایل‌های آهنگ">
              {musicSlots.map((slot) => (
                <li
                  key={slot.trackId}
                  className="overflow-hidden rounded-2xl border border-[#DDD6C8] bg-[#F8F6F1] p-3.5 space-y-2.5 dark:border-[#2A394A] dark:bg-[#0E141B]"
                >
                  <div className="flex min-w-0 items-center gap-2">
                    <input
                      type="text"
                      value={slot.title}
                      onChange={(e) => handleUpdateMusicTitle(slot.trackId, e.target.value)}
                      aria-label="عنوان آهنگ"
                      placeholder="نام آهنگ (مثلاً آهنگ ۱)"
                      className="min-h-[44px] min-w-0 w-full flex-1 rounded-xl border border-[#DDD6C8] bg-[#FFFFFF] px-3 text-base font-semibold text-[#1B1E24] focus:border-[#0E6B64] focus:outline-none dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:focus:border-[#3CB8AC]"
                    />

                    <button
                      type="button"
                      onClick={() =>
                        setPendingDeleteItem({
                          kind: 'music',
                          trackId: slot.trackId,
                          title: slot.title.trim() || 'این آهنگ'
                        })
                      }
                      aria-label={`حذف ${slot.title}`}
                      className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-[#B3261E] hover:bg-[#B3261E]/10 dark:text-[#F28B82] dark:hover:bg-[#F28B82]/15 focus-visible:outline-2 focus-visible:outline-[#B3261E]"
                    >
                      <Trash2 className="h-5 w-5" aria-hidden="true" />
                    </button>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleOpenPickerForSlot(slot.trackId)}
                    className="flex min-h-[46px] w-full items-center justify-center gap-2 rounded-xl border border-[#DDD6C8] bg-[#EFECE4] px-3 py-2 text-sm font-semibold text-[#1B1E24] transition-colors hover:bg-[#E2DDD1] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
                  >
                    <FileAudio className="h-4 w-4 shrink-0 text-[#0E6B64] dark:text-[#3CB8AC]" aria-hidden="true" />
                    <span className="truncate">
                      {slot.fileName
                        ? slot.fileName
                        : slot.builtInAudioKey
                        ? `${slot.title} (فایل صوتی پیش‌فرض)`
                        : 'انتخاب فایل (MP3، WAV، یا ویدیوی MP4/MKV)'}
                    </span>
                  </button>
                </li>
              ))}
            </ul>

            <button
              type="button"
              onClick={handleAddMusicSlot}
              className="flex min-h-[52px] w-full items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-[#0E6B64]/55 bg-[#F8F6F1] px-4 py-2.5 text-base font-bold text-[#0E6B64] transition-colors hover:bg-[#D6ECE9]/40 dark:border-[#3CB8AC]/55 dark:bg-[#0E141B] dark:text-[#3CB8AC] dark:hover:bg-[#163D3A]/45 focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
            >
              <Plus className="h-5 w-5" aria-hidden="true" />
              <span>+ افزودن آهنگ جدید</span>
            </button>
          </section>

          {/* Dedicated Section 2: Voice Practice Recorder Studio ("استودیوی ضبط صدا") */}
          <section
            aria-labelledby="voice-studio-heading"
            className="rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-4 sm:p-5 space-y-4 dark:border-[#2A394A] dark:bg-[#161F2A]"
          >
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Mic className="h-5 w-5 text-[#A86416] dark:text-[#E5A952]" aria-hidden="true" />
                <h2
                  id="voice-studio-heading"
                  className="text-lg font-bold text-[#1B1E24] dark:text-[#F1EDE4]"
                >
                  استودیوی ضبط صدا
                </h2>
              </div>
              {isRecording && (
                <span className="font-mono text-sm font-bold text-[#B3261E] dark:text-[#F28B82] tabular-nums">
                  {formatTimeSeconds(recordingSec)}
                </span>
              )}
            </div>

            {recorderError && (
              <p role="alert" className="text-sm font-medium text-[#B3261E] dark:text-[#F28B82]">
                {recorderError}
              </p>
            )}

            <button
              type="button"
              onClick={handleStartOrStopRecording}
              className={`flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 text-base font-bold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 ${
                isRecording
                  ? 'bg-[#B3261E] text-white hover:bg-[#8E1C16] dark:bg-[#F28B82] dark:text-[#3E0A06] focus-visible:outline-[#B3261E]'
                  : 'bg-[#F6E7D3] text-[#4A2B05] hover:bg-[#EED7BA] dark:bg-[#3B2B15] dark:text-[#F8DFB8] dark:hover:bg-[#4A371C] focus-visible:outline-[#A86416]'
              }`}
            >
              {isRecording ? (
                <>
                  <Square className="h-5 w-5" aria-hidden="true" />
                  <span className="tabular-nums">
                    توقف و ذخیره ضبط ({formatTimeSeconds(recordingSec)})
                  </span>
                </>
              ) : (
                <>
                  <Mic className="h-5 w-5" aria-hidden="true" />
                  <span>شروع ضبط صدای تمرین (۰۰:۰۰)</span>
                </>
              )}
            </button>

            {inlineAudioUrl && (
              <audio
                ref={inlineAudioRef}
                src={inlineAudioUrl}
                autoPlay
                onLoadedMetadata={(e) => {
                  const dur = e.currentTarget.duration;
                  if (Number.isFinite(dur) && dur > 0) {
                    setInlineDurationSec(dur);
                  }
                }}
                onTimeUpdate={(e) => setInlineCurrentSec(e.currentTarget.currentTime)}
                onEnded={() => {
                  setInlinePlaying(false);
                  setInlineCurrentSec(0);
                }}
              />
            )}

            {recordings.length > 0 && (
              <ul className="space-y-3" aria-label="فایل‌های ضبط‌شده">
                {recordings.map((rec) => {
                  const isActiveRec = playingRecId === rec.trackId;
                  const isPlayingThis = isActiveRec && inlinePlaying;
                  const totalSec = isActiveRec
                    ? inlineDurationSec || rec.durationSec || 1
                    : rec.durationSec || 1;
                  const currentVal = isActiveRec ? inlineCurrentSec : 0;

                  return (
                    <li
                      key={rec.trackId}
                      className="overflow-hidden rounded-2xl border border-[#DDD6C8] bg-[#F8F6F1] p-3.5 space-y-2.5 dark:border-[#2A394A] dark:bg-[#0E141B]"
                    >
                      <div className="flex min-w-0 items-center justify-between gap-2">
                        <div className="flex min-w-0 items-center gap-2.5">
                          <button
                            type="button"
                            onClick={() => handleToggleInlinePlayback(rec)}
                            aria-label={isPlayingThis ? `توقف ${rec.title}` : `پخش ${rec.title}`}
                            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#0E6B64] text-white hover:bg-[#0A554F] dark:bg-[#3CB8AC] dark:text-[#072623] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
                          >
                            {isPlayingThis ? (
                              <Pause className="h-5 w-5" aria-hidden="true" />
                            ) : (
                              <Play className="h-5 w-5 translate-x-0.5" aria-hidden="true" />
                            )}
                          </button>

                          <div className="min-w-0">
                            <p className="truncate text-base font-bold text-[#1B1E24] dark:text-[#F1EDE4]">
                              {rec.title}
                            </p>
                            <p className="text-xs text-[#525866] dark:text-[#9FB0C2] tabular-nums">
                              {formatTimeSeconds(rec.durationSec ?? 0)}
                              {rec.displayTimestamp ? ` · ${rec.displayTimestamp}` : ''}
                            </p>
                          </div>
                        </div>

                        <button
                          type="button"
                          onClick={() =>
                            setPendingDeleteItem({
                              kind: 'recording',
                              trackId: rec.trackId,
                              title: rec.title.trim() || 'این فایل ضبط‌شده'
                            })
                          }
                          aria-label={`حذف ${rec.title}`}
                          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-[#B3261E] hover:bg-[#B3261E]/10 dark:text-[#F28B82] dark:hover:bg-[#F28B82]/15 focus-visible:outline-2 focus-visible:outline-[#B3261E]"
                        >
                          <Trash2 className="h-5 w-5" aria-hidden="true" />
                        </button>
                      </div>

                      {/* Inline Playback Progress Bar */}
                      <div dir="ltr" className="flex items-center gap-2">
                        <span className="font-mono text-xs text-[#525866] dark:text-[#9FB0C2] tabular-nums">
                          {formatTimeSeconds(currentVal)}
                        </span>
                        <input
                          type="range"
                          min={0}
                          max={totalSec}
                          step={0.1}
                          value={Math.min(currentVal, totalSec)}
                          aria-label={`پیشرفت پخش ${rec.title}`}
                          disabled={!isActiveRec}
                          onChange={(e) => {
                            if (isActiveRec && inlineAudioRef.current) {
                              const next = Number(e.target.value);
                              inlineAudioRef.current.currentTime = next;
                              setInlineCurrentSec(next);
                            }
                          }}
                          className="naghmeh-slider flex-1"
                        />
                        <span className="font-mono text-xs text-[#525866] dark:text-[#9FB0C2] tabular-nums">
                          {formatTimeSeconds(rec.durationSec ?? inlineDurationSec)}
                        </span>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          <div className="pt-2">
            <button
              type="submit"
              disabled={isSaving}
              className="flex min-h-[58px] w-full items-center justify-center gap-2 rounded-2xl bg-[#0E6B64] px-6 py-3 text-lg font-bold text-white shadow-md shadow-[#0E6B64]/20 transition-colors hover:bg-[#0A554F] disabled:opacity-50 dark:bg-[#3CB8AC] dark:text-[#072623] dark:hover:bg-[#52C9BD] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0E6B64]"
            >
              <Check className="h-6 w-6" aria-hidden="true" />
              <span>ذخیره</span>
            </button>
          </div>
        </form>
      </main>

      {pendingDeleteItem && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-track-dialog-title"
        >
          <div className="w-full max-w-md rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-6 shadow-xl dark:border-[#2A394A] dark:bg-[#161F2A]">
            <h2
              id="delete-track-dialog-title"
              className="text-xl font-bold text-[#1B1E24] dark:text-[#F1EDE4]"
            >
              {pendingDeleteItem.kind === 'music' ? 'حذف فایل آهنگ' : 'حذف صدای ضبط‌شده'}
            </h2>
            <p className="mt-3 text-base leading-relaxed text-[#525866] dark:text-[#9FB0C2]">
              آیا مطمئن هستید که می‌خواهید «{pendingDeleteItem.title}» را حذف کنید؟
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setPendingDeleteItem(null)}
                className="min-h-[48px] rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-5 py-2.5 text-base font-semibold text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646]"
              >
                انصراف
              </button>
              <button
                type="button"
                onClick={() => {
                  if (pendingDeleteItem.kind === 'music') {
                    handleRemoveMusicSlot(pendingDeleteItem.trackId);
                  } else {
                    handleRemoveRecording(pendingDeleteItem.trackId);
                  }
                  setPendingDeleteItem(null);
                }}
                className="min-h-[48px] rounded-xl bg-[#B3261E] px-5 py-2.5 text-base font-bold text-white hover:bg-[#8E1C16] dark:bg-[#F28B82] dark:text-[#3E0A06]"
              >
                بله، حذف شود
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
