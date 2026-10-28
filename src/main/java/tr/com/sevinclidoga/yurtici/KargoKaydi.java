package tr.com.sevinclidoga.yurtici;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Yurtiçi'deki tek bir gönderi (belge). Gönderi servisinin ayrıntı sorgusundan da, rapor servisinin 18 işleminden
 * hangisinden gelirse gelsin aynı alanlarla dolar; cevapta olmayan alan {@code null} kalır.
 * <p>
 * Tarih/saatler Yurtiçi'nin yerel saatidir (Türkiye). Ücretler TL.
 *
 * @param takipNo            Yurtiçi takip numarası ({@code docId}, 12 hane)
 * @param belgeNo            irsaliye/belge numarası ({@code docNumber})
 * @param irsaliyeNo         {@code waybillNo}
 * @param faturaNo           {@code invoiceNumber}
 * @param belgeZamani        gönderinin Yurtiçi'de belgelendiği an
 * @param gonderenKodu       gönderen müşteri kodu
 * @param odeyenKodu         ödeyen (sözleşmeli) müşteri kodu
 * @param aliciKodu          alıcı müşteri kodu
 * @param gonderenAdresKodu  gönderenin Yurtiçi adres kodu ({@code senderAddressId})
 * @param aliciAdi           alıcı adı (kişisel veri)
 * @param aliciAdresi        alıcı adresi (kişisel veri)
 * @param aliciIl            alıcı ili
 * @param aliciIlce          alıcı ilçesi
 * @param cikisBirimi        çıkış şubesi
 * @param varisBirimi        varış şubesi
 * @param teslimBirimi       teslim eden şube
 * @param suankiBirim        son olayın gerçekleştiği şube
 * @param olayKodu           son olay ({@code cargoEventId}: YK yüklendi, IN indirildi, OK teslim, TX teslim edilmedi …)
 * @param olayAciklama       son olayın açıklaması
 * @param nedenKodu          son olayın nedeni ({@code cargoReasonId}: OK sorun yok, GOK dağıtıma çıktı, MSA müşteri şubeden alacak …)
 * @param nedenAciklama      nedenin açıklaması
 * @param tahminiTeslim      Yurtiçi'nin tahmini teslim günü
 * @param teslimZamani       teslim anı (teslim edilmediyse null)
 * @param ucret              KDV hariç ücret
 * @param kdv                KDV
 * @param ucretKdvli         KDV dahil ücret
 * @param kg                 Yurtiçi'nin ölçtüğü ağırlık
 * @param desi               Yurtiçi'nin ölçtüğü desi
 * @param desiKg             ücretlendirmeye esas desi/kg (ücret buna göre; kg=0 olup desi dolu olabilir)
 * @param koliSayisi         koli sayısı
 * @param geriDonus          teslim edilemeyip geri dönüş bilgisi (yoksa null)
 * @param ozelAlanlar        gönderiye bağlı özel alanlar: açıklama → değer (ör. "Sipariş No", "Anahtar Alan")
 * @param koliler            koli ayrıntısı
 * @param hareketler         şube şube olaylar (rapor isteğinde "hareketlerle" seçildiyse)
 * @param takipUrl           Yurtiçi'nin herkese açık takip sayfası
 */
public record KargoKaydi(
        String takipNo, String belgeNo, String irsaliyeNo, String faturaNo, LocalDateTime belgeZamani,
        String gonderenKodu, String odeyenKodu, String aliciKodu, String gonderenAdresKodu,
        String aliciAdi, String aliciAdresi, String aliciIl, String aliciIlce,
        String cikisBirimi, String varisBirimi, String teslimBirimi, String suankiBirim,
        String olayKodu, String olayAciklama, String nedenKodu, String nedenAciklama,
        LocalDate tahminiTeslim, LocalDateTime teslimZamani,
        BigDecimal ucret, BigDecimal kdv, BigDecimal ucretKdvli,
        BigDecimal kg, BigDecimal desi, BigDecimal desiKg, Integer koliSayisi,
        GeriDonus geriDonus,
        Map<String, String> ozelAlanlar, List<Koli> koliler, List<Hareket> hareketler, String takipUrl) {

    /** Normal kabul edilen neden kodları: OK (sorun yok), GOK (kuryede / dağıtıma çıktı). */
    public static final List<String> NORMAL_NEDENLER = List.of("OK", "GOK");

    /** Son olayın nedeni normal dışı mı (teslim edilemedi, müşteri şubeden alacak, adres sorunu …). */
    public boolean sorunluMu() {
        return nedenKodu != null && !NORMAL_NEDENLER.contains(nedenKodu);
    }

    public boolean teslimEdildiMi() {
        return teslimZamani != null || "OK".equals(olayKodu);
    }

    /** Özel alan değeri; açıklamaya göre ("Sipariş No") ya da numaraya göre ("3"). */
    public String ozelAlan(String aciklamaYaDaNo) {
        return ozelAlanlar.get(aciklamaYaDaNo);
    }

    /** Şube şube tek olay. */
    public record Hareket(LocalDateTime zaman, String olayKodu, String olayAdi, String nedenKodu, String nedenAdi,
                          Integer birimId, String birimAdi, String konum) {
    }

    /** Tek koli. */
    public record Koli(String koliNo, BigDecimal desi, BigDecimal kg, BigDecimal desiKg) {
    }

    /**
     * Teslim edilemeyip göndericiye geri dönüş.
     *
     * @param durumKodu  {@code rejectStatus} (7 teslim iptal, 10 iade sonlandırıldı, 11 iade onaylanmadı …)
     * @param durum      {@code rejectStatusExplanation}
     * @param neden      {@code rejectReasonExplanation} (çoğu "Müşteri İsteği")
     * @param subeNotu  {@code rejectDescription}: şubenin serbest notu (kısaltmalı)
     * @param iadeTakipNo geri dönen gönderinin takip numarası ({@code returnDocId})
     * @param iadeDurumu {@code returnStatusExplanation}
     * @param iadeUcretKdvli geri dönüş ücreti (KDV dahil)
     */
    public record GeriDonus(String durumKodu, String durum, String neden, String subeNotu, String iadeTakipNo,
                            String iadeDurumu, BigDecimal iadeUcretKdvli) {
    }
}
