package tr.com.sevinclidoga.yurtici;

/** Gönderi servisi işlemlerinin sonuç tipleri. */
public final class GonderiSonuclari {

    private GonderiSonuclari() {}

    /**
     * Gönderi açma sonucu. Aynı anahtarla ikinci açma Yurtiçi'de hata döner ("sistemde mevcuttur");
     * bu durumda {@link #zatenVarMi()} true olur ve gönderi kullanılabilir.
     */
    public record Acma(boolean basarili, boolean zatenVarMi, String kargoAnahtari, String faturaAnahtari, String isNo,
                       String hataKodu, String hataMesaji) {

        /** Gönderi Yurtiçi'de açık mı (yeni açıldı ya da zaten vardı). */
        public boolean acikMi() {
            return basarili || zatenVarMi;
        }
    }

    /** Tek anahtarın iptal sonucu. */
    public record Iptal(String kargoAnahtari, boolean basarili, GonderiDurumu durum, String durumMesaji, String hataKodu,
                        String hataMesaji) {
    }
}
