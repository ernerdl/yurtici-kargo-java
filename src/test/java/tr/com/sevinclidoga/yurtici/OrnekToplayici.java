package tr.com.sevinclidoga.yurtici;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test örneği toplayıcı (yalnız geliştirme): işlemleri gerçek parametrelerle çağırır, cevabı ANONİMLEŞTİRİP
 * {@code src/test/resources/ornek/} altına yazar. Okumalar canlıda (salt okuma), yazmalar Yurtiçi test ortamında.
 * <p>
 * Anonimleştirme: kişi/firma adı, adres, telefon, e-posta, vergi/kimlik no, barkod, takip/anahtar numaraları ve
 * kullanıcı adı ya "ANON" olur ya da biçimi korunarak sahte değere çevrilir (aynı değer her yerde aynı sahte değeri
 * alır, ilişkiler bozulmaz). Şube/olay adları, ücretler, kg/desi, tarih ve durum kodları korunur.
 * <pre>YURTICI_KULLANICI=… YURTICI_SIFRE=… YURTICI_MUSTERI_KODU=… \
 *   ORNEK_TESLIM_ANAHTAR=… ORNEK_YOLDA_ANAHTAR=… ORNEK_TAKIP_NO=… ORNEK_KOLI_NO=… \
 *   ORNEK_CIKIS_SUBE=… ORNEK_TESLIM_SUBE=… java … OrnekToplayici</pre>
 * Gerçek müşteri kodu / takip numarası / anahtar kodda tutulmaz; ortam değişkeninden okunur.
 */
public final class OrnekToplayici {

    static final Path HEDEF = Path.of("src/test/resources/ornek");

    /** Değeri tamamen silinecek alanlar (ad, adres, iletişim, kimlik). */
    static final Pattern SIL = Pattern.compile("(?i).*(custname|cust_name|address(?!id)|addresstxt|addresstext|phone|gsm|mail"
            + "|taxnumber|taxno|tckn|identity|receiverinfo|delempname|persongiver|receivername|sendername|consigneecustname"
            + "|description|barcodestringvalue|documentreceiver|additionalinfo|latitude|longitude|invcustname"
            + "|deliveredto|usercreate|userupdate|pickupdocumentno|signature|receivedby|teslimalan).*");

    /** Biçimi korunarak sahteye çevrilecek numaralar (takip, anahtar, belge, müşteri/adres kimlikleri). */
    static final Pattern SAHTE = Pattern.compile("(?i)^(docid|cargokey|invoicekey|waybillno|docnumber|invoicenumber|fieldvalue"
            + "|doccargoid|returndocid|keys|trackingurl|ttdocumentid|receivercustid|receiveraddressid|senderaddressid|jobid"
            + "|returncode|contractid|ngicargokey|ngidocumentkey|cargonumber|pod)$");

    /** Ücret alanları: anlaşmalı tarife gizli kalsın diye geri döndürülemez sahte değere çevrilir (0 korunur). */
    static final Pattern FIYAT = Pattern.compile("(?i)^(totalprice|totalvat|totalamount|totalpricewithvat|productprice"
            + "|returntotalprice|returntotalvat|returntotalamount|returntotalpricewithvat|ttinvoiceamount|price|amount|collectionamount)$");

    static final Map<String, String> esleme = new HashMap<>();
    static String kullaniciAdi = "";

