# Регистрация по номеру телефона

В приложении используется **Firebase Phone Auth**. Это настоящая отправка SMS: код
проверяет Firebase, приложение получает `uid` и номер пользователя.

Без Firebase-проекта кнопка регистрации честно показывает «SMS-регистрация не настроена».
Приложение не подменяет SMS локальным кодом.

## Что нужно сделать один раз

1. Создайте проект в [Firebase Console](https://console.firebase.google.com/).
2. Добавьте Android-приложение с package name `com.devai.studio`.
3. В разделе Authentication → Sign-in method включите **Phone**.
4. Добавьте SHA-1 и SHA-256 вашего ключа подписи.
   Для release-ключа DevAI:
   `54fe7482456c7cce0543c3ee700ef23a9f3583cc1bd82a980cdac1b045849d6a`.
5. Скопируйте `google-services.json` не нужен — приложению нужны только три строки.
6. Откройте `local.properties` в корне проекта и добавьте:

```properties
FIREBASE_API_KEY=AIza...
FIREBASE_APP_ID=1:123456789:android:abcdef
FIREBASE_PROJECT_ID=devai-studio
```

7. Пересоберите APK.

## Важно

- Firebase Phone Auth отправляет SMS через квоту проекта. Для реальных пользователей
  включите billing и проверьте лимиты.
- Для теста добавьте тестовый номер в Firebase → Authentication → Phone numbers.
- Номер и `uid` хранятся в `EncryptedSharedPreferences`.
- Ник можно менять отдельно, без регистрации.
