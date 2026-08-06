# ContentApp — SPEC (ID-нотация)

Единый источник правды для доработки логики по ID. Формат: `S` — экран,
`B/F/T/C` — элемент (Button/Field/Title/Card), `P` — процедура, `O` — оверлей.
Ссылаться в чате можно просто по ID, например: "доработай P4".

Уровень детализации: по умолчанию кратко (что делает). Переменные/функции внутри
процедуры — по запросу конкретного ID.

**v1** — без регистрации и оплаты. Экраны/процедуры/правила, связанные с ними
(бывшие S3/S4/S5, P5–P11, R1–R5, раздел "Бэкенд"), удалены из этого файла вместе
с кодом; полная версия сохранена в `ContentApp_v0.zip` (см. `START-HERE.md`).

---

## Экраны и элементы

### S1 — Главный экран (MainScreen.kt)
| ID | Тип | Что это |
|---|---|---|
| S1.T0 | Плашка | Название приложения ("MyApp", временное) — во всю ширину, от верха экрана до нижней границы S1.T1; общий компонент `AppNameBanner.kt`, используется и на S2 |
| S1.T1 | Заголовок | "Что хочу" над полем ввода |
| S1.F1 | Поле ввода | Короткая фраза-желание, во всю ширину экрана |
| S1.B1 | Кнопка | Скрепка — приложить фото. Под S1.F1, делит ширину поровну с S1.B2, той же высоты, что S1.B3/S1.B4. После выбора фото превращается в превью (тап — заменить) |
| S1.B2 | Кнопка | Enter — отправить. Рядом с S1.B1, той же высоты/размера |
| S1.C1 | Блок | "Я правильно поняла?" (появляется после S1.B2), с увеличенным отступом сверху |
| S1.F2 | Поле ввода | Внутри S1.C1 — редактируемый развёрнутый промт, вдвое выше исходной высоты (140dp) |
| S1.B3 | Кнопка | "Сделать как я хочу" |
| S1.B4 | Кнопка | "Сделать как ты поняла" |
| S1.B8 | Кнопка | Крестик поверх превью фото — убрать фото совсем (виден, если фото выбрано) |

Нижняя часть экрана (под S1.C1) намеренно пустая — открытый вопрос, что там будет
в будущем (см. `NEXT-STEPS.md`).

### S2 — Экран результата (ResultScreen.kt)
| ID | Тип | Что это |
|---|---|---|
| S1.T0 | Плашка | Название приложения — тот же общий компонент, что и на S1 (`AppNameBanner.kt`), самый верхний блок экрана |
| S2.T1 | Заголовок | Полный текст промта |
| S2.C1 | Блок | Карточка результата |
| S2.B1 | Кнопка | "Сохранить" |
| S2.B2 | Кнопка | "Новый запрос" — возвращает на S1, поля S1 (F1/C1) остаются заполненными |

### O1 — Оверлей ожидания (LoadingOverlay.kt)
Показывается поверх текущего экрана, отдельных элементов с ID нет.

---

## Процедуры

| ID | Что запускает | Кратко |
|---|---|---|
| P1 | S1.B2 | Разворачивание короткой фразы в полный промт через GigaChat API (текст + фото) |
| P3 | S1.B1 / S1.B8 | Прикрепление/замена фото через системный Photo Picker, S1.B8 — убрать фото |
| P4 | S1.B3 / S1.B4 | Фиксация промта, переход на S2 + генерация изображения (GigaChat text2image, бесплатно) |
| P12 | S2.B1 | Сохранение результата в галерею через MediaStore |

---

## Переходы по нажатию (onclick-нотация)

Формат: `{ID}.onclick() --> {ID экрана}.open()`, ниже с отступом — полное имя
элемента и что реально это делает (`Файл.расширение_процедура()` — между
расширением файла и именем процедуры подчёркивание вместо точки, чтобы не
путать конец имени файла с началом имени процедуры), затем многострочный
комментарий с объяснением роли процедуры и каждого параметра/переменной, и
только потом — сами действия.