    public static void main(String[] a) throws Exception {
        Files.createDirectories(HEDEF);
        String k = System.getenv("YURTICI_KULLANICI"), s = System.getenv("YURTICI_SIFRE");
        kullaniciAdi = k;
        var canli = new YurticiIstemci(new YurticiAyarlari(YurticiAyarlari.CANLI_ADRES, k, s, "TR", Duration.ofSeconds(120), false));
        var test = new YurticiIstemci(YurticiAyarlari.test());
        String C = env("YURTICI_MUSTERI_KODU"), D = env("ORNEK_TAKIP_NO"), CK = env("ORNEK_TESLIM_ANAHTAR");
        String YOLDA = env("ORNEK_YOLDA_ANAHTAR"), KOLI = env("ORNEK_KOLI_NO"), CIKIS = env("ORNEK_CIKIS_SUBE"), TESLIM = env("ORNEK_TESLIM_SUBE");

        // ── canlı, salt okuma ──
        kaydet(canli, "gonderi-detay-teslim", YurticiIslem.QUERY_SHIPMENT_DETAIL, p("keys", CK, "keyType", "0", "addHistoricalData", "true", "onlyTracking", "false"));
        kaydet(canli, "gonderi-detay-dagitimda", YurticiIslem.QUERY_SHIPMENT_DETAIL, p("keys", YOLDA, "keyType", "0", "addHistoricalData", "true", "onlyTracking", "false"));
        kaydet(canli, "gonderi-detay-bulunamadi", YurticiIslem.QUERY_SHIPMENT_DETAIL, p("keys", "SD-YOK-0000", "keyType", "0", "addHistoricalData", "true", "onlyTracking", "false"));
        kaydet(canli, "gonderi-sorgu-teslim", YurticiIslem.QUERY_SHIPMENT, p("keys", CK, "keyType", "0", "addHistoricalData", "false", "onlyTracking", "false"));
        kaydet(canli, "rapor-takip", YurticiIslem.DO_GET_TRACKING_INFO, p("custParamsVO", Map.of("senderCustIdArray", C), "fieldName", "3", "docIdArray", D, "withCargoLifecycle", "1"));
        kaydet(canli, "rapor-takip-gune-gore", YurticiIslem.DO_GET_TRACKING_INFO_WITH_DATE, p("customerIdVO", Map.of("senderCustIdArray", C), "dispatchDate", "20261022", "pageSequenceNumber", "1", "totalPageNumber", "0"));
        kaydet(canli, "rapor-tahsilatli-teslim", YurticiIslem.GET_DELIVERY_COLLECTION_INFO_BY_REFERENCE, p("startDate", "20261020", "endDate", "20261027", "dateType", "0", "custId", C));
        kaydet(canli, "rapor-gelen-koli", YurticiIslem.LIST_INCOMING_WITH_DOC_CARGO_REFERENCE, p("custParamsVO", Map.of("receiverCustIdArray", C), "docCargoId", KOLI, "deliveryUnitId", TESLIM, "startDate", "20261020", "endDate", "20261027", "dateParamType", "0", "withCargoLifecycle", "0"));
        kaydet(canli, "rapor-giden-koli", YurticiIslem.LIST_OUTBOUND_WITH_DOC_CARGO_REFERENCE, p("custParamsVO", Map.of("senderCustIdArray", C), "docCargoId", KOLI, "departureUnitId", CIKIS, "startDate", "20261020", "endDate", "20261027", "dateParamType", "0", "withCargoLifecycle", "0"));
        kaydet(canli, "rapor-transfer-merkezli", YurticiIslem.LIST_INV_DOC_INTERFACE_WITH_TRN_CENTER, p("invCustParamsVO", Map.of("invCustIdArray", C), "fieldName", "3", "docIdArray", D, "withTrnCenterFlag", "1", "withCargoLifecycle", "0"));
        kaydet(canli, "rapor-donem", YurticiIslem.LIST_INV_DOCUMENT_BY_CUST_ID_V2, p("custParamsVO", Map.of("senderCustIdArray", C), "subCustomerFlag", "0", "startDate", "20261022", "endDate", "20261022", "dateParamType", "0", "withCargoLifecycle", "0", "pageSequenceNumber", "1", "totalPages", "0"));
        kaydet(canli, "rapor-ozel-alan", YurticiIslem.LIST_INV_DOCUMENT_BY_GRP_SPECIAL_FIELD, p("custParamsVO", Map.of("senderCustIdArray", C), "fieldName", "53", "fieldValueArray", CK, "startDate", "20261020", "endDate", "20261027", "dateParamType", "0", "withCargoLifecycle", "0"));
        kaydet(canli, "rapor-odeyen-referans", YurticiIslem.LIST_INV_DOCUMENT_BY_REFERENCE_WITH_PAYER, p("customerVO", Map.of("invCustIdArray", C), "fieldName", "53", "fieldValueArray", CK, "withCargoLifecycle", "0"));
        kaydet(canli, "rapor-takip-no", YurticiIslem.LIST_INV_DOCUMENT_BY_TRACKING_NUMBER, p("trackingParamsVO", Map.of("docIdArray", D, "withCargoLifecycle", "1")));
        kaydet(canli, "rapor-referans", YurticiIslem.LIST_INV_DOCUMENT_INTERFACE_BY_REFERENCE, p("custParamsVO", Map.of("senderCustIdArray", C), "fieldName", "53", "fieldValueArray", CK, "startDate", "20261020", "endDate", "20261027", "dateParamType", "0", "withCargoLifecycle", "0"));
        kaydet(canli, "rapor-teslim-edilemeyen", YurticiIslem.LIST_INV_DOCUMENT_INTERFACE_WITH_FAILED_DELIVERY_V2, p("custParamsVO", Map.of("senderCustIdArray", C), "fieldName", "3", "departureUnitId", CIKIS, "startDate", "20261001", "endDate", "20261027", "dateParamType", "0"));
        kaydet(canli, "rapor-musteri", YurticiIslem.LIST_INV_DOCUMENT_WITH_CUST_ID, p("custParamsVO", Map.of("senderCustIdArray", C), "unitParamsVO", Map.of("departureUnitId", CIKIS), "subCustomerFlag", "0", "startDate", "20261022", "endDate", "20261022", "dateParamType", "0", "pageSequenceNumber", "1", "totalPages", "1"));
        kaydet(canli, "rapor-belge-tarihi", YurticiIslem.LIST_INV_DOCUMENT_WITH_DOC_DATE_INV_CUST_ID, p("invCustParamsVO", Map.of("invCustIdArray", C), "unitParamsVO", Map.of("departureUnitId", CIKIS), "startDate", "20261022", "endDate", "20261022", "pageSequenceNumber", "1", "totalPages", "1"));
        kaydet(canli, "rapor-ngi", YurticiIslem.LIST_NGI_SHIPPING, p("custParamsVO", Map.of("senderCustIdArray", C), "fieldName", "53", "fieldValueArray", CK, "startDate", "20261020", "endDate", "20261027", "dateParamType", "0", "withCargoLifecycle", "1"));
        kaydet(canli, "rapor-ngi-takip-no", YurticiIslem.LIST_NGI_SHIPPING_WITH_DOC_ID, p("customerVO", Map.of("invCustIdArray", C), "docIdArray", D, "withCargoLifecycle", "0"));
        kaydet(canli, "rapor-ngi-odeyen", YurticiIslem.LIST_NGI_SHIPPING_WITH_PAYER, p("customerVO", Map.of("invCustIdArray", C), "fieldName", "53", "fieldValueArray", CK, "withCargoLifecycle", "0"));
        // geri dönüş (rejectStatus) içeren gün: 21.10.2026 (Teslim İptal + İade Onaylanmadı + Borçlandırma İptal)
        kaydet(canli, "rapor-donem-geri-donuslu", YurticiIslem.LIST_INV_DOCUMENT_BY_CUST_ID_V2, p("custParamsVO", Map.of("senderCustIdArray", C), "subCustomerFlag", "0", "startDate", "20261021", "endDate", "20261021", "dateParamType", "0", "withCargoLifecycle", "0", "pageSequenceNumber", "1", "totalPages", "0"));

        // ── Yurtiçi test ortamı: yazma akışı ──
        String ck = "SDORNEK-" + System.currentTimeMillis() % 1_000_000;
        kaydet(test, "gonderi-ac", YurticiIslem.CREATE_SHIPMENT, p("ShippingOrderVO", p("cargoKey", ck, "invoiceKey", ck,
                "receiverCustName", "Deneme Alici Test", "receiverAddress", "Test Mahallesi Deneme Sokak No 1", "cityName", "Ankara",
                "townName", "Çankaya", "receiverPhone1", "5000000000", "desi", "2", "kg", "3", "cargoCount", "1")));
        kaydet(test, "gonderi-ac-eksik-alan", YurticiIslem.CREATE_SHIPMENT, p("ShippingOrderVO", p("cargoKey", ck + "X")));
        kaydet(test, "gonderi-detay-islem-gormemis", YurticiIslem.QUERY_SHIPMENT_DETAIL, p("keys", ck, "keyType", "0", "addHistoricalData", "true", "onlyTracking", "false"));
        kaydet(test, "gonderi-iptal", YurticiIslem.CANCEL_SHIPMENT, p("cargoKeys", ck));
        String rc = String.valueOf(10_000_000 + (System.currentTimeMillis() % 89_999_999));
        kaydet(test, "iade-kodu-tanimla", YurticiIslem.SAVE_RETURN_SHIPMENT_CODE, p("fieldName", "16", "returnCode", rc, "startDate", "20261028", "endDate", "20261110", "maxCount", "1"));
        kaydet(test, "iade-kodu-tekrar", YurticiIslem.SAVE_RETURN_SHIPMENT_CODE, p("fieldName", "16", "returnCode", rc, "startDate", "20261028", "endDate", "20261110", "maxCount", "1"));
        kaydet(test, "iade-kodu-alan-tanimsiz", YurticiIslem.SAVE_RETURN_SHIPMENT_CODE, p("fieldName", "3", "returnCode", rc + "9", "startDate", "20261028", "endDate", "20261110", "maxCount", "1"));
        kaydet(test, "iade-kodu-iptal", YurticiIslem.CANCEL_RETURN_SHIPMENT_CODE, p("fieldName", "16", "returnCode", rc));
        kaydet(test, "iade-kodu-iptal-yok", YurticiIslem.CANCEL_RETURN_SHIPMENT_CODE, p("fieldName", "16", "returnCode", rc));
        System.out.println("tamam: " + HEDEF.toAbsolutePath());
    }

