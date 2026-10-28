package tr.com.sevinclidoga.yurtici;

import java.util.List;

/**
 * Gönderi servisinden ayrıntılı sorgu ({@code queryShipmentDetail}, geçmişli).
 *
 * @param kargoAnahtari sorgulanan anahtar
 * @param bulundu       Yurtiçi bu anahtarı tanıyor mu. DİKKAT: "bulunamadı" Yurtiçi'de "Başarılı" cevapla gelir, asıl hata
 *                      detaydaki errCode 82519'dadır — burada çözülmüş hâli. İptal edilmiş gönderi de çoğu zaman bulunamaz.
 * @param durum         genel durum
 * @param durumKodu     ham kod (NOP, IND, DLV, CNL …)
 * @param durumMesaji   Yurtiçi'nin durum metni ("Kargo teslim edilmiştir.")
 * @param hataKodu      bulunamadıysa Yurtiçi kodu
 * @param hataMesaji    bulunamadıysa Yurtiçi mesajı
 * @param kayit         kurye aldıktan sonra dolar (ücret, kg, şubeler, hareketler); öncesinde null
 */
public record GonderiDetayi(String kargoAnahtari, boolean bulundu, GonderiDurumu durum, String durumKodu, String durumMesaji,
                            String hataKodu, String hataMesaji, KargoKaydi kayit) {

    /** Şube şube hareketler; kurye almadıysa boş. */
    public List<KargoKaydi.Hareket> hareketler() {
        return kayit == null ? List.of() : kayit.hareketler();
    }

    /** Gerçek takip numarası (kurye aldıktan sonra oluşur). */
    public String takipNo() {
        return kayit == null ? null : kayit.takipNo();
    }
}
