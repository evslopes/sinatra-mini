package pro.sinatra.mini.test;

import java.util.concurrent.atomic.AtomicLong;

public final class TestIdentifiers {

    private static final AtomicLong SEQ = new AtomicLong(1_000L);

    private TestIdentifiers() {
    }

    public static String nextCnpj() {
        long n = SEQ.incrementAndGet();
        long p1 = (n / 100_000_000L) % 100L;
        long p2 = (n / 100_000L) % 1_000L;
        long p3 = (n / 100L) % 1_000L;
        long p4 = n % 100L;
        return String.format("%02d.%03d.%03d/0001-%02d", p1, p2, p3, p4);
    }

    public static String nextNumeroPedido() {
        return "NP-" + SEQ.incrementAndGet();
    }
}
