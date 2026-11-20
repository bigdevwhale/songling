# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.17.0] - 2026-11-20

### Added
- **Ten new pets, 35 in all.** Each is found its own way:
  - the critic fox: give stars to 15 albums, songs or artists;
  - the penguin: hear 5 things from the «to hear» list;
  - the moon moth: play music on the evenings of 3 full moons;
  - the phoenix: come back to 5 artists after half a year without them;
  - the unicorn: raise 5 pets to grown;
  - the squirrel: 30 songs under two minutes, to the end;
  - the bee: 14 days of music in a row;
  - the sloth: one album on 7 different days;
  - the chameleon: albums from 6 different decades;
  - the DJ octopus: 3 sittings with eight artists or more.

  Each has its favourite music too: what you gave four stars, the moonlit evenings, songs heard for the first time, albums you keep coming back to. The moth, the phoenix and the unicorn are rare ones for Lingvale.

### Fixed
- **Genres in the pet's words are declined in Russian** (#43, #50, #51): «После рока Лирик всё ещё качает хвостом» instead of «После рок у Лирик…». The pet's name always stands as it is, and a genre of your own comes as «после стиля «…»».
- **An album opened from an artist's page goes back to the artist** when closed (#42), not to the list of all artists, so several albums can be put right one after another.

<!-- tg
10 новых питомцев: лиса-критик, пингвин, лунный мотылёк, феникс, единорог, белка, пчела, ленивец, хамелеон и осьминог-диджей. Каждый находится по-своему – от звёзд альбомам до музыки в полнолуние.
Жанры во фразах питомца теперь склоняются, а альбом, открытый со страницы артиста, при закрытии возвращает к артисту.
-->

## [1.16.0] - 2026-11-19

### Added
- **Forty new things for the den:** most places in the room now have two or three to choose from – a striped, record or fluffy rug, a cactus, sunflowers or a lemon tree, a synthesizer or a boombox, a violin, a banjo or a ukulele, neon hearts, moons and bolts, paper lanterns, a toy piano, a harp and more. A place shows one thing at a time, so every pet's room can be furnished its own way.
- In the shop, tap a place to see what can stand there: try a thing in the room, buy it, or put back one you already have. What you put away stays yours.

### Fixed
- **Eggs that ask for hours of music fill with every song:** the raven's egg and the other «N hours of …» eggs counted only whole hours, so they stood at «0 / 3» until a full hour of such music was heard. Now they count minutes and show «1 h 20 min / 3 h».

<!-- tg
В логове 40 новых вещей: у большинства мест теперь есть из чего выбрать – ковры, растения, синтезатор, скрипка, неон, фонарики и другое. У каждого питомца комната своя.
Яйца, которым нужны часы музыки (например, ворон), теперь растут с каждой песней, а не только после полного часа.
-->

## [1.15.0] - 2026-11-18

### Added
- **Lingvale can move Songling to a new phone:** the village saves the backups of all the -ling apps into one file and loads them back on another phone. It is the same backup the settings make, and Songling loads it only when you confirm it in Lingvale.

### Fixed
- **The backup keeps your own backdrop:** a picture you chose for the studio's background was left out of the backup, so it was gone after a restore. Now it travels in the file. Older backups still load.

<!-- tg
Lingvale теперь умеет переносить Songling на новый телефон: одна копия на все приложения семейства.
В копию попадает и ваша собственная картинка для фона – раньше она терялась.
-->

## [1.14.0] - 2026-11-17

### Added
- An artist's page tells whether they are alive: «Alive · born 1985», «Died · 1926–1991», and for bands «Together · since 1988» or «Broke up · 1981–1990». It comes from MusicBrainz, so after the update every artist is looked up once more in the background.

<!-- tg
На странице артиста теперь видно, жив ли он: «Жив · род. 1985» или «Умер · 1926–1991».
Для групп – «Вместе · с 1988» или «Распались · 1981–1990».
После обновления артисты один раз перепроверятся в фоне, поэтому отметки появятся не сразу.
-->

## [1.13.1] - 2026-11-16

### Fixed
- Statistics no longer offers last year as a period when there was no music in it, so a new listener doesn't get an empty «2025».
- Less than a minute of listening shows as «1 min», not «0 min»; a few minutes in hours show in minutes instead of «0.0 h».