    static String env(String ad) {
        String v = System.getenv(ad);
        if (v == null || v.isBlank()) throw new IllegalStateException("ortam değişkeni eksik: " + ad);
        return v;
    }

    static void kaydet(YurticiIstemci ist, String ad, YurticiIslem islem, Map<String, ?> prm) throws Exception {
        String ham;
        try {
            ham = ist.hamCagir(islem, prm);
        } catch (YurticiHatasi h) {
            System.out.println("ATLANDI " + ad + ": " + h.getMessage());
            return;
        }
        String anonim = anonimlestir(ham);
        Files.writeString(HEDEF.resolve(ad + ".xml"), anonim);
        System.out.printf("%-34s %8d bayt%n", ad, anonim.length());
    }

    static String anonimlestir(String xml) {
        Matcher m = Pattern.compile("<([A-Za-z][A-Za-z0-9_]*)>([^<]*)</\\1>").matcher(xml);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String ad = m.group(1), deger = m.group(2), yeni = deger;
            if (!deger.isBlank()) {
                // yurticikargo.com takip bağlantısı dışındaki her bağlantı (teslim belgesi, jetonlu kısa link) ve JWT silinir
                boolean baglanti = deger.startsWith("http") && !deger.startsWith("https://www.yurticikargo.com/");
                if (baglanti || deger.contains("eyJ")) yeni = "https://ornek.invalid/ANON";
                else if (FIYAT.matcher(ad).matches()) yeni = sahteFiyat(deger.trim());
                else if (SAHTE.matcher(ad).matches()) yeni = sahte(deger);
                else if (SIL.matcher(ad).matches() && !ad.endsWith("UnitName") && !ad.endsWith("Explanation")) yeni = "ANON";
                else if (ad.equals("senderCustId") || ad.equals("invCustId") || ad.equals("custId")) yeni = "100000001";
            }
            if (!kullaniciAdi.isEmpty()) yeni = yeni.replace(kullaniciAdi, "KULLANICI");
            m.appendReplacement(sb, Matcher.quoteReplacement("<" + ad + ">" + yeni + "</" + ad + ">"));
        }
        m.appendTail(sb);
        String s = sb.toString();
        // eşlenen numaralar başka yerde düz metin olarak geçebilir (URL, hata mesajı) → aynı sahte değer
        for (var e : esleme.entrySet()) if (e.getKey().length() >= 6) s = s.replace(e.getKey(), e.getValue());
        return kullaniciAdi.isEmpty() ? s : s.replace(kullaniciAdi, "KULLANICI");
    }

    static String sahteFiyat(String v) {
        try {
            if (Double.parseDouble(v) == 0) return v;
            byte[] h = java.security.MessageDigest.getInstance("SHA-256").digest(("ucret:" + v).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            long n = Math.floorMod(new java.math.BigInteger(1, h).longValue(), 95000L);
            return String.format(java.util.Locale.ROOT, "%.2f", 50 + n / 100.0);
        } catch (Exception e) {
            return v;
        }
    }

    /** Biçimi koruyan, değer başına sabit sahte: rakamlar rakam, harfler harf. */
    static String sahte(String v) {
        if (v.startsWith("http")) {
            int esit = v.lastIndexOf('=');
            return esit > 0 ? v.substring(0, esit + 1) + sahte(v.substring(esit + 1)) : v;
        }
        return esleme.computeIfAbsent(v, x -> {
            StringBuilder sb = new StringBuilder();
            int h = Math.abs(x.hashCode());
            for (int i = 0; i < x.length(); i++) {
                char c = x.charAt(i);
                int r = (h >> (i % 24)) + i * 7;
                if (Character.isDigit(c)) sb.append((char) ('0' + Math.floorMod(r, 10)));
                else if (Character.isLetter(c) && i > 2) sb.append((char) ('A' + Math.floorMod(r, 26)));
                else sb.append(c);
            }
            return sb.toString();
        });
    }

    static Map<String, Object> p(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private OrnekToplayici() {}

    @SuppressWarnings("unused")
    private static final List<String> NOT = List.of("ham cevap diske YAZILMAZ; yalnız anonim hali");
}
