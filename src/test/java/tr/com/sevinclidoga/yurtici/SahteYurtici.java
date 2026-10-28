package tr.com.sevinclidoga.yurtici;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Testler için sahte Yurtiçi: gelen SOAP isteğinin işlem adına göre {@code src/test/resources/ornek/} altındaki
 * anonim GERÇEK cevabı döndürür. Son isteğin gövdesini saklar (istek kurulumunu doğrulamak için).
 */
final class SahteYurtici implements AutoCloseable {

    private static final Pattern ISLEM = Pattern.compile("<ns:([A-Za-z0-9]+)>");

    private final HttpServer sunucu;
    private final Map<String, String> cevaplar = new ConcurrentHashMap<>();
    private final Map<String, Integer> durumlar = new ConcurrentHashMap<>();
    volatile String sonIstek;
    volatile String sonYol;
    volatile int istekSayisi;

    SahteYurtici() throws IOException {
        sunucu = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        sunucu.createContext("/", ex -> {
            String govde = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            sonIstek = govde;
            sonYol = ex.getRequestURI().getPath();
            istekSayisi++;
            Matcher m = ISLEM.matcher(govde);
            String islem = m.find() ? m.group(1) : "?";
            String cevap = cevaplar.get(islem);
            int kod = durumlar.getOrDefault(islem, 200);
            byte[] b = (cevap != null ? cevap : "sahte: tanımsız işlem " + islem).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/xml; charset=utf-8");
            ex.sendResponseHeaders(cevap != null ? kod : 404, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        sunucu.start();
    }

    /** İşleme örnek dosyasıyla cevap ver. */
    SahteYurtici cevap(String islem, String ornekAdi) {
        cevaplar.put(islem, ornek(ornekAdi));
        return this;
    }

    /** İşleme ham metin ve HTTP koduyla cevap ver. */
    SahteYurtici hamCevap(String islem, int kod, String govde) {
        cevaplar.put(islem, govde);
        durumlar.put(islem, kod);
        return this;
    }

    String adres() {
        return "http://127.0.0.1:" + sunucu.getAddress().getPort() + "/KOPSWebServices/";
    }

    YurticiAyarlari ayarlar() {
        return new YurticiAyarlari(adres(), "test-kullanici", "test-sifre", "TR", null, false);
    }

    static String ornek(String ad) {
        try (InputStream in = SahteYurtici.class.getResourceAsStream("/ornek/" + ad + ".xml")) {
            if (in == null) throw new IllegalArgumentException("örnek yok: " + ad);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Örnekte ilk geçen etiketin sayısal değeri — testler ücret gibi değerleri sabit yazmasın diye. */
    static java.math.BigDecimal ornekSayi(String ornekAdi, String etiket) {
        Matcher m = Pattern.compile("<" + etiket + ">([^<]+)</" + etiket + ">").matcher(ornek(ornekAdi));
        if (!m.find()) throw new IllegalArgumentException(ornekAdi + " içinde " + etiket + " yok");
        return new java.math.BigDecimal(m.group(1).trim());
    }

    @Override
    public void close() {
        sunucu.stop(0);
    }
}