<!-- tg
Исправлено: в статистике больше нет пустого прошлого года, если в нём ничего не слушали.
Меньше минуты музыки теперь показывается как «1 мин», а не «0 мин».
-->

## [1.13.0] - 2026-11-16

### Added
- **Lingvale, the village of the pets from all our apps:** Songling now has a small read-only bridge, so in Lingvale you can build the studio and its residents move in with their days. The bridge answers only Lingvale signed with our key; it changes nothing and nothing leaves the phone.

<!-- tg
Питомцы Songling теперь могут жить в Lingvale – общей деревне наших приложений.
В Lingvale для них можно построить студию, и жильцы переедут туда сами вместе со своими днями.
Данные остаются в Songling: деревня их только читает.
-->

## [1.12.1] - 2026-11-16

### Added
- An artist's page shows each album's genre beside its year, so a wrong guess shows without opening every album (#31).

### Fixed
- The shelf keeps the sort you picked: leaving it for another tab or closing the app no longer resets it to «most» (#30).

<!-- tg
Новое: на странице артиста у каждого альбома виден жанр – не нужно открывать каждый, чтобы проверить.
Исправлено: полка запоминает выбранную сортировку и больше не сбрасывает её на «больше всего» при переходе на другую вкладку.
-->

## [1.12.0] - 2026-11-15

### Added
- A record collection, where plectrums go once the den is furnished: the shop's new «Records» tab shows the covers you own and the albums you've heard that could join them, the most listened first. The first record costs 20, each next one 5 more, up to 150; an album heard whole costs half. An album's page has «Buy the record» too. When no music plays, the den's turntable has your newest record on it. The collection is every pet's and goes into backups.

### Changed
- The first egg of each kind stays free; another egg of a kind you already have costs 200 plectrums.

### Fixed
- Switching tabs and the «Furnish the den» page no longer stutter: the plectrum count was worked out again for every tile on the screen, and artist names were read anew on every listen. Goals opened in up to three quarters of a second; now at once.

<!-- tg
Новое: коллекция пластинок – в магазине вкладка «Пластинки», можно купить пластинку любого альбома, который ты слушал. Каждая следующая чуть дороже, прослушанный целиком альбом – вдвое дешевле.
Первое яйцо каждого вида по-прежнему даром, второе такое же – за 200 медиаторов.
Исправлено: переходы между вкладками и магазин логова больше не тормозят.
-->

## [1.11.0] - 2026-11-15

### Added
- Styles: an artist's styles as MusicBrainz tags name them (metalcore, electronicore, trip hop…), under their name on the artist's page. In the statistics, each genre shows the styles it was made of.
- An artist's genre from those tags, for songs and albums the catalogue doesn't know.

### Changed
- «-core» styles (metalcore, deathcore, electronicore) count as metal; hardcore stays punk.

### Fixed
- A band with «и» or «&» in its name (Король и Шут, Егор и Опизденевшие, Simon & Garfunkel) is one artist, not two. Listens already split come back together, with their songs, albums, goals and stars.
- Russian music gets its genre, year and cover on a phone set to another language: the store is picked by the name's letters, and the other one is asked too.
- What couldn't be looked up is asked again: in an hour when the service didn't answer, in two weeks when nothing was found. After the update, everything left without a genre or country is looked up once more.
- Artists are found by their other names too (MusicBrainz aliases), so more of them get a country.

<!-- tg
Новое: у артистов появились стили – metalcore, trip hop, post-punk и другие. В статистике под каждым жанром видно, из каких стилей он сложился.
Исправлено: «Король и Шут» и другие группы с «и» или «&» в названии больше не делятся на двух артистов.
Жанры и страны находятся у гораздо большего числа песен и артистов, русская музыка – даже на телефоне на английском.
-->

## [1.10.0] - 2026-11-14

### Added
- Four music things for the den (#26): a metronome on the sill (35), bongos on the floor (55), an electric guitar on the wall by the window (75) and a framed gold record above the turntable (100).

### Changed
- Each pet has a room of its own (#26): things and wallpaper are bought for the pet out now, and bringing out another pet brings out its room. A new pet starts with an empty one. What was bought before stays with every pet you have.

### Fixed
- Without the shelf, the lava lamp and the tapes stand on the window sill instead of hanging in the air.
- What plays on Last.fm shows in the den even while the phone's players aren't heard; the note about that comes under it instead of in its place.
- An album the catalogue finds no cover for takes Last.fm's picture, in the den and on the shelf.

<!-- tg
Новое: у каждого питомца теперь своя комната – вещи и обои покупаются для того, кто сейчас в логове. Всё купленное раньше осталось у каждого.
В магазине четыре музыкальные вещи: метроном, бонго, электрогитара и золотой диск.
Исправлено: без шкафа лава-лампа и кассеты стоят на подоконнике, а не висят в воздухе. Песня с Last.fm в логове видна всегда и с обложкой.
-->

## [1.9.0] - 2026-11-14

### Added
- Last.fm as it happens: connect a username (Settings → History and backups → Bring history over) and what you scrobble on a computer, a speaker or another phone comes in – every 15 minutes, and half a minute after a song ends while the app is open. It counts as heard: the pet eats it (headphones aside, which Last.fm can't tell). Only what comes after connecting is taken; a song this phone already heard isn't counted twice, and a song put in another album by hand goes there. No password; nothing is written to Last.fm.
- What plays on Last.fm shows in the den while nothing plays on the phone, and the pet dances to it.

### Changed
- An imported history (Last.fm, Spotify, ListenBrainz) feeds no pet: feeding and favourites count only what was heard here. It stays in the statistics, the saga and the awards. An imported past no longer makes an artist the lyrebird or a curious pet already ate as new «not new» after the fact.
- A pet's colouring – the rare one and a grown form's – leaves its eyes as they are.

### Fixed
- Importing no longer counts a listen twice: one of the same song that started about the same time as a listen already there is left out, and the import says how many were.

<!-- tg
Новое: Last.fm в реальном времени – введи имя в «Перенести историю», и то, что ты скробблишь на компьютере или колонке, будет приходить в Songling и кормить питомца. Что играет на Last.fm, видно в логове.
Импорт истории больше не кормит питомцев и не задваивает прослушивания, которые уже есть.
У питомцев с редкой окраской и цветом формы глаза больше не перекрашиваются.
-->

## [1.8.0] - 2026-11-14

### Added
- Several genres at once for a song or an album (#23): pick as many as fit; the first is the main one and gives the colour. A listen counts in full for each of them – in the genre statistics, filters, goals and new genres. For a pet that likes a genre, it is a whole favourite when that genre is the main one and half a favourite when it is one of the others. A song with its genre set by hand keeps its own list; otherwise it takes its album's.
- Tap the song playing in the den to open it: how often you heard it, its album, stars, genre (#25).
- The den widget says what it shows in the widget list (#17).

### Changed
- The history rounds a listen to the nearest half minute (#24): 1:15 is 1 min, 1:16–1:44 is 1.5 min, 1:50 is 2 min.

<!-- tg
Новое: у песни и альбома теперь может быть несколько жанров – выбери сколько нужно, первый станет главным. Прослушивание засчитывается в каждый.
Песню, которая играет в логове, можно нажать – откроется карточка: сколько раз слушал, альбом, звёзды, жанр.
В истории время округляется до ближайшей половины минуты: 1:50 – это 2 минуты, а не одна.
-->

## [1.7.1] - 2026-11-14

### Added
- Put a song in another album by hand (#22): «Album» on the song's sheet. Its listens move along, and its next listens land in that album whatever album the player names.

<!-- tg
Новое: у песни теперь можно поменять альбом – в карточке песни нажми на строку с альбомом.
Прослушивания переедут вместе с ней, и новые прослушивания этой песни тоже будут попадать в выбранный альбом.
-->

## [1.7.0] - 2026-11-14

### Added
- Delete an artist with their listens, songs and albums (after asking) – at the foot of the artist's sheet. Songs where they're only featured stay with the lead artist.
- 19 more awards, 70 in all (213 steps): weekends with music, a month of music, a collection of albums, a day of finds, music by those who are gone, short songs, one artist's day, a day of albums, a song that lasts through the months, stars, hearts and notes, World Music Day, Friday the 13th, pets in every form, the pets' favourite music, strokes, wallpapers and plectrums spent. «Menagerie» has a fifth step, 24 pets. The awards tab has a third view, «Nearly there»: the ten nearest to a new step. Steps already reached pay their plectrums at once.
- Six new pets, 25 in all: the Hummingbird (pop), the Magpie (songs by two artists or more), the Bullfinch (winter and Christmas songs), the Bat (records and the radio), the Little dragon (songs over eight minutes) and the Hound (your most played artist). Each egg is found by listening its way.

### Changed
- New colours: a near-white page with white cards by day, graphite by night, instead of the brown oatmeal. The colour of the music stays clear: oranges and yellows are amber, not tan.
- Less on every screen. The shelf's lines are two lines (who and when; time and stars on the right) and the «add» row is gone – the record in the dock adds. The pet's card lost its «to the next stage» line. The saga keeps three tiles; the rest moved to «In numbers», and postcards to the foot. Sheets lost the decorative arc.
- An album's sheet has two buttons – «Heard it» and «Want to hear» – and the rest under «More». One editor instead of two: title, artist, cover, year, kind, genre and songs together, with «Save» always in reach; a typed song can be renamed by a tap, and the heard songs show five at a time.
- Settings are a short menu – counting, apps, look and language, the pet and the window, your genres, history and backups, about – and each opens a page of its own instead of one long scroll.
- The den: the pet's card says who it is, today's music and what it says; three buttons instead of five (a concert is logged from «Log a listen», the shelf is filled from the dock's record).
- The shelf is a flat list instead of a card per line, with four tabs that fit across: albums, singles, songs, artists. Under the search: the order on the left, a «To hear» switch and the filters on the right. The history of listens moved to the saga, beside the calendar.
- The saga: the span and the filters share a line; the top lists show five; hours and weekdays share one card. Goals: «+ New» sits by the title.
- The whole interface, tidied. Sheets slide up and follow the finger down; pages come in from the side; tabs fade into each other; buttons, chips and cards sink a little under the finger. On a sheet and on a card, tiles and chips are flat wells instead of paper on paper, where only their shadow showed.
- Second-rank text is darker and reads better on the page, by day and by night. Empty bars show on any background.
- Tabs sit on one thin rule with a short bar under the open one. Arrows, crosses, the search glass, sort and filters are drawn in one style instead of font symbols.
- The shelf's search has a glass and a cross to clear it; fields light up while written in. Empty lists say what to do next. Stat tiles of a row are one height and long numbers shrink to fit. Records are label and value in two columns. Goals show their per cent.
- The first screens show the step and take «back» a step back. Song rows and small buttons are bigger under the thumb.
- A note at the top slides in and reads in both themes.

### Fixed
- Sheets no longer stutter while opening: the dim layer behind them is drawn without recomposing the sheet, and an album's songs are worked out once.
- The record no longer sinks into a dark sheet or the dark dock.
- «The shelf opens on» chips no longer stick to the edge of the settings card.

<!-- tg
Новое: шесть питомцев – колибри, сорока, снегирь, летучая мышь, дракончик и пёс, теперь их 25. И ещё 19 наград, всего 70.
Новый вид: светлые цвета вместо коричневых, меньше всего на каждом экране, короткие настройки, один удобный редактор альбома.
Артиста теперь можно удалить вместе с прослушиваниями. История прослушиваний переехала в сагу.
Исправлено: шторки больше не подтормаживают при открытии.
-->

## [1.6.0] - 2026-11-13

### Added
- Your own albums: «songs and cover» on any album, or «Make your own album» when the search finds nothing. Title, artist, year, a cover from the gallery, and the songs in order – typed in or picked from what you heard by that artist. Renaming folds the old one in: a player's «Unnamed» becomes the real album, its listens move along, and new listens under the old name land there too.
- Delete a song or an album together with its listens (after asking) – for a compilation in one file, a stray recording, a wrong album.
- «Only songs heard to the end»: a song counts in its last ten seconds.
- The «long track» limit (podcasts, compilations in one file) is yours to set, 10 to 90 minutes.
- Which tab the shelf opens on: albums, singles, songs, artists, to hear or history (Settings → Counting).

### Fixed
- Marking an album «heard whole» right after it played in a player no longer adds its time again: it only marks it whole. An album without a list of songs is filled in first, instead of making up one long «song» named after the album.
- The history says how to take a listen out: hold the line.

<!-- tg
Новое: свои альбомы – название, обложка из галереи и песни в нужном порядке; «Unnamed» можно превратить в настоящий альбом, и прослушивания переедут туда.
Песню или альбом теперь можно удалить вместе с прослушиваниями. В настройках – «только дослушанные», порог длинных треков и вкладка, с которой открывается полка.
Исправлено: отметка «прослушал целиком» больше не прибавляет лишнее время и не создаёт песню на 40 минут.
-->

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
