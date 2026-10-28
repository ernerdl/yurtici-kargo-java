package tr.com.sevinclidoga.yurtici;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Gönderi servisi + iade kodu — sahte sunucu, anonim gerçek cevaplar (canlı/test ortamı, 2026-10-28). */
class GonderiTest {

    SahteYurtici sahte;
    YurticiKargo yk;

    @BeforeEach
    void kur() throws Exception {
        sahte = new SahteYurtici();
        yk = new YurticiKargo(sahte.ayarlar(), "100000001");
    }

    @AfterEach
    void kapat() {
        sahte.close();
    }

    @Test
    void gonderiAc_basarili_istekDogruKurulur() {
        sahte.cevap("createShipment", "gonderi-ac");
        var t = GonderiTalebi.of("SD-SIP-1", "Deneme Alici", "Test Mahallesi Deneme Sokak No 1", "Ankara", "Çankaya",
                "+90 (500) 000 00 00", new BigDecimal("2.50"), new BigDecimal("3"), 1);

        var s = yk.gonderiAc(t);

        assertTrue(s.basarili());
        assertTrue(s.acikMi());
        assertFalse(s.zatenVarMi());
        assertNotNull(s.isNo());
        String istek = sahte.sonIstek;
        assertTrue(sahte.sonYol.endsWith("/ShippingOrderDispatcherServices"));
        assertTrue(istek.contains("<userLanguage>TR</userLanguage>"), "createShipment dil alanı userLanguage");
        assertTrue(istek.contains("<wsUserName>test-kullanici</wsUserName>"));
        assertTrue(istek.contains("<receiverPhone1>5000000000</receiverPhone1>"), "telefon 10 haneye indirilir");
        assertTrue(istek.contains("<desi>2.5</desi>"));
        assertTrue(istek.contains("<cargoKey>SD-SIP-1</cargoKey><invoiceKey>SD-SIP-1</invoiceKey>"), "fatura anahtarı boşsa kargo anahtarı");
        assertFalse(istek.contains("<emailAddress>"), "boş alan gönderilmez");
    }

    @Test
    void gonderiAc_eksikAlan_hataKoduVeMesajDoner() {
        sahte.cevap("createShipment", "gonderi-ac-eksik-alan");
        var s = yk.gonderiAc(GonderiTalebi.of("SD-X", "Deneme Alici", "Test Mahallesi 1", "Ankara", "Çankaya", "5000000000", null, null, 1));

        assertFalse(s.basarili());
        assertFalse(s.acikMi());
        assertNotNull(s.hataKodu());
        assertTrue(s.hataMesaji().contains("INVOICE_KEY"), s.hataMesaji());
    }

    @Test
    void gonderiAc_ayniAnahtarZatenAcik_zatenVarSayilir() {
        sahte.hamCevap("createShipment", 200, SahteYurtici.ornek("gonderi-ac-eksik-alan")
                .replaceAll("<errMessage>[^<]*</errMessage>", "<errMessage>SD-X kargo anahtarı sistemde mevcuttur.</errMessage>"));
        var s = yk.gonderiAc(GonderiTalebi.of("SD-X", "Deneme Alici", "Test Mahallesi 1", "Ankara", "Çankaya", "5000000000", null, null, 1));

        assertFalse(s.basarili());
        assertTrue(s.zatenVarMi());
        assertTrue(s.acikMi());
    }

    @Test
    void gonderiTalebi_telefonDogrulanir() {
        assertThrows(IllegalArgumentException.class, () ->
                GonderiTalebi.of("SD-X", "Deneme", "Adres 1", "Ankara", "Çankaya", "12345", null, null, 1));
        assertThrows(IllegalArgumentException.class, () ->
                GonderiTalebi.of(" ", "Deneme", "Adres 1", "Ankara", "Çankaya", "5000000000", null, null, 1));
        assertEquals("5321234567", GonderiTalebi.of("K", "A", "B", "C", "D", "0532 123 45 67", null, null, 0).telefon());
    }

