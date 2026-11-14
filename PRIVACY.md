# Songling — Privacy Policy

*Last updated: 13 November 2026*

Songling is an Android app: a pixel pet that hears the music you play, with a shelf of albums, stats and
goals. It is published as open source at <https://github.com/bigdevwhale/songling>.

## What we collect

**Nothing.** Songling has no accounts, no analytics, no advertising and no crash reporting.
We run no server, so nothing you listen to ever reaches us.

## What the app reads on your phone

- **What players are playing.** Android shows what a music player is playing (its media session) only to
  apps with notification access, so Songling asks for it. It takes the song title, the artist and the album
  from the players' media sessions, and only from the apps you allow in Settings → Counting (the app
  checks which app a session belongs to for that). It does not read the text of your notifications, your messages or anything else.
- **Which players are installed.** To let you choose which apps it listens to, the app lists the music
  players on the phone. The list stays on the phone.

## What stays on your device

The app keeps the following only in its private storage on your phone:

- your listening history (songs, artists, albums, when and how long you listened);
- your shelf: albums, songs and artists with their covers, genres, years, countries, stars, notes and
  «to hear» list, your own albums and genres;
- your pets, the den and what you bought for it, goals, tasks, awards and settings, including the town
  for the weather in the window.

## When the app goes online

Only for the things below. Each request goes straight from your phone to the service named; like any
request on the internet, it shows that service your IP address, and that service's own privacy policy
applies. No identifier of you or your device is sent.

- **Covers, genres, years and songs of an album.** The artist and the album or song title are sent to
  the iTunes Search API (`itunes.apple.com`), and the cover is downloaded from Apple's servers. This
  happens for what you heard and for what you search for when adding an album or a song.
- **Artists' countries.** The artist's name is sent to MusicBrainz (`musicbrainz.org`) to learn the
  country and whether the artist has passed away.
- **Band posters for the den.** The artist's name is sent to Deezer (`api.deezer.com`) to find the
  artist's picture.
- **The weather in the window** (can be turned off in Settings). The town is guessed from your phone's
  time zone or picked by you: its name is sent to Open-Meteo's geocoding (`geocoding-api.open-meteo.com`),
  and the town's coordinates to Open-Meteo (`api.open-meteo.com`) or, when that can't be reached,
  MET Norway (`api.met.no`). The app does not use your phone's location.

## Sharing

- **Files you pick** (a listening history from Spotify, Last.fm or ListenBrainz, a backup, a cover for
  your own album) are read only when you pick them through the system pickers: the app sees only the
  file you choose.
- **Backups and postcards** are saved or shared only where you send them, through the system dialogs.
- **Community links** in settings open Telegram or your browser, only when you tap them; what you do
  there is governed by those services' own policies.
- **Device backup.** Android may include the app's data in your own device backup to your Google account,
  if you have that turned on in system settings.

## Permissions

- **Internet** — only for the requests described above.
- **Notification access** (granted by you in system settings) — only to see what music players are
  playing, as described above. You can take it back at any time in system settings.

## Children

Songling is not directed at children under 13.

## Changes

Any change to this policy will be published on this page with a new date.

## Contact

Questions: open an issue at <https://github.com/bigdevwhale/songling/issues>.

---

# Songling — Политика конфиденциальности

*Обновлено: 13 ноября 2026*

Songling — приложение под Android: пиксельный питомец, который слышит вашу музыку, с полкой альбомов,
статистикой и целями. Код открыт: <https://github.com/bigdevwhale/songling>.

## Что мы собираем

**Ничего.** В приложении нет аккаунтов, аналитики, рекламы и отчётов о сбоях.
У нас нет сервера, поэтому ничего из того, что вы слушаете, к нам не попадает.

## Что приложение читает на телефоне

- **Что играет в плеерах.** Android показывает, что играет в музыкальном плеере (его медиасессию), только
  приложениям с доступом к уведомлениям, поэтому Songling его просит. Из медиасессий плееров берутся
  название песни, артист и альбом — и только из приложений, разрешённых в Настройки → Что считать
  (для этого приложение смотрит, какому приложению принадлежит сессия).
  Текст уведомлений, сообщения и всё прочее не читаются.
- **Какие плееры установлены.** Чтобы вы могли выбрать, какие приложения слушать, приложение показывает
  список музыкальных плееров на телефоне. Список остаётся на телефоне.

## Что хранится на устройстве

Только в собственном хранилище приложения на телефоне:

- история прослушиваний (песни, артисты, альбомы, когда и сколько слушали);
- полка: альбомы, песни и артисты с обложками, жанрами, годами, странами, звёздами, заметками и списком
  «Хочу послушать», ваши альбомы и жанры;
- питомцы, логово и купленное для него, цели, задания, награды и настройки, в том числе город для погоды
  за окном.

## Когда приложение выходит в интернет

Только в этих случаях. Каждый запрос идёт с телефона прямо в названный сервис; как любой запрос
в интернете, он сообщает этому сервису ваш IP-адрес, и там действует его политика конфиденциальности.
Никаких идентификаторов вас или устройства не передаётся.

- **Обложки, жанры, годы и песни альбома.** Артист и название альбома или песни отправляются в iTunes
  Search API (`itunes.apple.com`), обложка скачивается с серверов Apple. Это происходит для услышанного
  и для того, что вы ищете, добавляя альбом или песню.
- **Страны артистов.** Имя артиста отправляется в MusicBrainz (`musicbrainz.org`), чтобы узнать страну
  и жив ли музыкант.
- **Постеры групп в логове.** Имя артиста отправляется в Deezer (`api.deezer.com`) за его фотографией.
- **Погода за окном** (отключается в настройках). Город угадывается по часовому поясу телефона или
  выбирается вами: его название отправляется в геокодер Open-Meteo (`geocoding-api.open-meteo.com`),
  а координаты города — в Open-Meteo (`api.open-meteo.com`) или, если он недоступен, в MET Norway
  (`api.met.no`). Местоположение телефона приложение не использует.

## Передача

- **Файлы, которые вы выбираете** (история из Spotify, Last.fm или ListenBrainz, резервная копия, обложка
  своего альбома), читаются, только когда вы их выбрали через системный диалог: приложение видит только
  выбранный файл.
- **Резервные копии и открытки** сохраняются или отправляются только туда, куда вы их отправили через
  системный диалог.
- **Ссылки на сообщество** в настройках открывают Telegram или браузер, только когда вы на них нажимаете;
  там действуют правила этих сервисов.
- **Резервная копия устройства.** Android может включить данные приложения в резервную копию устройства
  в вашем аккаунте Google, если она включена в настройках.

## Разрешения

- **Интернет** — только для запросов, описанных выше.
- **Доступ к уведомлениям** (вы даёте его в системных настройках) — только чтобы видеть, что играют
  музыкальные плееры, как описано выше. Его можно отозвать в любой момент в системных настройках.

## Дети

Приложение не предназначено для детей младше 13 лет.

## Изменения

Изменения этой политики публикуются на этой странице с новой датой.

## Контакты

Вопросы — через issue: <https://github.com/bigdevwhale/songling/issues>.
