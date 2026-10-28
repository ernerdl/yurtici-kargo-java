package tr.com.sevinclidoga.yurtici;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Yeni gönderi ({@code createShipment}).
 *
 * @param kargoAnahtari  sizin ürettiğiniz tekil anahtar ({@code cargoKey}); gönderiyi sonra bununla sorgular/iptal edersiniz
 * @param faturaAnahtari {@code invoiceKey}; boşsa kargo anahtarı kullanılır
 * @param aliciAdi       alıcı adı soyadı / firma
 * @param aliciAdresi    açık adres (il/ilçe ayrıca verildiği için adrese yazmayın)
 * @param il             il adı (ör. "Ankara")
 * @param ilce           ilçe adı (ör. "Çankaya")
 * @param telefon        cep telefonu; +90 / 0 / boşluk temizlenip 10 haneye indirilir
 * @param eposta         isteğe bağlı
 * @param desi           isteğe bağlı (boşsa 1)
 * @param kg             isteğe bağlı (boşsa 1)
 * @param koliSayisi     en az 1
 * @param ozelAlan1      {@code specialField1} (isteğe bağlı)
 * @param aciklama       isteğe bağlı
 */
public record GonderiTalebi(String kargoAnahtari, String faturaAnahtari, String aliciAdi, String aliciAdresi, String il,
                            String ilce, String telefon, String eposta, BigDecimal desi, BigDecimal kg, int koliSayisi,
                            String ozelAlan1, String aciklama) {

    public GonderiTalebi {
        zorunlu(kargoAnahtari, "kargoAnahtari");
        zorunlu(aliciAdi, "aliciAdi");
        zorunlu(aliciAdresi, "aliciAdresi");
        zorunlu(il, "il");
        zorunlu(ilce, "ilce");
        zorunlu(telefon, "telefon");
        telefon = telefonTemizle(telefon);
        if (telefon.length() != 10) {
            throw new IllegalArgumentException("telefon 10 hane olmalı (5xxxxxxxxx), gelen: " + telefon.length() + " hane");
        }
        if (faturaAnahtari == null || faturaAnahtari.isBlank()) faturaAnahtari = kargoAnahtari;
        if (koliSayisi < 1) koliSayisi = 1;
    }

    /** En sık kullanım: zorunlu alanlar + desi/kg/koli. */
    public static GonderiTalebi of(String kargoAnahtari, String aliciAdi, String aliciAdresi, String il, String ilce,
                                   String telefon, BigDecimal desi, BigDecimal kg, int koliSayisi) {
        return new GonderiTalebi(kargoAnahtari, null, aliciAdi, aliciAdresi, il, ilce, telefon, null, desi, kg, koliSayisi,
                null, null);
    }

    /** +90 / 0 / boşluk / parantez temizlenir, 10 haneye indirilir. */
    static String telefonTemizle(String t) {
        String d = t.replaceAll("[^0-9]", "");
        if (d.startsWith("90") && d.length() == 12) d = d.substring(2);
        else if (d.startsWith("0") && d.length() == 11) d = d.substring(1);
        return d;
    }

    private static void zorunlu(String v, String ad) {
        if (Objects.requireNonNull(v, ad).isBlank()) throw new IllegalArgumentException(ad + " boş olamaz");
    }
}
