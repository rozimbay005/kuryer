# Kuryer kabineti — Android ilova (WebView + push-style xabarnoma)

## Bu ilova nima qiladi

- rozimbay.uz saytidagi kuryer kabinetini (login, buyurtmalar, profil) to'g'ridan-to'g'ri
  telefon ilovasi ko'rinishida ochadi.
- Kuryer tizimga kirgach, fon xizmati (foreground service) ishga tushadi va har
  15 soniyada `courier/api/get_orders.php` manzilini tekshirib turadi.
- Yangi buyurtma paydo bo'lsa — ilova yopiq yoki fon rejimida bo'lsa ham,
  ovoz va tebranish bilan bildirishnoma chiqadi.

## Muhim: doimiy bildirishnoma haqida

Android talabiga ko'ra, fon xizmati doimiy ishlashi uchun pastda doimiy
(o'chmaydigan) "Kuryer ilovasi ishlamoqda" bildirishnomasi ko'rinib turadi.
Bu — barcha yetkazib berish ilovalari (Yandex, Wolt va h.k.) da bo'ladigan
odatiy holat, ilova fonda ishlab turganini bildiradi.

## APK yasash (GitHub Actions orqali, avvalgi loyihadagidek)

1. Ushbu papkani GitHub repositoryga yuklang (`.github` papkasini ham,
   yashirin fayl ekanini unutmang).
2. Actions bo'limida "Build APK" workflow'ni "Run workflow" bilan ishga
   tushiring.
3. Tugagach, "Artifacts" bo'limidan `courier-cabinet-apk` faylini yuklab oling.

## O'rnatgandan keyin

1. Ilovani oching — avtomatik login sahifasi ochiladi.
2. Login/parolingiz bilan kiring.
3. Kirgandan so'ng, telefon "Bildirishnoma yuborishga ruxsat" so'raydi — albatta
   ruxsat bering, aks holda yangi buyurtma xabarnomasi kelmaydi.
4. Telefon sozlamalarida shu ilova uchun **battery optimization**ni o'chirib
   qo'ying ("Unrestricted" / "Cheklanmagan"), aks holda fon xizmati vaqti-vaqti
   bilan telefon tomonidan to'xtatilib qo'yilishi mumkin (ayniqsa Xiaomi,
   Huawei, Honor kabi brendlarda buni alohida ruxsat berish kerak bo'ladi).

## Keyingi qadam sifatida yaxshilash mumkin bo'lgan narsalar

- Hozircha "yangi buyurtma" tekshiruvi 15 soniyada bir marta bo'lyapti —
  bu polling (so'rov yuborish) usuli. Haqiqiy, zudlik bilan keladigan push
  xabarnoma uchun Firebase Cloud Messaging (FCM) qo'shish kerak bo'ladi —
  bu esa serverdagi PHP kodga ham o'zgartirish talab qiladi (buyurtma
  yaratilganda serverdan FCM orqali telefonga signal yuborish).
- Sessiya (login) muddati tugasa, fon xizmati ishlamay qoladi — ilovani
  qayta ochib, qayta login qilish kifoya.