```
S1.B1.onclick() --> (нет перехода)
    S1.B1.onclick() = MainScreen.kt_launchPhotoPicker()
      /*
       * Открывает системный Photo Picker (ActivityResultContracts.PickVisualMedia).
       * Разрешение READ_MEDIA_IMAGES не требуется — системный picker сам выдаёт
       * приложению доступ к выбранному URI.
       * Вызывается и когда фото ещё не выбрано (кнопка-скрепка), и когда уже
       * выбрано (тап по превью — заменить, тот же launcher).
       * selectedPhotoUri — переменная экрана S1: URI выбранного фото (null, если не выбрано).
       *                     Пока != null, кнопка-скрепка заменяется превью (см. thumbnailBitmap).
       * thumbnailBitmap — переменная экрана S1: уменьшенная копия фото для превью;
       *                     декодируется в LaunchedEffect(selectedPhotoUri) через
       *                     MainScreen.kt_decodeThumbnail() (сэмплирование, не полный размер).
       */
      photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly))
      selectedPhotoUri = <результат выбора>

S1.B8.onclick() --> S1.update()
    S1.B8.onclick() = MainScreen.kt (лямбда на IconButton, крестик поверх превью)
      /*
       * Виден только когда selectedPhotoUri != null (поверх превью фото).
       * Полностью убирает выбранное фото, ничего не открывая заново —
       * в отличие от тапа по самому превью (это снова S1.B1, замена).
       */
      selectedPhotoUri = null
      thumbnailBitmap = null

S1.B2.onclick() --> S1.C1.open()
    S1.B2.onclick() = MainScreen.kt_onExpandPromptClick()   [P1]
      /*
       * Разворачивает короткую фразу пользователя в детализированный промт через ИИ
       * и показывает блок подтверждения S1.C1.
       * shortPrompt — переменная экрана S1: текст, который пользователь ввёл в поле S1.F1.
       * selectedPhotoUri — если не null, фото читается в photoBytes и уходит вместе с текстом.
       * onExpandPrompt — суспенд-колбэк, переданный из AppNavigation.kt; реально вызывает
       *                   PromptExpansionService.kt_expandPrompt() -> GigaChatPromptExpansionService.kt
       *                   (GigaChat API, Сбер, бесплатный тариф, работает из России без VPN).
       * isExpanding — переменная экрана S1: пока true, поверх экрана показан LoadingOverlay (O1).
       * expandedPrompt — переменная экрана S1: развёрнутый промт из PromptExpansionResult.Success;
       *                   как только не пустая, на экране появляется блок S1.C1.
       * expansionError — переменная экрана S1: текст ошибки из PromptExpansionResult.Failed,
       *                   показывается под рядом кнопок красным текстом.
       */
      isExpanding = true
      photoBytes = selectedPhotoUri?.let { readPhotoBytes(it) }
      result = onExpandPrompt(shortPrompt, photoBytes)
      isExpanding = false
      Success --> expandedPrompt = result.expandedPrompt
      Failed  --> expansionError = result.reason

S1.B3.onclick() --> S2.open()
    S1.B3.onclick() = AppNavigation.kt_onSubmit(prompt: String = shortPrompt)   [P4]
      /*
       * Фиксирует промт как есть (исходную короткую фразу, без правок из блока S1.C1),
       * переходит на экран результата и запускает генерацию изображения.
       * prompt — текст, который отправляется на генерацию; для S1.B3 это shortPrompt.
       * lastPrompt — переменная состояния AppNavigation: хранит текущий промт для показа на S2.
       * isLoading — пока true, на S2 поверх экрана LoadingOverlay (O1).
       * imageGenerationService — единственная реализация в v1, GigaChatImageGenerationService.kt
       *                            (встроенная функция text2image GigaChat API, тот же ключ,
       *                            что и у P1, см. ai/GigaChatAuth.kt).
       * resultImageBytes / contentErrorMessage — результат генерации, показывается на S2.C1.
       */
      lastPrompt = prompt; resultImageBytes = null; contentErrorMessage = null; isLoading = true
      navController.navigate("result")
      result = imageGenerationService.generateImage(prompt)
      Success --> resultImageBytes = result.imageBytes
      Failed  --> contentErrorMessage = result.reason
      isLoading = false

S1.B4.onclick() --> S2.open()
    S1.B4.onclick() = AppNavigation.kt_onSubmit(prompt: String = editableText)   [P4]
      /*
       * То же самое, что S1.B3, но editableText — переменная блока S1.C1:
       * то, что пользователь подтвердил/поправил в S1.F2. Остальное идентично.
       */
      lastPrompt = prompt; resultImageBytes = null; contentErrorMessage = null; isLoading = true
      navController.navigate("result")
      result = imageGenerationService.generateImage(prompt)
      Success --> resultImageBytes = result.imageBytes
      Failed  --> contentErrorMessage = result.reason
      isLoading = false

S2.B1.onclick() --> (нет перехода)
    S2.B1.onclick() = AppNavigation.kt_saveResultImage(bytes: ByteArray)   [P12]
      /*
       * Сохраняет resultImageBytes в системную галерею через MediaStore
       * (content/ImageSaver.kt). На API 26-28 сначала запрашивает runtime-разрешение
       * WRITE_EXTERNAL_STORAGE (storagePermissionLauncher); на API 29+ разрешение
       * не нужно (scoped storage) — вызов идёт сразу.
       * pendingSaveBytes — переменная состояния AppNavigation: байты, ожидающие
       *                     сохранения, пока пользователь отвечает на запрос разрешения.
       * saveMessage / saveIsError — переменные состояния AppNavigation: результат
       *                     последней попытки сохранения, показывается на S2 под кнопками.
       */
      needsPermission = SDK_INT < 29 && разрешение не выдано
      needsPermission == true  --> запросить WRITE_EXTERNAL_STORAGE, дождаться ответа
      needsPermission == false --> сразу ImageSaver.kt_saveToGallery(context, bytes)
      Success --> saveMessage = "Сохранено в галерею"
      Failed  --> saveMessage = result.reason, saveIsError = true

S2.B2.onclick() --> S1.open()
    S2.B2.onclick() = AppNavigation.kt_onNewRequest()
      /*
       * Возвращает пользователя на главный экран для нового запроса. Параметров нет.
       */
      navController.popBackStack()
```

---

## Как этим пользоваться в чате

- "Доработай S1.B2" — меняем конкретный элемент интерфейса.
- "Что внутри P1?" — разворачиваю процедуру подробно (переменные, функции).
- После любого изменения — обновляю этот файл (единый источник правды) и,
  если нужно, показываю обновлённый снимок-схему под конкретную задачу.
