package tr.com.sevinclidoga.yurtici;

/** Yurtiçi isteği aldı ama işlemi reddetti ({@code outFlag ≠ 0} ya da {@code errCode}). Mesaj Yurtiçi'nin kendi metnidir. */
public class YurticiIslemHatasi extends YurticiHatasi {

    private final String kod;
    private final String yurticiMesaji;

    public YurticiIslemHatasi(String islem, String kod, String yurticiMesaji) {
        super(islem + ": " + yurticiMesaji + (kod != null ? " (kod " + kod + ")" : ""), false, null);
        this.kod = kod;
        this.yurticiMesaji = yurticiMesaji;
    }

    /** Yurtiçi hata kodu (ör. 82519 anahtar bulunamadı, 83647 ZPL yetkisi yok); yoksa null. */
    public String kod() {
        return kod;
    }

    public String yurticiMesaji() {
        return yurticiMesaji;
    }
}
