package tr.com.sevinclidoga.yurtici;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Rapor servisi — 17 çalışan işlemin hepsi, anonim gerçek cevaplarla (canlı, 2026-10-28). */
class RaporTest {

    static final LocalDate BAS = LocalDate.of(2026, 10, 1), SON = LocalDate.of(2026, 10, 27);

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

    record Durum(String islem, String ornek, Function<YurticiKargo, List<KargoKaydi>> cagri, int enAz) {
        @Override
        public String toString() {
            return islem;
        }
    }

    static Stream<Durum> hepsi() {
        return Stream.of(
                new Durum("listInvDocumentByCustIdV2", "rapor-donem", y -> y.donemGonderileri(BAS, SON, YurticiKargo.Yon.GONDEREN, false), 22),
                new Durum("listInvDocumentInterfaceWithFailedDeliveryV2", "rapor-teslim-edilemeyen", y -> y.teslimEdilemeyenler(BAS, SON, 2022), 8),
                new Durum("doGetTrackingInfo", "rapor-takip", y -> y.takipBilgisi(true, "1"), 1),
                new Durum("doGetTrackingInfoWithDate", "rapor-takip-gune-gore", y -> y.gunlukGonderiler(LocalDate.of(2026, 10, 22)), 22),
                new Durum("listInvDocumentByTrackingNumber", "rapor-takip-no", y -> y.takipNoIleBelgeler(true, "1"), 1),
                new Durum("listInvDocumentByGrpSpecialField", "rapor-ozel-alan", y -> y.ozelAlanIleBelgeler("53", List.of("SD-1"), BAS, SON, false), 1),
                new Durum("listInvDocumentInterfaceByReference", "rapor-referans", y -> y.referansIleBelgeler("53", List.of("SD-1"), BAS, SON, false), 1),
                new Durum("listInvDocumentByReferenceWithPayer", "rapor-odeyen-referans", y -> y.odeyenReferansIleBelgeler("53", List.of("SD-1"), false), 1),
                new Durum("listInvDocInterfaceWithTrnCenter", "rapor-transfer-merkezli", y -> y.transferMerkezliBelgeler("1"), 1),
                new Durum("listInvDocumentWithCustId", "rapor-musteri", y -> y.musteriBelgeleri(BAS, SON, 2022), 22),
                new Durum("listInvDocumentWithDocDateInvCustId", "rapor-belge-tarihi", y -> y.belgeTarihineGoreBelgeler(BAS, SON, 2022), 22),
                new Durum("listIncomingWithDocCargoReference", "rapor-gelen-koli", y -> y.gelenKoliIleBelgeler("1", 9169, BAS, SON), 1),
                new Durum("listOutboundWithDocCargoReference", "rapor-giden-koli", y -> y.gidenKoliIleBelgeler("1", 2022, BAS, SON), 1),
                new Durum("listNgiShipping", "rapor-ngi", y -> y.ngiListesi("53", List.of("SD-1"), BAS, SON, true), 1),
                new Durum("listNgiShippingWithDocId", "rapor-ngi-takip-no", y -> y.ngiTakipNoIle(false, "1"), 1),
                new Durum("listNgiShippingWithPayer", "rapor-ngi-odeyen", y -> y.ngiOdeyenReferansIle("53", List.of("SD-1"), false), 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hepsi")
    void herRapor_kayitlariOrtakModeleCevirir(Durum d) {
        sahte.cevap(d.islem(), d.ornek());
        List<KargoKaydi> l = d.cagri().apply(yk);

        assertTrue(sahte.sonYol.endsWith("/WsReportWithReferenceServices"));
        assertTrue(sahte.sonIstek.contains("<userName>test-kullanici</userName>"), "rapor servisi kimlik alanı userName");
        assertTrue(l.size() >= d.enAz(), d.islem() + " kayıt sayısı " + l.size());
        for (KargoKaydi k : l) {
            assertNotNull(k.takipNo(), "takip no");
            assertNotNull(k.ozelAlanlar());
            assertNotNull(k.koliler());
            assertNotNull(k.hareketler());
        }
    }

    @Test
    void donem_ucretKgVeOzelAlanlar_istekGonderenKoduyla() {
        sahte.cevap("listInvDocumentByCustIdV2", "rapor-donem");
        List<KargoKaydi> l = yk.donemGonderileri(BAS, SON, YurticiKargo.Yon.GONDEREN, false);

        assertEquals(22, l.size());
        assertTrue(sahte.sonIstek.contains("<senderCustIdArray>100000001</senderCustIdArray>"));
        assertTrue(sahte.sonIstek.contains("<withCargoLifecycle>0</withCargoLifecycle>"));
        assertTrue(l.stream().allMatch(k -> k.ucretKdvli() != null && k.ucretKdvli().signum() > 0), "her gönderinin KDV'li ücreti var");
        assertTrue(l.stream().allMatch(k -> k.ozelAlan("Sipariş No") != null && k.ozelAlan("3") != null));
        assertTrue(l.stream().anyMatch(k -> k.kg().signum() == 0 && k.desi().signum() > 0), "kg=0 desi dolu gönderi var → desiKg kullan");
        assertTrue(l.stream().allMatch(k -> k.desiKg() != null));
        assertEquals("DİKMEN", l.get(0).cikisBirimi());
    }

    @Test
    void donem_yonParametresi() {
        sahte.cevap("listInvDocumentByCustIdV2", "rapor-donem");
        yk.donemGonderileri(BAS, SON, YurticiKargo.Yon.ODEYEN, true);
        assertTrue(sahte.sonIstek.contains("<invCustIdArray>100000001</invCustIdArray>"));
        assertTrue(sahte.sonIstek.contains("<withCargoLifecycle>1</withCargoLifecycle>"));
        yk.donemGonderileri(BAS, SON, YurticiKargo.Yon.ALICI, false);
        assertTrue(sahte.sonIstek.contains("<receiverCustIdArray>100000001</receiverCustIdArray>"));
    }

    @Test
    void geriDonusler_durumNedenCozulur() {
        sahte.cevap("listInvDocumentByCustIdV2", "rapor-donem-geri-donuslu");
        List<KargoKaydi> donen = yk.donemGonderileri(BAS, SON, YurticiKargo.Yon.GONDEREN, false).stream()
                .filter(k -> k.geriDonus() != null && k.geriDonus().durum() != null).toList();

        assertEquals(3, donen.size());
        assertTrue(donen.stream().anyMatch(k -> "Teslim İptal".equals(k.geriDonus().durum())));
        assertTrue(donen.stream().anyMatch(k -> "İade Onaylanmadı".equals(k.geriDonus().durum())));
        assertTrue(donen.stream().anyMatch(k -> "Borçlandırma İptal".equals(k.geriDonus().durum())));
        assertTrue(donen.stream().allMatch(k -> k.geriDonus().neden() != null));
    }

    @Test
    void teslimEdilemeyenler_nedenler_vePArametreler() {
        sahte.cevap("listInvDocumentInterfaceWithFailedDeliveryV2", "rapor-teslim-edilemeyen");
        List<KargoKaydi> l = yk.teslimEdilemeyenler(BAS, SON, 2022);

        assertEquals(8, l.size());
        assertTrue(sahte.sonIstek.contains("<departureUnitId>2022</departureUnitId>"));
        assertTrue(sahte.sonIstek.contains("<fieldName>3</fieldName>"));
        assertTrue(l.stream().allMatch(KargoKaydi::sorunluMu), "hepsinin nedeni normal dışı");
        assertTrue(l.stream().anyMatch(k -> "Müşteri Şubeden Alacak".equals(k.nedenAciklama())));
        assertTrue(l.stream().anyMatch(k -> k.suankiBirim() != null));
    }

    @Test
    void takipBilgisi_desiPriceVeParcelLifeCycleBloklari() {
        sahte.cevap("doGetTrackingInfo", "rapor-takip");
        KargoKaydi k = yk.takipBilgisi(true, "1").get(0);

        assertEquals(SahteYurtici.ornekSayi("rapor-takip", "totalPriceWithVat"), k.ucretKdvli(), "desiPrice.totalPriceWithVat");
        assertEquals(0, new BigDecimal("25").compareTo(k.kg()), "desiPrice.totalWeight");
        assertEquals(8, k.hareketler().size(), "parcelLifeCycleArray");
        assertNotNull(k.cikisBirimi(), "routing.departureUnitName");
        assertNotNull(k.olayKodu(), "currentEventInfo");
        assertNotNull(k.teslimZamani(), "shipmentDeliveryDate/Time");
        assertTrue(sahte.sonIstek.contains("<docIdArray>1</docIdArray>"));
    }

    @Test
    void takipNoIleBelge_gonderenAdresKoduVeHareketler() {
        sahte.cevap("listInvDocumentByTrackingNumber", "rapor-takip-no");
        KargoKaydi k = yk.takipNoIleBelgeler(true, "1").get(0);

        assertNotNull(k.gonderenAdresKodu());
        assertEquals(8, k.hareketler().size(), "docCargoLifeCycleVOArray");
        assertEquals(SahteYurtici.ornekSayi("rapor-takip-no", "totalAmount"), k.ucretKdvli());
        assertTrue(sahte.sonIstek.contains("<trackingParamsVO><docIdArray>1</docIdArray><withCargoLifecycle>1</withCargoLifecycle></trackingParamsVO>"));
    }

    @Test
    void ngi_kisaltmaliAdlarCozulur() {
        sahte.cevap("listNgiShipping", "rapor-ngi");
        KargoKaydi k = yk.ngiListesi("53", List.of("SD-1"), BAS, SON, true).get(0);

        assertEquals("DİKMEN", k.cikisBirimi(), "depUnitName");
        assertEquals(SahteYurtici.ornekSayi("rapor-ngi", "totalPriceWithVat"), k.ucretKdvli(), "totalPriceWithVat");
        assertNotNull(k.teslimZamani());
        assertFalse(k.hareketler().isEmpty());
    }

    @Test
    void cokluDeger_tekrarEdenOgeOlarakGider() {
        sahte.cevap("listInvDocumentByGrpSpecialField", "rapor-ozel-alan");
        yk.ozelAlanIleBelgeler("53", List.of("SD-1", "SD-2"), BAS, SON, false);
        assertTrue(sahte.sonIstek.contains("<fieldValueArray>SD-1</fieldValueArray><fieldValueArray>SD-2</fieldValueArray>"));
    }

    @Test
    void tahsilatliTeslim_bosListe() {
        sahte.cevap("getDeliveryCollectionInfoByReference", "rapor-tahsilatli-teslim");
        assertTrue(yk.tahsilatliTeslimler(BAS, SON).isEmpty());
    }

    @Test
    void yurticiRaporuReddederse_mesajliHata() {
        sahte.hamCevap("listInvDocumentWithCustId", 200, "<env:Envelope xmlns:env='http://schemas.xmlsoap.org/soap/envelope/'><env:Body>"
                + "<ns1:listInvDocumentWithCustIdResponse xmlns:ns1='x'><ShippingResponseVO><outFlag>2</outFlag>"
                + "<outResult>Beklenmeyen bir hata oluştu !</outResult></ShippingResponseVO></ns1:listInvDocumentWithCustIdResponse>"
                + "</env:Body></env:Envelope>");
        var h = assertThrows(YurticiIslemHatasi.class, () -> yk.musteriBelgeleri(BAS, SON, 2022));
        assertTrue(h.getMessage().contains("Beklenmeyen"));
    }
}
