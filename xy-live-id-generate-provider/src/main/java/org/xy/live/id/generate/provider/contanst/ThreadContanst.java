package org.xy.live.id.generate.provider.contanst;

import java.util.concurrent.ThreadLocalRandom;

public interface ThreadContanst {

    String ID_GENERATE_THREAD = "id-generate-thread" + ThreadLocalRandom.current().nextInt(1000);

}
