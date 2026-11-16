import test from 'node:test';
import assert from 'node:assert/strict';
import {
  clampFontSize,
  computeSeekTime,
  createRehearsalToneWavBytes,
  filterSongs,
  formatPlaybackSpeedLabel,
  formatRecordingTimestamp,
  formatTimeSeconds,
  getFontOption,
  getLyricsPreview,
  getNextMusicTrackTitle,
  getNextRecordingTrackTitle,
  INITIAL_SEED_CATEGORIES,
  INITIAL_SEED_SONGS,
  MAX_FONT_SIZE,
  MIN_FONT_SIZE,
  PLAYBACK_SPEEDS,
  toPersianDigits,
  validateCategoryName,
  validateSongForm
} from './naghmehLogic';

test('clampFontSize respects min and max reading bounds', () => {
  assert.equal(clampFontSize(10), MIN_FONT_SIZE);
  assert.equal(clampFontSize(64), MAX_FONT_SIZE);
  assert.equal(clampFontSize(24), 24);
  assert.equal(clampFontSize(Number.NaN), 20);
});

test('formatTimeSeconds formats mm:ss accurately', () => {
  assert.equal(formatTimeSeconds(0), '00:00');
  assert.equal(formatTimeSeconds(9), '00:09');
  assert.equal(formatTimeSeconds(65), '01:05');
  assert.equal(formatTimeSeconds(125.9), '02:05');
  assert.equal(formatTimeSeconds(-4), '00:00');
});

test('computeSeekTime clamps forward and backward 5s seeks', () => {
  assert.equal(computeSeekTime(3, -5, 30), 0);
  assert.equal(computeSeekTime(15, -5, 30), 10);
  assert.equal(computeSeekTime(27, 5, 30), 30);
  assert.equal(computeSeekTime(10, 5, 30), 15);
});

test('filterSongs searches both title and lyrics and filters by categoryId', () => {
  const byTitle = filterSongs(INITIAL_SEED_SONGS, 'حافظ');
  assert.equal(byTitle.length, 1);
  assert.equal(byTitle[0].id, 'seed-1');
  assert.equal(byTitle[0].tracks.length, 2);

  const byLyrics = filterSongs(INITIAL_SEED_SONGS, 'نیستان');
  assert.equal(byLyrics.length, 1);
  assert.equal(byLyrics[0].id, 'seed-3');

  const byCategory = filterSongs(INITIAL_SEED_SONGS, '', 'cat-hafez');
  assert.equal(byCategory.length, 1);
  assert.equal(byCategory[0].id, 'seed-1');

  const byClassicAndQuery = filterSongs(INITIAL_SEED_SONGS, 'رودکی', 'cat-classic');
  assert.equal(byClassicAndQuery.length, 1);
  assert.equal(byClassicAndQuery[0].id, 'seed-2');

  const all = filterSongs(INITIAL_SEED_SONGS, '   ');
  assert.equal(all.length, INITIAL_SEED_SONGS.length);
});

test('validateCategoryName rejects blank and duplicate names', () => {
  assert.ok(validateCategoryName('   ', INITIAL_SEED_CATEGORIES) !== null);
  assert.ok(validateCategoryName('شعر حافظ', INITIAL_SEED_CATEGORIES) !== null);
  assert.equal(validateCategoryName('شعر سعدی', INITIAL_SEED_CATEGORIES), null);
});

test('getLyricsPreview extracts first two non-empty lines', () => {
  const preview = getLyricsPreview('\n  سطر اول  \n\nسطر دوم\nسطر سوم');
  assert.equal(preview, 'سطر اول\nسطر دوم');
});

test('validateSongForm rejects blank title or lyrics', () => {
  assert.ok(validateSongForm('', 'متن شعر') !== null);
  assert.ok(validateSongForm('عنوان', '   ') !== null);
  assert.equal(validateSongForm('غزل', 'بیت اول'), null);
});

test('getFontOption resolves Vazirmatn, Nastaliq, and Sahel with visible Persian titles', () => {
  assert.equal(getFontOption('vazirmatn').shortLabel, 'فونت وزیر');
  assert.equal(getFontOption('nastaliq').shortLabel, 'فونت نستعلیق');
  assert.equal(getFontOption('sahel').shortLabel, 'فونت ساحل');
  assert.equal(getFontOption('unknown').id, 'vazirmatn');
});

test('sequential Persian track and recording naming helpers', () => {
  assert.equal(toPersianDigits(12), '۱۲');
  assert.equal(getNextMusicTrackTitle(0), 'آهنگ ۱');
  assert.equal(getNextMusicTrackTitle(1), 'آهنگ ۲');
  assert.equal(getNextRecordingTrackTitle(0), 'ضبط صدا ۱');
  assert.equal(getNextRecordingTrackTitle(2), 'ضبط صدا ۳');
  assert.equal(formatRecordingTimestamp(new Date(2026, 10, 15, 9, 5)), '۰۹:۰۵ · ۱۱/۱۵');
});

test('PLAYBACK_SPEEDS exposes 0.75x, 0.9x, 1.0x, and 1.5x', () => {
  assert.deepEqual([...PLAYBACK_SPEEDS], [0.75, 0.9, 1.0, 1.5]);
  assert.equal(formatPlaybackSpeedLabel(1.5), '1.5x');
  assert.equal(formatPlaybackSpeedLabel(1.0), '1.0x');
});

test('createRehearsalToneWavBytes builds valid RIFF/WAVE PCM header for original and playback tracks', () => {
  const wavOrig = createRehearsalToneWavBytes('homayoun', 2);
  const wavPb = createRehearsalToneWavBytes('homayoun_playback', 2);
  assert.equal(String.fromCharCode(wavOrig[0], wavOrig[1], wavOrig[2], wavOrig[3]), 'RIFF');
  assert.equal(String.fromCharCode(wavPb[8], wavPb[9], wavPb[10], wavPb[11]), 'WAVE');
  assert.equal(wavOrig.byteLength, 44 + 22050 * 2 * 2);
});
