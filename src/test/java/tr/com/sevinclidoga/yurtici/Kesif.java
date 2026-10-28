package tr.com.sevinclidoga.yurtici;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Keşif aracı (yalnız geliştirme): bir işlemi çağırır, cevap ağacını KİŞİSEL VERİ MASKELİ basar.
 * <pre>
 *   java ... Kesif test|canli ISLEM_ADI alan=deger alan.ic=deger alan=a|b ...
 * </pre>
 * {@code a.b=x} iç içe öğe, {@code a=x|y} tekrar eden öğe. Canlı kimlik: YURTICI_KULLANICI / YURTICI_SIFRE ortam
 * değişkenleri. Canlıda yazma işlemi reddedilir (istemci kuralı).
 */
public final class Kesif {

    /** Ad, adres, telefon, e-posta, vergi/kimlik no ve teslim alan kişi gibi alanlar maskelenir. */
    static final Pattern GIZLI = Pattern.compile("(?i).*(custname|name$|address|addr|phone|gsm|mail|tax|tckn|identity"
            + "|receiverinfo|delemp|personGiver|receiverName|receiverCust|senderCust|consignee|description).*");
    static final List<String> ACIK = List.of("unitName", "eventName", "cityName", "townName", "arrivalUnitName",
            "departureUnitName", "deliveryUnitName", "arrivalTrCenterName", "departureTrCenterName", "reasonName",
            "fieldName", "productName", "cargoReasonName", "eventUnitName", "cargoEventName", "invCustName");

    public static void main(String[] a) {
        YurticiAyarlari ayar = a[0].equals("canli")
                ? YurticiAyarlari.canli(System.getenv("YURTICI_KULLANICI"), System.getenv("YURTICI_SIFRE"))
                : YurticiAyarlari.test();
        YurticiIslem islem = YurticiIslem.valueOf(a[1]);
        Map<String, Object> p = new LinkedHashMap<>();
        for (int i = 2; i < a.length; i++) {
            int e = a[i].indexOf('=');
            koy(p, a[i].substring(0, e), a[i].substring(e + 1));
        }
        long t = System.currentTimeMillis();
        try {
            XmlDugum d = new YurticiIstemci(ayar).cagir(islem, p);
            System.out.println("== " + islem.ad() + " (" + (System.currentTimeMillis() - t) + " ms)");
            System.out.print(maskele(d.dok()));
        } catch (YurticiHatasi h) {
            System.out.println("== " + islem.ad() + " HATA (ag=" + h.agHatasiMi() + "): " + h.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    static void koy(Map<String, Object> p, String yol, String deger) {
        int nokta = yol.indexOf('.');
        if (nokta < 0) {
            p.put(yol, deger.contains("|") ? List.of(deger.split("\\|")) : deger);
            return;
        }
        Map<String, Object> ic = (Map<String, Object>) p.computeIfAbsent(yol.substring(0, nokta), k -> new LinkedHashMap<>());
        koy(ic, yol.substring(nokta + 1), deger);
    }

    static String maskele(String dok) {
        StringBuilder sb = new StringBuilder();
        for (String satir : dok.split("\n")) {
            String s = satir.strip();
            int esit = s.indexOf(" = ");
            if (esit > 0) {
                String ad = s.substring(0, esit);
                if (GIZLI.matcher(ad).matches() && !ACIK.contains(ad)) {
                    satir = satir.substring(0, satir.indexOf(" = ")) + " = ***";
                }
            }
            sb.append(satir).append('\n');
        }
        return sb.toString();
    }
}
