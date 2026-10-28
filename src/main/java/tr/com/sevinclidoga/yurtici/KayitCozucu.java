package tr.com.sevinclidoga.yurtici;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yurtiçi'nin dört farklı kayıt ailesini tek {@link KargoKaydi}'na çevirir. Aynı bilgi ailelere göre farklı adla
 * gelir (ör. KDV dahil ücret: {@code totalAmount} / {@code totalPriceWithVat} / {@code desiPrice.totalPriceWithVat});
 * her alan için adaylar sırayla denenir.
 * <ol>
 *   <li>Standart belge: {@code shippingData*DetailVOArray}, {@code trackingDetailVOArray}, {@code shippingDataTrnVOArray},
 *       {@code shippingDetailResponseVOArray}, gönderi servisinin {@code shipmentDeliveryItemDetailVO}</li>
 *   <li>NGI listesi: {@code shippingDetailVOArray} (kısaltmalı adlar: depUnitName, arrUnitName …)</li>
 *   <li>Koli takibi: {@code parcelTrackingArray} (alt bloklar: currentEventInfo, desiPrice, routing, pod …)</li>
 * </ol>
 */
final class KayitCozucu {

    private static final DateTimeFormatter TARIH = DateTimeFormatter.BASIC_ISO_DATE;

    private KayitCozucu() {}

    /** Sonuç kökünün altındaki kayıt öğeleri (düz alan olmayan çocuklar). */
    static List<KargoKaydi> kayitlar(XmlDugum sonuc) {
        List<KargoKaydi> l = new ArrayList<>();
        for (XmlDugum c : sonuc.cocuklar()) {
            if (c.metin() == null && !c.cocuklar().isEmpty()) l.add(coz(c));
        }
        return l;
    }

    static KargoKaydi coz(XmlDugum r) {
        XmlDugum fiyat = r.cocuk("desiPrice").orElse(null);
        XmlDugum rota = r.cocuk("routing").orElse(null);
        XmlDugum sonOlay = r.cocuk("currentEventInfo").orElse(null);

        return new KargoKaydi(
                d(r, "docId"), d(r, "docNumber"), d(r, "waybillNo"), d(r, "invoiceNumber"),
                zaman(ilk(r, "documentDate", "departureDate"), ilk(r, "documentTime", "departureTime")),
                d(r, "senderCustId"), d(r, "invCustId"), d(r, "receiverCustId"), d(r, "senderAddressId"),
                ilk(r, "receiverCustName", "recCustName"), ilk(r, "receiverAddressTxt", "receiverAddressText", "recAddressTxt"),
                ilk(r, "receiverCityName", "arrCityName", "delCityName"),
                ilceBul(r),
                birim(ilk(r, "departureUnitName", "depUnitName"), rota, "departureUnitName"),
                ilk(r, "arrivalUnitName", "arrUnitName"),
                birim(ilk(r, "deliveryUnitName", "delUnitName"), rota, "deliveryUnitName"),
                ilkVeya(ilk(r, "currentUnitName"), sonOlay, "eventUnitName"),
                ilkVeya(d(r, "cargoEventId"), sonOlay, "cargoEventId"),
                ilkVeya(ilk(r, "cargoEventExplanation", "cargoEventName"), sonOlay, "cargoEventName"),
                ilkVeya(d(r, "cargoReasonId"), sonOlay, "cargoReasonId"),
                ilkVeya(ilk(r, "cargoReasonExplanation", "cargoReasonName"), sonOlay, "cargoReasonName"),
                tarih(d(r, "estimatedDeliveryDate")),
                zaman(ilk(r, "deliveryDate", "shipmentDeliveryDate"), ilk(r, "deliveryTime", "shipmentDeliveryTime")),
                sayiVeya(r, fiyat, "totalPrice"),
                sayiVeya(r, fiyat, "totalVat"),
                ilkSayi(sayi(r, "totalAmount"), sayi(r, "totalPriceWithVat"), fiyat == null ? null : fiyat.sayi("totalPriceWithVat")),
                ilkSayi(sayi(r, "totalKg"), fiyat == null ? null : fiyat.sayi("totalWeight")),
                sayiVeya(r, fiyat, "totalDesi"),
                sayi(r, "totalDesiKg"),
                ilkTam(r.tamsayi("totalCargo"), r.tamsayi("totalCargoCount"), fiyat == null ? null : fiyat.tamsayi("totalCargoCount")),
                geriDonus(r, fiyat),
                ozelAlanlar(r),
                koliler(r),
                hareketler(r),
                d(r, "trackingUrl"));
    }

    // ── alt bölümler ──

    private static KargoKaydi.GeriDonus geriDonus(XmlDugum r, XmlDugum fiyat) {
        String bayrak = d(r, "rejectFlag");
        String durumKodu = d(r, "rejectStatus");
        String iadeDoc = d(r, "returnDocId");
        boolean var = "1".equals(bayrak) || d(r, "rejectStatusExplanation") != null || d(r, "rejectReasonExplanation") != null
                || (iadeDoc != null && !iadeDoc.equals("0"));
        if (!var) return null;
        BigDecimal iadeUcret = ilkSayi(sayi(r, "returnTotalAmount"), fiyat == null ? null : fiyat.sayi("returnTotalPriceWithVat"));
        return new KargoKaydi.GeriDonus(durumKodu, d(r, "rejectStatusExplanation"),
                ilk(r, "rejectReasonExplanation", "rejectReason"), d(r, "rejectDescription"),
                iadeDoc != null && !iadeDoc.equals("0") ? iadeDoc : null, d(r, "returnStatusExplanation"),
                iadeUcret != null && iadeUcret.signum() != 0 ? iadeUcret : null);
    }

