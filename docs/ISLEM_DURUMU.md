# Yurtiçi işlemleri — gerçek davranış

Her işlem gerçek Yurtiçi'ye karşı denendi: okumalar **canlı** hesapta (salt okuma), yazmalar **Yurtiçi test ortamında**
(herkese açık test kullanıcısı `YKTEST` / `YK`). Bu belge yalnız davranışı anlatır.

Özet: **37 işlemden 27'si çalışıyor** (23 sorunsuz ✅ + 4 dikkat ⚠️). **10'u çalışmıyor**: 9'u Yurtiçi'nin hesapta bir
şey açmasını bekliyor 🔒 (6'sı NGI), 1'i Yurtiçi sunucu hatası ❌.

| Durum | Anlam |
|---|---|
| ✅ | Gerçek istekle denendi, doğru cevap geldi |
| ⚠️ | Çalışıyor, dikkat edilecek bir şey var |
| 🔒 | Yurtiçi'nin hesapta açması / tanımlaması gerekiyor |
| ❌ | Yurtiçi tarafında hata |

## Gönderi servisi (ShippingOrderDispatcherServices) — 8

| İşlem | Durum | Not |
|---|---|---|
| createShipment | ✅ | `ShippingOrderVO` {cargoKey, invoiceKey, receiverCustName, receiverAddress, cityName, townName, receiverPhone1, desi, kg, cargoCount}. Aynı anahtarla ikinci açma: "sistemde mevcuttur" |
| queryShipment | ✅ | |
| queryShipmentDetail | ✅ | `addHistoricalData=true` → şube şube hareketler + ücret + kg/desi. Kurye almadan önce yalnız durum (NOP) gelir |
| cancelShipment | ✅ | `cargoKeys`; `operationStatus=CNL`. İptal sonrası sorgu "bulunamadı" (82519) döner, "iptal" demez |
| saveReturnShipmentCode | 🔒 | Özel alan **16 = "İade Onay Kodu"**; Yurtiçi'nin kodları 8 haneli rakamdır. Sözleşmede bu alan tanımlı değilse: "Sözleşme Üzerinde Tanımlı Özel Alan Bilgisi Bulunamadı" → Yurtiçi'ye tanımlatın. Test ortamında çalışır. Aynı kod iki kez: "daha önceden kaydedilmiştir" |
| cancelReturnShipmentCode | ✅ | Olmayan kod: "Iade kodu sistemde bulunmamaktadır" |
| createShipmentDetail | 🔒 | `orderData` bir JSON metni; biçimi belgesiz, denenen biçimlerin hepsi `80859 CARGO_KEY parametresi hatalı`. Aynı işi createShipment görür |
| createShipmentWithDelivery | 🔒 | Zorunlular: `mainProdCode=STA` (Standart), personGiver, shipmentContent (≥5 karakter), description, `XESender` {senderCustId, senderAddressId}, `XEDocCargoZplFormatData` {docCargoZplDataFormat TXT/BASE64, zplType ZPL2/EPL}. Hepsi verilince: **83647 "ZPL servis yetkilendirmesi bulunamadı"** → Yurtiçi ZPL servisini açmalı. Gönderici adres kodu (`senderAddressId`) kendi gönderilerinizin `listInvDocumentByTrackingNumber` cevabında görünür |

## Rapor servisi (WsReportWithReferenceServices) — 18

Müşteri kodu (`senderCustId` / `invCustId` / `receiverCustId`) sözleşme kodunuzdur; şube kodları cevaplarda
(`departureUnitId`, `deliveryUnitId`) görünür.

| İşlem | Durum | Not |
|---|---|---|
| doGetTrackingInfo | ✅ | `fieldName=3`, `docIdArray`, `withCargoLifecycle=1` → hareketler, KDV dahil ücret, teslim belgesi bağlantısı |
| doGetTrackingInfoWithDate | ✅ | Bir günün (`dispatchDate`) tüm gönderileri + ücret |
| getDeliveryCollectionInfoByReference | ✅ | Tahsilatlı (kapıda ödemeli) teslimler; kapıda ödemesi olmayan hesapta boş döner — dolu cevap görülmedi |
| listIncomingWithDocCargoReference | ✅ | `docCargoId` (koli no) + `deliveryUnitId` zorunlu |
| listOutboundWithDocCargoReference | ✅ | `docCargoId` + `departureUnitId` zorunlu |
| listInvDocInterfaceWithTrnCenter | ✅ | Transfer merkezi bilgisiyle |
| **listInvDocumentByCustIdV2** | ✅ | **Tarih aralığındaki tüm gönderiler + ücret.** Sayfalamaz (`pageSequenceNumber=2` aynı kayıtları döndürür) ve **üst sınırı yoktur**: yıllarca geriye giden aralık tek istekte eksiksiz gelir. Gönderi başına ~4 KB, ~2 ms; pratikte ay ay çekin ya da zaman aşımını artırın |
| listInvDocumentByGrpSpecialField | ✅ | Özel alanla (ör. `fieldName=53` + kargo anahtarı) |
| listInvDocumentByReferenceWithPayer | ⚠️ | Çalışır, ücret alanları 0 döner |
| listInvDocumentByTrackingNumber | ✅ | `trackingParamsVO` {docIdArray, withCargoLifecycle}; gönderici adres kodu dahil |
| listInvDocumentByWaybillNo | ❌ | Her çağrıda `%s: geçersiz belirleyici` (doldurulmamış mesaj şablonu). Aynı bilgi diğer üç işlemden alınır |
| listInvDocumentInterfaceByReference | ✅ | |
| **listInvDocumentInterfaceWithFailedDeliveryV2** | ✅ | **Teslim edilemeyenler + neden.** `fieldName=3` ve `departureUnitId` zorunlu |
| listInvDocumentWithCustId | ✅ | `unitParamsVO.departureUnitId` zorunlu (yoksa "Beklenmeyen bir hata") |
| listInvDocumentWithDocDateInvCustId | ✅ | Aynı şekilde `unitParamsVO.departureUnitId` zorunlu |
| listNgiShipping | ✅ | NGI olmayan gönderileri de döndürür; en ayrıntılı kayıt (performans, teslim alan …) |
| listNgiShippingWithDocId | ⚠️ | Çalışır, ücret 0 döner |
| listNgiShippingWithPayer | ⚠️ | Çalışır, ücret 0 döner |

## NGI servisi (NgiShipmentInterfaceServices) — 11

Test ortamında denendi. NGI ile gönderi açma hesap bazında Yurtiçi'nin açmasını gerektirir; **alıcı adresi
değiştirme** yalnız NGI ile açılmış gönderide mümkündür (standart createShipment gönderisini NGI bulamaz).

| İşlem | Durum | Not |
|---|---|---|
| createNgiShipment, …WithAddress, …WithDelivery, …WithZplKtnDelivery | 🔒 | `1624 Bu web servisi parametre girişi yapmadan çağıramazsınız` |
| createNgiShipmentWithPayer, doSaveNgiPayerCustomers | 🔒 | `13 Beklenmeyen bir hata` — kardeş işlemler 1624 verdiği için büyük ihtimalle aynı neden |
| selectShipment | ✅ | Standart gönderiyi bulmaz ("kayıt bulunamadı") |
| updateConsigneeAddress | 🔒 | NGI gönderisi olmadan denenemez |
| updateShipmentDesiWeight | ⚠️ | **Var olmayan gönderi için bile "Başarılı."** döner → sonucu ayrıca sorgulayıp doğrulayın |
| cancelNgiShipment, cancelNgiShipmentWithoutReturn | ✅ | Olmayan anahtarda anlamlı hata (82923) |

## Tuzaklar

1. **"Bulunamadı" hatası "Başarılı" ile gelir:** `outFlag=0`, asıl hata detaydaki `errCode=82519`.
2. **`GOK` = "Kuryede/zimmetlendi (dağıtıma çıktı)"** — normaldir.
3. **Ücret desi-kg'ye göredir:** kg=0, desi dolu gönderi olur → `totalDesiKg` kullanın.
4. Saat alanı 5 hane gelebilir (`63744` = 06:37:44).
5. Kimlik alanlarının adı işleme göre değişir (`wsLanguage` / `userLanguage` / `wsUserLanguage`; `doSaveNgiPayerCustomers`
   `userName/password/userLanguage`) → `YurticiIslem` tablosu taşır.
6. İptal edilen gönderi sorgulanınca "iptal" değil "bulunamadı" döner.
7. Rapor işlemlerinin çoğu şube kodu ister; eksikse anlamsız "Beklenmeyen bir hata oluştu".
8. Aynı bilgi kayıt ailesine göre farklı adla gelir (KDV dahil ücret: `totalAmount` / `totalPriceWithVat` /
   `desiPrice.totalPriceWithVat`) → kütüphanedeki `KargoKaydi` hepsini tek modele çevirir.
9. **Kargo anahtarıyla sorgu yalnız kendi açtığınız gönderiyi görür** (`queryShipment` / `queryShipmentDetail`): elle
   açılmış gönderi 82519 döner, yolda ya da teslim edilmiş olsa bile. Takip numarasıyla `listInvDocumentByTrackingNumber`
   bütün gönderileri görür (teslim: olay kodu `OK`).

## Özel alanlar

Görülen alan türleri: 3 Sipariş No, 14 Müşteri Kargo Ref Kodu, 16 İade Onay Kodu, 20 Kampanya Anahtar Alanı,
53 Anahtar Alan, 62 Fatura. Hangilerinin kullanılabildiği sözleşmeye bağlıdır.

## Geri dönüş (teslim edilemeyip göndericiye dönen) bilgisi

Rapor kayıtlarında: `rejectFlag`, `rejectStatus` / `rejectStatusExplanation` (7 Teslim İptal, 10 İade Sonlandırıldı,
11 İade Onaylanmadı, 8 Borçlandırma İptal …), `rejectReasonExplanation`, `rejectDescription` (şube notu),
`returnDocId`, `returnStatusExplanation`, `returnTotal*` → `KargoKaydi.geriDonus()`.

## Yurtiçi'ye sorulacaklar (hesabınızda açılması gerekenler)

1. Sözleşmeye **16 numaralı "İade Onay Kodu"** alanının tanımlanması (iade kodu için).
2. **NGI web servisi** (hata 1624) — alıcı adresi değiştirme için şart.
3. `createShipmentWithDelivery` için **ZPL servis yetkilendirmesi** (hata 83647).
4. `createShipmentDetail` `orderData` JSON biçiminin belgesi.
