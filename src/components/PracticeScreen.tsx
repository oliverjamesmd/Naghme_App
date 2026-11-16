import React, { useEffect, useRef, useState } from 'react';
import {
  ArrowRight,
  ChevronUp,
  Mic,
  Moon,
  Music,
  Pause,
  Pencil,
  Play,
  RotateCcw,
  RotateCw,
  Square,
  Sun
} from 'lucide-react';
import {
  computeSeekTime,
  formatPlaybackSpeedLabel,
  formatRecordingTimestamp,
  formatTimeSeconds,
  getFontOption,
  getNextRecordingTrackTitle,
  MAX_FONT_SIZE,
  MediaTrack,
  MIN_FONT_SIZE,
  PERSIAN_FONT_OPTIONS,
  PersianFontId,
  PLAYBACK_SPEEDS,
  Song,
  toPersianDigits
} from '../utils/naghmehLogic';
import { resolveTrackAudioUrl, saveAudioBlob } from '../utils/storage';

interface PracticeScreenProps {
  song: Song;
  selectedFontId: PersianFontId;
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onSelectFont: (fontId: PersianFontId) => void;
  onBack: () => void;
  onEdit: (songId: string) => void;
  onIncreaseFontSize: (songId: string) => void;
  onDecreaseFontSize: (songId: string) => void;
  onAddRecordedTrack: (songId: string, track: MediaTrack) => void;
}

