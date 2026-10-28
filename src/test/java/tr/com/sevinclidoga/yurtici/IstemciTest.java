package tr.com.sevinclidoga.yurtici;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Alt seviye istemci: güvenlik kilidi, SOAP fault, ağ hatası, XML güvenliği, kimlik alan adları, zaman çözümü. */
class IstemciTest {

    @Test
    void canlidaYazma_izinsizReddedilir_istekGitmez() {
        var ist = new YurticiIstemci(YurticiAyarlari.canli("k", "s"));
        var h = assertThrows(YurticiHatasi.class, () -> ist.cagir(YurticiIslem.CREATE_SHIPMENT, Map.of()));
        assertTrue(h.getMessage().contains("canlı ortamda yazma"));
        assertFalse(h.agHatasiMi());
        // okuma işlemi kilide takılmaz (burada ağa gitmeye çalışır; yalnız kilidin olmadığını görmek için ayar kontrolü)
        assertFalse(YurticiIslem.QUERY_SHIPMENT_DETAIL.yazmaMi());
        assertTrue(YurticiIslem.SAVE_RETURN_SHIPMENT_CODE.yazmaMi());
    }

    @Test
    void islemTablosu_37_islem_16_yazma_ve_kimlikAlanlari() {
        assertEquals(37, YurticiIslem.values().length);
        assertEquals(16, java.util.Arrays.stream(YurticiIslem.values()).filter(YurticiIslem::yazmaMi).count());
        assertEquals("userLanguage", YurticiIslem.CREATE_SHIPMENT.dilAlani());
        assertEquals("wsLanguage", YurticiIslem.QUERY_SHIPMENT_DETAIL.dilAlani());
        assertEquals("wsUserLanguage", YurticiIslem.CREATE_SHIPMENT_WITH_DELIVERY.dilAlani());
        assertEquals("userName", YurticiIslem.DO_GET_TRACKING_INFO.kullaniciAlani());
        assertEquals("userName", YurticiIslem.DO_SAVE_NGI_PAYER_CUSTOMERS.kullaniciAlani());
        assertEquals("userLanguage", YurticiIslem.DO_SAVE_NGI_PAYER_CUSTOMERS.dilAlani());
    }

    @Test
    void soapFault_istisnaOlur() throws Exception {
        try (var sahte = new SahteYurtici()) {
            sahte.hamCevap("queryShipment", 500, "<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/'><env:Body>"
                    + "<env:Fault><faultcode>env:Server</faultcode><faultstring>Kimlik doğrulanamadı</faultstring></env:Fault>"
                    + "</env:Body></env:Envelope>");
            var ist = new YurticiIstemci(sahte.ayarlar());
            var h = assertThrows(YurticiHatasi.class, () -> ist.cagir(YurticiIslem.QUERY_SHIPMENT, Map.of("keys", "x")));
            assertTrue(h.getMessage().contains("Kimlik doğrulanamadı"));
            assertFalse(h.agHatasiMi());
        }
    }

    @Test
    void sunucuHatasi_agHatasiSayilir() throws Exception {
        try (var sahte = new SahteYurtici()) {
            sahte.hamCevap("queryShipment", 503, "bakımda");
            var ist = new YurticiIstemci(sahte.ayarlar());
            var h = assertThrows(YurticiHatasi.class, () -> ist.cagir(YurticiIslem.QUERY_SHIPMENT, Map.of()));
            assertTrue(h.agHatasiMi(), "tekrar denenebilir");
        }
    }

    @Test
    void baglantiYok_agHatasi() {
        var ayar = new YurticiAyarlari("http://127.0.0.1:1/", "k", "s", "TR", Duration.ofSeconds(2), false);
        var h = assertThrows(YurticiHatasi.class, () -> new YurticiIstemci(ayar).cagir(YurticiIslem.QUERY_SHIPMENT, Map.of()));
        assertTrue(h.agHatasiMi());
    }

    @Test
    void xml_doctypeReddedilir_bozukCevapIstisna() {
        assertThrows(YurticiHatasi.class, () -> XmlDugum.coz("<?xml version='1.0'?><!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>"));
        assertThrows(YurticiHatasi.class, () -> XmlDugum.coz("<<bozuk"));
    }

    @Test
    void istekteOzelKarakterlerKacisli_sifreLogaDusmez() throws Exception {
        try (var sahte = new SahteYurtici()) {
            sahte.cevap("queryShipmentDetail", "gonderi-detay-bulunamadi");
            new YurticiKargo(sahte.ayarlar(), "1").gonderiSorgula("A&B<C>");
            assertTrue(sahte.sonIstek.contains("<keys>A&amp;B&lt;C&gt;</keys>"));
            assertFalse(sahte.ayarlar().toString().contains("test-sifre"));
        }
    }

    @Test
    void zaman_besHaneliSaat_veIkiNoktali() {
        assertEquals(LocalDateTime.of(2026, 10, 23, 6, 37, 44), KayitCozucu.zaman("20261023", "63744"));
        assertEquals(LocalDateTime.of(2026, 10, 23, 14, 5, 18), KayitCozucu.zaman("20261023", "140518"));
        assertEquals(LocalDateTime.of(2026, 10, 23, 14, 5), KayitCozucu.zaman("20261023", "14:05"));
        assertNull(KayitCozucu.zaman("0", "1"));
        assertNull(KayitCozucu.zaman(null, null));
    }

    @Test
    void durumKodlari() {
        assertEquals(GonderiDurumu.ISLEM_GORMEMIS, GonderiDurumu.koddan("NOP"));
        assertEquals(GonderiDurumu.YOLDA, GonderiDurumu.koddan("IND"));
        assertEquals(GonderiDurumu.TESLIM_EDILDI, GonderiDurumu.koddan("DLV"));
        assertEquals(GonderiDurumu.TESLIM_EDILDI, GonderiDurumu.koddan("OK"));
        assertEquals(GonderiDurumu.IPTAL, GonderiDurumu.koddan("CNL"));
        assertEquals(GonderiDurumu.BILINMIYOR, GonderiDurumu.koddan("XYZ"));
    }
}
