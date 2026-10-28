package tr.com.sevinclidoga.yurtici;

/**
 * İstek Yurtiçi'ye ulaşmadı, zaman aşımına düştü, SOAP fault döndü ya da cevap çözülemedi.
 * Yurtiçi'nin "işlem başarısız" cevapları (outFlag / errCode) istisna DEĞİLDİR; sonuç nesnesinde döner.
 */
public class YurticiHatasi extends RuntimeException {

    private final boolean agHatasi;

    public YurticiHatasi(String mesaj, boolean agHatasi, Throwable neden) {
        super(mesaj, neden);
        this.agHatasi = agHatasi;
    }

    /** Ağ / zaman aşımı: aynı istek sonra tekrar denenebilir. */
    public boolean agHatasiMi() {
        return agHatasi;
    }
}
