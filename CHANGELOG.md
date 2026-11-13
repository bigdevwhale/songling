# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [Unreleased]

## [1.5.0] - 2026-11-13

### Added
- A new pet, the Raven (#15): a keeper of songs by musicians who have passed away. Its egg opens after three hours of their music, and their songs are its favourite food. Whether an artist is gone comes from MusicBrainz (people only; a band that split up doesn't count); the artists already in your library are looked up again once.

### Fixed
- A song by several artists («A, B», «A feat. B», «A & B») counts for each of them, not only the first-billed one: in the top, artist goals and new artists (#20).
- The den's line no longer gets stuck on the previous pet after switching, and the «… to the next stage» line with its frozen time is gone – the bar under the pet shows it (#18).

<!-- tg
Новое: питомец Ворон – хранитель песен ушедших музыкантов. Яйцо открывается после трёх часов их музыки.
Исправлено: песня нескольких артистов теперь засчитывается каждому, а не только первому. Подпись «до следующей стадии» больше не застревает от прошлого питомца.
-->

## [1.4.1] - 2026-11-12

### Changed
- The pets on the first screens sway slowly and softly instead of dancing, each one in its own time.

<!-- tg
Питомцы на первых экранах теперь покачиваются медленно и плавно.
-->

## [1.4.0] - 2026-11-12

### Added
- Awards: 51 of them, most with several steps (155 in all) – hours, days and streaks, new artists and whole albums, countries and decades, night and morning listening, songs on repeat, songs by two artists or more (#11), Christmas and New Year songs and music at New Year's midnight (#9), concerts and vinyl, pets and the den. Every step pays 5 plectrums. They live on a tab of their own in the Saga, which counts the new ones.
- Postcards to share: the den, the pet, your music over a period, top artists, albums and songs, the song on repeat, genres, when you listen, the year as a square a day, countries and decades, awards and all your pets. Each one as a post (4:5) or a story (9:16), sent anywhere from the share menu or kept in the gallery. In the Saga, at the top.
- A tap on a bar of any chart in the Saga says what it stands for: the day or month and how much music it had (#14).

### Changed
- The pets, their eggs and the den are drawn twice as fine. The eggs are drawn anew, each keeping its colours and pattern.
- The bottom menu and the den's buttons have plain one-colour icons in the theme's colours.

### Fixed
- The app could stop hearing music although its access was still on: Android dropped the link to the players (after an update, or when it closed the app) and didn't bring it back. The app now asks for it back by itself, and if the players still can't be heard, the den says so and a tap reconnects (#7).

<!-- tg
Награды: 51 штука со ступенями, за каждую ступень – медиаторы. Есть за новогодние песни и за песни нескольких артистов.
Открытки для соцсетей: логово, питомец, итоги, топы, жанры, год и награды – в «Саге».
Нажми на столбик графика – покажет, сколько музыки было в тот день.
Приложение снова само подключается к плеерам, если система его отключила. Питомцы, яйца и логово нарисованы детальнее.
-->

## [1.3.0] - 2026-11-11

### Added
- A listen can be taken out of the history: hold it down in the history and remove it – it leaves the statistics, the pet's time and the goals too (#12).
- The player widget shows how far into the song it is and how long the song is; the time runs on its own while it plays (#6).

### Changed
- The den is drawn anew: all eight wallpapers and every thing in it, in the same pixel-art look as the pets. The clock, the shelf's spines, the turntable's label, the speakers, the lights and the lava lamp still move and change as before; the lava lamp and the tapes stand on the shelf, the posters hang higher.
- The first onboarding screen shows three grown pets again, over the records and the tape, and picking an egg shows what it grows into.

### Fixed
- A whole album could count as a third of its time: when a song change reached the app late or not at all, the next songs were folded into one listen, which was then cut to the first song's length. The app now asks the players every 15 seconds what they play, and catches a missed song change, pause or new player there (#13).
- A song played again on repeat without a pause counts as a new listen again.

<!-- tg
Время прослушивания больше не теряется: целый альбом засчитывается целиком, а не на треть.
Логово перерисовано – новые обои и все предметы.
Прослушивание можно убрать из истории: удерживай его в истории.
В виджете плеера – сколько прошло и сколько длится песня.
-->

## [1.2.0] - 2026-11-11

### Changed
- All 18 pets are drawn anew, at every stage: a fuller pixel-art look, with what each one loves worn on it – the lyre tail, the saxophone tail, the record shell, the boombox.

### Fixed
- A listen could grow far past its song – one song showing as 35 minutes – when the player's «stopped» reached the app late or not at all. The clock now stops when the player stopped, the app asks the player itself every 15 seconds, and one listen is never longer than its song; listens already stretched are trimmed to their song (#10).
- One long listen no longer counts as the whole album: that takes at least two of its songs (#10).
- An album of two or three songs can be heard whole (all of its songs), and the songs of an album may come in any order with others in between, as long as no more than 40 minutes pass between two of them (#8).
- A genre set by hand on an album or an artist now shows in the statistics; the catalogue's guess for the song, and its «other» most of all, no longer outranks it. A genre set by hand on a song outranks the album's (#5).

<!-- tg
Питомцы перерисованы – все 18, на каждой стадии.
Исправлено: одна песня больше не превращается в 35 минут прослушивания и не засчитывает альбом целиком.
Альбом из двух-трёх песен теперь засчитывается, а песни альбома можно слушать в любом порядке, вперемешку с другими.
Жанр, выставленный вручную у альбома или артиста, теперь виден в статистике вместо «другое».
-->

## [1.1.0] - 2026-11-10

### Added
- A player widget: what plays in any player, drawn as a record with the cover for a label or a tape with the cover for a sticker (as chosen in Settings), with previous, play/pause and next (#2).
- Choose which apps the pet hears: every player but the ticked ones, or only the ticked ones. The list shows the phone's players by name. A player left out no longer makes the pet dance either (#4).
- Onboarding asks which apps to hear, and its first screen shows records and a tape instead of pets.

### Fixed
- With three-button navigation the dock covered the den's last lines, and there was nothing to scroll (#1).
- The cover of the song playing now shows at once, from the player itself, instead of only after the catalogue found it.

<!-- tg
Новое: виджет плеера – пластинка или кассета с обложкой и кнопками.
Можно выбрать, какие приложения слушает питомец: все, кроме отмеченных, или только отмеченные.
Обложка того, что играет, теперь видна сразу, а внизу логова больше ничего не прячется под панелью.
-->

## [1.0.0] - 2026-11-10

The first version.

- A pet that hears what any player plays (through notification access) and grows from every minute of it: 18 species, four stages, six grown forms, natures and a rare colouring.
- The den: a pixel room with sixteen things and eight wallpapers to buy with plectrums, tried on first, two poster frames for an album and a band, and a window with the real weather and time of day in your town.
- The shelf: albums, singles, songs and artists with covers, genres, years and stars; a «to hear» list; search, sorting and filters; your own genres.
- Logging by hand: a whole album, a song from an album or on its own, from a record, the radio or a concert.
- Saga: stats with filters, top lists, hours and weekdays, countries and decades, a calendar of the year.
- Goals on new albums, artists, genres and countries, an artist, an album or a song, and three weekly tasks.
- Records or tapes, light or dark, pixel backgrounds; history from Spotify, Last.fm and ListenBrainz; backups.

<!-- tg
Songling – питомец, который слушает с вами музыку из любого плеера и растёт от каждой минуты.
18 питомцев, логово с вещами и постерами, полка с альбомами, статистика, цели и задания недели.
-->