export const PracticeScreen: React.FC<PracticeScreenProps> = ({
  song,
  selectedFontId,
  isDarkTheme,
  onToggleTheme,
  onSelectFont,
  onBack,
  onEdit,
  onIncreaseFontSize,
  onDecreaseFontSize,
  onAddRecordedTrack
}) => {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);
  const recordingTimerRef = useRef<number | null>(null);
  const recordingSecRef = useRef(0);

  const [selectedTrackId, setSelectedTrackId] = useState<string | null>(
    () => song.tracks[0]?.trackId ?? null
  );
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentSec, setCurrentSec] = useState(0);
  const [durationSec, setDurationSec] = useState(0);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1.0);
  const [audioError, setAudioError] = useState<string | null>(null);
  const [wakeLockActive, setWakeLockActive] = useState(false);

  const [isRecording, setIsRecording] = useState(false);
  const [recordingSec, setRecordingSec] = useState(0);
  const [recorderError, setRecorderError] = useState<string | null>(null);
  const [speedMenuOpen, setSpeedMenuOpen] = useState(false);
  const speedMenuContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!speedMenuOpen) return;
    const handleOutside = (event: PointerEvent) => {
      if (
        speedMenuContainerRef.current &&
        !speedMenuContainerRef.current.contains(event.target as Node)
      ) {
        setSpeedMenuOpen(false);
      }
    };
    document.addEventListener('pointerdown', handleOutside);
    return () => document.removeEventListener('pointerdown', handleOutside);
  }, [speedMenuOpen]);

  const fontOption = getFontOption(selectedFontId);

  const activeTrack =
    song.tracks.find((t) => t.trackId === selectedTrackId) ?? song.tracks[0] ?? null;

  useEffect(() => {
    if (song.tracks.length > 0 && !song.tracks.some((t) => t.trackId === selectedTrackId)) {
      setSelectedTrackId(song.tracks[0].trackId);
    }
  }, [song.tracks, selectedTrackId]);

  // Keep screen awake via Wake Lock API while PracticeScreen is active
  useEffect(() => {
    let sentinel: WakeLockSentinel | null = null;
    let cancelled = false;

    const requestWakeLock = async () => {
      try {
        if ('wakeLock' in navigator && navigator.wakeLock) {
          sentinel = await navigator.wakeLock.request('screen');
          if (!cancelled) {
            setWakeLockActive(true);
          }
        }
      } catch {
        // Wake Lock may be restricted in headless/background tabs
      }
    };

    requestWakeLock();

    const handleVisibility = () => {
      if (document.visibilityState === 'visible' && !sentinel) {
        requestWakeLock();
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);

    return () => {
      cancelled = true;
      document.removeEventListener('visibilitychange', handleVisibility);
      if (sentinel) {
        sentinel.release().catch(() => {});
      }
    };
  }, []);

  // Cleanup active recording if leaving screen
  useEffect(() => {
    return () => {
      if (recordingTimerRef.current !== null) {
        window.clearInterval(recordingTimerRef.current);
      }
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, []);

  // Load audio/video-audio Blob URL whenever active track changes
  useEffect(() => {
    let activeUrl: string | null = null;
    let cancelled = false;

    setIsPlaying(false);
    setCurrentSec(0);
    setDurationSec(0);
    setAudioError(null);
    setAudioUrl(null);

    if (!activeTrack) {
      return;
    }

    resolveTrackAudioUrl(activeTrack).then((url) => {
      if (cancelled) {
        if (url) URL.revokeObjectURL(url);
        return;
      }
      if (!url) {
        setAudioError('فایل پیدا نشد. لطفاً دوباره از دستگاه انتخاب کنید.');
      } else {
        activeUrl = url;
        setAudioUrl(url);
      }
    });

    return () => {
      cancelled = true;
      if (activeUrl) {
        URL.revokeObjectURL(activeUrl);
      }
    };
  }, [activeTrack?.trackId, activeTrack?.audioId, activeTrack?.builtInAudioKey]);

  // Sync playback speed to HTML5 audio element (preserves pitch cleanly)
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    audio.playbackRate = playbackSpeed;
    if ('preservesPitch' in audio) {
      (audio as HTMLAudioElement & { preservesPitch: boolean }).preservesPitch = true;
    }
  }, [playbackSpeed, audioUrl]);

  const handleTogglePlayPause = async () => {
    const audio = audioRef.current;
    if (!audio || !audioUrl) return;
    if (isPlaying) {
      audio.pause();
      setIsPlaying(false);
    } else {
      try {
        audio.playbackRate = playbackSpeed;
        await audio.play();
        setIsPlaying(true);
        setAudioError(null);
      } catch {
        setAudioError('پخش این فایل در مرورگر امکان‌پذیر نشد.');
      }
    }
  };

  const handleSeekTo = (nextSec: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    const clamped = computeSeekTime(nextSec, 0, durationSec);
    audio.currentTime = clamped;
    setCurrentSec(clamped);
  };

  const handleSeekDelta = (deltaSec: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    const next = computeSeekTime(audio.currentTime, deltaSec, durationSec || audio.duration || 0);
    audio.currentTime = next;
    setCurrentSec(next);
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

    if (isPlaying && audioRef.current) {
      audioRef.current.pause();
      setIsPlaying(false);
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

      recorder.onstop = async () => {
        stream.getTracks().forEach((track) => track.stop());
        mediaStreamRef.current = null;
        const finalDuration = recordingSecRef.current;
        recordingSecRef.current = 0;
        setRecordingSec(0);

        const blob = new Blob(recordedChunksRef.current, {
          type: recorder.mimeType || 'audio/webm'
        });
        if (blob.size === 0) {
          setRecorderError('فایل ضبط‌شده خالی بود. دوباره تلاش کنید.');
          return;
        }

        const audioId = `rec-${Date.now()}`;
        await saveAudioBlob(audioId, blob);

        const existingRecCount = song.tracks.filter((t) => t.trackType === 'recording').length;
        const now = new Date();
        const newTrack: MediaTrack = {
          trackId: `track-${Date.now()}`,
          songId: song.id,
          title: getNextRecordingTrackTitle(existingRecCount),
          audioId,
          trackType: 'recording',
          durationSec: finalDuration,
          displayTimestamp: formatRecordingTimestamp(now),
          createdAt: now.getTime()
        };

        onAddRecordedTrack(song.id, newTrack);
        setSelectedTrackId(newTrack.trackId);
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

  return (
    <div className="flex h-dvh flex-col bg-[#F8F6F1] text-[#1B1E24] dark:bg-[#0E141B] dark:text-[#F1EDE4]">
      <header className="shrink-0 border-b border-[#DDD6C8] bg-[#FFFFFF] dark:border-[#2A394A] dark:bg-[#161F2A]">
        <div className="mx-auto flex max-w-4xl items-center justify-between gap-2 px-3 py-2.5 sm:px-6">
          <div className="flex min-w-0 items-center gap-1.5 sm:gap-2">
            <button
              type="button"
              onClick={onBack}
              aria-label="بازگشت به فهرست"
              className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-[#1B1E24] transition-colors hover:bg-[#EFECE4] dark:text-[#F1EDE4] dark:hover:bg-[#1F2B38] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
            >
              <ArrowRight className="h-6 w-6" aria-hidden="true" />
            </button>
            <h1 className="truncate text-base font-bold text-[#1B1E24] dark:text-[#F1EDE4] sm:text-xl">
              {song.title}
            </h1>
          </div>

          <div className="flex items-center gap-1.5 shrink-0">
            <button
              type="button"
              onClick={onToggleTheme}
              data-testid="theme-toggle"
              aria-label={isDarkTheme ? 'تغییر به حالت روز' : 'تغییر به حالت شب'}
              title={isDarkTheme ? 'حالت روز' : 'حالت شب'}
              className="flex h-11 w-11 items-center justify-center rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] text-[#A86416] transition-colors hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#E5A952] dark:hover:bg-[#273646] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
            >
              {isDarkTheme ? (
                <Sun className="h-5 w-5" aria-hidden="true" />
              ) : (
                <Moon className="h-5 w-5" aria-hidden="true" />
              )}
            </button>

            <div className="flex items-center gap-1" dir="ltr">
              <button
                type="button"
                onClick={() => onDecreaseFontSize(song.id)}
                disabled={song.fontSize <= MIN_FONT_SIZE}
                aria-label="کوچک کردن اندازه متن"
                className="min-h-[42px] min-w-[42px] rounded-xl border border-[#DDD6C8] bg-[#EFECE4] px-2 text-sm font-bold text-[#1B1E24] transition-colors hover:bg-[#E2DDD1] disabled:opacity-40 dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
              >
                A-
              </button>
              <button
                type="button"
                onClick={() => onIncreaseFontSize(song.id)}
                disabled={song.fontSize >= MAX_FONT_SIZE}
                aria-label="بزرگ کردن اندازه متن"
                className="min-h-[42px] min-w-[42px] rounded-xl border border-[#0E6B64]/30 bg-[#D6ECE9] px-2 text-sm font-bold text-[#063A36] transition-colors hover:bg-[#C3E3DF] disabled:opacity-40 dark:border-[#3CB8AC]/40 dark:bg-[#163D3A] dark:text-[#BFECE6] dark:hover:bg-[#1E4F4B] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
              >
                A+
              </button>
            </div>

            <button
              type="button"
              onClick={() => onEdit(song.id)}
              aria-label="ویرایش شعر"
              className="flex h-11 w-11 items-center justify-center rounded-xl text-[#525866] transition-colors hover:bg-[#EFECE4] hover:text-[#1B1E24] dark:text-[#9FB0C2] dark:hover:bg-[#1F2B38] dark:hover:text-[#F1EDE4] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
            >
              <Pencil className="h-5 w-5" aria-hidden="true" />
            </button>
          </div>
        </div>

        {/* Clearly Visible Persian Font Selector Chip Row */}
        <div
          role="group"
          aria-label="انتخاب فونت فارسی"
          className="border-t border-[#EFECE4] bg-[#FAF8F3] dark:border-[#2A394A] dark:bg-[#121A24]"
        >
          <div className="mx-auto flex max-w-4xl items-center gap-2 overflow-x-auto px-3 py-2 sm:px-6">
            {PERSIAN_FONT_OPTIONS.map((opt) => {
              const active = opt.id === selectedFontId;
              return (
                <button
                  key={opt.id}
                  type="button"
                  aria-pressed={active}
                  onClick={() => onSelectFont(opt.id)}
                  className={`min-h-[40px] shrink-0 rounded-xl border px-3.5 py-1.5 text-sm whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-[#0E6B64] ${
                    opt.cssClass
                  } ${
                    active
                      ? 'border-[#0E6B64] bg-[#D6ECE9] font-bold text-[#063A36] dark:border-[#3CB8AC] dark:bg-[#163D3A] dark:text-[#BFECE6]'
                      : 'border-[#DDD6C8] bg-[#FFFFFF] font-medium text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:hover:bg-[#1F2B38]'
                  }`}
                >
                  {opt.shortLabel}
                </button>
              );
            })}
          </div>
        </div>
      </header>

      {/* Manual Scroll High-Contrast Centered RTL Lyrics Area (No Auto-Scroll) */}
      <main className="flex-1 overflow-y-auto px-5 py-6 sm:px-8 sm:py-8">
        <div className="mx-auto max-w-3xl">
          <div className="mb-5 flex flex-wrap items-center justify-center gap-2 text-sm text-[#525866] dark:text-[#9FB0C2]">
            <Sun className="h-4 w-4 shrink-0 text-[#A86416] dark:text-[#E5A952]" aria-hidden="true" />
            <span>
              {wakeLockActive
                ? 'صفحه در زمان تمرین روشن می‌ماند'
                : 'حالت مطالعه بدون مزاحمت'}
            </span>
            <span aria-hidden="true">·</span>
            <span>{fontOption.shortLabel}</span>
            <span aria-hidden="true">·</span>
            <span className="tabular-nums" data-testid="font-size-indicator">
              اندازه: {toPersianDigits(song.fontSize)}
            </span>
          </div>

          <article
            data-testid="practice-lyrics"
            data-font={fontOption.id}
            style={{
              fontSize: `${song.fontSize}px`,
              lineHeight: fontOption.lineHeight
            }}
            className={`whitespace-pre-line text-center font-normal text-[#1B1E24] dark:text-[#F1EDE4] pb-12 ${fontOption.cssClass}`}
          >
            {song.lyrics}
          </article>
        </div>
      </main>

      {/* Ultra-Slim Floating Mini-Studio Player Dock */}
      <div className="shrink-0 px-3 pb-2.5 pt-1 sm:px-6 sm:pb-3.5">
        <section
          aria-label="پخش‌کننده صوت و ضبط تمرین"
          className="mx-auto max-w-2xl rounded-2xl border border-[#E5DEC9]/90 bg-[#FFFFFF] px-3.5 py-2.5 shadow-[0_10px_28px_-10px_rgba(14,107,100,0.16),0_3px_10px_rgba(27,30,36,0.06)] dark:border-[#253445] dark:bg-[#151F2B] dark:shadow-[0_14px_34px_-10px_rgba(0,0,0,0.7)] sm:px-4"
        >
          <div className="space-y-1.5">
            {/* Row 1: Compact Track Pills + Inline Tabular Time Counter */}
            <div className="flex items-center justify-between gap-2.5">
              {song.tracks.length > 0 ? (
                <div
                  className="no-scrollbar flex min-w-0 flex-1 items-center gap-1.5 overflow-x-auto"
                  role="group"
                  aria-label="انتخاب فایل صوتی یا ضبط‌شده"
                >
                  {song.tracks.map((track) => {
                    const isSelected = activeTrack?.trackId === track.trackId;
                    const isRec = track.trackType === 'recording';
                    return (
                      <button
                        key={track.trackId}
                        type="button"
                        aria-pressed={isSelected}
                        title={track.title}
                        onClick={() => setSelectedTrackId(track.trackId)}
                        className={`inline-flex h-7 max-w-[175px] shrink-0 items-center gap-1 rounded-full px-2.5 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-[#0E6B64] ${
                          isSelected
                            ? isRec
                              ? 'bg-[#F8E7D2] text-[#7A3F08] dark:bg-[#3A2814] dark:text-[#F6C88A]'
                              : 'bg-[#0E6B64] text-white dark:bg-[#2EA89C] dark:text-[#052320]'
                            : 'bg-[#F3EFE6] text-[#525866] hover:bg-[#EAE3D5] hover:text-[#1B1E24] dark:bg-[#1E2B3A] dark:text-[#9CB0C4] dark:hover:bg-[#263648] dark:hover:text-[#F1EDE4]'
                        }`}
                      >
                        {isRec ? (
                          <Mic className="h-3 w-3 shrink-0" aria-hidden="true" />
                        ) : (
                          <Music className="h-3 w-3 shrink-0" aria-hidden="true" />
                        )}
                        <span dir="auto" className="truncate">{track.title}</span>
                      </button>
                    );
                  })}
                </div>
              ) : (
                <span className="text-xs font-medium text-[#6B7280] dark:text-[#8496AA]">
                  بدون فایل صوتی
                </span>
              )}

              {activeTrack && (
                <div
                  dir="ltr"
                  className="flex shrink-0 items-center gap-1 font-mono text-[11px] font-semibold tracking-tight text-[#5C6370] dark:text-[#9CB0C4] tabular-nums"
                >
                  <span data-testid="audio-current-time">
                    {formatTimeSeconds(currentSec)}
                  </span>
                  <span aria-hidden="true" className="opacity-50">/</span>
                  <span data-testid="audio-duration-time">
                    {formatTimeSeconds(durationSec)}
                  </span>
                </div>
              )}
            </div>

            {audioError && (
              <p role="alert" className="text-xs font-medium text-[#B3261E] dark:text-[#F28B82]">
                {audioError}
              </p>
            )}
            {recorderError && (
              <p role="alert" className="text-xs font-medium text-[#B3261E] dark:text-[#F28B82]">
                {recorderError}
              </p>
            )}

            {/* Slim Interactive Scrubber */}
            {activeTrack && (
              <>
                {audioUrl && (
                  <audio
                    ref={audioRef}
                    src={audioUrl}
                    preload="metadata"
                    onLoadedMetadata={(e) => {
                      const dur = e.currentTarget.duration;
                      if (Number.isFinite(dur) && dur > 0) {
                        setDurationSec(dur);
                      }
                      e.currentTarget.playbackRate = playbackSpeed;
                    }}
                    onTimeUpdate={(e) => {
                      setCurrentSec(e.currentTarget.currentTime);
                    }}
                    onEnded={() => {
                      setIsPlaying(false);
                      setCurrentSec(0);
                    }}
                  />
                )}

                {(() => {
                  const safeDuration = durationSec > 0 ? durationSec : 1;
                  const clampedSec = Math.min(Math.max(currentSec, 0), safeDuration);
                  const progressPct = durationSec > 0 ? (clampedSec / safeDuration) * 100 : 0;

                  return (
                    <div dir="ltr" className="w-full">
                      <label htmlFor="audio-progress-slider" className="sr-only">
                        نوار زمان پخش
                      </label>
                      <div className="group/scrubber relative flex h-4 w-full items-center">
                        <div className="pointer-events-none relative h-1 w-full overflow-hidden rounded-full bg-[#EAE4D7] transition-all group-hover/scrubber:h-1.5 dark:bg-[#243242]">
                          <div
                            style={{ width: `${progressPct}%` }}
                            className="h-full rounded-full bg-gradient-to-r from-[#0E6B64] to-[#1A9A8F] dark:from-[#2EA89C] dark:to-[#56D8CC]"
                          />
                        </div>
                        <div
                          style={{ left: `calc(${progressPct}% - 6px)` }}
                          className="pointer-events-none absolute top-1/2 h-3 w-3 -translate-y-1/2 rounded-full bg-[#0E6B64] ring-2 ring-[#FFFFFF] shadow-xs transition-transform duration-150 group-hover/scrubber:scale-125 dark:bg-[#56D8CC] dark:ring-[#151F2B]"
                        />
                        <input
                          id="audio-progress-slider"
                          type="range"
                          min={0}
                          max={safeDuration}
                          step={0.1}
                          value={clampedSec}
                          disabled={!audioUrl || durationSec <= 0}
                          onChange={(e) => handleSeekTo(Number(e.target.value))}
                          className="absolute inset-0 z-10 h-full w-full cursor-pointer opacity-0 disabled:cursor-default"
                        />
                      </div>
                    </div>
                  );
                })()}
              </>
            )}

            {/* Row 2: Single Unified 44px Control Bar (Speed on Right + 5s/Play/5s Center + Record on Left) */}
            <div className="flex items-center justify-between gap-2 pt-0.5">
              {/* Speed Control (RTL start = Right): Compact Popover Pill on Mobile / Inline 4-Speed Pill on Desktop */}
              {activeTrack && (
                <div ref={speedMenuContainerRef} className="relative shrink-0">
                  {/* Mobile Speed Popover Trigger */}
                  <button
                    type="button"
                    dir="ltr"
                    onClick={() => setSpeedMenuOpen((prev) => !prev)}
                    aria-expanded={speedMenuOpen}
                    aria-haspopup="true"
                    aria-label={`سرعت پخش: ${formatPlaybackSpeedLabel(playbackSpeed)}`}
                    className="inline-flex h-10 items-center gap-1 rounded-full bg-[#F3EFE6] px-3 text-xs font-bold text-[#1B1E24] transition-colors hover:bg-[#EAE3D5] dark:bg-[#1E2B3A] dark:text-[#F1EDE4] dark:hover:bg-[#273647] focus-visible:outline-2 focus-visible:outline-[#0E6B64] sm:hidden tabular-nums"
                  >
                    <span>{formatPlaybackSpeedLabel(playbackSpeed)}</span>
                    <ChevronUp
                      className={`h-3.5 w-3.5 transition-transform duration-150 ${
                        speedMenuOpen ? 'rotate-180' : ''
                      }`}
                      aria-hidden="true"
                    />
                  </button>

                  {speedMenuOpen && (
                    <div
                      dir="ltr"
                      role="group"
                      aria-label="انتخاب سرعت پخش"
                      className="absolute bottom-12 right-0 z-30 flex items-center gap-1 rounded-full border border-[#E5DEC9] bg-[#FFFFFF] p-1 shadow-xl dark:border-[#2A394A] dark:bg-[#1A2635] sm:hidden"
                    >
                      {PLAYBACK_SPEEDS.map((speed) => {
                        const active = Math.abs(playbackSpeed - speed) < 0.01;
                        const label = formatPlaybackSpeedLabel(speed);
                        return (
                          <button
                            key={speed}
                            type="button"
                            aria-pressed={active}
                            aria-label={`سرعت ${label}`}
                            onClick={() => {
                              setPlaybackSpeed(speed);
                              setSpeedMenuOpen(false);
                            }}
                            className={`h-9 rounded-full px-2.5 text-xs font-bold tabular-nums transition-colors ${
                              active
                                ? 'bg-[#0E6B64] text-white dark:bg-[#2EA89C] dark:text-[#052320]'
                                : 'text-[#525866] hover:bg-[#F3EFE6] dark:text-[#9CB0C4] dark:hover:bg-[#243447]'
                            }`}
                          >
                            {label}
                          </button>
                        );
                      })}
                    </div>
                  )}

                  {/* Desktop Inline 4-Speed Segmented Pill */}
                  <div
                    dir="ltr"
                    className="hidden sm:inline-flex items-center rounded-full bg-[#F3EFE6] p-1 dark:bg-[#0D131A]"
                    role="group"
                    aria-label="سرعت پخش"
                  >
                    {PLAYBACK_SPEEDS.map((speed) => {
                      const active = Math.abs(playbackSpeed - speed) < 0.01;
                      const label = formatPlaybackSpeedLabel(speed);
                      return (
                        <button
                          key={speed}
                          type="button"
                          aria-pressed={active}
                          aria-label={`سرعت ${label}`}
                          onClick={() => setPlaybackSpeed(speed)}
                          className={`min-h-[32px] rounded-full px-2.5 text-xs font-bold tabular-nums transition-all duration-150 focus-visible:outline-2 focus-visible:outline-[#0E6B64] ${
                            active
                              ? 'bg-[#FFFFFF] text-[#0E6B64] shadow-xs dark:bg-[#233242] dark:text-[#56D8CC]'
                              : 'text-[#5C6370] hover:text-[#1B1E24] dark:text-[#8FA3B8] dark:hover:text-[#F1EDE4]'
                          }`}
                        >
                          {label}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Center Transport (-5s · Play · +5s) */}
              {activeTrack && (
                <div dir="ltr" className="flex items-center justify-center gap-1.5 sm:gap-2.5">
                  <button
                    type="button"
                    onClick={() => handleSeekDelta(-5)}
                    disabled={!audioUrl}
                    aria-label="۵ ثانیه به عقب"
                    className="flex h-10 min-w-[42px] items-center justify-center gap-0.5 rounded-full px-1.5 text-xs font-bold text-[#3D434F] transition-colors hover:bg-[#F3EFE6] hover:text-[#0E6B64] active:scale-95 disabled:opacity-35 dark:text-[#C4D1DF] dark:hover:bg-[#1E2B3A] dark:hover:text-[#56D8CC] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
                  >
                    <RotateCcw className="h-4 w-4" aria-hidden="true" />
                    <span className="tabular-nums">5</span>
                  </button>

                  <button
                    type="button"
                    onClick={handleTogglePlayPause}
                    disabled={!audioUrl}
                    aria-label={isPlaying ? 'توقف' : 'پخش'}
                    className="flex h-11 w-11 items-center justify-center rounded-full bg-gradient-to-br from-[#14867D] to-[#0A5750] text-white shadow-[0_6px_16px_-4px_rgba(14,107,100,0.45)] transition-all duration-150 hover:brightness-105 active:scale-[0.95] disabled:opacity-40 dark:from-[#4AD4C6] dark:to-[#279E93] dark:text-[#052320] dark:shadow-[0_6px_18px_-4px_rgba(74,212,198,0.35)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0E6B64]"
                  >
                    {isPlaying ? (
                      <Pause className="h-5 w-5 fill-current" aria-hidden="true" />
                    ) : (
                      <Play className="h-5 w-5 translate-x-0.5 fill-current" aria-hidden="true" />
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => handleSeekDelta(5)}
                    disabled={!audioUrl}
                    aria-label="۵ ثانیه به جلو"
                    className="flex h-10 min-w-[42px] items-center justify-center gap-0.5 rounded-full px-1.5 text-xs font-bold text-[#3D434F] transition-colors hover:bg-[#F3EFE6] hover:text-[#0E6B64] active:scale-95 disabled:opacity-35 dark:text-[#C4D1DF] dark:hover:bg-[#1E2B3A] dark:hover:text-[#56D8CC] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
                  >
                    <span className="tabular-nums">5</span>
                    <RotateCw className="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
              )}

              {/* Record Action (RTL end = Left) */}
              <button
                type="button"
                onClick={handleStartOrStopRecording}
                aria-label={isRecording ? 'پایان ضبط تمرین' : 'ضبط تمرین جدید'}
                className={`inline-flex h-10 items-center justify-center gap-1.5 rounded-full px-3 text-xs font-bold transition-all duration-150 active:scale-[0.97] focus-visible:outline-2 focus-visible:outline-offset-2 ${
                  !activeTrack ? 'w-full' : 'shrink-0'
                } ${
                  isRecording
                    ? 'bg-[#C62828] text-white shadow-sm shadow-[#C62828]/25 hover:bg-[#AD1F1F] dark:bg-[#F28B82] dark:text-[#3E0A06] focus-visible:outline-[#C62828]'
                    : 'bg-[#FAF0E4] text-[#85460B] hover:bg-[#F4E3CE] dark:bg-[#2A2015] dark:text-[#F5C07A] dark:hover:bg-[#382B1C] focus-visible:outline-[#A86416]'
                }`}
              >
                {isRecording ? (
                  <>
                    <Square className="h-3.5 w-3.5 shrink-0 fill-current" aria-hidden="true" />
                    <span className="tabular-nums">
                      پایان ({formatTimeSeconds(recordingSec)})
                    </span>
                  </>
                ) : (
                  <>
                    <Mic className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                    <span className="sm:hidden">ضبط</span>
                    <span className="hidden sm:inline">ضبط تمرین</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

