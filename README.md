# Yurtiçi Kargo Java İstemcisi

Yurtiçi Kargo KOPS web servisleri (gönderi, rapor, NGI) için bağımsız **Java 21** istemcisi. Çalışma zamanı bağımlılığı
yok (`java.net.http` + JDK XML).

Her işlem **gerçek Yurtiçi'ye karşı denendi** (2026-10-28): okumalar canlı hesapta, yazmalar Yurtiçi test ortamında.
Hangi işlemin çalıştığı, hangi parametreyi zorunlu tuttuğu ve tuzakları: [`docs/ISLEM_DURUMU.md`](docs/ISLEM_DURUMU.md).
37 işlemin tam listesi: [`docs/ISLEM_ENVANTERI.md`](docs/ISLEM_ENVANTERI.md).

## Hızlı başlangıç

```java
var yk = new YurticiKargo(YurticiAyarlari.canli(kullanici, sifre), musteriKodu);

// Gönderi aç
var sonuc = yk.gonderiAc(GonderiTalebi.of("SD-SIP-1001", "Ayşe Yılmaz", "Atatürk Cad. No 5 D 3",
        "Ankara", "Çankaya", "0532 123 45 67", new BigDecimal("2"), new BigDecimal("3"), 1));
if (!sonuc.acikMi()) System.out.println(sonuc.hataMesaji());

// Durum, Yurtiçi'nin ölçtüğü kg/desi, ücret, şube şube hareketler
GonderiDetayi d = yk.gonderiSorgula("SD-SIP-1001");
if (d.bulundu() && d.kayit() != null) {
    System.out.println(d.durum() + " " + d.kayit().ucretKdvli() + " TL, " + d.kayit().desiKg() + " desi/kg");
    d.hareketler().forEach(h -> System.out.println(h.zaman() + " " + h.birimAdi() + " " + h.olayAdi()));
}

// Ayın tüm gönderileri + ücretleri (tek istek, sayfalama yok)
List<KargoKaydi> ay = yk.donemGonderileri(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
        YurticiKargo.Yon.GONDEREN, false);

// Teslim edilemeyenler + nedenleri (çıkış şubesi kimliği zorunlu)
yk.teslimEdilemeyenler(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), cikisSubeId)
  .forEach(k -> System.out.println(k.takipNo() + " " + k.nedenAciklama()));

// İptal (kurye almadıysa)
yk.gonderiIptal("SD-SIP-1001");
```

**Müşteri kodu** (`musteriKodu`): Yurtiçi cari kodunuz; herhangi bir rapor cevabında `senderCustId` olarak görünür.
**Çıkış şubesi kimliği**: `takipNoIleBelgeler(true, takipNo)` sonucundaki ilk hareketin `birimId()`'si.

## Metotlar

| Metot | Yurtiçi işlemi | Not |
|---|---|---|
| `gonderiAc` | createShipment | Aynı anahtar ikinci kez → `zatenVarMi()` |
| `gonderiSorgula` | queryShipmentDetail | Durum + ücret + kg/desi + hareketler; bulunamazsa `bulundu=false` |
| `gonderiIptal` | cancelShipment | Anahtar başına sonuç |
| `iadeKoduTanimla` / `iadeKoduIptal` | saveReturnShipmentCode / cancel… | Sözleşmede alan 16 "İade Onay Kodu" tanımlı olmalı |
| `donemGonderileri` | listInvDocumentByCustIdV2 | Gönderen / ödeyen / alıcı yönünde; sınırsız aralık |
| `teslimEdilemeyenler` | …WithFailedDeliveryV2 | Nedenleriyle |
| `takipBilgisi` | doGetTrackingInfo | KDV'li ücret, koli hareketleri |
| `gunlukGonderiler` | doGetTrackingInfoWithDate | Bir günün gönderileri |
| `takipNoIleBelgeler` | listInvDocumentByTrackingNumber | Gönderen adres kodu dahil |
| `ozelAlanIleBelgeler`, `referansIleBelgeler`, `odeyenReferansIleBelgeler` | …GrpSpecialField / …ByReference / …WithPayer | Alan 3 Sipariş No, 53 Anahtar, 14 Müşteri Ref |
| `transferMerkezliBelgeler` | listInvDocInterfaceWithTrnCenter | |
| `musteriBelgeleri`, `belgeTarihineGoreBelgeler` | …WithCustId / …WithDocDateInvCustId | Çıkış şubesi zorunlu |
| `gelenKoliIleBelgeler`, `gidenKoliIleBelgeler` | listIncoming… / listOutbound… | Koli no + şube zorunlu |
| `ngiListesi`, `ngiTakipNoIle`, `ngiOdeyenReferansIle` | listNgiShipping… | NGI olmayan gönderileri de döndürür |
| `tahsilatliTeslimler` | getDeliveryCollectionInfoByReference | Dolu cevap henüz görülmedi |

