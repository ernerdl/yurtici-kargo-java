package tr.com.sevinclidoga.yurtici;

import java.time.Duration;
import java.util.Objects;

/**
 * Bağlantı ayarları.
 *
 * @param tabanAdres    servislerin kök adresi, sonu "/" ile (bkz. {@link #CANLI_ADRES}, {@link #TEST_ADRES})
 * @param kullanici     Yurtiçi web servis kullanıcı adı
 * @param sifre         Yurtiçi web servis şifresi
 * @param dil           "TR" ya da "EN"
 * @param zamanAsimi    tek istek için üst süre
 * @param canliYazmaIzni canlı adreste yazma işlemlerine (gönderi açma, iptal, adres değiştirme…) izin.
 *                      Varsayılan kapalı: deneme yaparken yanlışlıkla gerçek kayıt açılmasın.
 */
public record YurticiAyarlari(String tabanAdres, String kullanici, String sifre, String dil, Duration zamanAsimi,
                              boolean canliYazmaIzni) {

    public static final String CANLI_ADRES = "https://webservices.yurticikargo.com/KOPSWebServices/";
    public static final String TEST_ADRES = "https://testwebservices.yurticikargo.com/KOPSWebServices/";

    /** Yurtiçi entegrasyon belgesinde herkese açık yayımlanan test kullanıcısı (yalnız test ortamı). */
    public static final String TEST_KULLANICI = "YKTEST";
    public static final String TEST_SIFRE = "YK";

    public YurticiAyarlari {
        Objects.requireNonNull(tabanAdres, "tabanAdres");
        Objects.requireNonNull(kullanici, "kullanici");
        Objects.requireNonNull(sifre, "sifre");
        if (!tabanAdres.endsWith("/")) tabanAdres = tabanAdres + "/";
        if (dil == null || dil.isBlank()) dil = "TR";
        if (zamanAsimi == null) zamanAsimi = Duration.ofSeconds(30);
    }

    /** Canlı ortam, yazma izni KAPALI. */
    public static YurticiAyarlari canli(String kullanici, String sifre) {
        return new YurticiAyarlari(CANLI_ADRES, kullanici, sifre, "TR", null, false);
    }

    /** Yurtiçi test ortamı, herkese açık test kullanıcısıyla. */
    public static YurticiAyarlari test() {
        return new YurticiAyarlari(TEST_ADRES, TEST_KULLANICI, TEST_SIFRE, "TR", null, false);
    }

    /** Aynı ayarlar, canlı yazma izni açık. Bilerek çağrılmalı. */
    public YurticiAyarlari canliYazmayaIzinVer() {
        return new YurticiAyarlari(tabanAdres, kullanici, sifre, dil, zamanAsimi, true);
    }

    public boolean canliMi() {
        return tabanAdres.startsWith(CANLI_ADRES);
    }

    /** Şifre loglara/istisnalara düşmesin. */
    @Override
    public String toString() {
        return "YurticiAyarlari[" + tabanAdres + ", kullanici=" + kullanici + ", canliYazmaIzni=" + canliYazmaIzni + "]";
    }
}
