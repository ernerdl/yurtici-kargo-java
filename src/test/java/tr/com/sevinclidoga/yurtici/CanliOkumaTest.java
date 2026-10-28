package tr.com.sevinclidoga.yurtici;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CANLI Yurtiçi'ye karşı SALT OKUMA denemeleri — yazma işlemi yoktur. Varsayılan derlemede çalışmaz:
 * <pre>
 * YURTICI_KULLANICI=… YURTICI_SIFRE=… YURTICI_MUSTERI_KODU=… ./mvnw test -Pcanli-deneme
 * </pre>
 * Kimlik yoksa testler atlanır. Gönderi ve şube bilgileri hesabın kendi son gönderilerinden bulunur; kodda müşteri
 * verisi tutulmaz.
 */
@Tag("canli")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanliOkumaTest {

    YurticiKargo yk;
    List<KargoKaydi> sonGonderiler;
    KargoKaydi teslimEdilmis;
    int cikisBirimi;
    final LocalDate bugun = LocalDate.now(), ayBasi = LocalDate.now().minusDays(30);

    @BeforeAll
    void kur() {
        String k = System.getenv("YURTICI_KULLANICI"), s = System.getenv("YURTICI_SIFRE"), m = System.getenv("YURTICI_MUSTERI_KODU");
        assumeTrue(k != null && s != null && m != null, "YURTICI_KULLANICI / YURTICI_SIFRE / YURTICI_MUSTERI_KODU yok → atlandı");
        yk = new YurticiKargo(new YurticiAyarlari(YurticiAyarlari.CANLI_ADRES, k, s, "TR", Duration.ofSeconds(120), false), m);
        sonGonderiler = yk.donemGonderileri(ayBasi, bugun, YurticiKargo.Yon.GONDEREN, false);
        assumeTrue(!sonGonderiler.isEmpty(), "son 30 günde gönderi yok → atlandı");
        teslimEdilmis = sonGonderiler.stream().filter(KargoKaydi::teslimEdildiMi).findFirst().orElse(sonGonderiler.get(0));
        // Çıkış şubesi kimliği: hareketli belgenin ilk olayının (ilk yükleme) şubesi
        cikisBirimi = yk.takipNoIleBelgeler(true, teslimEdilmis.takipNo()).get(0).hareketler().stream()
                .map(KargoKaydi.Hareket::birimId).filter(java.util.Objects::nonNull).findFirst().orElse(0);
    }

    @Test
    void donemGonderileri_herBirindeUcretVar() {
        assertTrue(sonGonderiler.stream().allMatch(g -> g.takipNo() != null));
        assertTrue(sonGonderiler.stream().filter(KargoKaydi::teslimEdildiMi).allMatch(g -> g.ucretKdvli() != null));
    }

    @Test
    void gonderiSorgula_anahtarla() {
        String anahtar = teslimEdilmis.ozelAlan("Anahtar Alan");
        assumeTrue(anahtar != null, "gönderide Anahtar Alan yok");
        GonderiDetayi d = yk.gonderiSorgula(anahtar);
        assertTrue(d.bulundu(), d.hataMesaji());
        assertNotNull(d.durum());
        if (d.kayit() != null) assertEquals(teslimEdilmis.takipNo(), d.takipNo());
    }

    @Test
    void gonderiSorgula_olmayanAnahtar_bulunamadi() {
        GonderiDetayi d = yk.gonderiSorgula("YOK-" + System.nanoTime());
        assertFalse(d.bulundu());
        assertEquals("82519", d.hataKodu());
    }

    @Test
    void takip_ve_takipNoIleBelge() {
        var t = yk.takipBilgisi(true, teslimEdilmis.takipNo());
        assertEquals(1, t.size());
        assertNotNull(t.get(0).ucretKdvli());
        var b = yk.takipNoIleBelgeler(true, teslimEdilmis.takipNo()).get(0);
        assertNotNull(b.gonderenAdresKodu());
        assertFalse(b.hareketler().isEmpty());
    }

    @Test
    void gunlukGonderiler_dun() {
        assertDoesNotThrow(() -> yk.gunlukGonderiler(bugun.minusDays(1)));
    }

    @Test
    void sube_gerektirenRaporlar() {
        assumeTrue(cikisBirimi > 0, "çıkış şubesi bulunamadı");
        assertDoesNotThrow(() -> yk.teslimEdilemeyenler(ayBasi, bugun, cikisBirimi));
        assertDoesNotThrow(() -> yk.musteriBelgeleri(bugun.minusDays(3), bugun, cikisBirimi));
        assertDoesNotThrow(() -> yk.belgeTarihineGoreBelgeler(bugun.minusDays(3), bugun, cikisBirimi));
        String koli = teslimEdilmis.koliler().isEmpty() ? null : teslimEdilmis.koliler().get(0).koliNo();
        if (koli != null) assertFalse(yk.gidenKoliIleBelgeler(koli, cikisBirimi, ayBasi, bugun).isEmpty());
    }

    @Test
    void referans_ve_ozelAlan_ve_ngi() {
        String anahtar = teslimEdilmis.ozelAlan("Anahtar Alan");
        assumeTrue(anahtar != null);
        List<String> d = List.of(anahtar);
        assertFalse(yk.ozelAlanIleBelgeler(YurticiKargo.ALAN_ANAHTAR, d, ayBasi, bugun, false).isEmpty());
        assertFalse(yk.referansIleBelgeler(YurticiKargo.ALAN_ANAHTAR, d, ayBasi, bugun, false).isEmpty());
        assertFalse(yk.odeyenReferansIleBelgeler(YurticiKargo.ALAN_ANAHTAR, d, false).isEmpty());
        assertFalse(yk.ngiListesi(YurticiKargo.ALAN_ANAHTAR, d, ayBasi, bugun, false).isEmpty());
        assertFalse(yk.ngiOdeyenReferansIle(YurticiKargo.ALAN_ANAHTAR, d, false).isEmpty());
        assertFalse(yk.ngiTakipNoIle(false, teslimEdilmis.takipNo()).isEmpty());
        assertFalse(yk.transferMerkezliBelgeler(teslimEdilmis.takipNo()).isEmpty());
    }

    @Test
    void tahsilatliTeslimler_calisir() {
        assertDoesNotThrow(() -> yk.tahsilatliTeslimler(ayBasi, bugun));
    }
}
