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
   - SHA-1: `8D:31:13:15:18:62:BF:20:39:B2:8A:A1:4F:76:9E:6C:DB:C6:54:EF`
   - SHA-256: `54:FE:74:82:45:6C:7C:CE:05:43:C3:EE:70:0E:F2:3A:9F:35:83:CC:1B:D8:2A:98:0C:DA:C1:B0:45:84:9D:6A`
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
