package tr.com.sevinclidoga.yurtici;

/**
 * Yurtiçi KOPS web servislerinin 37 işleminin tamamı (canlı WSDL'den üretildi, 2026-10-28).
 * <p>
 * Kimlik alanlarının adı işleme göre değişir — aynı serviste bile {@code wsLanguage} / {@code userLanguage} /
 * {@code wsUserLanguage} karışıktır; bu tablo doğru adları taşır. {@link #yazmaMi()} gerçek kayıt oluşturan /
 * değiştiren işlemleri işaretler: bunlar canlı ortamda açık izin olmadan çalışmaz ({@link YurticiAyarlari}).
 */
public enum YurticiIslem {

    CANCEL_RETURN_SHIPMENT_CODE(YurticiServis.GONDERI, "cancelReturnShipmentCode", "wsUserName", "wsPassword", "wsLanguage", true),
    CANCEL_SHIPMENT(YurticiServis.GONDERI, "cancelShipment", "wsUserName", "wsPassword", "userLanguage", true),
    CREATE_SHIPMENT(YurticiServis.GONDERI, "createShipment", "wsUserName", "wsPassword", "userLanguage", true),
    CREATE_SHIPMENT_DETAIL(YurticiServis.GONDERI, "createShipmentDetail", "wsUserName", "wsPassword", "userLanguage", true),
    CREATE_SHIPMENT_WITH_DELIVERY(YurticiServis.GONDERI, "createShipmentWithDelivery", "wsUserName", "wsPassword", "wsUserLanguage", true),
    QUERY_SHIPMENT(YurticiServis.GONDERI, "queryShipment", "wsUserName", "wsPassword", "wsLanguage", false),
    QUERY_SHIPMENT_DETAIL(YurticiServis.GONDERI, "queryShipmentDetail", "wsUserName", "wsPassword", "wsLanguage", false),
    SAVE_RETURN_SHIPMENT_CODE(YurticiServis.GONDERI, "saveReturnShipmentCode", "wsUserName", "wsPassword", "wsLanguage", true),
    DO_GET_TRACKING_INFO(YurticiServis.RAPOR, "doGetTrackingInfo", "userName", "password", "language", false),
    DO_GET_TRACKING_INFO_WITH_DATE(YurticiServis.RAPOR, "doGetTrackingInfoWithDate", "userName", "password", "language", false),
    GET_DELIVERY_COLLECTION_INFO_BY_REFERENCE(YurticiServis.RAPOR, "getDeliveryCollectionInfoByReference", "userName", "password", "language", false),
    LIST_INCOMING_WITH_DOC_CARGO_REFERENCE(YurticiServis.RAPOR, "listIncomingWithDocCargoReference", "userName", "password", "language", false),
    LIST_INV_DOC_INTERFACE_WITH_TRN_CENTER(YurticiServis.RAPOR, "listInvDocInterfaceWithTrnCenter", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_BY_CUST_ID_V2(YurticiServis.RAPOR, "listInvDocumentByCustIdV2", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_BY_GRP_SPECIAL_FIELD(YurticiServis.RAPOR, "listInvDocumentByGrpSpecialField", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_BY_REFERENCE_WITH_PAYER(YurticiServis.RAPOR, "listInvDocumentByReferenceWithPayer", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_BY_TRACKING_NUMBER(YurticiServis.RAPOR, "listInvDocumentByTrackingNumber", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_BY_WAYBILL_NO(YurticiServis.RAPOR, "listInvDocumentByWaybillNo", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_INTERFACE_BY_REFERENCE(YurticiServis.RAPOR, "listInvDocumentInterfaceByReference", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_INTERFACE_WITH_FAILED_DELIVERY_V2(YurticiServis.RAPOR, "listInvDocumentInterfaceWithFailedDeliveryV2", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_WITH_CUST_ID(YurticiServis.RAPOR, "listInvDocumentWithCustId", "userName", "password", "language", false),
    LIST_INV_DOCUMENT_WITH_DOC_DATE_INV_CUST_ID(YurticiServis.RAPOR, "listInvDocumentWithDocDateInvCustId", "userName", "password", "language", false),
    LIST_NGI_SHIPPING(YurticiServis.RAPOR, "listNgiShipping", "userName", "password", "language", false),
    LIST_NGI_SHIPPING_WITH_DOC_ID(YurticiServis.RAPOR, "listNgiShippingWithDocId", "userName", "password", "language", false),
    LIST_NGI_SHIPPING_WITH_PAYER(YurticiServis.RAPOR, "listNgiShippingWithPayer", "userName", "password", "language", false),
    LIST_OUTBOUND_WITH_DOC_CARGO_REFERENCE(YurticiServis.RAPOR, "listOutboundWithDocCargoReference", "userName", "password", "language", false),
    CANCEL_NGI_SHIPMENT(YurticiServis.NGI, "cancelNgiShipment", "wsUserName", "wsPassword", "wsUserLanguage", true),
    CANCEL_NGI_SHIPMENT_WITHOUT_RETURN(YurticiServis.NGI, "cancelNgiShipmentWithoutReturn", "wsUserName", "wsPassword", "wsUserLanguage", true),
    CREATE_NGI_SHIPMENT(YurticiServis.NGI, "createNgiShipment", "wsUserName", "wsPassword", "wsUserLanguage", true),
    CREATE_NGI_SHIPMENT_WITH_ADDRESS(YurticiServis.NGI, "createNgiShipmentWithAddress", "wsUserName", "wsPassword", "wsUserLanguage", true),
    CREATE_NGI_SHIPMENT_WITH_DELIVERY(YurticiServis.NGI, "createNgiShipmentWithDelivery", "wsUserName", "wsPassword", "wsUserLanguage", true),
    CREATE_NGI_SHIPMENT_WITH_PAYER(YurticiServis.NGI, "createNgiShipmentWithPayer", "wsUserName", "wsPassword", "wsUserLanguage", true),
    CREATE_NGI_SHIPMENT_WITH_ZPL_KTN_DELIVERY(YurticiServis.NGI, "createNgiShipmentWithZplKtnDelivery", "wsUserName", "wsPassword", "wsUserLanguage", true),
    DO_SAVE_NGI_PAYER_CUSTOMERS(YurticiServis.NGI, "doSaveNgiPayerCustomers", "userName", "password", "userLanguage", true),
    SELECT_SHIPMENT(YurticiServis.NGI, "selectShipment", "wsUserName", "wsPassword", "wsUserLanguage", false),
    UPDATE_CONSIGNEE_ADDRESS(YurticiServis.NGI, "updateConsigneeAddress", "wsUserName", "wsPassword", "wsUserLanguage", true),
    UPDATE_SHIPMENT_DESI_WEIGHT(YurticiServis.NGI, "updateShipmentDesiWeight", "wsUserName", "wsPassword", "wsUserLanguage", true);

    private final YurticiServis servis;
    private final String ad;
    private final String kullaniciAlani;
    private final String sifreAlani;
    private final String dilAlani;
    private final boolean yazma;

    YurticiIslem(YurticiServis servis, String ad, String kullaniciAlani, String sifreAlani, String dilAlani, boolean yazma) {
        this.servis = servis;
        this.ad = ad;
        this.kullaniciAlani = kullaniciAlani;
        this.sifreAlani = sifreAlani;
        this.dilAlani = dilAlani;
        this.yazma = yazma;
    }

    public YurticiServis servis() { return servis; }

    /** SOAP işlem adı (ör. {@code queryShipmentDetail}). */
    public String ad() { return ad; }

    public String kullaniciAlani() { return kullaniciAlani; }

    public String sifreAlani() { return sifreAlani; }

    public String dilAlani() { return dilAlani; }

    /** Gerçek kayıt oluşturur / değiştirir / iptal eder mi. */
    public boolean yazmaMi() { return yazma; }
}
