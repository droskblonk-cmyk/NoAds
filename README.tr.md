# NoAds

**Ödüllü reklamlar başladığı anda biter, ödülü yine alırsın.**

[English](README.md)

NoAds, Android için açık kaynak bir Xposed modülü. LSPosed ve onun bakımı süren devamı
[Vector](https://github.com/JingMatrix/Vector) için yapıldı. Bir oyun
reklam SDK'sından ödüllü reklam istediğinde ("2× coin için reklam izle") cevabı SDK yerine NoAds verir.
Reklam hiç açılmaz ve oyun birkaç milisaniye sonra "reklam bitti, ödül kazanıldı" haberini alır. NoAds
ayrıca geçiş ve açılış reklamlarını atlar, banner'ların yüklenmesini engeller.

## Nasıl çalışır

Oyunlar reklamı kendileri göstermez. İşi bir reklam SDK'sına verir ve onun callback'lerini beklerler:

```
oyun ── show() ──▶ reklam SDK'sı ── 30 sn video ──▶ "ödül kazanıldı" ──▶ oyun ödülü verir
```

NoAds `show()` çağrısını yakalar ve SDK'nın kendi yaşam döngüsü callback'lerini, SDK'nın kullandığı
sırayla, hemen iletir:

```
oyun ── show() ──▶ NoAds ── birkaç ms ──▶ "açıldı" → "ödül kazanıldı" → "kapandı" ──▶ oyun ödülü verir
```

Neredeyse her oyun birkaç SDK'dan birini kullandığı için tek modül binlerce oyunu kapsar. NoAds'ın
iletebileceği tüm callback'ler tek bir dosyada listelenir:
[`Flows.kt`](module/src/main/kotlin/io/github/sevincfurkan/noads/sdk/Flows.kt).

## Desteklenen SDK'lar

| SDK | Ödüllü | Geçiş | Açılış | Banner |
|---|:---:|:---:|:---:|:---:|
| AdMob / Google Mobile Ads | ✓ | ✓ | ✓ | ✓ |
| AppLovin MAX | ✓ | ✓ | ✓ | ✓ |
| Unity Ads | ✓ | ✓ | yok | ✓ |
| Unity LevelPlay (ironSource): ad unit API ve eski statik `IronSource` API'si | ✓ | ✓ | yok | ✓ |

**Şimdiye kadar test edilenler:** AdMob'un dört formatı da, örnek uygulamayla Android 15'te (Magisk 31.1
ve Vector 2.2 kurulu emülatör) uçtan uca test edildi. Ödül, 9 saniyelik video yerine `show()`'dan 10–25 ms
sonra geliyor ve hiçbir gösterim kaydedilmiyor. AppLovin MAX, Unity Ads ve LevelPlay, SDK binary'lerine
karşı kontrol edildi ama henüz gerçek oyunlarda denenmedi. Geri bildirimler memnuniyetle karşılanır.

Hook'ların kullandığı her sınıf, metot ve callback, `SdkContractTest` ile gerçek SDK binary'lerine karşı
kontrol edilir. Bir SDK güncellemesi bir şeyi yeniden adlandırırsa NoAds sessizce bozulmaz, derleme
kırılır. Unity, Flutter ve React Native ile yapılmış oyunlar da kapsanır, çünkü onlar da aynı Java
SDK'larını kullanır. Her reklam türü NoAds uygulamasından ayrı ayrı açılıp kapatılabilir.

## NoAds'ın asla yapmadıkları

- **Sahte gösterim, tıklama ya da gelir bildirmez.** Reklam hiç gösterilmediği için hiçbir reklamveren
  ücret ödemez. Impression, click, paid-event ve revenue callback'leri hiçbir zaman çağrılmaz; testler
  bunu zorunlu kılar.
- **Mediation SDK'sına kimsenin görmediği bir reklamı saydırmaz.** Reklam ağı hook'ları (AdMob, Unity
  Ads), bir mediation SDK'sının adapter'ından gelen çağrılara dokunmaz. NoAds o mediation SDK'sını
  destekliyorsa reklam bir üst katmanda atlanır.
- **Oyununu bozmaz.** Beklenmedik bir durumda (bilinmeyen SDK sürümü, eksik veri) orijinal metot çalışır
  ve gerçek reklamı görürsün.
- **Anti-cheat, root tespiti ya da bütünlük kontrollerini atlatmaz.** Bunları kontrol eden oyunlar kapsam
  dışı.

## Sınırlar

- **Sunucu tarafında doğrulanan ödüller çalışmaz.** Birçok online oyun ödülü ancak reklam ağı, reklamın
  izlendiğini oyunun sunucusuna bildirince verir (server-side verification). NoAds bunu taklit edemez,
  etmez de. Ödülün cihazda verildiği oyunlarda çalışır; çoğu çevrimdışı ve casual oyun böyledir.
- **Root gerekir**: Zygisk açık Magisk ya da KernelSU, ayrıca LSPosed veya Vector.
- **Kullanım şartları**: reklam atlamak bir uygulamanın şartlarına aykırı olabilir. Online oyunlar bu
  yüzden ban verebilir.
- **Yalnızca Android.** iOS'ta jailbreak gerekirdi.

## Kurulum

1. Cihazını [Magisk](https://github.com/topjohnwu/Magisk) ya da [KernelSU](https://kernelsu.org) ile
   root'la ve Zygisk'i aç.
2. [Vector](https://github.com/JingMatrix/Vector)'ü Magisk ya da KernelSU üzerinden kur. Vector, 2023'ten
   beri yeni sürüm çıkmayan ve Android 14'te kalan LSPosed'in devamı.
3. NoAds APK'sını kur.
4. Vector (LSPosed) yöneticisinde **NoAds**'ı etkinleştir ve çalışmasını istediğin oyunları işaretle.
5. O oyunları zorla durdurup yeniden aç.

Reklam türlerini açıp kapatmak için başlatıcıdan **NoAds**'ı aç. Değişiklikler hemen geçerli olur.

## Kaynaktan derleme

JDK 17 veya üstü ve Android SDK (platform 36) gerekir.

```bash
./gradlew :module:assembleDebug
```

```bash
./gradlew :module:testDebugUnitTest
```

Unit testler callback akışlarını, reflection yardımcılarını ve SDK sözleşmelerini kapsar.

### Örnek uygulamayla dene

`sample/`, Google'ın herkese açık test reklamlarıyla coin kazandıran küçük bir "oyun". Her callback'in
`show()` çağrısından ne kadar sonra geldiğini loglar:

```bash
./gradlew :sample:installDebug
```

```bash
adb shell am start -n io.github.sevincfurkan.noads.sample/.MainActivity --es run rewarded
```

```bash
adb logcat -s NoAdsSample NoAds
```

NoAds aktifken `earned 10 coins 3 ms after show()` satırını görürsün, reklam açılmaz. NoAds yokken test
reklamı oynar. Diğer senaryolar: `rewarded_interstitial`, `interstitial`, `app_open`, `banner`.

## Katkı

Yeni bir SDK eklemek tek bir Kotlin dosyası ve testlerinden ibaret; bkz. [CONTRIBUTING.md](CONTRIBUTING.md).

## Sevdiğin oyunları destekle

Atlanan her ödüllü reklam, geliştiricinin karşılığını alamadığı bir ödül demek. Bir oyunu seviyorsan
"reklamları kaldır" paketini satın al ya da stüdyoyu başka bir yolla destekle.

## Sorumluluk reddi

NoAds kendi cihazında kişisel kullanım içindir. Google, AppLovin, Unity ya da burada adı geçen başka bir
şirketle bağlantılı değildir ve onlar tarafından desteklenmez. İsimler ve markalar sahiplerine aittir.
Kullandığın uygulamaların şartlarına uymak senin sorumluluğundadır.

## Lisans

[GPL-3.0](LICENSE). Fork'lar da açık kaynak kalmak zorunda, böylece herkes bir sürümün iddia ettiğini
yaptığını kontrol edebilir.
