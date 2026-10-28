package tr.com.sevinclidoga.yurtici;

/** Gönderinin genel durumu ({@code operationStatus}). Ham kod {@link GonderiDetayi#durumKodu()} içinde de durur. */
public enum GonderiDurumu {

    /** NOP — Yurtiçi'ye bildirildi, kurye henüz almadı. */
    ISLEM_GORMEMIS,

    /** IND — kurye aldı, yolda / teslimatta. */
    YOLDA,

    /** DLV / OK — teslim edildi. */
    TESLIM_EDILDI,

    /** CNL — iptal edildi. Not: iptal edilen gönderi sonradan sorgulanınca çoğu zaman "bulunamadı" döner. */
    IPTAL,

    /** Tanınmayan kod. */
    BILINMIYOR;

    public static GonderiDurumu koddan(String kod) {
        if (kod == null) return BILINMIYOR;
        return switch (kod.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "NOP" -> ISLEM_GORMEMIS;
            case "IND" -> YOLDA;
            case "DLV", "OK", "DELIVERED" -> TESLIM_EDILDI;
            case "CNL" -> IPTAL;
            default -> BILINMIYOR;
        };
    }
}
