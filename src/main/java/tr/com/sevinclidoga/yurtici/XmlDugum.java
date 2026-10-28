package tr.com.sevinclidoga.yurtici;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Yurtiçi cevabının sade ağaç hâli: ad (ad alanı öneki atılmış), metin, çocuklar.
 * Değişmez; DOM'a bağımlılığı dışarı sızdırmaz. Çözümleme XXE'ye kapalıdır (DOCTYPE reddedilir).
 */
public final class XmlDugum {

    private final String ad;
    private final String metin;
    private final List<XmlDugum> cocuklar;

    private XmlDugum(String ad, String metin, List<XmlDugum> cocuklar) {
        this.ad = ad;
        this.metin = metin;
        this.cocuklar = cocuklar;
    }

    /** XML metnini çözer. */
    public static XmlDugum coz(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            f.setNamespaceAware(false);
            var b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return donustur(b.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement());
        } catch (Exception e) {
            throw new YurticiHatasi("Yurtiçi cevabı çözülemedi: " + e.getMessage(), false, e);
        }
    }

    private static XmlDugum donustur(Element e) {
        List<XmlDugum> c = new ArrayList<>();
        StringBuilder m = new StringBuilder();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) c.add(donustur((Element) n));
            else if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) m.append(n.getNodeValue());
        }
        String ad = e.getNodeName();
        int iki = ad.indexOf(':');
        String t = m.toString().trim();
        return new XmlDugum(iki >= 0 ? ad.substring(iki + 1) : ad, c.isEmpty() && !t.isEmpty() ? t : null, List.copyOf(c));
    }

    public String ad() { return ad; }

    /** Yaprak düğümün metni; yoksa null. */
    public String metin() { return metin; }

    public List<XmlDugum> cocuklar() { return cocuklar; }

    /** Doğrudan çocuklardan verilen adlı ilki. */
    public Optional<XmlDugum> cocuk(String ad) {
        return cocuklar.stream().filter(c -> c.ad.equals(ad)).findFirst();
    }

    /** Doğrudan çocuklardan verilen adlı hepsi (Yurtiçi dizileri aynı adla tekrar eder). */
    public List<XmlDugum> hepsi(String ad) {
        return cocuklar.stream().filter(c -> c.ad.equals(ad)).toList();
    }

    /** Ağacın herhangi bir derinliğindeki verilen adlı ilk düğüm (kendisi dahil). */
    public Optional<XmlDugum> bul(String ad) {
        if (this.ad.equals(ad)) return Optional.of(this);
        for (XmlDugum c : cocuklar) {
            Optional<XmlDugum> b = c.bul(ad);
            if (b.isPresent()) return b;
        }
        return Optional.empty();
    }

    /** Doğrudan çocuğun metni; yoksa ya da boşsa null. */
    public String deger(String ad) {
        return cocuk(ad).map(XmlDugum::metin).orElse(null);
    }

    public BigDecimal sayi(String ad) {
        String v = deger(ad);
        if (v == null) return null;
        try { return new BigDecimal(v.replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }

    public Integer tamsayi(String ad) {
        String v = deger(ad);
        if (v == null) return null;
        try { return Integer.valueOf(v); } catch (NumberFormatException e) { return null; }
    }

    /** Hata ayıklama için girintili döküm. */
    public String dok() {
        StringBuilder sb = new StringBuilder();
        dok(sb, 0);
        return sb.toString();
    }

    private void dok(StringBuilder sb, int d) {
        sb.append("  ".repeat(d)).append(ad);
        if (metin != null) sb.append(" = ").append(metin);
        sb.append('\n');
        for (XmlDugum c : cocuklar) c.dok(sb, d + 1);
    }

    @Override
    public String toString() {
        return metin != null ? ad + "=" + metin : ad + cocuklar;
    }
}
