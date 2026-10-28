package tr.com.sevinclidoga.yurtici;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Yurtiçi KOPS SOAP istemcisi: 37 işlemin hepsini {@link #cagir(YurticiIslem, Map)} ile çağırır.
 * <p>
 * Kimlik alanlarını (adı işleme göre değişen) kendisi ekler; parametreler sıralı bir Map olarak verilir.
 * Değer String/Number/Boolean ise düz alan, Map ise iç içe öğe, Collection ise aynı adla tekrar eden öğe olur.
 * Cevabın gövdesi (SOAP zarfı ve işlem sarmalayıcısı atılmış) {@link XmlDugum} olarak döner.
 * <p>
 * İş parçacığı güvenli. Yazma işlemleri canlı adreste {@link YurticiAyarlari#canliYazmaIzni()} yoksa reddedilir.
 */
public class YurticiIstemci {

    private final YurticiAyarlari ayarlar;
    private final HttpClient http;

    public YurticiIstemci(YurticiAyarlari ayarlar) {
        this.ayarlar = Objects.requireNonNull(ayarlar, "ayarlar");
        this.http = HttpClient.newBuilder().connectTimeout(ayarlar.zamanAsimi()).build();
    }

    public YurticiAyarlari ayarlar() {
        return ayarlar;
    }

    /**
     * İşlemi çağırır.
     *
     * @return işlem cevabının içindeki sonuç öğesi (ör. {@code ShipmentDeliveryResultVO})
     * @throws YurticiHatasi ağ/zaman aşımı, SOAP fault, çözülemeyen cevap ya da izinsiz canlı yazma
     */
    public XmlDugum cagir(YurticiIslem islem, Map<String, ?> parametreler) {
        if (islem.yazmaMi() && ayarlar.canliMi() && !ayarlar.canliYazmaIzni()) {
            throw new YurticiHatasi(islem.ad() + " canlı ortamda yazma işlemidir; YurticiAyarlari.canliYazmayaIzinVer() "
                    + "olmadan çalıştırılmaz", false, null);
        }
        String govde = govdeYaz(islem, parametreler);
        String cevap = gonder(islem, zarf(islem, govde));
        XmlDugum kok = XmlDugum.coz(cevap);
        XmlDugum body = kok.cocuk("Body").orElseThrow(() -> new YurticiHatasi("SOAP Body yok", false, null));
        XmlDugum ilk = body.cocuklar().isEmpty() ? null : body.cocuklar().get(0);
        if (ilk == null) throw new YurticiHatasi(islem.ad() + ": boş SOAP Body", false, null);
        if (ilk.ad().equals("Fault")) {
            throw new YurticiHatasi(islem.ad() + " SOAP fault: " + ilk.deger("faultstring"), false, null);
        }
        // ilk = <ns1:islemResponse>; içindeki tek öğe asıl sonuç
        return ilk.cocuklar().size() == 1 ? ilk.cocuklar().get(0) : ilk;
    }

    /** Ham SOAP cevabı (ayrıştırmadan) — keşif ve hata ayıklama için. */
    public String hamCagir(YurticiIslem islem, Map<String, ?> parametreler) {
        if (islem.yazmaMi() && ayarlar.canliMi() && !ayarlar.canliYazmaIzni()) {
            throw new YurticiHatasi(islem.ad() + " canlı ortamda yazma işlemidir", false, null);
        }
        return gonder(islem, zarf(islem, govdeYaz(islem, parametreler)));
    }

    String govdeYaz(YurticiIslem islem, Map<String, ?> parametreler) {
        Map<String, Object> tum = new LinkedHashMap<>();
        tum.put(islem.kullaniciAlani(), ayarlar.kullanici());
        tum.put(islem.sifreAlani(), ayarlar.sifre());
        tum.put(islem.dilAlani(), ayarlar.dil());
        if (parametreler != null) tum.putAll(parametreler);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : tum.entrySet()) ogeYaz(sb, e.getKey(), e.getValue());
        return sb.toString();
    }

    private static void ogeYaz(StringBuilder sb, String ad, Object deger) {
        if (deger == null) return;
        if (deger instanceof Collection<?> liste) {
            for (Object o : liste) ogeYaz(sb, ad, o);
            return;
        }
        sb.append('<').append(ad).append('>');
        if (deger instanceof Map<?, ?> ic) {
            for (Map.Entry<?, ?> e : ic.entrySet()) ogeYaz(sb, String.valueOf(e.getKey()), e.getValue());
        } else {
            sb.append(kacis(String.valueOf(deger)));
        }
        sb.append("</").append(ad).append('>');
    }

    private static String zarf(YurticiIslem islem, String govde) {
        // Yurtiçi: işlem öğesi ad alanlı, çocuklar ad alanSIZ (canlıda doğrulandı)
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns=\""
                + islem.servis().adAlani() + "\"><soapenv:Body><ns:" + islem.ad() + ">" + govde + "</ns:" + islem.ad()
                + "></soapenv:Body></soapenv:Envelope>";
    }

    private String gonder(YurticiIslem islem, String zarf) {
        HttpRequest istek = HttpRequest.newBuilder(URI.create(ayarlar.tabanAdres() + islem.servis().yol()))
                .timeout(ayarlar.zamanAsimi())
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(zarf, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> c = http.send(istek, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // SOAP fault 500 ile gelir ama gövdesi anlamlıdır → çağıran çözer
            if (c.statusCode() >= 500 && !c.body().contains("Fault")) {
                throw new YurticiHatasi(islem.ad() + " HTTP " + c.statusCode(), true, null);
            }
            if (c.statusCode() >= 400 && c.statusCode() < 500) {
                throw new YurticiHatasi(islem.ad() + " HTTP " + c.statusCode(), false, null);
            }
            return c.body();
        } catch (HttpTimeoutException e) {
            throw new YurticiHatasi(islem.ad() + " zaman aşımı (" + ayarlar.zamanAsimi().toSeconds() + " sn)", true, e);
        } catch (IOException e) {
            throw new YurticiHatasi(islem.ad() + " bağlantı hatası: " + e.getMessage(), true, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new YurticiHatasi(islem.ad() + " kesildi", true, e);
        }
    }

    static String kacis(String v) {
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
