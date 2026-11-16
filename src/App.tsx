import { useEffect, useMemo, useState } from 'react';
import { AddEditScreen } from './components/AddEditScreen';
import { AndroidSourceModal } from './components/AndroidSourceModal';
import { HomeScreen } from './components/HomeScreen';
import { PracticeScreen } from './components/PracticeScreen';
import {
  Category,
  clampFontSize,
  filterSongs,
  FONT_STEP,
  MediaTrack,
  PersianFontId,
  Song,
  validateCategoryName
} from './utils/naghmehLogic';
import {
  deleteAudioBlob,
  loadCategoriesFromStorage,
  loadFontFromStorage,
  loadSongsFromStorage,
  loadThemeFromStorage,
  saveCategoriesToStorage,
  saveFontToStorage,
  saveSongsToStorage,
  saveThemeToStorage
} from './utils/storage';

type AppRoute =
  | { screen: 'home' }
  | { screen: 'practice'; songId: string }
  | { screen: 'add_edit'; songId: string | null };

export default function App() {
  const [songs, setSongs] = useState<Song[]>(() => loadSongsFromStorage());
  const [categories, setCategories] = useState<Category[]>(() => loadCategoriesFromStorage());
  const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);
  const [selectedFontId, setSelectedFontId] = useState<PersianFontId>(() => loadFontFromStorage());
  const [isDarkTheme, setIsDarkTheme] = useState<boolean>(() => loadThemeFromStorage());
  const [searchQuery, setSearchQuery] = useState('');
  const [route, setRoute] = useState<AppRoute>({ screen: 'home' });
  const [showAndroidCode, setShowAndroidCode] = useState(false);

  useEffect(() => {
    saveSongsToStorage(songs);
  }, [songs]);

  useEffect(() => {
    saveCategoriesToStorage(categories);
  }, [categories]);

  useEffect(() => {
    saveFontToStorage(selectedFontId);
  }, [selectedFontId]);

  useEffect(() => {
    saveThemeToStorage(isDarkTheme);
    document.documentElement.classList.toggle('dark', isDarkTheme);
  }, [isDarkTheme]);

  const handleToggleTheme = () => {
    setIsDarkTheme((prev) => !prev);
  };

  const handleCreateCategory = (rawName: string): Category | null => {
    const trimmed = rawName.trim();
    if (validateCategoryName(trimmed, categories) !== null) {
      return (
        categories.find((c) => c.name.trim().toLowerCase() === trimmed.toLowerCase()) ?? null
      );
    }
    const created: Category = {
      id: `cat-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      name: trimmed,
      createdAt: Date.now()
    };
    setCategories((prev) => [...prev, created]);
    return created;
  };

  const handleDeleteCategory = (categoryId: string) => {
    setCategories((prev) => prev.filter((c) => c.id !== categoryId));
    setSongs((prev) =>
      prev.map((song) =>
        song.categoryId === categoryId ? { ...song, categoryId: null } : song
      )
    );
    if (selectedCategoryId === categoryId) {
      setSelectedCategoryId(null);
    }
  };

  const filteredSongs = useMemo(
    () => filterSongs(songs, searchQuery, selectedCategoryId),
    [songs, searchQuery, selectedCategoryId]
  );

  const activePracticeSong =
    route.screen === 'practice'
      ? songs.find((s) => s.id === route.songId) ?? null
      : null;

  const activeEditSong =
    route.screen === 'add_edit' && route.songId
      ? songs.find((s) => s.id === route.songId) ?? null
      : null;

  const handleAdjustFontSize = (songId: string, delta: number) => {
    setSongs((prev) =>
      prev.map((song) =>
        song.id === songId
          ? { ...song, fontSize: clampFontSize(song.fontSize + delta) }
          : song
      )
    );
  };

  const handleAddRecordedTrack = (songId: string, track: MediaTrack) => {
    setSongs((prev) =>
      prev.map((song) =>
        song.id === songId
          ? { ...song, tracks: [...song.tracks, track] }
          : song
      )
    );
  };

  const handleSaveSong = (saved: Song) => {
    setSongs((prev) => {
      const exists = prev.some((s) => s.id === saved.id);
      if (exists) {
        return prev.map((s) => (s.id === saved.id ? saved : s));
      }
      return [saved, ...prev];
    });
    setRoute({ screen: 'home' });
  };

  const handleDeleteSong = (song: Song) => {
    setSongs((prev) => prev.filter((s) => s.id !== song.id));
    song.tracks.forEach((track) => {
      if (!track.builtInAudioKey) {
        deleteAudioBlob(track.audioId);
      }
    });
    if (route.screen === 'practice' && route.songId === song.id) {
      setRoute({ screen: 'home' });
    }
  };

  return (
    <>
      {route.screen === 'home' && (
        <HomeScreen
          songs={filteredSongs}
          categories={categories}
          selectedCategoryId={selectedCategoryId}
          searchQuery={searchQuery}
          isDarkTheme={isDarkTheme}
          onToggleTheme={handleToggleTheme}
          onSelectCategory={setSelectedCategoryId}
          onCreateCategory={handleCreateCategory}
          onDeleteCategory={handleDeleteCategory}
          onSearchQueryChange={setSearchQuery}
          onSelectSong={(songId) => setRoute({ screen: 'practice', songId })}
          onEditSong={(songId) => setRoute({ screen: 'add_edit', songId })}
          onAddSong={() => setRoute({ screen: 'add_edit', songId: null })}
          onDeleteSong={handleDeleteSong}
          onOpenAndroidCode={() => setShowAndroidCode(true)}
        />
      )}

      {route.screen === 'practice' && activePracticeSong && (
        <PracticeScreen
          song={activePracticeSong}
          selectedFontId={selectedFontId}
          isDarkTheme={isDarkTheme}
          onToggleTheme={handleToggleTheme}
          onSelectFont={setSelectedFontId}
          onBack={() => setRoute({ screen: 'home' })}
          onEdit={(songId) => setRoute({ screen: 'add_edit', songId })}
          onIncreaseFontSize={(songId) => handleAdjustFontSize(songId, FONT_STEP)}
          onDecreaseFontSize={(songId) => handleAdjustFontSize(songId, -FONT_STEP)}
          onAddRecordedTrack={handleAddRecordedTrack}
        />
      )}

      {route.screen === 'add_edit' && (
        <AddEditScreen
          existingSong={activeEditSong}
          categories={categories}
          isDarkTheme={isDarkTheme}
          onToggleTheme={handleToggleTheme}
          onCreateCategory={handleCreateCategory}
          onSave={handleSaveSong}
          onCancel={() => {
            if (activeEditSong) {
              setRoute({ screen: 'practice', songId: activeEditSong.id });
            } else {
              setRoute({ screen: 'home' });
            }
          }}
        />
      )}

      {showAndroidCode && (
        <AndroidSourceModal onClose={() => setShowAndroidCode(false)} />
      )}
    </>
  );
}
