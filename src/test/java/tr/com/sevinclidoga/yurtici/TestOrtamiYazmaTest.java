package tr.com.sevinclidoga.yurtici;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Yurtiçi TEST ORTAMINDA yazma akışları (herkese açık test kullanıcısı YKTEST). Gerçek gönderi oluşmaz.
 * Varsayılan derlemede çalışmaz: {@code ./mvnw test -Pcanli-deneme}.
 */
@Tag("canli")
class TestOrtamiYazmaTest {

    final YurticiKargo yk = new YurticiKargo(YurticiAyarlari.test(), "1010954");

    @Test
    void gonderi_ac_sorgula_iptal() {
        String anahtar = "LIBTEST-" + System.currentTimeMillis();
        var acma = yk.gonderiAc(GonderiTalebi.of(anahtar, "Deneme Alici Test", "Test Mahallesi Deneme Sokak No 1", "Ankara",
                "Çankaya", "5000000000", new BigDecimal("2"), new BigDecimal("3"), 1));
        assertTrue(acma.basarili(), acma.hataMesaji());

        var ikinci = yk.gonderiAc(GonderiTalebi.of(anahtar, "Deneme Alici Test", "Test Mahallesi Deneme Sokak No 1", "Ankara",
                "Çankaya", "5000000000", null, null, 1));
        assertTrue(ikinci.acikMi(), "aynı anahtar: zaten var ya da başarılı — " + ikinci.hataMesaji());

        GonderiDetayi d = yk.gonderiSorgula(anahtar);
        assertTrue(d.bulundu());
        assertEquals(GonderiDurumu.ISLEM_GORMEMIS, d.durum());

        var iptal = yk.gonderiIptal(anahtar);
        assertTrue(iptal.get(0).basarili(), iptal.get(0).hataMesaji());
        assertEquals(GonderiDurumu.IPTAL, iptal.get(0).durum());
    }

    @Test
    void iadeKodu_tanimla_tekrar_iptal() {
        String kod = String.valueOf(10_000_000 + (System.currentTimeMillis() % 89_999_999));
        yk.iadeKoduTanimla(kod, LocalDate.now(), LocalDate.now().plusDays(7), 1);
        var tekrar = assertThrows(YurticiIslemHatasi.class, () -> yk.iadeKoduTanimla(kod, LocalDate.now(), LocalDate.now(), 1));
        assertTrue(tekrar.yurticiMesaji().contains("daha önceden"));
        yk.iadeKoduIptal(kod);
        assertThrows(YurticiIslemHatasi.class, () -> yk.iadeKoduIptal(kod));
    }
}
