# Moliya — Android ilova

Google Apps Script veb-ilovasini telefonda oddiy ilova kabi ochadi.
APK'ni GitHub o'zi yig'adi (GitHub Actions), kompyuterga hech narsa o'rnatish shart emas.

## Sozlash
Faqat `gradle.properties` faylidagi 3 qator:

| Qator | Nima |
|---|---|
| `APP_NAME` | Telefonda ko'rinadigan nom |
| `APP_ID` | Ilovaning noyob ID'si (har bir ilova uchun boshqacha bo'lsin) |
| `APP_URL` | Apps Script `/exec` havolasi |

## Muhim: Apps Script'ni to'g'ri joylash
Apps Script'da **Deploy → Manage deployments → ✏️** :
- **Execute as:** Me
- **Who has access:** **Anyone**

"Anyone with Google account" yoki "Only myself" bo'lsa, ilova Google kirish oynasini ochadi,
Google esa ilova ichida kirishni bloklaydi.

Telefonda chiroyli ko'rinishi uchun `doGet` ichida:
```js
return HtmlService.createHtmlOutputFromFile('index')
  .addMetaTag('viewport', 'width=device-width, initial-scale=1')
  .setTitle('Moliya');
```

## Yangilash
Apps Script kodini o'zgartirsangiz, APK'ni qayta yig'ish shart emas —
ilova har ochilganda saytning eng yangi versiyasini ko'rsatadi
(faqat "New deployment" emas, mavjud deploymentni "New version" qilib yangilang, shunda havola o'zgarmaydi).