Bütün raporlar aynı modeli döndürür: **`KargoKaydi`** (takip no, şubeler, son olay ve nedeni, ücret/KDV/KDV dahil,
kg/desi/desi-kg, geri dönüş bilgisi, özel alanlar, koliler, hareketler). Hazır metodu olmayan işlemler için
`yk.istemci().cagir(YurticiIslem.X, parametreler)`.

## Bilinmesi gereken tuzaklar (kütüphane çözer)

- "Bulunamadı" cevabı **"Başarılı"** ile gelir; hata detaydaki `errCode` 82519'dadır.
- `GOK` = "Kuryede / dağıtıma çıktı" — sorun değildir (`KargoKaydi.sorunluMu()` bunu bilir).
- Ücret kg'ye değil **desi-kg**'ye göre; kg=0 desi=6 olan gönderi vardır.
- Kimlik alanlarının adı işleme göre değişir (`wsLanguage` / `userLanguage` / `wsUserLanguage`).
- Rapor işlemlerinin çoğu şube kodu ister; eksikse Yurtiçi anlamsız "Beklenmeyen bir hata" der.
- İptal edilmiş gönderi sorgulanınca çoğu zaman "iptal" değil "bulunamadı" döner.

## Çalışmayanlar / Yurtiçi'den beklenenler

NGI ile gönderi açma ve **adres değiştirme** (hesapta kapalı, hata 1624), `createShipmentWithDelivery` (ZPL yetkisi, 83647),
iade kodu (sözleşmeye alan 16 tanımlanmalı), `createShipmentDetail` (belgesiz JSON biçimi), `listInvDocumentByWaybillNo`
(Yurtiçi sunucu hatası). Ayrıntı: `docs/ISLEM_DURUMU.md`.

## Güvenlik

- Canlı ortamda yazma işlemleri (gönderi açma/iptal, iade kodu…) `YurticiAyarlari.canliYazmayaIzinVer()` olmadan çalışmaz.
- XML çözümleme XXE'ye kapalı; şifre `toString` / hata mesajlarına düşmez.
- Test örnekleri (`src/test/resources/ornek/`) gerçek cevaplardan **anonimleştirilerek** üretildi (`OrnekToplayici`).

## Test

```bash
./mvnw test                         # çevrimdışı: sahte sunucu + anonim gerçek cevaplar
YURTICI_KULLANICI=… YURTICI_SIFRE=… YURTICI_MUSTERI_KODU=… ./mvnw test -Pcanli-deneme
                                    # canlı salt okuma + Yurtiçi test ortamında yazma
```

Keşif aracı: `./kesif.sh test|canli ISLEM alan=deger ...` (çıktı kişisel veri maskeli).

## Lisans ve sorumluluk

[MIT](LICENSE). Yurtiçi Kargo ile resmi bir bağı yoktur; Yurtiçi'nin web servisleri ve adı Yurtiçi Kargo'ya aittir.
Servis davranışı Yurtiçi tarafından değiştirilebilir — `docs/ISLEM_DURUMU.md` 2026-10-28 itibarıyla gözlenendir.
