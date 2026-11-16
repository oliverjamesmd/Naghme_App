import React, { useMemo, useState } from 'react';
import { Code2, FolderOpen, Moon, Music, Pencil, Plus, Search, Sun, Trash2, X } from 'lucide-react';
import {
  Category,
  getLyricsPreview,
  Song,
  toPersianDigits,
  validateCategoryName
} from '../utils/naghmehLogic';

interface HomeScreenProps {
  songs: Song[];
  categories: Category[];
  selectedCategoryId: string | null;
  searchQuery: string;
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onSelectCategory: (categoryId: string | null) => void;
  onCreateCategory: (name: string) => Category | null;
  onDeleteCategory: (categoryId: string) => void;
  onSearchQueryChange: (query: string) => void;
  onSelectSong: (songId: string) => void;
  onEditSong: (songId: string) => void;
  onAddSong: () => void;
  onDeleteSong: (song: Song) => void;
  onOpenAndroidCode: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  songs,
  categories,
  selectedCategoryId,
  searchQuery,
  isDarkTheme,
  onToggleTheme,
  onSelectCategory,
  onCreateCategory,
  onDeleteCategory,
  onSearchQueryChange,
  onSelectSong,
  onEditSong,
  onAddSong,
  onDeleteSong,
  onOpenAndroidCode
}) => {
  const [songPendingDelete, setSongPendingDelete] = useState<Song | null>(null);
  const [categoryPendingDelete, setCategoryPendingDelete] = useState<Category | null>(null);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [categoryError, setCategoryError] = useState<string | null>(null);

  const categoryNameById = useMemo(() => {
    const map = new Map<string, string>();
    categories.forEach((cat) => map.set(cat.id, cat.name));
    return map;
  }, [categories]);

  const handleAddCategorySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const validationErr = validateCategoryName(newCategoryName, categories);
    if (validationErr) {
      setCategoryError(validationErr);
      return;
    }
    onCreateCategory(newCategoryName);
    setNewCategoryName('');
    setCategoryError(null);
  };

  return (
    <div className="min-h-dvh bg-[#F8F6F1] text-[#1B1E24] dark:bg-[#0E141B] dark:text-[#F1EDE4]">
      <header className="border-b border-[#DDD6C8] bg-[#FFFFFF] dark:border-[#2A394A] dark:bg-[#161F2A]">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-2 px-4 py-3.5 sm:px-6 sm:py-4">
          <h1 className="truncate text-lg font-bold tracking-tight text-[#1B1E24] dark:text-[#F1EDE4] sm:text-2xl">
            شعرها و آهنگ‌های من
          </h1>

          <div className="flex items-center gap-2 shrink-0">
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

            <button
              type="button"
              onClick={onOpenAndroidCode}
              className="inline-flex min-h-[44px] items-center gap-2 rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-3 sm:px-4 py-2 text-sm font-semibold text-[#1B1E24] transition-colors hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0E6B64] whitespace-nowrap"
            >
              <Code2 className="h-4 w-4 text-[#0E6B64] dark:text-[#3CB8AC]" aria-hidden="true" />
              <span>کد اندروید</span>
            </button>

            <button
              type="button"
              onClick={onAddSong}
              className="hidden sm:inline-flex min-h-[44px] items-center gap-2 rounded-xl bg-[#0E6B64] px-5 py-2 text-sm font-bold text-white transition-colors hover:bg-[#0A554F] dark:bg-[#3CB8AC] dark:text-[#072623] dark:hover:bg-[#52C9BD] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0E6B64] whitespace-nowrap"
            >
              <Plus className="h-5 w-5" aria-hidden="true" />
              <span>افزودن شعر جدید</span>
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-4 py-6 pb-28 sm:px-6 sm:pb-12">
        <div className="mb-3.5">
          <label htmlFor="song-search-input" className="sr-only">
            جستجو در عنوان یا متن شعر
          </label>
          <div className="relative">
            <Search
              className="pointer-events-none absolute right-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#525866] dark:text-[#9FB0C2]"
              aria-hidden="true"
            />
            <input
              id="song-search-input"
              type="search"
              value={searchQuery}
              onChange={(e) => onSearchQueryChange(e.target.value)}
              placeholder="جستجو در عنوان یا متن شعر..."
              className="min-h-[52px] w-full rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] pr-12 pl-12 text-base text-[#1B1E24] placeholder:text-[#6B7280] focus:border-[#0E6B64] focus:outline-none focus:ring-2 focus:ring-[#0E6B64]/20 dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:placeholder:text-[#8496AA] dark:focus:border-[#3CB8AC] dark:focus:ring-[#3CB8AC]/25"
            />
            {searchQuery.length > 0 && (
              <button
                type="button"
                onClick={() => onSearchQueryChange('')}
                aria-label="پاک کردن جستجو"
                className="absolute left-2 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-xl text-[#525866] hover:bg-[#EFECE4] hover:text-[#1B1E24] dark:text-[#9FB0C2] dark:hover:bg-[#1F2B38] dark:hover:text-[#F1EDE4] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
              >
                <X className="h-5 w-5" aria-hidden="true" />
              </button>
            )}
          </div>
        </div>

        {/* Category Filter Bar + "+ دسته جدید" */}
        <div
          role="group"
          aria-label="فیلتر دسته‌بندی شعرها"
          className="mb-6 flex items-center gap-2 overflow-x-auto pb-1"
        >
          <button
            type="button"
            aria-pressed={selectedCategoryId === null}
            onClick={() => onSelectCategory(null)}
            className={`min-h-[42px] shrink-0 rounded-xl border px-4 py-1.5 text-sm whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-[#0E6B64] ${
              selectedCategoryId === null
                ? 'border-[#0E6B64] bg-[#D6ECE9] font-bold text-[#063A36] dark:border-[#3CB8AC] dark:bg-[#163D3A] dark:text-[#BFECE6]'
                : 'border-[#DDD6C8] bg-[#FFFFFF] font-medium text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#161F2A] dark:text-[#F1EDE4] dark:hover:bg-[#1F2B38]'
            }`}
          >
            همه
          </button>

          {categories.map((cat) => {
            const active = selectedCategoryId === cat.id;
            return (
              <button
                key={cat.id}
                type="button"
                aria-pressed={active}
                onClick={() => onSelectCategory(cat.id)}
                className={`min-h-[42px] shrink-0 rounded-xl border px-4 py-1.5 text-sm whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-[#0E6B64] ${
                  active
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
              setCategoryError(null);
              setShowCategoryModal(true);
            }}
            className="inline-flex min-h-[42px] shrink-0 items-center gap-1.5 rounded-xl border border-dashed border-[#0E6B64]/60 bg-[#FAF8F3] px-3.5 py-1.5 text-sm font-bold text-[#0E6B64] hover:bg-[#D6ECE9]/40 dark:border-[#3CB8AC]/60 dark:bg-[#121A24] dark:text-[#3CB8AC] dark:hover:bg-[#163D3A]/45 whitespace-nowrap focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
          >
            <Plus className="h-4 w-4" aria-hidden="true" />
            <span>دسته جدید / مدیریت</span>
          </button>
        </div>

        {songs.length === 0 ? (
          <section
            aria-label="فهرست خالی شعرها"
            className="rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] px-6 py-16 text-center dark:border-[#2A394A] dark:bg-[#161F2A]"
          >
            <h2 className="text-xl font-bold text-[#1B1E24] dark:text-[#F1EDE4]">
              {searchQuery.trim() || selectedCategoryId
                ? 'شعری در این دسته یا جستجو پیدا نشد'
                : 'هنوز شعری اضافه نشده است'}
            </h2>
            <p className="mt-2 text-base text-[#525866] dark:text-[#9FB0C2]">
              {searchQuery.trim() || selectedCategoryId
                ? 'دسته «همه» را انتخاب کنید یا عبارت جستجو را تغییر دهید.'
                : 'برای شروع، دکمه «افزودن شعر جدید» را لمس کنید.'}
            </p>
            {searchQuery.trim() || selectedCategoryId ? (
              <button
                type="button"
                onClick={() => {
                  onSearchQueryChange('');
                  onSelectCategory(null);
                }}
                className="mt-6 inline-flex min-h-[48px] items-center justify-center rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-6 py-2.5 text-base font-semibold text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646]"
              >
                نمایش همه شعرها
              </button>
            ) : (
              <button
                type="button"
                onClick={onAddSong}
                className="mt-6 inline-flex min-h-[48px] items-center justify-center gap-2 rounded-xl bg-[#0E6B64] px-6 py-2.5 text-base font-bold text-white hover:bg-[#0A554F] dark:bg-[#3CB8AC] dark:text-[#072623] dark:hover:bg-[#52C9BD]"
              >
                <Plus className="h-5 w-5" aria-hidden="true" />
                <span>افزودن شعر جدید</span>
              </button>
            )}
          </section>
        ) : (
          <ul
            aria-label="فهرست شعرها"
            className="grid grid-cols-1 gap-3 sm:gap-4 lg:grid-cols-2"
            data-testid="songs-list"
          >
            {songs.map((song) => {
              const preview = getLyricsPreview(song.lyrics, 2);
              const categoryName = song.categoryId ? categoryNameById.get(song.categoryId) : null;
              const trackSummary =
                song.tracks.length === 0
                  ? null
                  : song.tracks.length === 1
                  ? song.tracks[0].title
                  : `${toPersianDigits(song.tracks.length)} فایل صوتی و ضبط‌شده`;

              return (
                <li
                  key={song.id}
                  className="flex flex-col justify-between rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-4 sm:p-5 transition-colors hover:border-[#0E6B64]/50 dark:border-[#2A394A] dark:bg-[#161F2A] dark:hover:border-[#3CB8AC]/55"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <button
                        type="button"
                        onClick={() => onSelectSong(song.id)}
                        className="min-h-[44px] flex-1 text-right text-lg sm:text-xl font-bold leading-snug text-[#1B1E24] hover:text-[#0E6B64] dark:text-[#F1EDE4] dark:hover:text-[#3CB8AC] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0E6B64] rounded-lg"
                      >
                        {song.title}
                      </button>

                      <div className="flex items-center gap-1 shrink-0">
                        <button
                          type="button"
                          onClick={() => onEditSong(song.id)}
                          aria-label={`ویرایش ${song.title}`}
                          className="flex h-11 w-11 items-center justify-center rounded-xl text-[#525866] transition-colors hover:bg-[#EFECE4] hover:text-[#1B1E24] dark:text-[#9FB0C2] dark:hover:bg-[#1F2B38] dark:hover:text-[#F1EDE4] focus-visible:outline-2 focus-visible:outline-[#0E6B64]"
                        >
                          <Pencil className="h-5 w-5" aria-hidden="true" />
                        </button>
                        <button
                          type="button"
                          onClick={() => setSongPendingDelete(song)}
                          aria-label={`حذف ${song.title}`}
                          className="flex h-11 w-11 items-center justify-center rounded-xl text-[#B3261E] transition-colors hover:bg-[#B3261E]/10 dark:text-[#F28B82] dark:hover:bg-[#F28B82]/15 focus-visible:outline-2 focus-visible:outline-[#B3261E]"
                        >
                          <Trash2 className="h-5 w-5" aria-hidden="true" />
                        </button>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => onSelectSong(song.id)}
                      aria-label={`تمرین ${song.title}`}
                      className="mt-1.5 block w-full text-right rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0E6B64]"
                    >
                      <p className="whitespace-pre-line text-base leading-relaxed text-[#525866] dark:text-[#9FB0C2] line-clamp-2">
                        {preview}
                      </p>
                    </button>
                  </div>

                  <div className="mt-3 sm:mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-[#EFECE4] pt-2.5 sm:pt-3 dark:border-[#2A394A]">
                    <div className="flex flex-wrap items-center gap-3 min-w-0 text-sm">
                      {trackSummary ? (
                        <div className="flex items-center gap-1.5 font-medium text-[#0E6B64] dark:text-[#3CB8AC]">
                          <Music className="h-4 w-4 shrink-0" aria-hidden="true" />
                          <span className="truncate max-w-[165px]">{trackSummary}</span>
                        </div>
                      ) : (
                        <span className="text-[#6B7280] dark:text-[#8496AA]">بدون فایل صوتی</span>
                      )}

                      {categoryName && (
                        <div className="flex items-center gap-1 text-[#525866] dark:text-[#9FB0C2]">
                          <span aria-hidden="true">·</span>
                          <FolderOpen className="h-3.5 w-3.5 shrink-0 text-[#A86416] dark:text-[#E5A952]" aria-hidden="true" />
                          <span className="truncate max-w-[140px]">{categoryName}</span>
                        </div>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={() => onSelectSong(song.id)}
                      className="min-h-[44px] rounded-xl px-3 py-1.5 text-sm font-bold text-[#0E6B64] hover:bg-[#0E6B64]/10 dark:text-[#3CB8AC] dark:hover:bg-[#3CB8AC]/15 focus-visible:outline-2 focus-visible:outline-[#0E6B64] whitespace-nowrap"
                    >
                      شروع تمرین
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </main>

      {/* Mobile primary floating action button */}
      <div className="fixed bottom-4 left-4 z-30 sm:hidden">
        <button
          type="button"
          onClick={onAddSong}
          className="inline-flex min-h-[50px] items-center gap-2 rounded-2xl bg-[#0E6B64] px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-[#0E6B64]/25 transition-transform active:scale-[0.98] dark:bg-[#3CB8AC] dark:text-[#072623] dark:shadow-black/35 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0E6B64]"
        >
          <Plus className="h-5 w-5" aria-hidden="true" />
          <span>افزودن شعر جدید</span>
        </button>
      </div>

      {/* Category Manager Dialog */}
      {showCategoryModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="category-dialog-title"
        >
          <div className="w-full max-w-md rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-5 sm:p-6 shadow-xl dark:border-[#2A394A] dark:bg-[#161F2A]">
            <div className="flex items-center justify-between gap-2">
              <h2 id="category-dialog-title" className="text-xl font-bold text-[#1B1E24] dark:text-[#F1EDE4]">
                دسته‌بندی‌های شعر
              </h2>
              <button
                type="button"
                onClick={() => setShowCategoryModal(false)}
                aria-label="بستن پنجره دسته‌بندی"
                className="flex h-10 w-10 items-center justify-center rounded-xl text-[#525866] hover:bg-[#EFECE4] dark:text-[#9FB0C2] dark:hover:bg-[#1F2B38]"
              >
                <X className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>

            <form onSubmit={handleAddCategorySubmit} className="mt-4 space-y-2.5">
              <label htmlFor="new-category-modal-input" className="sr-only">
                نام دسته‌بندی جدید
              </label>
              <div className="flex min-w-0 gap-2">
                <input
                  id="new-category-modal-input"
                  type="text"
                  value={newCategoryName}
                  onChange={(e) => {
                    setNewCategoryName(e.target.value);
                    setCategoryError(null);
                  }}
                  placeholder="مثلاً: شعر حافظ"
                  className="min-h-[48px] min-w-0 flex-1 rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-3.5 text-base text-[#1B1E24] placeholder:text-[#6B7280] focus:border-[#0E6B64] focus:outline-none dark:border-[#2A394A] dark:bg-[#0E141B] dark:text-[#F1EDE4] dark:placeholder:text-[#8496AA] dark:focus:border-[#3CB8AC]"
                />
                <button
                  type="submit"
                  className="min-h-[48px] shrink-0 rounded-xl bg-[#0E6B64] px-4 text-sm font-bold text-white hover:bg-[#0A554F] dark:bg-[#3CB8AC] dark:text-[#072623] dark:hover:bg-[#52C9BD]"
                >
                  افزودن
                </button>
              </div>
              {categoryError && (
                <p role="alert" className="text-sm font-medium text-[#B3261E] dark:text-[#F28B82]">
                  {categoryError}
                </p>
              )}
            </form>

            {categories.length > 0 && (
              <ul
                aria-label="لیست دسته‌بندی‌ها"
                className="mt-5 max-h-60 space-y-2 overflow-y-auto"
              >
                {categories.map((cat) => (
                  <li
                    key={cat.id}
                    className="flex items-center justify-between gap-2 rounded-xl border border-[#EFECE4] bg-[#FAF8F3] px-3.5 py-2 dark:border-[#2A394A] dark:bg-[#0E141B]"
                  >
                    <span className="truncate text-base font-medium text-[#1B1E24] dark:text-[#F1EDE4]">
                      {cat.name}
                    </span>
                    <button
                      type="button"
                      onClick={() => setCategoryPendingDelete(cat)}
                      aria-label={`حذف دسته ${cat.name}`}
                      className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-[#B3261E] hover:bg-[#B3261E]/10 dark:text-[#F28B82] dark:hover:bg-[#F28B82]/15"
                    >
                      <Trash2 className="h-4 w-4" aria-hidden="true" />
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="mt-5 flex justify-end">
              <button
                type="button"
                onClick={() => setShowCategoryModal(false)}
                className="min-h-[44px] rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-5 text-sm font-semibold text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4]"
              >
                بستن
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete confirmation dialog */}
      {songPendingDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-dialog-title"
        >
          <div className="w-full max-w-md rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-6 shadow-xl dark:border-[#2A394A] dark:bg-[#161F2A]">
            <h2 id="delete-dialog-title" className="text-xl font-bold text-[#1B1E24] dark:text-[#F1EDE4]">
              حذف شعر
            </h2>
            <p className="mt-3 text-base leading-relaxed text-[#525866] dark:text-[#9FB0C2]">
              آیا مطمئن هستید که می‌خواهید «{songPendingDelete.title}» را حذف کنید؟
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setSongPendingDelete(null)}
                className="min-h-[48px] rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-5 py-2.5 text-base font-semibold text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646]"
              >
                انصراف
              </button>
              <button
                type="button"
                onClick={() => {
                  onDeleteSong(songPendingDelete);
                  setSongPendingDelete(null);
                }}
                className="min-h-[48px] rounded-xl bg-[#B3261E] px-5 py-2.5 text-base font-bold text-white hover:bg-[#8E1C16] dark:bg-[#F28B82] dark:text-[#3E0A06]"
              >
                بله، حذف شود
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Category delete confirmation dialog */}
      {categoryPendingDelete && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-black/55 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-category-dialog-title"
        >
          <div className="w-full max-w-md rounded-2xl border border-[#DDD6C8] bg-[#FFFFFF] p-6 shadow-xl dark:border-[#2A394A] dark:bg-[#161F2A]">
            <h2
              id="delete-category-dialog-title"
              className="text-xl font-bold text-[#1B1E24] dark:text-[#F1EDE4]"
            >
              حذف دسته‌بندی
            </h2>
            <p className="mt-3 text-base leading-relaxed text-[#525866] dark:text-[#9FB0C2]">
              آیا مطمئن هستید که می‌خواهید دسته‌بندی «{categoryPendingDelete.name}» را حذف کنید؟ شعرهای داخل این دسته حذف نمی‌شوند.
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setCategoryPendingDelete(null)}
                className="min-h-[48px] rounded-xl border border-[#DDD6C8] bg-[#F8F6F1] px-5 py-2.5 text-base font-semibold text-[#1B1E24] hover:bg-[#EFECE4] dark:border-[#2A394A] dark:bg-[#1F2B38] dark:text-[#F1EDE4] dark:hover:bg-[#273646]"
              >
                انصراف
              </button>
              <button
                type="button"
                onClick={() => {
                  onDeleteCategory(categoryPendingDelete.id);
                  setCategoryPendingDelete(null);
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