    /** {@code invDocFieldVOArray} / {@code specialFieldArray} + kaydın kendi fieldName/fieldValue'su. */
    private static Map<String, String> ozelAlanlar(XmlDugum r) {
        Map<String, String> m = new LinkedHashMap<>();
        List<XmlDugum> alanlar = new ArrayList<>(r.hepsi("invDocFieldVOArray"));
        alanlar.addAll(r.hepsi("specialFieldArray"));
        for (XmlDugum f : alanlar) {
            String no = f.deger("fieldName"), ad = f.deger("fieldNameExplanation"), deger = f.deger("fieldValue");
            if (deger == null) continue;
            if (no != null) m.putIfAbsent(no, deger);
            if (ad != null) m.putIfAbsent(ad, deger);
        }
        String no = d(r, "fieldName"), deger = d(r, "fieldValue");
        if (no != null && deger != null) m.putIfAbsent(no, deger);
        return Map.copyOf(m);
    }

    private static List<KargoKaydi.Koli> koliler(XmlDugum r) {
        List<KargoKaydi.Koli> l = new ArrayList<>();
        for (XmlDugum k : r.hepsi("docCargoVOArray")) {
            l.add(new KargoKaydi.Koli(k.deger("docCargoId"), k.sayi("desi"), k.sayi("kg"), k.sayi("desiKg")));
        }
        return List.copyOf(l);
    }

    /** Üç ayrı hareket dizisi biçimi: rapor (docCargoLifeCycleVOArray), koli takibi (parcelLifeCycleArray), gönderi servisi (invDocCargoVOArray). */
    static List<KargoKaydi.Hareket> hareketler(XmlDugum r) {
        List<KargoKaydi.Hareket> l = new ArrayList<>();
        for (XmlDugum h : r.hepsi("docCargoLifeCycleVOArray")) l.add(hareket(h));
        for (XmlDugum h : r.hepsi("invDocCargoVOArray")) l.add(hareket(h));
        for (XmlDugum h : r.hepsi("parcelLifeCycleArray")) {
            l.add(new KargoKaydi.Hareket(zaman(h.deger("cargoEventDate"), h.deger("cargoEventTime")), h.deger("cargoEventId"),
                    h.deger("cargoEventName"), h.deger("cargoReasonId"), h.deger("cargoReasonName"), h.tamsayi("eventUnitId"),
                    h.deger("eventUnitName"), h.deger("eventLocation")));
        }
        l.removeIf(h -> h.zaman() == null);
        return List.copyOf(l);
    }

    private static KargoKaydi.Hareket hareket(XmlDugum h) {
        String konum = h.deger("townName") != null && h.deger("cityName") != null
                ? h.deger("townName") + "/" + h.deger("cityName") : h.deger("cityName");
        return new KargoKaydi.Hareket(zaman(h.deger("eventDate"), h.deger("eventTime")), h.deger("eventId"), h.deger("eventName"),
                h.deger("reasonId"), h.deger("reasonName"), h.tamsayi("unitId"), h.deger("unitName"), konum);
    }

    private static String ilceBul(XmlDugum r) {
        String ilce = d(r, "receiverTownName");
        if (ilce != null) return ilce;
        String konum = d(r, "receiverLocation");   // "BEYKOZ/İSTANBUL"
        return konum != null && konum.contains("/") ? konum.substring(0, konum.indexOf('/')) : null;
    }

    // ── küçük yardımcılar ──

    private static String d(XmlDugum r, String ad) {
        return r.deger(ad);
    }

    private static String ilk(XmlDugum r, String... adlar) {
        for (String a : adlar) {
            String v = r.deger(a);
            if (v != null) return v;
        }
        return null;
    }

    private static String ilkVeya(String v, XmlDugum blok, String ad) {
        return v != null ? v : blok == null ? null : blok.deger(ad);
    }

    private static String birim(String v, XmlDugum rota, String ad) {
        return ilkVeya(v, rota, ad);
    }

    private static BigDecimal sayi(XmlDugum r, String ad) {
        return r.sayi(ad);
    }

    private static BigDecimal sayiVeya(XmlDugum r, XmlDugum blok, String ad) {
        BigDecimal v = r.sayi(ad);
        return v != null ? v : blok == null ? null : blok.sayi(ad);
    }

    private static BigDecimal ilkSayi(BigDecimal... v) {
        for (BigDecimal b : v) if (b != null) return b;
        return null;
    }

    private static Integer ilkTam(Integer... v) {
        for (Integer i : v) if (i != null) return i;
        return null;
    }

    static LocalDate tarih(String t) {
        if (t == null || t.length() != 8 || !t.chars().allMatch(Character::isDigit)) return null;
        try {
            return LocalDate.parse(t, TARIH);
        } catch (Exception e) {
            return null;
        }
    }

    /** {@code yyyyMMdd} + saat ({@code HHmmss}, 5 haneli "63744" = 06:37:44, ya da "HH:mm:ss"). */
    static LocalDateTime zaman(String tarih, String saat) {
        LocalDate g = tarih(tarih);
        if (g == null) return null;
        if (saat == null || saat.isBlank()) return g.atStartOfDay();
        try {
            if (saat.contains(":")) return g.atTime(LocalTime.parse(saat.length() == 5 ? saat + ":00" : saat));
            String s = String.format("%6s", saat.trim()).replace(' ', '0');
            return g.atTime(Integer.parseInt(s.substring(0, 2)), Integer.parseInt(s.substring(2, 4)), Integer.parseInt(s.substring(4, 6)));
        } catch (Exception e) {
            return g.atStartOfDay();
        }
    }

    static Optional<XmlDugum> sonucBlogu(XmlDugum sonuc, String ad) {
        return sonuc.bul(ad);
    }
}
