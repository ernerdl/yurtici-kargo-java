package tr.com.sevinclidoga.yurtici;

/** Yurtiçi KOPS'taki üç SOAP servisi: adres yolu ve XML ad alanı. */
public enum YurticiServis {

    /** Gönderi oluşturma / iptal / sorgu, iade kodu. */
    GONDERI("ShippingOrderDispatcherServices", "http://yurticikargo.com.tr/ShippingOrderDispatcherServices"),

    /** Raporlar: takip, fatura/ücret, teslim edilemeyenler, tahsilat. */
    RAPOR("WsReportWithReferenceServices", "http://yurticikargo.com.tr/sswIntegrationServices"),

    /** NGI gönderi arayüzü: adres güncelleme, desi/ağırlık düzeltme, ödeyen müşteri. */
    NGI("NgiShipmentInterfaceServices", "http://yurticikargo.com.tr/NgiShipmentInterfaceServices");

    private final String yol;
    private final String adAlani;

    YurticiServis(String yol, String adAlani) {
        this.yol = yol;
        this.adAlani = adAlani;
    }

    public String yol() { return yol; }

    public String adAlani() { return adAlani; }
}
