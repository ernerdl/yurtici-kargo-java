package tr.com.sevinclidoga.yurtici;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Yurtiçi Kargo — hazır metotlar. 2026-10-28'de canlı (okuma) ve test ortamında (yazma) çalıştığı doğrulanan
 * işlemlerin hepsi buradadır; ayrıntı ve tuzaklar {@code docs/ISLEM_DURUMU.md}.
 * <pre>{@code
 * var yk = new YurticiKargo(YurticiAyarlari.canli(kullanici, sifre), musteriKodu);
 * GonderiDetayi d = yk.gonderiSorgula("SD-SIP-123");
 * List<KargoKaydi> donmeyenler = yk.teslimEdilemeyenler(LocalDate.of(2026,10,1), LocalDate.of(2026,10,31), cikisSubeId);
 * }</pre>
 * Yurtiçi bir isteği reddederse {@link YurticiIslemHatasi} (Yurtiçi'nin mesajı ve kodu ile), ağ/zaman aşımında
 * {@link YurticiHatasi} fırlar. İş parçacığı güvenlidir.
 */
public class YurticiKargo {

    private static final DateTimeFormatter TARIH = DateTimeFormatter.BASIC_ISO_DATE;

    /** Özel alan 3 = "Sipariş No" (raporlarda referans alanı olarak çalıştığı doğrulandı). */
    public static final String ALAN_SIPARIS_NO = "3";
    /** Özel alan 53 = "Anahtar Alan" (kargo anahtarı). */
    public static final String ALAN_ANAHTAR = "53";
    /** Özel alan 14 = "Müşteri Kargo Ref Kodu". */
    public static final String ALAN_MUSTERI_REF = "14";
    /** Özel alan 16 = "İade Onay Kodu" (iade kodu için; sözleşmede tanımlı olmalı). */
    public static final String ALAN_IADE_ONAY_KODU = "16";

    /** Rapor isteğinde müşteri kodunun hangi rolde aranacağı. */
    public enum Yon {
        /** Gönderen müşteri. */
        GONDEREN("senderCustIdArray"),
        /** Ödeyen (sözleşmeli) müşteri — başkasının gönderip sizin ödedikleriniz dahil. */
        ODEYEN("invCustIdArray"),
        /** Alıcı müşteri. */
        ALICI("receiverCustIdArray");

        final String alan;

        Yon(String alan) {
            this.alan = alan;
        }
    }

    private final YurticiIstemci istemci;
    private final String musteriKodu;

    /**
     * @param ayarlar     bağlantı
     * @param musteriKodu Yurtiçi müşteri (cari) kodunuz — rapor işlemleri için. Herhangi bir gönderi sorgusunun
     *                    cevabında {@code senderCustId} olarak görünür.
     */
    public YurticiKargo(YurticiAyarlari ayarlar, String musteriKodu) {
        this(new YurticiIstemci(ayarlar), musteriKodu);
    }

    public YurticiKargo(YurticiIstemci istemci, String musteriKodu) {
        this.istemci = Objects.requireNonNull(istemci, "istemci");
        this.musteriKodu = Objects.requireNonNull(musteriKodu, "musteriKodu");
    }

    /** Hazır metodu olmayan işlemler için alt seviye istemci. */
    public YurticiIstemci istemci() {
        return istemci;
    }

    // ═══════════════════════════ GÖNDERİ ═══════════════════════════

    /** Gönderi açar ({@code createShipment}). Aynı anahtar zaten açıksa hata değil, {@code zatenVarMi=true} döner. */
    public GonderiSonuclari.Acma gonderiAc(GonderiTalebi t) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("cargoKey", t.kargoAnahtari());
        vo.put("invoiceKey", t.faturaAnahtari());
        vo.put("receiverCustName", t.aliciAdi());
        vo.put("receiverAddress", t.aliciAdresi());
        vo.put("cityName", t.il());
        vo.put("townName", t.ilce());
        vo.put("receiverPhone1", t.telefon());
        vo.put("emailAddress", t.eposta());
        vo.put("desi", sayiMetni(t.desi()));
        vo.put("kg", sayiMetni(t.kg()));
        vo.put("cargoCount", t.koliSayisi());
        vo.put("specialField1", t.ozelAlan1());
        vo.put("description", t.aciklama());
        XmlDugum k = istemci.cagir(YurticiIslem.CREATE_SHIPMENT, Map.of("ShippingOrderVO", vo));
        XmlDugum d = k.cocuk("shippingOrderDetailVO").orElse(k);
        String errCode = d.deger("errCode");
        String errMessage = tekBosluk(d.deger("errMessage"));
        boolean basarili = "0".equals(k.deger("outFlag")) && (errCode == null || "0".equals(errCode));
        boolean zatenVar = !basarili && errMessage != null && errMessage.contains("sistemde mevcuttur");
        return new GonderiSonuclari.Acma(basarili, zatenVar,
                Objects.requireNonNullElse(d.deger("cargoKey"), t.kargoAnahtari()), d.deger("invoiceKey"),
                k.deger("jobId"), basarili ? null : errCode,
                basarili ? null : Objects.requireNonNullElse(errMessage, k.deger("outResult")));
    }

    /** Gönderileri iptal eder ({@code cancelShipment}); kurye almadıysa çalışır. Anahtar başına sonuç döner. */
    public List<GonderiSonuclari.Iptal> gonderiIptal(String... kargoAnahtarlari) {
        XmlDugum k = istemci.cagir(YurticiIslem.CANCEL_SHIPMENT, Map.of("cargoKeys", String.join(",", kargoAnahtarlari)));
        List<GonderiSonuclari.Iptal> l = new ArrayList<>();
        for (XmlDugum d : k.hepsi("shippingCancelDetailVO")) {
            String errCode = d.deger("errCode");
            GonderiDurumu durum = GonderiDurumu.koddan(d.deger("operationStatus"));
            l.add(new GonderiSonuclari.Iptal(d.deger("cargoKey"), durum == GonderiDurumu.IPTAL && (errCode == null || "0".equals(errCode)),
                    durum, d.deger("operationMessage"), errCode, tekBosluk(d.deger("errMessage"))));
        }
        if (l.isEmpty() && !"0".equals(k.deger("outFlag"))) {
            throw new YurticiIslemHatasi("cancelShipment", k.deger("errCode"), k.deger("outResult"));
        }
        return l;
    }

    /**
     * Gönderinin durumu, Yurtiçi'nin ölçtüğü kg/desi, ücret ve şube şube hareketleri
     * ({@code queryShipmentDetail}, geçmişli). Bulunamayan anahtar istisna değil, {@code bulundu=false} döner.
     */
    public GonderiDetayi gonderiSorgula(String kargoAnahtari) {
        XmlDugum k = istemci.cagir(YurticiIslem.QUERY_SHIPMENT_DETAIL, sirali("keys", kargoAnahtari, "keyType", "0",
                "addHistoricalData", "true", "onlyTracking", "false"));
        if (!"0".equals(k.deger("outFlag"))) {
            throw new YurticiIslemHatasi("queryShipmentDetail", k.deger("errCode"), k.deger("outResult"));
        }
        XmlDugum d = k.cocuk("shipmentDeliveryDetailVO").orElse(null);
        if (d == null) {
            return new GonderiDetayi(kargoAnahtari, false, GonderiDurumu.BILINMIYOR, null, null, null, k.deger("outResult"), null);
        }
        String errCode = d.deger("errCode");
        if (errCode != null && !"0".equals(errCode)) {
            // "Bulunamadı" Başarılı cevapla gelir; asıl hata burada (82519)
            return new GonderiDetayi(kargoAnahtari, false, GonderiDurumu.BILINMIYOR, null, null, errCode,
                    tekBosluk(d.deger("errMessage")), null);
        }
        String kod = d.deger("operationStatus");
        KargoKaydi kayit = d.cocuk("shipmentDeliveryItemDetailVO").map(KayitCozucu::coz).orElse(null);
        return new GonderiDetayi(kargoAnahtari, true, GonderiDurumu.koddan(kod), kod, d.deger("operationMessage"), null, null, kayit);
    }

    // ═══════════════════════════ İADE KODU ═══════════════════════════

    /**
     * Müşterinin şubeye / YK Plus dolabına "iade kodu" ile bırakacağı iade için kod tanımlar ({@code saveReturnShipmentCode}).
     * Sözleşmenizde {@link #ALAN_IADE_ONAY_KODU} tanımlı olmalı; değilse Yurtiçi "Sözleşme Üzerinde Tanımlı Özel Alan
     * Bilgisi Bulunamadı" der. Yurtiçi'nin kendi kodları 8 haneli rakamdır.
     */
    public void iadeKoduTanimla(String kod, LocalDate baslangic, LocalDate bitis, int enFazlaKullanim) {
        XmlDugum k = istemci.cagir(YurticiIslem.SAVE_RETURN_SHIPMENT_CODE, sirali("fieldName", ALAN_IADE_ONAY_KODU,
                "returnCode", kod, "startDate", baslangic.format(TARIH), "endDate", bitis.format(TARIH),
                "maxCount", String.valueOf(enFazlaKullanim)));
        basariliOlmali("saveReturnShipmentCode", k);
    }

    /** Tanımlı iade kodunu iptal eder ({@code cancelReturnShipmentCode}). Olmayan kodda Yurtiçi hata döner. */
    public void iadeKoduIptal(String kod) {
        XmlDugum k = istemci.cagir(YurticiIslem.CANCEL_RETURN_SHIPMENT_CODE, sirali("fieldName", ALAN_IADE_ONAY_KODU, "returnCode", kod));
        basariliOlmali("cancelReturnShipmentCode", k);
    }

    // ═══════════════════════════ RAPORLAR ═══════════════════════════

    /**
     * Tarih aralığındaki tüm gönderiler + ücret ({@code listInvDocumentByCustIdV2}). Sayfalamaz, üst sınırı yoktur
     *; büyük aralıkta zaman aşımını artırın ya da ay ay çağırın.
     */
    public List<KargoKaydi> donemGonderileri(LocalDate bas, LocalDate son, Yon yon, boolean hareketlerle) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_BY_CUST_ID_V2, sirali("custParamsVO", Map.of(yon.alan, musteriKodu),
                "subCustomerFlag", "0", "startDate", t(bas), "endDate", t(son), "dateParamType", "0",
                "withCargoLifecycle", bayrak(hareketlerle), "pageSequenceNumber", "1", "totalPages", "0"));
    }

    /** Teslim edilemeyenler + nedenleri ({@code listInvDocumentInterfaceWithFailedDeliveryV2}). Çıkış şubesi zorunlu. */
    public List<KargoKaydi> teslimEdilemeyenler(LocalDate bas, LocalDate son, int cikisBirimId) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_INTERFACE_WITH_FAILED_DELIVERY_V2, sirali("custParamsVO",
                Map.of(Yon.GONDEREN.alan, musteriKodu), "fieldName", ALAN_SIPARIS_NO, "departureUnitId", cikisBirimId,
                "startDate", t(bas), "endDate", t(son), "dateParamType", "0"));
    }

    /** Takip numarasıyla koli takibi + KDV'li ücret + teslim belgesi bağlantısı ({@code doGetTrackingInfo}). */
    public List<KargoKaydi> takipBilgisi(boolean hareketlerle, String... takipNolari) {
        return rapor(YurticiIslem.DO_GET_TRACKING_INFO, sirali("custParamsVO", Map.of(Yon.GONDEREN.alan, musteriKodu),
                "fieldName", ALAN_SIPARIS_NO, "docIdArray", liste(takipNolari), "withCargoLifecycle", bayrak(hareketlerle)));
    }

    /** Bir günde gönderilenler + ücret ({@code doGetTrackingInfoWithDate}). */
    public List<KargoKaydi> gunlukGonderiler(LocalDate gun) {
        return rapor(YurticiIslem.DO_GET_TRACKING_INFO_WITH_DATE, sirali("customerIdVO", Map.of(Yon.GONDEREN.alan, musteriKodu),
                "dispatchDate", t(gun), "pageSequenceNumber", "1", "totalPageNumber", "0"));
    }

    /** Takip numarasıyla tam belge (gönderen adres kodu dahil) ({@code listInvDocumentByTrackingNumber}). */
    public List<KargoKaydi> takipNoIleBelgeler(boolean hareketlerle, String... takipNolari) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_BY_TRACKING_NUMBER, Map.of("trackingParamsVO",
                sirali("docIdArray", liste(takipNolari), "withCargoLifecycle", bayrak(hareketlerle))));
    }

    /** Özel alan değeriyle ({@code listInvDocumentByGrpSpecialField}); ör. alan 53 + kargo anahtarları. */
    public List<KargoKaydi> ozelAlanIleBelgeler(String alanNo, List<String> degerler, LocalDate bas, LocalDate son, boolean hareketlerle) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_BY_GRP_SPECIAL_FIELD, sirali("custParamsVO", Map.of(Yon.GONDEREN.alan, musteriKodu),
                "fieldName", alanNo, "fieldValueArray", degerler, "startDate", t(bas), "endDate", t(son), "dateParamType", "0",
                "withCargoLifecycle", bayrak(hareketlerle)));
    }

    /** Referans (özel alan) ile ({@code listInvDocumentInterfaceByReference}). */
    public List<KargoKaydi> referansIleBelgeler(String alanNo, List<String> degerler, LocalDate bas, LocalDate son, boolean hareketlerle) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_INTERFACE_BY_REFERENCE, sirali("custParamsVO", Map.of(Yon.GONDEREN.alan, musteriKodu),
                "fieldName", alanNo, "fieldValueArray", degerler, "startDate", t(bas), "endDate", t(son), "dateParamType", "0",
                "withCargoLifecycle", bayrak(hareketlerle)));
    }

    /** Ödeyen müşteri + referans ile, tarihsiz ({@code listInvDocumentByReferenceWithPayer}). Not: ücret alanları 0 döner. */
    public List<KargoKaydi> odeyenReferansIleBelgeler(String alanNo, List<String> degerler, boolean hareketlerle) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_BY_REFERENCE_WITH_PAYER, sirali("customerVO", Map.of("invCustIdArray", musteriKodu),
                "fieldName", alanNo, "fieldValueArray", degerler, "withCargoLifecycle", bayrak(hareketlerle)));
    }

    /** Transfer merkezi bilgisiyle ({@code listInvDocInterfaceWithTrnCenter}). */
    public List<KargoKaydi> transferMerkezliBelgeler(String... takipNolari) {
        return rapor(YurticiIslem.LIST_INV_DOC_INTERFACE_WITH_TRN_CENTER, sirali("invCustParamsVO", Map.of("invCustIdArray", musteriKodu),
                "fieldName", ALAN_SIPARIS_NO, "docIdArray", liste(takipNolari), "withTrnCenterFlag", "1", "withCargoLifecycle", "0"));
    }

    /** Çıkış şubesine göre müşteri belgeleri ({@code listInvDocumentWithCustId}). Şube olmadan Yurtiçi "Beklenmeyen hata" verir. */
    public List<KargoKaydi> musteriBelgeleri(LocalDate bas, LocalDate son, int cikisBirimId) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_WITH_CUST_ID, sirali("custParamsVO", Map.of(Yon.GONDEREN.alan, musteriKodu),
                "unitParamsVO", Map.of("departureUnitId", cikisBirimId), "subCustomerFlag", "0", "startDate", t(bas),
                "endDate", t(son), "dateParamType", "0", "pageSequenceNumber", "1", "totalPages", "1"));
    }

    /** Belge tarihine göre ({@code listInvDocumentWithDocDateInvCustId}). Çıkış şubesi zorunlu. */
    public List<KargoKaydi> belgeTarihineGoreBelgeler(LocalDate bas, LocalDate son, int cikisBirimId) {
        return rapor(YurticiIslem.LIST_INV_DOCUMENT_WITH_DOC_DATE_INV_CUST_ID, sirali("invCustParamsVO",
                Map.of("invCustIdArray", musteriKodu), "unitParamsVO", Map.of("departureUnitId", cikisBirimId),
                "startDate", t(bas), "endDate", t(son), "pageSequenceNumber", "1", "totalPages", "1"));
    }

    /** Bize gelen koli ({@code listIncomingWithDocCargoReference}); koli no + teslim şubesi zorunlu. */
    public List<KargoKaydi> gelenKoliIleBelgeler(String koliNo, int teslimBirimId, LocalDate bas, LocalDate son) {
        return rapor(YurticiIslem.LIST_INCOMING_WITH_DOC_CARGO_REFERENCE, sirali("custParamsVO", Map.of(Yon.ALICI.alan, musteriKodu),
                "docCargoId", koliNo, "deliveryUnitId", teslimBirimId, "startDate", t(bas), "endDate", t(son),
                "dateParamType", "0", "withCargoLifecycle", "0"));
    }

    /** Bizden giden koli ({@code listOutboundWithDocCargoReference}); koli no + çıkış şubesi zorunlu. */
    public List<KargoKaydi> gidenKoliIleBelgeler(String koliNo, int cikisBirimId, LocalDate bas, LocalDate son) {
        return rapor(YurticiIslem.LIST_OUTBOUND_WITH_DOC_CARGO_REFERENCE, sirali("custParamsVO", Map.of(Yon.GONDEREN.alan, musteriKodu),
                "docCargoId", koliNo, "departureUnitId", cikisBirimId, "startDate", t(bas), "endDate", t(son),
                "dateParamType", "0", "withCargoLifecycle", "0"));
    }

    /** NGI listesi (NGI olmayan gönderileri de döndürür; ayrıntılı alanlar: performans, teslim alan …) ({@code listNgiShipping}). */
    public List<KargoKaydi> ngiListesi(String alanNo, List<String> degerler, LocalDate bas, LocalDate son, boolean hareketlerle) {
        return rapor(YurticiIslem.LIST_NGI_SHIPPING, sirali("custParamsVO", Map.of(Yon.GONDEREN.alan, musteriKodu),
                "fieldName", alanNo, "fieldValueArray", degerler, "startDate", t(bas), "endDate", t(son), "dateParamType", "0",
                "withCargoLifecycle", bayrak(hareketlerle)));
    }

    /** NGI listesi, takip numarasıyla ({@code listNgiShippingWithDocId}). Not: ücret 0 döner. */
    public List<KargoKaydi> ngiTakipNoIle(boolean hareketlerle, String... takipNolari) {
        return rapor(YurticiIslem.LIST_NGI_SHIPPING_WITH_DOC_ID, sirali("customerVO", Map.of("invCustIdArray", musteriKodu),
                "docIdArray", liste(takipNolari), "withCargoLifecycle", bayrak(hareketlerle)));
    }

    /** NGI listesi, ödeyen + referans ({@code listNgiShippingWithPayer}). Not: ücret 0 döner. */
    public List<KargoKaydi> ngiOdeyenReferansIle(String alanNo, List<String> degerler, boolean hareketlerle) {
        return rapor(YurticiIslem.LIST_NGI_SHIPPING_WITH_PAYER, sirali("customerVO", Map.of("invCustIdArray", musteriKodu),
                "fieldName", alanNo, "fieldValueArray", degerler, "withCargoLifecycle", bayrak(hareketlerle)));
    }

    /**
     * Tahsilatlı (kapıda ödemeli) teslimler ({@code getDeliveryCollectionInfoByReference}). Kapıda ödemeli gönderisi
     * olmayan hesapta boş liste döndüğü doğrulandı; DOLU cevap henüz görülmedi, alan eşlemesi doğrulanmadı.
     */
    public List<KargoKaydi> tahsilatliTeslimler(LocalDate bas, LocalDate son) {
        return rapor(YurticiIslem.GET_DELIVERY_COLLECTION_INFO_BY_REFERENCE, sirali("startDate", t(bas), "endDate", t(son),
                "dateType", "0", "custId", musteriKodu));
    }

    // ── yardımcılar ──

    private List<KargoKaydi> rapor(YurticiIslem islem, Map<String, ?> p) {
        XmlDugum k = istemci.cagir(islem, p);
        if (k.deger("outFlag") != null && !"0".equals(k.deger("outFlag"))) {
            throw new YurticiIslemHatasi(islem.ad(), k.deger("errCode"), Objects.requireNonNullElse(k.deger("outResult"), "Yurtiçi hata"));
        }
        return KayitCozucu.kayitlar(k);
    }

    private static void basariliOlmali(String islem, XmlDugum k) {
        if (!"0".equals(k.deger("outFlag"))) {
            throw new YurticiIslemHatasi(islem, k.deger("errCode"), k.deger("outResult"));
        }
    }

    private static Map<String, Object> sirali(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static List<String> liste(String... v) {
        if (v == null || v.length == 0) throw new IllegalArgumentException("en az bir değer gerekli");
        return Arrays.asList(v);
    }

    private static String t(LocalDate g) {
        return Objects.requireNonNull(g, "tarih").format(TARIH);
    }

    private static String bayrak(boolean b) {
        return b ? "1" : "0";
    }

    private static String sayiMetni(BigDecimal b) {
        return b == null ? "1" : b.stripTrailingZeros().toPlainString();
    }

    private static String tekBosluk(String s) {
        return s == null ? null : s.replaceAll("\\s+", " ").trim();
    }
}
