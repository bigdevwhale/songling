# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow кладёт в GitHub Release ещё и `Songling-v<версия>.aab`.
  Первая версия для Play — **v1.7.1** (`Songling-v1.7.1.aab` уже в релизе). Секреты `SIGNING_*`
  в репозитории заданы; без них сборка подписывается debug-ключом, а такой файл Play отклонит.
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках приложения перестаёт работать.
  Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.songling`.
  2. **Select key** → ключ с SHA-256 `1449575df39c031ce18eebc22398363af7fb814a2ecd266c503d3749b48f0706`
     (сертификат CN=Songling, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.songling` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория (на двух языках, с разделом
  про доступ к уведомлениям). После push ссылка:
  `https://github.com/bigdevwhale/songling/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).
- **Автозагрузка из CI** (`.github/workflows/play.yml`) — после каждого успешного Release берёт AAB из
  релиза и отправляет в Play, release notes — из `listing/*/release_notes.txt` на теге. До первой ручной
  загрузки она не нужна, поэтому:
  1. В репозитории Settings → Environments → создать **`google-play`** с Required reviewer (вы) —
     иначе job запустится без подтверждения сразу после релиза.
  2. Play Console → Users and permissions → пригласить `play-publisher@intense-context-514611-h3.iam.gserviceaccount.com`
     с правом выпускать релизы для Songling.
  3. В Google Cloud у пула `github` (Workload Identity Federation) разрешить репозиторию
     `bigdevwhale/songling` действовать от имени этого сервисного аккаунта — так же, как для остальных
     приложений.
  Пока это не сделано, отклоняйте запуск «Google Play» после релиза (или он упадёт на авторизации — без вреда).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Songling: Music Pet & Stats` |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты 1080×1920 собраны `tools/play.py` из экранов демо (`docs/raw/<язык>/*.png`, 1080×2400):
Play не берёт картинки длиннее двух ширин, поэтому экран уходит за нижний край под подписью;
строка состояния телефона срезана, у листа альбома оставлена только карточка. На скриншотах:
логово, бестиарий, полка, альбом, сага, награды, обстановка логова, открытки.
Нужен `pip install pillow`. Как пересобрать:

```bash
./gradlew :app:testDebugUnitTest -Prender -Pdemo --tests app.songling.RenderStore   # экраны демо без телефона, en и ru
python tools/store_shots.py   # docs/raw, docs/screenshots и скриншоты сайта (../stillcraft/assets)
python tools/play.py          # графика для Play
```

На скриншотах — настоящие альбомы и артисты из демо (Radiohead, Miles Davis, Daft Punk…) с их обложками
из iTunes и постером группы на стене. Обычно Play такое пропускает: это история прослушиваний
пользователя, а не реклама чужой музыки. Если придерутся к обложкам («Impersonation / IP»), переснимите
демо с вымышленными альбомами или замажьте обложки на скриншотах.

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **Music & Audio** |
| Tags (до 5) | из списка Play: Music & audio, Music player, Entertainment и близкие к «статистика прослушиваний» |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/songling/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Приложения со статистикой прослушиваний (Last.fm, stats.fm, Pano Scrobbler) живут
в **Music & Audio** — там Songling и будут искать по «music stats», «scrobbler», «статистика музыки».
Питомец — оформление дневника, поэтому не Games. Категорию можно сменить позже.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*
(Доступ к уведомлениям даёт сам пользователь в системных настройках; без него можно отмечать музыку
вручную, демо-данные для проверки не нужны.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types**.
- Насилия, страха, сексуального контента, ненормативной лексики, наркотиков, азартных игр — **No**.
  Медиаторы — внутренняя валюта, которую только зарабатывают музыкой; за деньги её не купить, покупок нет.
- **Users interact / share content** — **No**: общения между пользователями нет. Открытки уходят
  через системное меню «Поделиться» туда, куда пользователь сам отправит.
- **Web browser / unrestricted internet** — **No**.
- **Location** — **No**: местоположение телефона не используется (город для погоды — по часовому поясу
  или выбранный вручную, другим пользователям не передаётся).
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over** (политика: приложение не для детей младше 13).
  Младше 13 не отмечать: иначе действует политика Families с отдельными требованиями.
- *Could your store listing unintentionally appeal to children?* → **No**: это дневник прослушиваний
  со статистикой и переносом истории; питомец — оформление. Если Google не согласится из-за пиксельного
  питомца, ответ меняется на Yes без последствий для 13+.

**News apps** — No.

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**

Нам ничего не приходит, но Play считает «сбором» любую передачу с телефона, в том числе в чужие
сервисы. Честный минимум:

- *Does your app collect or share any of the required user data types?* → **Yes**.
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (всё по HTTPS).
- *Do you provide a way for users to request that their data is deleted?* → **No** (аккаунтов нет,
  на серверах ничего не хранится; данные удаляются вместе с приложением).
- Типы данных:
  - **App activity → Other actions** — названия услышанных песен, альбомов и имена артистов уходят
    в iTunes, MusicBrainz и Deezer за обложками, жанрами, странами и фото. Collected: **Yes**.
    Shared: **No** (передача сервису, который выполняет функцию приложения, — не «sharing»).
    **Processed ephemerally: Yes**. Required: **Yes** (без этого нет обложек и жанров; отключить нельзя).
    Purpose: **App functionality**.
  - **App activity → In-app search history** — то, что ищут при добавлении альбома или песни (уходит
    в iTunes). Collected: **Yes**. Shared: **No**. Processed ephemerally: **Yes**. Required: **No**.
    Purpose: **App functionality**.
  - **Location → Approximate location** — координаты города для погоды за окном (Open-Meteo, MET Norway).
    Collected: **Yes**. Shared: **No**. Processed ephemerally: **Yes**. Required: **No** (погоду можно
    выключить в настройках). Purpose: **App functionality**.
- Ничего больше: аналитики, рекламы, идентификаторов устройства нет. Сама история прослушиваний,
  список установленных плееров и всё прочее с телефона не уходят. Системное резервное копирование
  Android делает система, а не приложение, его не декларируем.
- Итог в карточке: «Collects: App activity, Location», «No data shared with third parties»,
  «Data is encrypted in transit».

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — No.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Permissions**
- **Доступ к уведомлениям** (`BIND_NOTIFICATION_LISTENER_SERVICE`) — отдельной формы декларации в Play
  для него нет, но политика «Permissions and APIs that Access Sensitive Information» требует, чтобы доступ
  был нужен для основной функции и о нём честно предупреждали. Это выполнено: без него питомец не слышит
  музыку; перед переходом в системные настройки онбординг и логово объясняют, что берутся только песня,
  артист и альбом (`Onboarding.kt`, `DenScreen.kt`); то же написано в описании и в `PRIVACY.md`.
  Если ревьюер спросит, ответ:
  > Songling is a music diary with a pet. Android exposes what a music player is playing (its media session)
  > only to apps that hold notification access, so Songling uses NotificationListenerService solely to call
  > MediaSessionManager.getActiveSessions() and read the song title, artist and album of music players the
  > user allows in Settings. It never reads notification content, and the data stays on the device.
  > The app explains this before sending the user to the system setting.
- **Список приложений** — `QUERY_ALL_PACKAGES` нет, плееры перечислены в `<queries>` манифеста,
  декларация не нужна.
- Остальное — только `INTERNET`. Картинки выбираются через системный выбор файлов, резервные копии —
  через системный диалог. Foreground-сервисов, будильников и местоположения нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Songling-v1.7.1.aab` из GitHub Release.
3. Release name: `1.7.1`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели;
   с доступом к уведомлениям может дольше.
5. После установки из Play проверьте: онбординг → «Дать доступ» открывает системный экран доступа
   к уведомлениям; песня из Spotify или YouTube Music через 30 секунд появляется в истории;
   Настройки → Язык → Русский/English — экран переключается сразу.

## Следующие версии

- `release_notes.txt` переписать под новую версию (≤500 символов — `play.yml` это проверяет;
  для первой там «First release on Google Play»). Дальше релиз уходит в Play сам через `play.yml`
  после подтверждения в окружении `google-play`.
- Скриншоты пересобирать, только если поменялись показанные экраны.

## Риск: iTunes, MusicBrainz, Deezer, Open-Meteo

Все бесплатные и без ключей; запросы идут с собственным User-Agent (`Meta.AGENT`). MusicBrainz просит не
чаще одного запроса в секунду — `Meta` спрашивает по одному. Если приложение разойдётся, iTunes Search
может ограничивать частоту (около 20 запросов в минуту с одного IP) — у одного пользователя это не
упрётся; Open-Meteo бесплатен для некоммерческого использования.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.songling`.