    @Test
    void gonderiSorgula_teslimEdilmis_ucretKgSubelerHareketler() {
        sahte.cevap("queryShipmentDetail", "gonderi-detay-teslim");
        GonderiDetayi d = yk.gonderiSorgula("SD-B2B-1");

        assertTrue(d.bulundu());
        assertEquals(GonderiDurumu.TESLIM_EDILDI, d.durum());
        assertEquals("DLV", d.durumKodu());
        KargoKaydi k = d.kayit();
        assertNotNull(k);
        assertEquals(SahteYurtici.ornekSayi("gonderi-detay-teslim", "totalAmount"), k.ucretKdvli());
        assertEquals(0, new BigDecimal("25").compareTo(k.kg()));
        assertEquals("DİKMEN", k.cikisBirimi());
        assertEquals("BEYKOZ", k.teslimBirimi());
        assertEquals(LocalDateTime.of(2026, 10, 23, 14, 5, 18), k.teslimZamani());
        assertTrue(k.teslimEdildiMi());
        assertFalse(k.sorunluMu());
        assertEquals(8, d.hareketler().size());
        assertEquals("Kargo Yüklendi", d.hareketler().get(0).olayAdi());
        assertNotNull(d.takipNo());
        assertTrue(sahte.sonIstek.contains("<addHistoricalData>true</addHistoricalData>"));
        assertTrue(sahte.sonIstek.contains("<wsLanguage>TR</wsLanguage>"), "queryShipmentDetail dil alanı wsLanguage");
    }

    @Test
    void gonderiSorgula_dagitimda_GOKsorunSayilmaz() {
        sahte.cevap("queryShipmentDetail", "gonderi-detay-dagitimda");
        GonderiDetayi d = yk.gonderiSorgula("SD-B2B-2");

        assertEquals(GonderiDurumu.YOLDA, d.durum());
        assertEquals("GOK", d.kayit().nedenKodu());
        assertFalse(d.kayit().sorunluMu(), "GOK = kuryede/dağıtıma çıktı, normal");
        assertNull(d.kayit().teslimZamani());
        assertEquals(SahteYurtici.ornekSayi("gonderi-detay-dagitimda", "totalAmount"), d.kayit().ucretKdvli());
    }

    @Test
    void gonderiSorgula_bulunamadi_basariliCevaptakiErrCodeYakalanir() {
        sahte.cevap("queryShipmentDetail", "gonderi-detay-bulunamadi");
        GonderiDetayi d = yk.gonderiSorgula("SD-YOK");

        assertFalse(d.bulundu());
        assertEquals("82519", d.hataKodu());
        assertTrue(d.hataMesaji().contains("bulunmamaktadır"));
        assertFalse(d.hataMesaji().contains("  "), "çift boşluk temizlenir");
        assertNull(d.kayit());
    }

    @Test
    void gonderiSorgula_kuryeAlmamis_kayitYok() {
        sahte.cevap("queryShipmentDetail", "gonderi-detay-islem-gormemis");
        GonderiDetayi d = yk.gonderiSorgula("SD-YENI");

        assertTrue(d.bulundu());
        assertEquals(GonderiDurumu.ISLEM_GORMEMIS, d.durum());
        assertNull(d.kayit());
        assertTrue(d.hareketler().isEmpty());
    }

    @Test
    void gonderiIptal_CNL() {
        sahte.cevap("cancelShipment", "gonderi-iptal");
        var l = yk.gonderiIptal("SD-1");

        assertEquals(1, l.size());
        assertTrue(l.get(0).basarili());
        assertEquals(GonderiDurumu.IPTAL, l.get(0).durum());
        assertTrue(sahte.sonIstek.contains("<userLanguage>TR</userLanguage>"), "cancelShipment dil alanı userLanguage");
    }

    @Test
    void iadeKodu_tanimla_iptal_ve_Yurtici_retleri() {
        sahte.cevap("saveReturnShipmentCode", "iade-kodu-tanimla");
        assertDoesNotThrow(() -> yk.iadeKoduTanimla("12345678", LocalDate.of(2026, 10, 28), LocalDate.of(2026, 11, 10), 1));
        assertTrue(sahte.sonIstek.contains("<fieldName>16</fieldName>"));
        assertTrue(sahte.sonIstek.contains("<startDate>20261028</startDate>"));

        sahte.cevap("saveReturnShipmentCode", "iade-kodu-tekrar");
        var h = assertThrows(YurticiIslemHatasi.class, () -> yk.iadeKoduTanimla("12345678", LocalDate.now(), LocalDate.now(), 1));
        assertTrue(h.yurticiMesaji().contains("daha önceden"));

        sahte.cevap("saveReturnShipmentCode", "iade-kodu-alan-tanimsiz");
        h = assertThrows(YurticiIslemHatasi.class, () -> yk.iadeKoduTanimla("1", LocalDate.now(), LocalDate.now(), 1));
        assertTrue(h.yurticiMesaji().contains("Özel Alan"));

        sahte.cevap("cancelReturnShipmentCode", "iade-kodu-iptal");
        assertDoesNotThrow(() -> yk.iadeKoduIptal("12345678"));
        sahte.cevap("cancelReturnShipmentCode", "iade-kodu-iptal-yok");
        h = assertThrows(YurticiIslemHatasi.class, () -> yk.iadeKoduIptal("12345678"));
        assertTrue(h.yurticiMesaji().contains("bulunmamaktadır"));
    }
}
